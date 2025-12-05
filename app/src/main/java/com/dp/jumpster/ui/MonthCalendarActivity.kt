package com.dp.jumpster.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.util.ShareCardGenerator
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MonthCalendarActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var selectedInfoText: TextView
    private lateinit var monthTitleText: TextView
    private lateinit var monthSumText: TextView
    private lateinit var weekSumText: TextView
    private lateinit var weekHeaderLayout: LinearLayout
    private val dayCountMap = HashMap<LocalDate, Int>()
    private var currentMonthSum = 0
    private var currentMonthStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_month_calendar)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_monthly_stats)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        calendarView = findViewById(R.id.calendarView)
        selectedInfoText = findViewById(R.id.text_selected_info)
        monthTitleText = findViewById(R.id.text_month_title)
        monthSumText = findViewById(R.id.text_month_sum)
        weekSumText = findViewById(R.id.text_week_sum)
        weekHeaderLayout = findViewById(R.id.week_header_layout)

        val currentMonth = YearMonth.now()
        currentMonthStr = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        monthTitleText.text = getString(R.string.fmt_month_title, currentMonthStr)

        setupWeekHeader()

        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val firstDayOfWeek = daysOfWeek().first()
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        calendarView.monthScrollListener = { month: CalendarMonth ->
            currentMonthStr = month.yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            monthTitleText.text = getString(R.string.fmt_month_title, currentMonthStr)
            loadMonthData(month.yearMonth)
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.bind(data)
            }
        }

        loadMonthData(currentMonth)
    }

    private fun setupWeekHeader() {
        weekHeaderLayout.removeAllViews()
        val labels = listOf(
            getString(R.string.week_day_mon),
            getString(R.string.week_day_tue),
            getString(R.string.week_day_wed),
            getString(R.string.week_day_thu),
            getString(R.string.week_day_fri),
            getString(R.string.week_day_sat),
            getString(R.string.week_day_sun)
        )
        val itemWeight = 1f
        labels.forEach { label ->
            val tv = TextView(this)
            tv.text = label
            tv.gravity = Gravity.CENTER
            tv.textSize = 12f
            tv.setTextColor(0xFF666666.toInt())
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, itemWeight)
            weekHeaderLayout.addView(tv, lp)
        }
    }

    private fun loadMonthData(yearMonth: YearMonth) {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val startStr = startDate.format(DateTimeFormatter.ISO_DATE)
        val endStr = endDate.format(DateTimeFormatter.ISO_DATE)

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@MonthCalendarActivity).jumpRecordDao()
            val records = dao.getRecordsBetween(startStr, endStr)
            dayCountMap.clear()
            records.forEach { r ->
                val local = LocalDate.parse(r.date)
                dayCountMap[local] = r.count
            }
            currentMonthSum = dayCountMap.filterKeys { it.year == yearMonth.year && it.month == yearMonth.month }
                .values.sum()
            launch(Dispatchers.Main) {
                calendarView.notifyCalendarChanged()
                updateStats(yearMonth)
            }
        }
    }

    private fun updateStats(month: YearMonth) {
        val monthStr = month.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
        monthTitleText.text = getString(R.string.fmt_month_title, monthStr)

        lifecycleScope.launch(Dispatchers.IO) {
            val monthTotal = currentMonthSum // Use the calculated sum

            // Calculate week total
            val today = LocalDate.now()
            // Assume Monday is start of week
            val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val endOfWeek = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))

            var weekTotal = 0
            dayCountMap.forEach { (date, count) ->
                if (!date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)) {
                    weekTotal += count
                }
            }

            launch(Dispatchers.Main) {
                monthSumText.text = getString(R.string.fmt_month_total, monthTotal)
                weekSumText.text = getString(R.string.fmt_week_total, weekTotal)

                if (dayCountMap.isEmpty()) {
                    Toast.makeText(this@MonthCalendarActivity, getString(R.string.msg_no_month_records), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateWeekSum(date: LocalDate) {
        // Helper to update week sum when a day is selected
        lifecycleScope.launch(Dispatchers.IO) {
             val startOfWeek = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
             val endOfWeek = date.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
             
             var weekTotal = 0
             // We need to query DB or use dayCountMap if it covers the range. 
             // dayCountMap only has current month. Safer to query DB for accurate week sum across months.
             val dao = AppDatabase.getInstance(this@MonthCalendarActivity).jumpRecordDao()
             // Querying a bit inefficiently here but safe
             val records = dao.getRecordsBetween(startOfWeek.format(DateTimeFormatter.ISO_DATE), endOfWeek.format(DateTimeFormatter.ISO_DATE))
             weekTotal = records.sumOf { it.count }
             
             launch(Dispatchers.Main) {
                 weekSumText.text = getString(R.string.fmt_week_total, weekTotal)
             }
        }
    }

    private fun showDayDetails(date: LocalDate) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        lifecycleScope.launch(Dispatchers.IO) {
            val record = AppDatabase.getInstance(this@MonthCalendarActivity).jumpRecordDao().getRecordByDate(dateStr)
            val count = record?.count ?: 0

            launch(Dispatchers.Main) {
                selectedInfoText.text = getString(R.string.fmt_selected_day_info, dateStr, count)

                // Click to jump to details
                if (count > 0) {
                    val intent = Intent(this@MonthCalendarActivity, DayEntriesActivity::class.java)
                    intent.putExtra("date", dateStr)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_month, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_month -> {
                shareMonth()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun shareMonth() {
        if (currentMonthSum > 0) {
            ShareCardGenerator(this).shareMonth(currentMonthSum, currentMonthStr)
        } else {
            Toast.makeText(this, getString(R.string.msg_no_month_records), Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrateClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (_: SecurityException) {
            // Ignore
        }
    }

    private inner class DayViewContainer(view: View) : ViewContainer(view) {
        private val dayText: TextView = view.findViewById(R.id.text_day_number)
        private val countText: TextView = view.findViewById(R.id.text_day_count)
        private var boundDay: CalendarDay? = null
        fun bind(day: CalendarDay) {
            boundDay = day
            val date = day.date
            dayText.text = date.dayOfMonth.toString()
            val count = dayCountMap[date]
            countText.isVisible = count != null && count > 0
            countText.text = count?.toString() ?: ""

            val isCurrentMonthDate = day.position == DayPosition.MonthDate
            val baseColor = if (isCurrentMonthDate) 0xFF111111.toInt() else 0xFFBBBBBB.toInt()
            dayText.setTextColor(baseColor)
            val isToday = date == LocalDate.now()
            dayText.setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)

            val primary = ContextCompat.getColor(this@MonthCalendarActivity, R.color.sport_primary)
            val bgColor = when {
                (count ?: 0) <= 0 -> 0x00000000
                count!! <= 200 -> ColorUtils.setAlphaComponent(primary, (0.12f * 255).toInt())
                count <= 800 -> ColorUtils.setAlphaComponent(primary, (0.25f * 255).toInt())
                else -> ColorUtils.setAlphaComponent(primary, (0.38f * 255).toInt())
            }
            view.setBackgroundColor(bgColor)

            view.setOnClickListener {
                vibrateClick(it)
                val c = dayCountMap[date] ?: 0
                selectedInfoText.text = getString(R.string.fmt_selected_day_info, date.format(DateTimeFormatter.ISO_DATE), c)
                updateWeekSum(date)
                val i = Intent(this@MonthCalendarActivity, DayEntriesActivity::class.java)
                i.putExtra("date", date.format(DateTimeFormatter.ISO_DATE))
                startActivity(i)
            }
        }
    }
}
