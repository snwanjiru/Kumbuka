package app.kumbuka.data.local.dao

import androidx.room.*
import app.kumbuka.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY dateInMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE remoteId = :remoteId")
    suspend fun getTransactionByRemoteId(remoteId: Long): TransactionEntity?

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity): Unit

    @Query("SELECT * FROM transactions WHERE transactionType = :type")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
