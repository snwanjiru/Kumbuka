package app.kumbuka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kumbuka.data.TokenManager
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.network.DashboardSummaryResponse
import app.kumbuka.network.MonthlyTrend
import app.kumbuka.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
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

data class ContactSummary(
    val name: String,
    val totalLent: Double,
    val totalBorrowed: Double,
    val isOverdue: Boolean
)

data class CashFlowGroup(
    val key: String,
    val items: List<TransactionEntity>,
    val type: String,
    val representative: TransactionEntity,
    val totalAmount: Double,
    val totalBalance: Double,
    val latestDate: Long
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // ── User Identity stream ──────────────────────────────────────────────────
    // Combines Name and Email to provide the best possible display name
    val userName: StateFlow<String> = combine(
        tokenManager.userNameFlow,
        tokenManager.userEmailFlow
    ) { name, email ->
        name ?: email ?: "Kumbuka User"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Loading..."
    )

    // ── Selected tab state ────────────────────────────────────────────────────
    private val _selectedTab = MutableStateFlow(HomeTab.Dashboard)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    // ── Tab data loading state ────────────────────────────────────────────────
    private val _tabState = MutableStateFlow<TabState>(TabState.Idle)
    val tabState: StateFlow<TabState> = _tabState.asStateFlow()

    // ── Cash Flow UI State ───────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilters = MutableStateFlow(setOf<String>())
    val selectedFilters: StateFlow<Set<String>> = _selectedFilters.asStateFlow()

    private val _selectedDueOption = MutableStateFlow<String?>(null)
    val selectedDueOption: StateFlow<String?> = _selectedDueOption.asStateFlow()

    // ── Transactions stream ──────────────────────────────────────────────────
    val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepository
        .getAllTransactions()
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ── Filtered & Grouped Transactions for Cash Flow ─────────────────────────
    val cashFlowGroups: StateFlow<List<CashFlowGroup>> = combine(
        allTransactions,
        _searchQuery,
        _selectedFilters,
        _selectedDueOption
    ) { transactions, query, filters, dueOption ->
        val currentTime = System.currentTimeMillis()
        
        // 1. Filter
        val filtered = transactions.filter { t ->
            val matchesSearch = if (query.isBlank()) true 
                               else t.name.contains(query, ignoreCase = true) || 
                                    t.phoneNumber.contains(query)
            
            val matchesType = if (filters.isEmpty()) true
                             else {
                                 var matches = false
                                 if ("Lent" in filters && t.transactionType == "lent") matches = true
                                 if ("Borrowed" in filters && t.transactionType == "borrowed") matches = true
                                 
                                 if ("Due" in filters && t.dueDateInMillis != null) {
                                     if (dueOption == null) {
                                         matches = true
                                     } else {
                                         val days = when (dueOption) {
                                             "1 Day" -> 1
                                             "2 Days" -> 2
                                             "3 Days" -> 3
                                             "4 Days" -> 4
                                             "5 Days" -> 5
                                             "6 Days" -> 6
                                             "1 Week" -> 7
                                             "2 Weeks" -> 14
                                             else -> 0
                                         }
                                         val limit = currentTime + (days.toLong() * 24 * 60 * 60 * 1000)
                                         if (t.dueDateInMillis >= currentTime && t.dueDateInMillis <= limit) {
                                             matches = true
                                         }
                                     }
                                 }
                                 
                                 if ("Overdue" in filters && t.dueDateInMillis != null && t.dueDateInMillis < currentTime) matches = true
                                 matches
                             }
            matchesSearch && matchesType
        }

        // 2. Group by Person
        val personGroups = filtered.groupBy {
            val normalizedPhone = it.phoneNumber.filter { char -> char.isDigit() }.takeLast(9)
            "${it.name.lowercase()}|$normalizedPhone"
        }
        
        // 3. Sort People by latest transaction
        val sortedPeople = personGroups.entries.sortedByDescending { it.value.maxOf { t -> t.dateInMillis } }
        
        // 4. Flatten into CashFlowGroups
        val resultGroups = mutableListOf<CashFlowGroup>()
        sortedPeople.forEach { entry ->
            val personKey = entry.key
            val personTransactions = entry.value
            val typeGroups = personTransactions.groupBy { it.transactionType }
            
            val typesInOrder = typeGroups.keys.sortedBy { if (it == "lent") 0 else 1 }
            
            typesInOrder.forEach { type ->
                val itemsOfType = typeGroups[type]!!
                val groupKey = "$personKey|$type"
                val representative = itemsOfType.first()
                val totalAmount = itemsOfType.sumOf { it.amount }
                val totalBalance = itemsOfType.sumOf { it.balance }
                val latestDate = itemsOfType.maxOf { it.dateInMillis }
                
                resultGroups.add(CashFlowGroup(
                    key = groupKey,
                    items = itemsOfType,
                    type = type,
                    representative = representative,
                    totalAmount = totalAmount,
                    totalBalance = totalBalance,
                    latestDate = latestDate
                ))
            }
        }
        resultGroups
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ── Contact Summaries for Dashboard ───────────────────────────────────────
    val contactSummaries: StateFlow<List<ContactSummary>> = allTransactions
        .map { transactions ->
            val now = System.currentTimeMillis()
            transactions.groupBy {
                val normalizedPhone = it.phoneNumber.filter { char -> char.isDigit() }.takeLast(9)
                "${it.name.lowercase()}|$normalizedPhone"
            }.map { (key, personTransactions) ->
                val name = personTransactions.first().name
                val totalBalanceLent = personTransactions.filter { it.transactionType == "lent" }.sumOf { it.balance }
                val totalBalanceBorrowed = personTransactions.filter { it.transactionType == "borrowed" }.sumOf { it.balance }
                val overdue = personTransactions.any { 
                    it.status != "PAID" && it.dueDateInMillis != null && it.dueDateInMillis < now 
                }
                ContactSummary(name, totalBalanceLent, totalBalanceBorrowed, overdue)
            }.sortedByDescending { it.totalLent + it.totalBorrowed }
        }
        .flowOn(Dispatchers.Default)
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

                // Calculate Debt Aging
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
                
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                val nowTime = calendar.timeInMillis
                
                // Earliest transaction or 5 months ago
                val earliestTransactionDate = transactions.minOfOrNull { it.dateInMillis } ?: nowTime
                calendar.timeInMillis = nowTime
                calendar.add(Calendar.MONTH, -5)
                val fiveMonthsAgo = calendar.timeInMillis
                
                val startTime = Math.min(earliestTransactionDate, fiveMonthsAgo)
                
                val loopCal = Calendar.getInstance().apply {
                    timeInMillis = startTime
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                val currentCal = Calendar.getInstance().apply {
                    timeInMillis = nowTime
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                
                while (loopCal.timeInMillis <= currentCal.timeInMillis) {
                    val monthLabel = monthFormat.format(loopCal.time)
                    
                    val monthStart = loopCal.timeInMillis
                    
                    val nextMonthCal = (loopCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                    }
                    val monthEnd = nextMonthCal.timeInMillis

                    val mLent = lent.filter { it.dateInMillis in monthStart until monthEnd }.sumOf { it.amount }
                    val mBorrowed = borrowed.filter { it.dateInMillis in monthStart until monthEnd }.sumOf { it.amount }
                    
                    trendData.add(MonthlyTrend(monthLabel, mLent, mBorrowed))

                    loopCal.add(Calendar.MONTH, 1)
                }

                DashboardSummaryResponse(
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
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun recordPayment(transaction: TransactionEntity, amount: Double, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = transactionRepository.recordPayment(transaction, amount)
            onResult(result)
        }
    }

    // ── Select a tab and load its data ────────────────────────────────────────
    fun selectTab(tab: HomeTab) {
        viewModelScope.launch {
            _selectedTab.value = tab
            loadDataForTab(tab)
        }
    }

    private suspend fun loadDataForTab(tab: HomeTab) {
        // Only show Loading state if we don't have any transactions yet.
        // This prevents the "glitchy" UI repositioning when swiping tabs
        // because the UI won't flash a spinner and rebuild the list from scratch.
        if (allTransactions.value.isEmpty()) {
            _tabState.value = TabState.Loading
        }

        // Always try to sync with backend regardless of tab to ensure data is fresh.
        // We set Success immediately after sync finishes.
        transactionRepository.syncWithBackend()
            .onSuccess { _tabState.value = TabState.Success }
            .onFailure { _tabState.value = TabState.Success }
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

    // ── UI Control Actions ────────────────────────────────────────────────────
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilter(criteria: String) {
        val current = _selectedFilters.value
        _selectedFilters.value = if (criteria in current) {
            current - criteria
        } else {
            // Mutual exclusivity logic
            var next = current + criteria
            if (criteria == "Lent") next = next - "Borrowed"
            if (criteria == "Borrowed") next = next - "Lent"
            if (criteria == "Due") next = next - "Overdue"
            if (criteria == "Overdue") next = next - "Due"
            next
        }
    }

    fun updateDueOption(option: String?) {
        _selectedDueOption.value = option
    }

    fun clearFilters() {
        _selectedFilters.value = emptySet()
        _selectedDueOption.value = null
    }
}
