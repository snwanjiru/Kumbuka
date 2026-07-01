package app.kumbuka.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.kumbuka.data.local.dao.TransactionDao
import app.kumbuka.data.local.entity.TransactionEntity

@Database(entities = [TransactionEntity::class], version = 3, exportSchema = false)
abstract class KumbukaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
