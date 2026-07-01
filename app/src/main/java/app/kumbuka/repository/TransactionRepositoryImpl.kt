package app.kumbuka.repository

import app.kumbuka.data.local.dao.TransactionDao
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.network.*
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val api: TransactionApiService
) : TransactionRepository {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun saveTransaction(transaction: TransactionEntity): Result<Unit> {
        // 1. Save locally first and get the generated ID
        val localId = transactionDao.insertTransaction(transaction)

        // 2. Try to save to backend
        return try {
            val response = if (transaction.transactionType == "lent") {
                val remoteId = transaction.remoteId
                if (remoteId != null) {
                    api.updateLoanLent(
                        id = remoteId,
                        request = LoanLentRequest(
                            personName = transaction.name,
                            phoneNumber = transaction.phoneNumber,
                            amountLent = transaction.amount,
                            dateLent = dateFormatter.format(Date(transaction.dateInMillis)),
                            dueDate = transaction.dueDateInMillis?.let { dateFormatter.format(Date(it)) },
                            notes = transaction.notes
                        )
                    )
                } else {
                    api.createLoanLent(
                        LoanLentRequest(
                            personName = transaction.name,
                            phoneNumber = transaction.phoneNumber,
                            amountLent = transaction.amount,
                            dateLent = dateFormatter.format(Date(transaction.dateInMillis)),
                            dueDate = transaction.dueDateInMillis?.let { dateFormatter.format(Date(it)) },
                            notes = transaction.notes
                        )
                    )
                }
            } else {
                val remoteId = transaction.remoteId
                if (remoteId != null) {
                    api.updateLoanBorrowed(
                        id = remoteId,
                        request = LoanBorrowedRequest(
                            personName = transaction.name,
                            phoneNumber = transaction.phoneNumber,
                            amountBorrowed = transaction.amount,
                            dateBorrowed = dateFormatter.format(Date(transaction.dateInMillis)),
                            dueDate = transaction.dueDateInMillis?.let { dateFormatter.format(Date(it)) },
                            notes = transaction.notes
                        )
                    )
                } else {
                    api.createLoanBorrowed(
                        LoanBorrowedRequest(
                            personName = transaction.name,
                            phoneNumber = transaction.phoneNumber,
                            amountBorrowed = transaction.amount,
                            dateBorrowed = dateFormatter.format(Date(transaction.dateInMillis)),
                            dueDate = transaction.dueDateInMillis?.let { dateFormatter.format(Date(it)) },
                            notes = transaction.notes
                        )
                    )
                }
            }

            if (response.isSuccessful) {
                // Update local record with remote fields from backend
                val toUpdate = if (transaction.transactionType == "lent") {
                    val body = response.body() as? LoanLentResponse
                    body?.let {
                        val existingByRemoteId = transactionDao.getTransactionByRemoteId(it.id)
                        if (existingByRemoteId != null && existingByRemoteId.id != localId) {
                            transactionDao.deleteTransactionById(localId)
                        }
                        transaction.copy(
                            id = existingByRemoteId?.id ?: localId,
                            remoteId = it.id,
                            amount = it.amountLent,
                            amountPaid = it.amountPaid,
                            balance = it.balance,
                            status = it.status
                        )
                    }
                } else {
                    val body = response.body() as? LoanBorrowedResponse
                    body?.let {
                        val existingByRemoteId = transactionDao.getTransactionByRemoteId(it.id)
                        if (existingByRemoteId != null && existingByRemoteId.id != localId) {
                            transactionDao.deleteTransactionById(localId)
                        }
                        transaction.copy(
                            id = existingByRemoteId?.id ?: localId,
                            remoteId = it.id,
                            amount = it.amountBorrowed,
                            amountPaid = it.amountPaid,
                            balance = it.balance,
                            status = it.status
                        )
                    }
                }
                
                if (toUpdate != null) {
                    transactionDao.insertTransaction(toUpdate)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            // If network fails, we still have it locally (offline first)
            // But we return failure so the UI can show a warning if needed
            Result.failure(e)
        }
    }

    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity): Result<Unit> {
        transactionDao.deleteTransaction(transaction)
        // If we have a remoteId, delete from backend as well
        transaction.remoteId?.let { remoteId ->
            try {
                if (transaction.transactionType == "lent") {
                    api.deleteLoanLent(remoteId)
                } else {
                    api.deleteLoanBorrowed(remoteId)
                }
            } catch (e: Exception) {
                // Log or handle sync error - possibly mark for deletion later
            }
        }
        return Result.success(Unit)
    }

    override fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByType(type)
    }

    override suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    override suspend fun recordPayment(transaction: TransactionEntity, amount: Double): Result<Unit> {
        val remoteId = transaction.remoteId ?: return Result.failure(Exception("Cannot record payment for unsynced transaction"))
        
        return try {
            val response = if (transaction.transactionType == "lent") {
                api.recordLentPayment(remoteId, PaymentRequest(amount))
            } else {
                api.recordBorrowedPayment(remoteId, PaymentRequest(amount))
            }

            if (response.isSuccessful) {
                val toUpdate = if (transaction.transactionType == "lent") {
                    val body = response.body() as? LoanLentResponse
                    body?.let {
                        transaction.copy(
                            amountPaid = it.amountPaid,
                            balance = it.balance,
                            status = it.status
                        )
                    }
                } else {
                    val body = response.body() as? LoanBorrowedResponse
                    body?.let {
                        transaction.copy(
                            amountPaid = it.amountPaid,
                            balance = it.balance,
                            status = it.status
                        )
                    }
                }
                
                if (toUpdate != null) {
                    transactionDao.insertTransaction(toUpdate)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDashboardSummary(): Result<DashboardSummaryResponse> {
        return try {
            val response = api.getDashboardSummary()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            // If network fails (e.g. offline), calculate from local data for a seamless experience
            val localSummary = calculateLocalSummary()
            Result.success(localSummary)
        }
    }

    private suspend fun calculateLocalSummary(): DashboardSummaryResponse {
        val transactions = transactionDao.getAllTransactionsList()
        val now = System.currentTimeMillis()

        val lent = transactions.filter { it.transactionType == "lent" }
        val borrowed = transactions.filter { it.transactionType == "borrowed" }

        // Calculate Debt Aging for lent transactions
        val agingBuckets = mutableMapOf(
            "1-7 Days" to 0.0,
            "8-30 Days" to 0.0,
            "30+ Days" to 0.0
        )

        lent.filter { it.status != "PAID" && it.dueDateInMillis != null && it.dueDateInMillis < now }.forEach { loan ->
            val diff = now - loan.dueDateInMillis!!
            val days = diff / (1000 * 60 * 60 * 24)
            when {
                days <= 7 -> agingBuckets["1-7 Days"] = agingBuckets["1-7 Days"]!! + loan.balance
                days <= 30 -> agingBuckets["8-30 Days"] = agingBuckets["8-30 Days"]!! + loan.balance
                else -> agingBuckets["30+ Days"] = agingBuckets["30+ Days"]!! + loan.balance
            }
        }

        // Calculate Monthly Trend (Full History, minimum 6 months)
        val trendData = mutableListOf<MonthlyTrend>()
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        
        // Find the earliest transaction date or default to 5 months ago to ensure a graph exists
        val fiveMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -5) }.timeInMillis
        val earliestDate = transactions.minOfOrNull { it.dateInMillis } ?: now
        val effectiveStartDate = Math.min(earliestDate, fiveMonthsAgo)

        val startCal = Calendar.getInstance().apply { 
            timeInMillis = effectiveStartDate 
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val currentCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val loopCal = startCal.clone() as Calendar
        while (loopCal.timeInMillis <= currentCal.timeInMillis) {
            val monthLabel = monthFormat.format(loopCal.time)
            
            val monthStart = loopCal.timeInMillis
            val nextMonthCal = (loopCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val monthEnd = nextMonthCal.timeInMillis

            val mLent = lent.filter { it.dateInMillis in monthStart until monthEnd }.sumOf { it.amount }
            val mBorrowed = borrowed.filter { it.dateInMillis in monthStart until monthEnd }.sumOf { it.amount }
            
            trendData.add(MonthlyTrend(monthLabel, mLent, mBorrowed))
            loopCal.add(Calendar.MONTH, 1)
        }

        return DashboardSummaryResponse(
            totalLent = lent.sumOf { it.amount },
            totalBorrowed = borrowed.sumOf { it.amount },
            amountOwedToMe = lent.sumOf { it.balance },
            amountIOwe = borrowed.sumOf { it.balance },
            activeLoansLent = lent.count { it.status != "PAID" },
            activeLoansBorrowed = borrowed.count { it.status != "PAID" },
            overdueLoans = transactions.count { it.status != "PAID" && it.dueDateInMillis != null && it.dueDateInMillis < now },
            debtAging = agingBuckets,
            monthlyTrend = trendData
        )
    }

    override suspend fun syncWithBackend(): Result<Unit> = supervisorScope {
        try {
            // Run both API calls in parallel to save time, especially on cold starts
            val lentDeferred = async { api.getAllLoansLent() }
            val borrowedDeferred = async { api.getAllLoansBorrowed() }

            val lentResponse = lentDeferred.await()
            val borrowedResponse = borrowedDeferred.await()

            if (lentResponse.isSuccessful && borrowedResponse.isSuccessful) {
                val lentLoans = lentResponse.body() ?: emptyList()
                val borrowedLoans = borrowedResponse.body() ?: emptyList()

                // Convert to entities and save to Room
                val entities = mutableListOf<TransactionEntity>()
                
                lentLoans.forEach { loan ->
                    entities.add(TransactionEntity(
                        remoteId = loan.id,
                        name = loan.personName,
                        phoneNumber = loan.phoneNumber,
                        amount = loan.amountLent,
                        dateInMillis = parseDate(loan.dateLent),
                        dueDateInMillis = loan.dueDate?.let { parseDate(it) },
                        notes = loan.notes ?: "",
                        transactionType = "lent",
                        amountPaid = loan.amountPaid,
                        balance = loan.balance,
                        status = loan.status
                    ))
                }

                borrowedLoans.forEach { loan ->
                    entities.add(TransactionEntity(
                        remoteId = loan.id,
                        name = loan.personName,
                        phoneNumber = loan.phoneNumber,
                        amount = loan.amountBorrowed,
                        dateInMillis = parseDate(loan.dateBorrowed),
                        dueDateInMillis = loan.dueDate?.let { parseDate(it) },
                        notes = loan.notes ?: "",
                        transactionType = "borrowed",
                        amountPaid = loan.amountPaid,
                        balance = loan.balance,
                        status = loan.status
                    ))
                }

                // ── SAFE SYNC ────────────────────────────────────────────────
                // Map the remote records to local entities, preserving local 'id's
                val toSave = entities.map { entity ->
                    val existing = entity.remoteId?.let { transactionDao.getTransactionByRemoteId(it) }
                    if (existing != null) {
                        entity.copy(id = existing.id)
                    } else {
                        entity
                    }
                }
                
                // Bulk insert to trigger only one Flow emission
                transactionDao.insertTransactions(toSave)

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync data from backend"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLocalData() {
        transactionDao.clearAll()
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        
        return try {
            if (dateStr.contains("T")) {
                // Remove trailing 'Z' if present for SimpleDateFormat
                val cleanDate = dateStr.replace("Z", "")
                isoDateFormatter.parse(cleanDate)?.time ?: System.currentTimeMillis()
            } else {
                dateFormatter.parse(dateStr)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseError(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = JSONObject(errorBody)
                json.optString("message", "An error occurred")
            } else {
                "Error ${response.code()}"
            }
        } catch (e: Exception) {
            "An error occurred"
        }
    }
}
