package com.dp.jumpster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dp.jumpster.data.JumpRepository

class ViewModelFactory(private val repository: JumpRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodayCountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodayCountViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TrendViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrendViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(MonthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonthViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(DayEntriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DayEntriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
