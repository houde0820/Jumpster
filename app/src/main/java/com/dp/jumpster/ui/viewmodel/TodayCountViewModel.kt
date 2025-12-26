package com.dp.jumpster.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dp.jumpster.data.JumpEntry
import com.dp.jumpster.data.JumpRecord
import com.dp.jumpster.data.JumpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodayCountViewModel(private val repository: JumpRepository) : ViewModel() {

    private val _todayCount = MutableLiveData<Int>()
    val todayCount: LiveData<Int> = _todayCount

    private val _entries = MutableLiveData<List<JumpEntry>>()
    val entries: LiveData<List<JumpEntry>> = _entries

    // Event used to highlight a specific item (e.g. newly added)
    private val _highlightEntryId = MutableLiveData<Long?>()
    val highlightEntryId: LiveData<Long?> = _highlightEntryId

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage
    
    // 庆祝动画事件
    private val _celebrateEvent = MutableLiveData<Pair<Int, Int>>() // prev, current
    val celebrateEvent: LiveData<Pair<Int, Int>> = _celebrateEvent
    
    // 文本缩放动画事件
    private val _animateCountEvent = MutableLiveData<Boolean>()
    val animateCountEvent: LiveData<Boolean> = _animateCountEvent

    private var loadedDate: String = ""

    fun loadData() {
        val todayStr = LocalDate.now().toString()
        loadedDate = todayStr
        refreshData(todayStr)
    }
    
    private fun refreshData(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = repository.getReviewByDate(date)
            val count = record?.count ?: 0
            
            val entryList = repository.getRecentEntries(date, 10)
            
            launch(Dispatchers.Main) {
                _todayCount.value = count
                _entries.value = entryList
            }
        }
    }

    fun addJump(inputVal: Int, inputStartTime: Long, onDateChanged: () -> Unit) {
        val todayStr = LocalDate.now().toString()
        if (loadedDate != todayStr) {
            onDateChanged()
            loadData()
            return
        }

        val currentCount = _todayCount.value ?: 0
        val newCount = currentCount + inputVal
        
        saveTodayCountAndEntry(
            prev = currentCount,
            type = "add",
            inputVal = inputVal,
            finalCount = newCount,
            startTime = inputStartTime
        )
    }

    fun coverJump(inputVal: Int, inputStartTime: Long, onDateChanged: () -> Unit) {
        val todayStr = LocalDate.now().toString()
        if (loadedDate != todayStr) {
            onDateChanged()
            loadData()
            return
        }

        val currentCount = _todayCount.value ?: 0
        saveTodayCountAndEntry(
            prev = currentCount,
            type = "cover",
            inputVal = inputVal,
            finalCount = inputVal,
            startTime = inputStartTime
        )
    }

    private fun saveTodayCountAndEntry(prev: Int, type: String, inputVal: Int, finalCount: Int, startTime: Long) {
        // Optimistic update
        _todayCount.value = finalCount
        
        val endTime = System.currentTimeMillis()
        val todayStr = loadedDate

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRecord(JumpRecord(todayStr, finalCount))
            val newId = repository.insertEntry(
                JumpEntry(
                    id = 0,
                    date = todayStr,
                    type = type,
                    value = inputVal,
                    totalAfter = finalCount,
                    timestamp = endTime,
                    startTime = startTime,
                    endTime = endTime
                )
            )
            
            // Re-fetch to ensure consistency
            val entryList = repository.getRecentEntries(todayStr, 10)
            
            launch(Dispatchers.Main) {
                _entries.value = entryList
                _highlightEntryId.value = newId
                _animateCountEvent.value = true
                _celebrateEvent.value = prev to finalCount
            }
        }
    }

    fun undoLatest() {
        val todayStr = LocalDate.now().toString()
        viewModelScope.launch(Dispatchers.IO) {
            val latest = repository.getLatestByDate(todayStr) ?: run {
                launch(Dispatchers.Main) {
                    _toastMessage.value = "没有可撤销的记录"
                }
                return@launch
            }

            val entries = repository.getEntriesByDate(todayStr)
            val prevTotal = if (entries.size > 1) {
                entries[1].totalAfter // Second latest
            } else {
                0
            }

            repository.insertRecord(JumpRecord(todayStr, prevTotal))
            repository.deleteEntryById(latest.id)
            
            val updatedList = repository.getRecentEntries(todayStr, 10)
            
            launch(Dispatchers.Main) {
                _todayCount.value = prevTotal
                _entries.value = updatedList
                _highlightEntryId.value = null
                _toastMessage.value = "撤销成功"
            }
        }
    }
    
    fun clearHighlight() {
        _highlightEntryId.value = null
    }

    fun resetToast() {
         _toastMessage.value = ""
    }
}
