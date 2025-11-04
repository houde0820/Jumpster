package com.dp.jumpster.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jump_entry")
data class JumpEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,             // yyyy-MM-dd
    val type: String,             // "add" or "cover"
    val value: Int,               // 本次输入的数值
    val totalAfter: Int,          // 本次操作后的累计总数
    val timestamp: Long,          // 毫秒时间戳
    val startTime: Long = 0,      // 开始时间戳（毫秒）
    val endTime: Long = 0         // 结束时间戳（毫秒）
)
