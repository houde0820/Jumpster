package com.dp.jumpster.ui

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpRecord
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrendChartActivity : AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var tabLayout: TabLayout
    private lateinit var chartTypeTabs: TabLayout
    private lateinit var noDataText: View

    private var currentPeriod = PERIOD_WEEK // 默认显示周趋势
    private var currentChartType = CHART_TYPE_BAR // 默认显示柱状图

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("E", Locale.CHINA)

    companion object {
        private const val PERIOD_WEEK = 0
        private const val PERIOD_MONTH = 1
        private const val CHART_TYPE_BAR = 0
        private const val CHART_TYPE_LINE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trend_chart)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "跳绳趋势"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lineChart = findViewById(R.id.line_chart)
        barChart = findViewById(R.id.bar_chart)
        tabLayout = findViewById(R.id.tab_layout)
        chartTypeTabs = findViewById(R.id.chart_type_tabs)
        noDataText = findViewById(R.id.text_no_data)

        setupCharts()
        setupTabListeners()
        loadData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupCharts() {
        // 设置折线图
        with(lineChart) {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_text_secondary)
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.textColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_text_secondary)
            axisLeft.textColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_text_secondary)
            axisRight.isEnabled = false
        }

        // 设置柱状图
        with(barChart) {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_text_secondary)
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.textColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_text_secondary)
            axisLeft.textColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_text_secondary)
            axisRight.isEnabled = false
        }
    }

    private fun setupTabListeners() {
        // 周/月切换
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentPeriod = tab.position
                loadData()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 柱状图/折线图切换
        chartTypeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentChartType = tab.position
                updateChartVisibility()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun updateChartVisibility() {
        when (currentChartType) {
            CHART_TYPE_BAR -> {
                barChart.visibility = View.VISIBLE
                lineChart.visibility = View.GONE
            }
            CHART_TYPE_LINE -> {
                barChart.visibility = View.GONE
                lineChart.visibility = View.VISIBLE
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val (startDate, endDate) = getDateRange()
            val db = AppDatabase.getInstance(this@TrendChartActivity)
            val records = db.jumpRecordDao().getRecordsBetween(startDate, endDate)
            
            // 转换为映射，方便查找
            val recordMap = records.associateBy { it.date }
            
            // 生成日期序列
            val dateList = generateDateList(startDate, endDate)
            
            // 准备图表数据
            val entries = dateList.mapIndexed { index, date ->
                val count = recordMap[date]?.count ?: 0
                when (currentChartType) {
                    CHART_TYPE_BAR -> BarEntry(index.toFloat(), count.toFloat())
                    CHART_TYPE_LINE -> Entry(index.toFloat(), count.toFloat())
                    else -> null
                }
            }
            
            // 准备X轴标签
            val xLabels = generateXLabels(dateList)
            
            launch(Dispatchers.Main) {
                if (records.isEmpty()) {
                    showNoData()
                } else {
                    hideNoData()
                    when (currentChartType) {
                        CHART_TYPE_BAR -> updateBarChart(entries.filterIsInstance<BarEntry>(), xLabels)
                        CHART_TYPE_LINE -> updateLineChart(entries.filterIsInstance<Entry>(), xLabels)
                    }
                }
            }
        }
    }

    private fun getDateRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_YEAR, when (currentPeriod) {
            PERIOD_WEEK -> -6  // 过去7天
            PERIOD_MONTH -> -29 // 过去30天
            else -> -6
        })
        
        val startDate = dateFormat.format(calendar.time)
        return Pair(startDate, endDate)
    }

    private fun generateDateList(startDate: String, endDate: String): List<String> {
        val start = dateFormat.parse(startDate) ?: Date()
        val end = dateFormat.parse(endDate) ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = start
        
        val dates = mutableListOf<String>()
        while (!calendar.time.after(end)) {
            dates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    private fun generateXLabels(dateList: List<String>): List<String> {
        return dateList.map { dateStr ->
            val date = dateFormat.parse(dateStr) ?: Date()
            when (currentPeriod) {
                PERIOD_WEEK -> dayOfWeekFormat.format(date)
                PERIOD_MONTH -> shortDateFormat.format(date)
                else -> shortDateFormat.format(date)
            }
        }
    }

    private fun updateBarChart(entries: List<BarEntry>, xLabels: List<String>) {
        val dataSet = BarDataSet(entries, "跳绳次数").apply {
            color = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_primary)
            valueTextColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_on_background)
            valueTextSize = 10f
        }
        
        val data = BarData(dataSet).apply {
            barWidth = 0.7f
        }
        
        barChart.apply {
            this.data = data
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            xAxis.labelCount = xLabels.size
            animateY(500)
            invalidate()
        }
    }

    private fun updateLineChart(entries: List<Entry>, xLabels: List<String>) {
        val dataSet = LineDataSet(entries, getString(R.string.label_jump_count)).apply {
            color = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_primary)
            valueTextColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_on_background)
            valueTextSize = 10f
            lineWidth = 2f
            setCircleColor(ContextCompat.getColor(this@TrendChartActivity, R.color.sport_primary))
            circleRadius = 4f
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.LINEAR
        }
        
        val data = LineData(dataSet)
        
        lineChart.apply {
            this.data = data
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            xAxis.labelCount = xLabels.size
            animateY(500)
            invalidate()
        }
    }

    private fun showNoData() {
        noDataText.visibility = View.VISIBLE
        barChart.visibility = View.GONE
        lineChart.visibility = View.GONE
    }

    private fun hideNoData() {
        noDataText.visibility = View.GONE
        updateChartVisibility()
    }
}
