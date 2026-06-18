package app.kumbuka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.network.DashboardSummaryResponse
import app.kumbuka.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// HomeTab enum — represents the dashboard tabs
// ─────────────────────────────────────────────────────────────────────────────
enum class HomeTab {
    Dashboard,  // Overview (default)
    CashFlow    // Detailed cash flow tracking
}

// ─────────────────────────────────────────────────────────────────────────────
// TabState sealed class — represents the loading state for the current tab
// ─────────────────────────────────────────────────────────────────────────────
sealed class TabState {
    object Idle : TabState()
    object Loading : TabState()
    data class Error(val message: String) : TabState()
    object Success : TabState()
}

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel
//
// Manages:
//   • Selected tab state (which of the 3 tabs is active)
//   • Tab data loading state (idle, loading, error, success)
//   • Transaction data for the Cash Flow tab
//
// Why @HiltViewModel?
// Allows HomeScreen to request the ViewModel via hiltViewModel() parameter.
// This makes the screen testable — in tests, pass a FakeHomeViewModel directly.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // ── Selected tab state ────────────────────────────────────────────────────
    private val _selectedTab = MutableStateFlow(HomeTab.Dashboard)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    // ── Tab data loading state ────────────────────────────────────────────────
    private val _tabState = MutableStateFlow<TabState>(TabState.Idle)
    val tabState: StateFlow<TabState> = _tabState.asStateFlow()

    // ── Transactions stream ──────────────────────────────────────────────────
    val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepository
        .getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ── Dashboard Summary (Reactive) ──────────────────────────────────────────
    val dashboardSummary: StateFlow<DashboardSummaryResponse?> = allTransactions
        .map { transactions ->
            if (transactions.isEmpty()) null
            else {
                val now = System.currentTimeMillis()
                val lent = transactions.filter { it.transactionType == "lent" }
                val borrowed = transactions.filter { it.transactionType == "borrowed" }

                DashboardSummaryResponse(
                    totalLent = lent.sumOf { it.amount },
                    totalBorrowed = borrowed.sumOf { it.amount },
                    amountOwedToMe = lent.sumOf { it.amount },
                    amountIOwe = borrowed.sumOf { it.amount },
                    activeLoansLent = lent.size,
                    activeLoansBorrowed = borrowed.size,
                    overdueLoans = transactions.count { it.dueDateInMillis != null && it.dueDateInMillis < now }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ── Select a tab and load its data ────────────────────────────────────────
    fun selectTab(tab: HomeTab) {
        viewModelScope.launch {
            _selectedTab.value = tab
            loadDataForTab(tab)
        }
    }

    private suspend fun loadDataForTab(tab: HomeTab) {
        _tabState.value = TabState.Loading
        when (tab) {
            HomeTab.Dashboard -> {
                _tabState.value = TabState.Success
            }
            HomeTab.CashFlow -> {
                transactionRepository.syncWithBackend()
                    .onSuccess { _tabState.value = TabState.Success }
                    .onFailure { _tabState.value = TabState.Success }
            }
        }
    }

    // ── Reset state when screen appears ───────────────────────────────────────
    fun resetState(initialTab: HomeTab = HomeTab.Dashboard) {
        _selectedTab.value = initialTab
        viewModelScope.launch {
            loadDataForTab(initialTab)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
