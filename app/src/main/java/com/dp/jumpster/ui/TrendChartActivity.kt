package com.dp.jumpster.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpRepository
import com.dp.jumpster.ui.viewmodel.TrendUiState
import com.dp.jumpster.ui.viewmodel.TrendViewModel
import com.dp.jumpster.ui.viewmodel.ViewModelFactory
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
import kotlinx.coroutines.launch

class TrendChartActivity : AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var tabLayout: TabLayout
    private lateinit var chartTypeTabs: TabLayout
    private lateinit var noDataText: View
    
    private lateinit var viewModel: TrendViewModel

    private var currentChartType = 0 // 0: Bar, 1: Line

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trend_chart)

        // Initialize ViewModel
        val db = AppDatabase.getInstance(this)
        val repository = JumpRepository(db.jumpRecordDao(), db.jumpEntryDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TrendViewModel::class.java]

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
        
        setupObservers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupCharts() {
        // Shared chart setup logic
        val configChart = { chart: com.github.mikephil.charting.charts.BarLineChartBase<*> ->
            chart.description.isEnabled = false
            chart.legend.isEnabled = true
            chart.legend.textColor = ContextCompat.getColor(this, R.color.sport_text_secondary)
            chart.setTouchEnabled(true)
            chart.setScaleEnabled(true)
            chart.setPinchZoom(true)
            chart.setDrawGridBackground(false)
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.granularity = 1f
            chart.xAxis.textColor = ContextCompat.getColor(this, R.color.sport_text_secondary)
            chart.axisLeft.textColor = ContextCompat.getColor(this, R.color.sport_text_secondary)
            chart.axisRight.isEnabled = false
        }
        
        configChart(lineChart)
        configChart(barChart)
    }

    private fun setupTabListeners() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.setPeriod(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        chartTypeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentChartType = tab.position
                updateChartVisibility()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TrendUiState.Loading -> {
                             // Optional: Show loading indicator
                        }
                        is TrendUiState.Empty -> {
                            showNoData()
                        }
                        is TrendUiState.Success -> {
                            hideNoData()
                            updateCharts(state)
                        }
                    }
                }
            }
        }
    }

    private fun updateChartVisibility() {
        if (noDataText.visibility == View.VISIBLE) return
        
        when (currentChartType) {
            0 -> {
                barChart.visibility = View.VISIBLE
                lineChart.visibility = View.GONE
            }
            1 -> {
                barChart.visibility = View.GONE
                lineChart.visibility = View.VISIBLE
            }
        }
    }

    private fun updateCharts(state: TrendUiState.Success) {
        val xLabels = state.xLabels
        
        // Update Bar Chart
        val barDataSet = BarDataSet(state.barEntries, "跳绳次数").apply {
            color = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_primary)
            valueTextColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_on_background)
            valueTextSize = 10f
        }
        val barData = BarData(barDataSet).apply {
            barWidth = 0.7f
        }
        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        barChart.xAxis.labelCount = xLabels.size
        barChart.animateY(500)
        barChart.invalidate() 

        // Update Line Chart
        val lineDataSet = LineDataSet(state.lineEntries, getString(R.string.label_jump_count)).apply {
            color = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_primary)
            valueTextColor = ContextCompat.getColor(this@TrendChartActivity, R.color.sport_on_background)
            valueTextSize = 10f
            lineWidth = 2f
            setCircleColor(ContextCompat.getColor(this@TrendChartActivity, R.color.sport_primary))
            circleRadius = 4f
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.LINEAR
        }
        val lineData = LineData(lineDataSet)
        lineChart.data = lineData
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        lineChart.xAxis.labelCount = xLabels.size
        lineChart.animateY(500)
        lineChart.invalidate()
        
        updateChartVisibility()
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

