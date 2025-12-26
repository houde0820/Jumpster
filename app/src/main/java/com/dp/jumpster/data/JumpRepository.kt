package com.dp.jumpster.data

import com.dp.jumpster.data.JumpEntry
import com.dp.jumpster.data.JumpRecord

class JumpRepository(
    private val jumpRecordDao: JumpRecordDao,
    private val jumpEntryDao: JumpEntryDao
) {

    suspend fun getReviewByDate(date: String): JumpRecord? {
        return jumpRecordDao.getRecordByDate(date)
    }

    suspend fun insertRecord(record: JumpRecord) {
        jumpRecordDao.insertRecord(record)
    }

    suspend fun getRecordsBetween(startDate: String, endDate: String): List<JumpRecord> {
        return jumpRecordDao.getRecordsBetween(startDate, endDate)
    }

    suspend fun getEntryById(id: Long): JumpEntry? {
        return jumpEntryDao.getById(id)
    }

    suspend fun getEntriesByDate(date: String): List<JumpEntry> {
        return jumpEntryDao.getEntriesByDate(date)
    }

    suspend fun getRecentEntries(date: String, limit: Int): List<JumpEntry> {
        return jumpEntryDao.getRecentEntries(date, limit)
    }

    suspend fun getLatestByDate(date: String): JumpEntry? {
        return jumpEntryDao.getLatestByDate(date)
    }

    suspend fun insertEntry(entry: JumpEntry): Long {
        return jumpEntryDao.insert(entry)
    }

    suspend fun deleteEntryById(id: Long) {
        jumpEntryDao.deleteById(id)
    }
}
