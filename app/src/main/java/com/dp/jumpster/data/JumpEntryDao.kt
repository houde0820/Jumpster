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

    @Query("SELECT * FROM jump_entry WHERE date = :date ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestByDate(date: String): JumpEntry?

    @Query("SELECT totalAfter FROM jump_entry WHERE date = :date ORDER BY timestamp DESC, id DESC LIMIT 1 OFFSET 1")
    suspend fun getPrevTotalAfter(date: String): Int?

    @Query("SELECT totalAfter FROM jump_entry WHERE date = :date ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestTotalAfter(date: String): Int?

    @Query("DELETE FROM jump_entry WHERE id = :id")
    suspend fun deleteById(id: Long)
}
