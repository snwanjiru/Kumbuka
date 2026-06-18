package app.kumbuka.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: Long? = null, // ID from the backend
    val name: String,
    val phoneNumber: String,
    val amount: Double,
    val dateInMillis: Long,
    val dueDateInMillis: Long?,
    val notes: String,
    val transactionType: String // "lent" or "borrowed"
)
