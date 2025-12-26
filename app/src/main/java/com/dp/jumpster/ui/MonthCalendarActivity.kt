package com.dp.jumpster.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpRecord
import com.dp.jumpster.data.JumpRepository
import com.dp.jumpster.ui.viewmodel.MonthUiState
import com.dp.jumpster.ui.viewmodel.MonthViewModel
import com.dp.jumpster.ui.viewmodel.ViewModelFactory
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MonthCalendarActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var totalCountText: TextView
    private lateinit var weekCountText: TextView // Corresponds to text_week_sum
    private lateinit var weekHeaderLayout: LinearLayout

    private lateinit var viewModel: MonthViewModel
    
    // Cache for binding
    private var recordsMap: Map<LocalDate, JumpRecord> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_month_calendar)

        val db = AppDatabase.getInstance(this)
        val repository = JumpRepository(db.jumpRecordDao(), db.jumpEntryDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MonthViewModel::class.java]

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "月度记录"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        calendarView = findViewById(R.id.calendarView)
        totalCountText = findViewById(R.id.text_month_sum) // Fixed ID
        weekCountText = findViewById(R.id.text_week_sum) // Fixed ID
        weekHeaderLayout = findViewById(R.id.week_header_layout)
        
        setupWeekHeader()
        setupCalendar()
        setupObservers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupWeekHeader() {
        val daysOfWeek = daysOfWeek()
        daysOfWeek.forEach { dayOfWeek ->
            val textView = TextView(this)
            textView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            textView.text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.textSize = 14f
            textView.setTextColor(getColor(R.color.sport_text_secondary))
            weekHeaderLayout.addView(textView)
        }
    }

    private fun setupCalendar() {
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.bind(data, recordsMap[data.date])
            }
        }

        calendarView.monthScrollListener = { month ->
             supportActionBar?.title = "${month.yearMonth.year}年${month.yearMonth.monthValue}月"
             viewModel.onMonthChanged(month.yearMonth)
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val daysOfWeek = daysOfWeek()
        
        calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        calendarView.scrollToMonth(currentMonth)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MonthUiState.Loading -> {
                            // Valid to show loading
                        }
                        is MonthUiState.Success -> {
                            recordsMap = state.records
                            calendarView.notifyCalendarChanged()
                            updateStats(state)
                        }
                    }
                }
            }
        }
    }

    private fun updateStats(state: MonthUiState.Success) {
        totalCountText.text = "本月总计: ${state.totalCount}"
        weekCountText.text = "日均: ${state.avgCount}"
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.text_day_number)
        val countText: TextView = view.findViewById(R.id.text_day_count)
        lateinit var day: CalendarDay

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    val intent = Intent(this@MonthCalendarActivity, DayEntriesActivity::class.java)
                    intent.putExtra("date", day.date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    startActivity(intent)
                }
            }
        }

        fun bind(day: CalendarDay, record: JumpRecord?) {
            this.day = day
            textView.text = day.date.dayOfMonth.toString()
            
            if (day.position == DayPosition.MonthDate) {
                textView.setTextColor(getColor(R.color.sport_on_background))
                if (record != null && record.count > 0) {
                    countText.text = record.count.toString()
                    countText.visibility = View.VISIBLE
                    // Optional: highlight background or text
                } else {
                    countText.visibility = View.GONE
                }
            } else {
                textView.setTextColor(Color.GRAY)
                countText.visibility = View.GONE
            }
        }
    }
}
