package com.dp.jumpster.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface JumpRecordDao {
    @Query("SELECT * FROM jump_record WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): JumpRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: JumpRecord)

    @Query("SELECT * FROM jump_record WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getRecordsBetween(startDate: String, endDate: String): List<JumpRecord>
    
    @androidx.room.Delete
    suspend fun delete(record: JumpRecord)
}
