package app.kumbuka.repository

import app.kumbuka.data.local.dao.TransactionDao
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.network.*
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

            if (response.isSuccessful) {
                // Update local record with remoteId from backend
                val remoteId = if (transaction.transactionType == "lent") {
                    (response.body() as? LoanLentResponse)?.id
                } else {
                    (response.body() as? LoanBorrowedResponse)?.id
                }
                
                if (remoteId != null) {
                    // Update the remote record by checking if it already exists in the DAO
                    val existing = transactionDao.getTransactionByRemoteId(remoteId)
                    val toUpdate = transaction.copy(id = existing?.id ?: localId, remoteId = remoteId)
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

        return DashboardSummaryResponse(
            totalLent = lent.sumOf { it.amount },
            totalBorrowed = borrowed.sumOf { it.amount },
            amountOwedToMe = lent.sumOf { it.amount }, // Simplified: assuming full amount is owed if locally stored
            amountIOwe = borrowed.sumOf { it.amount },
            activeLoansLent = lent.size,
            activeLoansBorrowed = borrowed.size,
            overdueLoans = transactions.count { it.dueDateInMillis != null && it.dueDateInMillis < now }
        )
    }

    override suspend fun syncWithBackend(): Result<Unit> {
        return try {
            val lentResponse = api.getAllLoansLent()
            val borrowedResponse = api.getAllLoansBorrowed()

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
                        transactionType = "lent"
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
                        transactionType = "borrowed"
                    ))
                }

                // ── SAFE SYNC ────────────────────────────────────────────────
                // We check if a record with the same remoteId already exists.
                // If it does, we update it (preserving the local primary key 'id').
                // If not, we insert it as a new record.
                entities.forEach { entity ->
                    val existing = entity.remoteId?.let { transactionDao.getTransactionByRemoteId(it) }
                    if (existing != null) {
                        transactionDao.insertTransaction(entity.copy(id = existing.id))
                    } else {
                        transactionDao.insertTransaction(entity)
                    }
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync data from backend"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
