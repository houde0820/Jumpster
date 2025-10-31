package com.dp.jumpster.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DayEntriesActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var titleText: TextView
    private lateinit var rv: RecyclerView
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

        lifecycleScope.launch(Dispatchers.IO) {
            val list = AppDatabase.getInstance(this@DayEntriesActivity).jumpEntryDao().getEntriesByDate(date)
            launch(Dispatchers.Main) { adapter.submitList(list) }
        }
    }
}
