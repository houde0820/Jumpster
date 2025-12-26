package com.dp.jumpster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dp.jumpster.data.JumpRepository
import com.dp.jumpster.data.JumpRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

sealed class MonthUiState {
    data object Loading : MonthUiState()
    data class Success(
        val currentMonth: YearMonth,
        val records: Map<LocalDate, JumpRecord>,
        val totalCount: Int,
        val totalDays: Int,
        val avgCount: Int
    ) : MonthUiState()
}

class MonthViewModel(private val repository: JumpRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MonthUiState>(MonthUiState.Loading)
    val uiState: StateFlow<MonthUiState> = _uiState.asStateFlow()

    private var currentMonth: YearMonth = YearMonth.now()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadData(currentMonth)
    }

    fun onMonthChanged(month: YearMonth) {
        currentMonth = month
        loadData(month)
    }

    private fun loadData(month: YearMonth) {
        viewModelScope.launch(Dispatchers.IO) {
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            
            val startStr = startDate.format(dateFormatter)
            val endStr = endDate.format(dateFormatter)

            val records = repository.getRecordsBetween(startStr, endStr)
            
            // Map String date to LocalDate
            val recordMap = records.associateBy { 
                LocalDate.parse(it.date, dateFormatter) 
            }
            
            val totalCount = records.sumOf { it.count }
            val totalDays = records.size // Days with records
            val avgCount = if (totalDays > 0) totalCount / totalDays else 0

            _uiState.value = MonthUiState.Success(
                currentMonth = month,
                records = recordMap,
                totalCount = totalCount,
                totalDays = totalDays,
                avgCount = avgCount
            )
        }
    }
}
