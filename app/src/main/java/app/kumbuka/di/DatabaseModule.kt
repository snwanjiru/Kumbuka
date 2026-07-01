package app.kumbuka.di

import android.content.Context
import androidx.room.Room
import app.kumbuka.data.local.KumbukaDatabase
import app.kumbuka.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KumbukaDatabase {
        return Room.databaseBuilder(
            context,
            KumbukaDatabase::class.java,
            "kumbuka_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(database: KumbukaDatabase): TransactionDao {
        return database.transactionDao()
    }
}
