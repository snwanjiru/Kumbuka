package app.kumbuka.repository

import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.network.DashboardSummaryResponse
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun saveTransaction(transaction: TransactionEntity): Result<Unit>
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    suspend fun deleteTransaction(transaction: TransactionEntity): Result<Unit>
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: Long): TransactionEntity?

    // New: Record payment
    suspend fun recordPayment(transaction: TransactionEntity, amount: Double): Result<Unit>

    // New: Fetch summary from backend
    suspend fun getDashboardSummary(): Result<DashboardSummaryResponse>

    // New: Sync local data with backend
    suspend fun syncWithBackend(): Result<Unit>

    // New: Clear all local transactions (e.g. on logout)
    suspend fun clearLocalData()
}
