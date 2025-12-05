package com.dp.jumpster.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpEntry
import com.dp.jumpster.util.DataConsistencyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EntryDetailActivity : AppCompatActivity() {
    private var entryId: Long = 0
    private var entryDate: String = ""
    
    companion object {
        fun start(context: Context, entry: JumpEntry) {
            val i = Intent(context, EntryDetailActivity::class.java)
            i.putExtra("id", entry.id)
            i.putExtra("date", entry.date)
            i.putExtra("type", entry.type)
            i.putExtra("value", entry.value)
            i.putExtra("totalAfter", entry.totalAfter)
            i.putExtra("timestamp", entry.timestamp)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_detail)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        
        entryId = intent.getLongExtra("id", 0)
        entryDate = intent.getStringExtra("date") ?: ""
        val type = intent.getStringExtra("type") ?: ""
        val value = intent.getIntExtra("value", 0)
        val total = intent.getIntExtra("totalAfter", 0)
        val ts = intent.getLongExtra("timestamp", 0L)

        val tvTitle: TextView = findViewById(R.id.text_title)
        val tvDate: TextView = findViewById(R.id.text_date)
        val tvType: TextView = findViewById(R.id.text_type)
        val tvValue: TextView = findViewById(R.id.text_value)
        val tvTotal: TextView = findViewById(R.id.text_total)
        val btnDelete: Button = findViewById(R.id.btn_delete)
        val btnEdit: Button = findViewById(R.id.btn_edit)
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        tvTitle.text = if (type == "add") "追加记录" else "覆盖记录"
        tvDate.text = "时间：${timeFmt.format(Date(ts))} (${entryDate})"
        tvType.text = "类型：${if (type == "add") "追加" else "覆盖"}"
        tvValue.text = if (type == "add") "本次数量：$value" else "覆盖为：$total"
        tvTotal.text = "操作后累计：$total"
        
        // 删除按钮
        btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
        
        // 编辑按钮
        btnEdit.setOnClickListener {
            showEditDialog(type, value)
        }
    }
    
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除这条记录吗？删除后将重新计算当天总数。")
            .setPositiveButton("删除") { _, _ ->
                deleteEntry()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteEntry() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@EntryDetailActivity)
            
            // 删除Entry
            db.jumpEntryDao().deleteById(entryId)
            
            // 重新计算JumpRecord
            DataConsistencyHelper.recalculateAndUpdateRecord(db, entryDate)
            
            launch(Dispatchers.Main) {
                Toast.makeText(this@EntryDetailActivity, "已删除", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK) // 通知调用者刷新数据
                finish()
            }
        }
    }
    
    private fun showEditDialog(type: String, currentValue: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_entry, null)
        val editValue = dialogView.findViewById<EditText>(R.id.edit_value)
        editValue.setText(currentValue.toString())
        editValue.setSelection(editValue.text.length)
        
        AlertDialog.Builder(this)
            .setTitle(if (type == "add") "编辑追加数量" else "编辑覆盖值")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val newValue = editValue.text.toString().toIntOrNull()
                if (newValue == null || newValue <= 0) {
                    Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                updateEntry(newValue)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun updateEntry(newValue: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@EntryDetailActivity)
            val entryDao = db.jumpEntryDao()
            
            // 获取当前Entry
            val currentEntry = entryDao.getById(entryId) ?: return@launch
            
            // 更新value
            val updatedEntry = currentEntry.copy(value = newValue)
            entryDao.update(updatedEntry)
            
            // 重新计算该Entry之后的所有记录
            DataConsistencyHelper.recalculateEntriesAfter(db, entryDate, currentEntry.timestamp)
            
            launch(Dispatchers.Main) {
                Toast.makeText(this@EntryDetailActivity, "已更新", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }
    }
}
