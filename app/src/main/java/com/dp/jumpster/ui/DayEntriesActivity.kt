package com.dp.jumpster.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DayEntriesActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var titleText: TextView
    private lateinit var rv: RecyclerView
    private lateinit var lineChart: LineChart
    private val adapter = TodayEntryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_entries)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        titleText = findViewById(R.id.text_day_title)
        rv = findViewById(R.id.rv_entries)
        lineChart = findViewById(R.id.line_chart)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        (rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        val date = intent.getStringExtra("date") ?: ""
        titleText.text = "$date 记录"

        adapter.onItemClickListener = object : TodayEntryAdapter.OnItemClickListener {
            override fun onItemClick(entry: JumpEntry) {
                EntryDetailActivity.start(this@DayEntriesActivity, entry)
            }
        }

        setupChart()
        loadData(date)
    }
    
    private fun setupChart() {
        // 设置图表基本属性
        lineChart.apply {
            description.isEnabled = false  // 隐藏描述
            legend.isEnabled = true       // 显示图例
            legend.textColor = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary) // 图例文字颜色
            setTouchEnabled(true)         // 允许触摸
            setScaleEnabled(true)         // 允许缩放
            setPinchZoom(true)            // 允许捕捉缩放
            setDrawGridBackground(false)  // 不绘制网格背景
            
            // 设置X轴
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM  // X轴在底部
                granularity = 1f                      // 最小间隔
                setDrawGridLines(true)                // 绘制网格线
                textColor = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary)               // 文字颜色
            }
            
            // 设置左侧Y轴
            axisLeft.apply {
                setDrawGridLines(true)    // 绘制网格线
                textColor = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary)   // 文字颜色
            }
            
            // 关闭右侧Y轴
            axisRight.isEnabled = false
            
            // 设置空数据提示
            setNoDataText("暂无数据")
            setNoDataTextColor(ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary))
        }
    }
    
    private fun loadData(date: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@DayEntriesActivity)
            val entries = db.jumpEntryDao().getEntriesByDate(date)
            
            // 更新列表
            launch(Dispatchers.Main) { adapter.submitList(entries) }
            
            // 更新图表
            if (entries.isNotEmpty()) {
                updateChart(entries)
            }
        }
    }
    
    private fun updateChart(entries: List<JumpEntry>) {
        // 将数据按时间戳正序排序
        val sortedEntries = entries.sortedBy { it.timestamp }
        
        // 准备图表数据
        val chartEntries = mutableListOf<Entry>()
        val timeLabels = mutableListOf<String>()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        // 添加数据点
        sortedEntries.forEachIndexed { index, entry ->
            chartEntries.add(Entry(index.toFloat(), entry.totalAfter.toFloat()))
            timeLabels.add(timeFmt.format(Date(entry.timestamp)))
        }
        
        // 创建数据集
        val dataSet = LineDataSet(chartEntries, getString(R.string.label_cumulative)).apply {
            color = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_primary)
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_secondary))
            setDrawCircleHole(false)
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_on_background)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@DayEntriesActivity, R.drawable.fade_sport_primary)
        }
        
        // 创建 LineData 对象
        val lineData = LineData(dataSet)
        
        // 创建格式化器
        val formatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < timeLabels.size) timeLabels[index] else ""
            }
        }
        
        // 在主线程上更新UI
        lifecycleScope.launch(Dispatchers.Main) {
            lineChart.xAxis.valueFormatter = formatter
            lineChart.data = lineData
            lineChart.invalidate()
        }
    }
}
