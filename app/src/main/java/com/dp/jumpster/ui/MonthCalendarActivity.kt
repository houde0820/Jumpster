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
        supportActionBar?.title = "月度统计"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        calendarView = findViewById(R.id.calendarView)
        selectedInfoText = findViewById(R.id.text_selected_info)
        monthTitleText = findViewById(R.id.text_month_title)
        monthSumText = findViewById(R.id.text_month_sum)
        weekSumText = findViewById(R.id.text_week_sum)
        weekHeaderLayout = findViewById(R.id.week_header_layout)

        val currentMonth = YearMonth.now()
        currentMonthStr = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        monthTitleText.text = "$currentMonthStr 跳绳统计"

        setupWeekHeader()

        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val firstDayOfWeek = daysOfWeek().first()
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        calendarView.monthScrollListener = { month: CalendarMonth ->
            currentMonthStr = month.yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            monthTitleText.text = "$currentMonthStr 跳绳统计"
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
        val labels = listOf("一", "二", "三", "四", "五", "六", "日")
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
                monthSumText.text = "本月合计：$currentMonthSum"
                updateWeekSum(LocalDate.now())
            }
        }
    }

    private fun updateWeekSum(date: LocalDate) {
        val startOfWeek = date.with(DayOfWeek.MONDAY)
        val endOfWeek = date.with(DayOfWeek.SUNDAY)
        var sum = 0
        var d = startOfWeek
        while (!d.isAfter(endOfWeek)) {
            sum += dayCountMap[d] ?: 0
            d = d.plusDays(1)
        }
        weekSumText.text = "本周合计：$sum"
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
            Toast.makeText(this, "本月还没有记录哦", Toast.LENGTH_SHORT).show()
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
            // 无权限时忽略，仅保留触觉反馈
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
                selectedInfoText.text = "$date：累计 $c"
                updateWeekSum(date)
                val i = Intent(this@MonthCalendarActivity, DayEntriesActivity::class.java)
                i.putExtra("date", date.format(DateTimeFormatter.ISO_DATE))
                startActivity(i)
            }
        }
    }
}
