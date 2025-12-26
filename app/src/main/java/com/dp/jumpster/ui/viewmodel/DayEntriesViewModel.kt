package com.dp.jumpster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dp.jumpster.data.JumpEntry
import com.dp.jumpster.data.JumpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DayEntriesUiState {
    data object Loading : DayEntriesUiState()
    data class Success(val entries: List<JumpEntry>) : DayEntriesUiState()
}

class DayEntriesViewModel(private val repository: JumpRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DayEntriesUiState>(DayEntriesUiState.Loading)
    val uiState: StateFlow<DayEntriesUiState> = _uiState.asStateFlow()

    fun loadData(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = repository.getEntriesByDate(date)
            // Sort logic is handled in UI normally, but can be here. 
            // Original code sorted by timestamp for chart.
            // Let's pass raw entries or sorted?
            // Repository returns List<JumpEntry>.
            _uiState.value = DayEntriesUiState.Success(entries)
        }
    }
}
