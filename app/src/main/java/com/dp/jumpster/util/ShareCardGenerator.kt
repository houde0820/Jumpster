package com.dp.jumpster.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.FileProvider
import com.dp.jumpster.R
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 分享卡片生成器
 * 用于生成今日/本月跳绳成绩分享卡片
 */
class ShareCardGenerator(private val context: Context) {

    /**
     * 生成今日成绩分享卡片并调用系统分享
     * @param count 今日跳绳次数
     * @param date 日期，格式为 yyyy-MM-dd
     */
    fun shareToday(count: Int, date: String) {
        val view = generateShareCardView(
            title = "今日成绩",
            dateText = formatDateText(date),
            count = count
        )
        shareCardView(view, "今日跳绳成绩")
    }

    /**
     * 生成本月成绩分享卡片并调用系统分享
     * @param count 本月跳绳总次数
     * @param monthStr 月份，格式为 yyyy-MM
     */
    fun shareMonth(count: Int, monthStr: String) {
        val view = generateShareCardView(
            title = "本月成绩",
            dateText = formatMonthText(monthStr),
            count = count
        )
        shareCardView(view, "本月跳绳成绩")
    }

    /**
     * 生成分享卡片视图
     */
    private fun generateShareCardView(title: String, dateText: String, count: Int): View {
        val view = LayoutInflater.from(context).inflate(R.layout.share_card_layout, null)
        
        // 设置标题和日期
        view.findViewById<TextView>(R.id.text_date_title).text = title
        view.findViewById<TextView>(R.id.text_date_value).text = dateText
        
        // 设置跳绳次数，添加千位分隔符
        val formattedCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(count)
        view.findViewById<TextView>(R.id.text_count_value).text = formattedCount
        
        return view
    }

    /**
     * 将视图转换为位图
     */
    private fun viewToBitmap(view: View): Bitmap {
        // 测量视图大小
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        // 布局视图
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        
        // 创建位图并绘制视图
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        
        return bitmap
    }

    /**
     * 保存位图到临时文件
     */
    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        
        val file = File(cachePath, "shared_image_${System.currentTimeMillis()}.png")
        
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }
        
        return file
    }

    /**
     * 分享卡片视图
     */
    private fun shareCardView(view: View, title: String) {
        // 转换视图为位图
        val bitmap = viewToBitmap(view)
        
        // 保存位图到临时文件
        val file = saveBitmapToFile(bitmap)
        
        // 获取文件 URI
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // 创建分享 Intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "我的${title}：${view.findViewById<TextView>(R.id.text_count_value).text} 次！")
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // 启动分享 Intent
        context.startActivity(Intent.createChooser(shareIntent, "分享到"))
    }

    /**
     * 格式化日期文本
     */
    private fun formatDateText(dateStr: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        
        return try {
            val date = inputFormat.parse(dateStr) ?: return dateStr
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * 格式化月份文本
     */
    private fun formatMonthText(monthStr: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        
        return try {
            val date = inputFormat.parse(monthStr) ?: return monthStr
            outputFormat.format(date)
        } catch (e: Exception) {
            monthStr
        }
    }
}
