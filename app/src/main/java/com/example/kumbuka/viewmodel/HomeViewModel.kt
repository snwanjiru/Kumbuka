package com.example.kumbuka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
//   • Future: Circle data, activity data per tab
//
// Why @HiltViewModel?
// Allows HomeScreen to request the ViewModel via hiltViewModel() parameter.
// This makes the screen testable — in tests, pass a FakeHomeViewModel directly.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    // Future: wire CircleRepository, ActivityRepository here
    // For now, constructor is empty but ready for injection
) : ViewModel() {

    // ── Selected tab state ────────────────────────────────────────────────────
    private val _selectedTab = MutableStateFlow(HomeTab.Dashboard)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    // ── Tab data loading state ────────────────────────────────────────────────
    private val _tabState = MutableStateFlow<TabState>(TabState.Idle)
    val tabState: StateFlow<TabState> = _tabState.asStateFlow()

    // ── Select a tab and load its data ────────────────────────────────────────
    fun selectTab(tab: HomeTab) {
        viewModelScope.launch {
            _selectedTab.value = tab
            _tabState.value = TabState.Loading

            // Future: call circleRepository or activityRepository based on tab
            // For now, immediately succeed to show the tab
            _tabState.value = TabState.Success
        }
    }

    // ── Reset state when screen appears ───────────────────────────────────────
    fun resetState() {
        _selectedTab.value = HomeTab.Dashboard
        _tabState.value = TabState.Idle
    }
}