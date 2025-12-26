package com.dp.jumpster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dp.jumpster.data.JumpRepository
import com.dp.jumpster.data.JumpRecord
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// UI State for Trend Screen
sealed class TrendUiState {
    data object Loading : TrendUiState()
    data object Empty : TrendUiState()
    data class Success(
        val barEntries: List<BarEntry>,
        val lineEntries: List<Entry>,
        val xLabels: List<String>
    ) : TrendUiState()
}

class TrendViewModel(private val repository: JumpRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TrendUiState>(TrendUiState.Loading)
    val uiState: StateFlow<TrendUiState> = _uiState.asStateFlow()

    private var currentPeriod = PERIOD_WEEK
    
    // Date Formatters (java.time)
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault())
    private val weekDayFormatter = DateTimeFormatter.ofPattern("E", Locale.CHINA)

    companion object {
        const val PERIOD_WEEK = 0
        const val PERIOD_MONTH = 1
    }

    init {
        loadData(PERIOD_WEEK)
    }

    fun setPeriod(period: Int) {
        if (currentPeriod != period) {
            currentPeriod = period
            loadData(period)
        }
    }

    private fun loadData(period: Int) {
        _uiState.value = TrendUiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            val end = LocalDate.now()
            val start = when (period) {
                PERIOD_WEEK -> end.minusDays(6) // Past 7 days including today
                PERIOD_MONTH -> end.minusDays(29) // Past 30 days
                else -> end.minusDays(6)
            }
            
            val startStr = start.format(isoFormatter)
            val endStr = end.format(isoFormatter)

            val records = repository.getRecordsBetween(startStr, endStr)
            val recordMap = records.associateBy { it.date }

            // Generate full date range list
            val dates = mutableListOf<LocalDate>()
            var curr = start
            while (!curr.isAfter(end)) {
                dates.add(curr)
                curr = curr.plusDays(1)
            }

            if (records.isEmpty()) {
                 // Even if empty, we might want to show empty chart with 0s?
                 // But logic says if no records at all, maybe show empty state.
                 // However, for "trend", showing 0-line is better than "No Data".
                 // Let's stick to original logic: if strictly empty list from DB, maybe show empty?
                 // Original logic: if (records.isEmpty()) showNoData(). 
                 // But wait, getRecordsBetween returns only existing records. 
                 // So if I have 0 records in DB, it returns empty.
                 // Correct handling: check if records list is empty.
                 if (records.isEmpty()) {
                     _uiState.value = TrendUiState.Empty
                     return@launch
                 }
            }

            // Map to Chart Entries
            val barEntries = mutableListOf<BarEntry>()
            val lineEntries = mutableListOf<Entry>()
            val xLabels = mutableListOf<String>()

            dates.forEachIndexed { index, date ->
                val dateStr = date.format(isoFormatter)
                val count = recordMap[dateStr]?.count ?: 0
                
                barEntries.add(BarEntry(index.toFloat(), count.toFloat()))
                lineEntries.add(Entry(index.toFloat(), count.toFloat()))
                
                val label = if (period == PERIOD_WEEK) {
                    date.format(weekDayFormatter)
                } else {
                    date.format(shortDateFormatter)
                }
                xLabels.add(label)
            }

            _uiState.value = TrendUiState.Success(
                barEntries = barEntries,
                lineEntries = lineEntries,
                xLabels = xLabels
            )
        }
    }
}
