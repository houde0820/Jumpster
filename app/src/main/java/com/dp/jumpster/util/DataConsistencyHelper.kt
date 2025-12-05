package com.dp.jumpster.util

import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpRecord

/**
 * 数据一致性辅助类
 * 确保删除或修改Entry后，JumpRecord的数据保持一致
 */
object DataConsistencyHelper {
    
    /**
     * 删除Entry后重新计算并更新JumpRecord
     * @param db 数据库实例
     * @param date 需要更新的日期 (yyyy-MM-dd)
     */
    suspend fun recalculateAndUpdateRecord(db: AppDatabase, date: String) {
        val entryDao = db.jumpEntryDao()
        val recordDao = db.jumpRecordDao()
        
        // 获取该日期的最新totalAfter值
        val latestTotal = entryDao.getLatestTotalAfter(date)
        
        if (latestTotal != null && latestTotal > 0) {
            // 有记录，更新JumpRecord
            recordDao.insertRecord(JumpRecord(date, latestTotal))
        } else {
            // 没有记录或总数为0，删除JumpRecord
            val existingRecord = recordDao.getRecordByDate(date)
            if (existingRecord != null) {
                recordDao.delete(existingRecord)
            }
        }
    }
    
    /**
     * 重新计算某条Entry之后的所有记录的totalAfter值
     * 当修改了某条Entry的value时调用
     * @param db 数据库实例
     * @param date 日期
     * @param fromTimestamp 从哪个时间戳开始重新计算（包含该时间戳）
     */
    suspend fun recalculateEntriesAfter(db: AppDatabase, date: String, fromTimestamp: Long) {
        val entryDao = db.jumpEntryDao()
        
        // 获取该日期所有Entry，按时间正序
        val allEntries = entryDao.getEntriesByDate(date).sortedBy { it.timestamp }
        
        if (allEntries.isEmpty()) return
        
        // 找到需要重新计算的起始位置
        val startIndex = allEntries.indexOfFirst { it.timestamp >= fromTimestamp }
        if (startIndex == -1) return
        
        // 获取前一个Entry的totalAfter作为基准
        val prevTotal = if (startIndex > 0) {
            allEntries[startIndex - 1].totalAfter
        } else {
            0
        }
        
        // 重新计算并更新
        var runningTotal = prevTotal
        for (i in startIndex until allEntries.size) {
            val entry = allEntries[i]
            runningTotal = if (entry.type == "add") {
                runningTotal + entry.value
            } else {
                entry.value // cover类型直接设置为value
            }
            
            // 更新Entry的totalAfter
            val updatedEntry = entry.copy(totalAfter = runningTotal)
            entryDao.update(updatedEntry)
        }
        
        // 更新JumpRecord
        recalculateAndUpdateRecord(db, date)
    }
}
