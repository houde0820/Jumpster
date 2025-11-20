package com.dp.jumpster.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.dp.jumpster.R
import com.dp.jumpster.ui.TodayCountActivity
import java.util.concurrent.TimeUnit

/**
 * 定时提醒服务
 * 使用 AlarmManager 每10分钟提醒一次，更加省电且可靠
 */
class ReminderService : Service() {
    
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences
    
    // 提醒间隔时间（毫秒）
    private val reminderInterval = TimeUnit.MINUTES.toMillis(10)
    
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_REMINDER -> startReminder()
            ACTION_STOP_REMINDER -> stopReminder()
            ACTION_RECORD_JUMP -> recordJumpAndStartTimerIfNeeded()
            ACTION_TRIGGER_ALARM -> onAlarmTriggered()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        // 释放媒体播放器资源
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
    
    /**
     * 启动提醒服务
     */
    private fun startReminder() {
        // 创建并显示前台服务通知
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 保存提醒状态
        prefs.edit().putBoolean(KEY_REMINDER_ACTIVE, true).apply()
        
        // 注意：这里不立即安排提醒，而是等待用户记录跳绳数据时才启动计时器
    }
    
    /**
     * 停止提醒服务
     */
    private fun stopReminder() {
        cancelAlarm()
        
        // 释放媒体播放器资源
        mediaPlayer?.release()
        mediaPlayer = null
        
        // 保存提醒状态
        prefs.edit()
            .putBoolean(KEY_REMINDER_ACTIVE, false)
            .putBoolean(KEY_TIMER_STARTED, false)
            .apply()
        
        // 停止前台服务
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * 安排下一次提醒
     */
    private fun scheduleNextReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = SystemClock.elapsedRealtime() + reminderInterval
        
        val intent = Intent(this, ReminderService::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
    
    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderService::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * 记录跳绳数据并在需要时启动计时器
     */
    private fun recordJumpAndStartTimerIfNeeded() {
        // 更新最后一次跳绳记录时间
        val currentTime = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_JUMP_TIME, currentTime).apply()
        
        // 每次记录跳绳都重置闹钟
        if (prefs.getBoolean(KEY_REMINDER_ACTIVE, false)) {
            // 如果计时器未启动，标记为启动
            if (!prefs.getBoolean(KEY_TIMER_STARTED, false)) {
                prefs.edit().putBoolean(KEY_TIMER_STARTED, true).apply()
                Toast.makeText(this, "已启动跳绳提醒计时器", Toast.LENGTH_SHORT).show()
            }
            // 重新安排下一次提醒（重置计时）
            scheduleNextReminder()
        }
    }
    
    private fun onAlarmTriggered() {
        // 播放声音和振动
        playReminderSound()
        vibrate()
        
        // 安排下一次提醒
        scheduleNextReminder()
    }
    
    /**
     * 播放提醒音效
     */
    private fun playReminderSound() {
        try {
            // 释放之前的媒体播放器资源
            mediaPlayer?.release()
            
            // 创建新的媒体播放器
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(applicationContext, soundUri).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 振动提醒
     */
    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            500,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "跳绳提醒服务"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, TodayCountActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("跳绳提醒")
            .setContentText("每10分钟提醒一次")
            .setSmallIcon(R.drawable.ic_trend)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "reminder_channel"
        private const val ALARM_REQUEST_CODE = 2001
        
        private const val KEY_REMINDER_ACTIVE = "reminder_active"
        private const val KEY_TIMER_STARTED = "timer_started"
        private const val KEY_LAST_JUMP_TIME = "last_jump_time"
        
        const val ACTION_START_REMINDER = "com.dp.jumpster.action.START_REMINDER"
        const val ACTION_STOP_REMINDER = "com.dp.jumpster.action.STOP_REMINDER"
        const val ACTION_RECORD_JUMP = "com.dp.jumpster.action.RECORD_JUMP"
        const val ACTION_TRIGGER_ALARM = "com.dp.jumpster.action.TRIGGER_ALARM"
        
        /**
         * 检查提醒服务是否处于活动状态
         */
        fun isReminderActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_REMINDER_ACTIVE, false)
        }
        
        /**
         * 启动提醒服务
         */
        fun startReminder(context: Context) {
            val intent = Intent(context, ReminderService::class.java).apply {
                action = ACTION_START_REMINDER
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 停止提醒服务
         */
        fun stopReminder(context: Context) {
            val intent = Intent(context, ReminderService::class.java).apply {
                action = ACTION_STOP_REMINDER
            }
            context.startService(intent)
        }
        
        /**
         * 记录跳绳数据并启动计时器（如果需要）
         */
        fun recordJump(context: Context) {
            val intent = Intent(context, ReminderService::class.java).apply {
                action = ACTION_RECORD_JUMP
            }
            context.startService(intent)
        }
        
        /**
         * 获取最后一次跳绳记录时间
         */
        fun getLastJumpTime(context: Context): Long {
            val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
            return prefs.getLong(KEY_LAST_JUMP_TIME, 0)
        }
        
        /**
         * 检查计时器是否已启动
         */
        fun isTimerStarted(context: Context): Boolean {
            val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_TIMER_STARTED, false)
        }
    }
}
