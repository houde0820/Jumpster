package com.dp.jumpster.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dp.jumpster.R
import com.dp.jumpster.service.ReminderService
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var reminderSwitch: SwitchMaterial
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        toolbar = findViewById(R.id.toolbar)
        reminderSwitch = findViewById(R.id.switch_reminder)
        
        // 设置工具栏
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        // 初始化提醒开关状态
        reminderSwitch.isChecked = ReminderService.isReminderActive(this)
        
        // 设置提醒开关监听器
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ReminderService.startReminder(this)
                
                // 显示提示信息，说明计时器将在用户记录第一次跳绳数据时启动
                if (ReminderService.isTimerStarted(this)) {
                    Toast.makeText(this, "已开启每10分钟提醒", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "已开启提醒，待记录第一次跳绳数据后开始计时", Toast.LENGTH_LONG).show()
                }
            } else {
                ReminderService.stopReminder(this)
                Toast.makeText(this, "已关闭提醒", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 如果提醒已开启，显示当前状态
        if (ReminderService.isReminderActive(this)) {
            if (ReminderService.isTimerStarted(this)) {
                // 计时器已启动
                val lastJumpTime = ReminderService.getLastJumpTime(this)
                if (lastJumpTime > 0) {
                    val timeAgo = (System.currentTimeMillis() - lastJumpTime) / 1000 / 60 // 分钟
                    val message = "提醒已开启，最后一次跳绳记录于$timeAgo分钟前"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "提醒已开启，每10分钟提醒一次", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 提醒已开启但计时器尚未启动
                Toast.makeText(this, "提醒已开启，待记录第一次跳绳数据后开始计时", Toast.LENGTH_LONG).show()
            }
        }
    }
}
