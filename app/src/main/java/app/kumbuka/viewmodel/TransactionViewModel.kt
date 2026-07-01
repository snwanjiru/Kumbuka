package app.kumbuka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getTransactionById(id: Long, onResult: (TransactionEntity?) -> Unit) {
        viewModelScope.launch {
            val transaction = repository.getTransactionById(id)
            onResult(transaction)
        }
    }

    fun saveTransaction(
        id: Long = 0,
        name: String,
        phoneNumber: String,
        amount: String,
        dateInMillis: Long,
        dueDateInMillis: Long?,
        notes: String,
        transactionType: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) repository.getTransactionById(id) else null
            
            // Clean amount string: remove commas, currency symbols, and spaces
            val cleanAmount = amount.replace(Regex("[^0-9.]"), "")
            val amountValue = cleanAmount.toDoubleOrNull() ?: 0.0

            val transaction = existing?.copy(
                name = name,
                phoneNumber = phoneNumber,
                amount = amountValue,
                balance = existing.balance, // Keep existing balance
                dateInMillis = dateInMillis,
                dueDateInMillis = dueDateInMillis,
                notes = notes
            ) ?: TransactionEntity(
                id = id,
                name = name,
                phoneNumber = phoneNumber,
                amount = amountValue,
                balance = amountValue, // Set initial balance for new records
                dateInMillis = dateInMillis,
                dueDateInMillis = dueDateInMillis,
                notes = notes,
                transactionType = transactionType.lowercase() // Ensure lowercase for repository logic
            )
            
            // We call saveTransaction which writes to Room FIRST.
            // Then it tries to sync with the backend.
            val result = repository.saveTransaction(transaction)
            
            // If the network is down, the result will be failure, but the record 
            // is still in the local DB. We navigate back so the user sees it.
            if (result.isFailure) {
                // Optional: We could show a "Saved locally (Sync pending)" message here
            }
            onSuccess()
        }
    }

    fun recordPayment(transaction: TransactionEntity, amount: Double, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.recordPayment(transaction, amount)
            onResult(result)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
