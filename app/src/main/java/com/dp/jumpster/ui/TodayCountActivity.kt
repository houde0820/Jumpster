package com.dp.jumpster.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TodayCountActivity : AppCompatActivity() {
    private lateinit var countText: TextView
    private lateinit var inputEdit: EditText
    private lateinit var addButton: Button
    private lateinit var coverButton: Button
    private var todayCount = 0
    private val todayStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_count)
        countText = findViewById(R.id.text_count_total)
        inputEdit = findViewById(R.id.edit_input)
        addButton = findViewById(R.id.btn_add)
        coverButton = findViewById(R.id.btn_cover)

        refreshTodayCount()
        addButton.setOnClickListener { onAddClick() }
        coverButton.setOnClickListener { onCoverClick() }
    }

    private fun refreshTodayCount() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@TodayCountActivity).jumpRecordDao()
            val record = dao.getRecordByDate(todayStr)
            todayCount = record?.count ?: 0
            launch(Dispatchers.Main) {
                countText.text = "今日累计：$todayCount"
            }
        }
    }

    private fun onAddClick() {
        val addVal = inputEdit.text.toString().toIntOrNull() ?: 0
        val newCount = todayCount + addVal
        saveTodayCount(newCount)
    }

    private fun onCoverClick() {
        val coverVal = inputEdit.text.toString().toIntOrNull() ?: 0
        saveTodayCount(coverVal)
    }

    private fun saveTodayCount(count: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@TodayCountActivity).jumpRecordDao()
            dao.insertRecord(JumpRecord(todayStr, count))
            todayCount = count
            launch(Dispatchers.Main) {
                countText.text = "今日累计：$todayCount"
                inputEdit.setText("")
            }
        }
    }
}
