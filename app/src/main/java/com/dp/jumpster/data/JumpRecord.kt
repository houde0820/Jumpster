package com.dp.jumpster.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jump_record")
data class JumpRecord(
    @PrimaryKey val date: String,    // 格式 yyyy-MM-dd
    val count: Int
)
