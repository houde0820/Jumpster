package com.dp.jumpster.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JumpEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JumpEntry): Long

    @Query("SELECT * FROM jump_entry WHERE date = :date ORDER BY timestamp DESC, id DESC")
    suspend fun getEntriesByDate(date: String): List<JumpEntry>
}
