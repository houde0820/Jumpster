package com.dp.jumpster.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dp.jumpster.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据导出辅助类
 * 支持导出跳绳数据为CSV格式
 */
class DataExportHelper(private val context: Context) {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * 导出所有跳绳数据为CSV格式
     */
    suspend fun exportToCSV(): File? = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val entryDao = db.jumpEntryDao()
            val recordDao = db.jumpRecordDao()
            
            // 获取所有记录的日期范围
            val earliestDate = "2020-01-01" // 可以优化为查询最早记录
            val latestDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val records = recordDao.getRecordsBetween(earliestDate, latestDate)
            
            if (records.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "没有数据可导出", Toast.LENGTH_SHORT).show()
                }
                return@withContext null
            }
            
            // 创建CSV文件
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "jumpster_export_$timestamp.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            file.bufferedWriter().use { writer ->
                // 写入CSV头
                writer.write("日期,总数,类型,数值,操作后累计,时间戳,开始时间,结束时间\n")
                
                // 写入JumpRecord和对应的JumpEntry
                records.forEach { record ->
                    val entries = entryDao.getEntriesByDate(record.date)
                    if (entries.isEmpty()) {
                        // 只有汇总数据，没有详细记录
                        writer.write("${record.date},${record.count},,,,,\n")
                    } else {
                        entries.forEach { entry ->
                            val startTimeStr = if (entry.startTime > 0) dateFormatter.format(Date(entry.startTime)) else ""
                            val endTimeStr = if (entry.endTime > 0) dateFormatter.format(Date(entry.endTime)) else ""
                            val timestampStr = dateFormatter.format(Date(entry.timestamp))
                            
                            writer.write(
                                "${entry.date},${record.count},${entry.type},${entry.value}," +
                                "${entry.totalAfter},$timestampStr,$startTimeStr,$endTimeStr\n"
                            )
                        }
                    }
                }
            }
            
            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
    }
    
    /**
     * 分享导出的CSV文件
     */
    suspend fun exportAndShare() {
        val file = exportToCSV() ?: return
        
        withContext(Dispatchers.Main) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "跳绳数据导出")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "分享跳绳数据"))
                Toast.makeText(context, "数据已导出", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
