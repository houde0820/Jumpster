package com.dp.jumpster.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpEntry
import com.dp.jumpster.data.JumpRepository
import com.dp.jumpster.ui.viewmodel.DayEntriesViewModel
import com.dp.jumpster.ui.viewmodel.DayEntriesUiState
import com.dp.jumpster.ui.viewmodel.ViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class DayEntriesActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var titleText: TextView
    private lateinit var rv: RecyclerView
    private lateinit var lineChart: LineChart
    private val adapter = TodayEntryAdapter()
    private var currentDate: String = ""
    
    private lateinit var viewModel: DayEntriesViewModel

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Data modified, reload
            viewModel.loadData(currentDate)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_entries)
        
        val db = AppDatabase.getInstance(this)
        val repository = JumpRepository(db.jumpRecordDao(), db.jumpEntryDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[DayEntriesViewModel::class.java]

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

        currentDate = intent.getStringExtra("date") ?: ""
        titleText.text = "$currentDate 记录"

        adapter.onItemClickListener = object : TodayEntryAdapter.OnItemClickListener {
            override fun onItemClick(entry: JumpEntry) {
                val intent = Intent(this@DayEntriesActivity, EntryDetailActivity::class.java)
                intent.putExtra("id", entry.id)
                intent.putExtra("date", entry.date)
                intent.putExtra("type", entry.type)
                intent.putExtra("value", entry.value)
                intent.putExtra("totalAfter", entry.totalAfter)
                intent.putExtra("timestamp", entry.timestamp)
                detailLauncher.launch(intent)
            }
        }

        setupChart()
        setupObservers()
        
        // Initial Load
        if (currentDate.isNotEmpty()) {
            viewModel.loadData(currentDate)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload every time on resume to ensure fresh data
        if (currentDate.isNotEmpty()) {
            viewModel.loadData(currentDate)
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                         is DayEntriesUiState.Loading -> {
                             // Loading
                         }
                         is DayEntriesUiState.Success -> {
                             adapter.submitList(state.entries)
                             if (state.entries.isNotEmpty()) {
                                 updateChart(state.entries)
                             }
                         }
                    }
                }
            }
        }
    }
    
    private fun setupChart() {
        // Basic configuration
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary)
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(true)
                textColor = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary)
            }
            
            // Left Y Axis
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary)
            }
            
            // Right Y Axis
            axisRight.isEnabled = false
            
            setNoDataText("暂无数据")
            setNoDataTextColor(ContextCompat.getColor(this@DayEntriesActivity, R.color.sport_text_secondary))
        }
    }
    
    private fun updateChart(entries: List<JumpEntry>) {
        val sortedEntries = entries.sortedBy { it.timestamp }
        
        val chartEntries = mutableListOf<Entry>()
        val timeLabels = mutableListOf<String>()
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        
        sortedEntries.forEachIndexed { index, entry ->
            chartEntries.add(Entry(index.toFloat(), entry.totalAfter.toFloat()))
            val localTime = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
            timeLabels.add(timeFmt.format(localTime))
        }
        
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
        
        val lineData = LineData(dataSet)
        
        val formatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < timeLabels.size) timeLabels[index] else ""
            }
        }
        
        lineChart.xAxis.valueFormatter = formatter
        lineChart.data = lineData
        lineChart.invalidate()
    }
}
