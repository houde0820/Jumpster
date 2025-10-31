package com.dp.jumpster.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dp.jumpster.R
import com.dp.jumpster.data.JumpEntry
import java.text.SimpleDateFormat
import java.util.*

class EntryDetailActivity : AppCompatActivity() {
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
        val type = intent.getStringExtra("type") ?: ""
        val value = intent.getIntExtra("value", 0)
        val total = intent.getIntExtra("totalAfter", 0)
        val ts = intent.getLongExtra("timestamp", 0L)
        val date = intent.getStringExtra("date") ?: ""

        val tvTitle: TextView = findViewById(R.id.text_title)
        val tvDate: TextView = findViewById(R.id.text_date)
        val tvType: TextView = findViewById(R.id.text_type)
        val tvValue: TextView = findViewById(R.id.text_value)
        val tvTotal: TextView = findViewById(R.id.text_total)
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        tvTitle.text = if (type == "add") "追加记录" else "覆盖记录"
        tvDate.text = "时间：${timeFmt.format(Date(ts))} (${date})"
        tvType.text = "类型：${if (type == "add") "追加" else "覆盖"}"
        tvValue.text = if (type == "add") "本次数量：$value" else "覆盖为：$total"
        tvTotal.text = "操作后累计：$total"
    }
}
