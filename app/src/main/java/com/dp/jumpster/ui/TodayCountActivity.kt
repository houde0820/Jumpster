package com.dp.jumpster.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpEntry
import com.dp.jumpster.data.JumpRecord
import com.dp.jumpster.service.ReminderService
import com.dp.jumpster.util.ShareCardGenerator
import com.dp.jumpster.util.SimpleCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TodayCountActivity : AppCompatActivity() {
    private lateinit var countText: TextView
    private lateinit var inputEdit: EditText
    private lateinit var addButton: Button
    private lateinit var coverButton: Button
    private lateinit var rvToday: RecyclerView
    private lateinit var fireworksView: FireworksView
    private val adapter = TodayEntryAdapter()

    private var todayCount = 0
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateLock = ReentrantLock()
    private val todayStr: String
        get() = dateLock.withLock {
            dateFormatter.format(Date())
        }

    // 记录开始时间
    private var inputStartTime: Long = 0
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化数据库实例，避免重复创建
        db = AppDatabase.getInstance(this)
        setContentView(R.layout.activity_today_count)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_settings_gear)
        toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        countText = findViewById(R.id.text_count_total)
        inputEdit = findViewById(R.id.edit_input)
        addButton = findViewById(R.id.btn_add)
        coverButton = findViewById(R.id.btn_cover)
        rvToday = findViewById(R.id.rv_today_entries)
        fireworksView = findViewById(R.id.fireworks)
        rvToday.layoutManager = LinearLayoutManager(this)
        rvToday.adapter = adapter
        (rvToday.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // 默认聚焦并弹出数字键盘
        inputEdit.requestFocus()
        inputEdit.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(inputEdit, InputMethodManager.SHOW_IMPLICIT)
        }
        
        // 键盘回车默认追加
        inputEdit.setOnEditorActionListener { _, _, _ ->
            onAddClick()
            true
        }
        
        // 记录输入开始时间
        inputEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                inputStartTime = System.currentTimeMillis()
            }
        }
        inputStartTime = System.currentTimeMillis() // 初始化时记录开始时间

        adapter.onItemClickListener = object : TodayEntryAdapter.OnItemClickListener {
            override fun onItemClick(entry: JumpEntry) {
                val i = Intent(this@TodayCountActivity, DayEntriesActivity::class.java)
                i.putExtra("date", todayStr)
                startActivity(i)
            }
        }

        refreshTodayCount()
        refreshTodayEntries()
        addButton.setOnClickListener {
            vibrateClick(it)
            onAddClick()
        }
        coverButton.setOnClickListener {
            vibrateClick(it)
            onCoverClick()
        }


        // 如果提醒已开启，显示提醒状态
        if (ReminderService.isReminderActive(this)) {
            showReminderStatus()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_today, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_month -> {
                startActivity(Intent(this, MonthCalendarActivity::class.java))
                true
            }
            R.id.action_trend -> {
                startActivity(Intent(this, TrendChartActivity::class.java))
                true
            }
            R.id.action_share -> {
                shareToday()
                true
            }
            R.id.action_undo -> {
                onUndoLatest()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun shareToday() {
        if (todayCount > 0) {
            ShareCardGenerator(this).shareToday(todayCount, todayStr)
        } else {
            Toast.makeText(this, "今日还没有记录哦", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示提醒状态信息
     */
    private fun showReminderStatus() {
        val isActive = ReminderService.isReminderActive(this)
        val isTimerStarted = ReminderService.isTimerStarted(this)
        val lastJumpTime = ReminderService.getLastJumpTime(this)
        
        if (isActive) {
            if (isTimerStarted) {
                val message = "提醒已开启，每10分钟提醒一次"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                val message = "提醒已开启，待记录第一次跳绳数据后开始计时"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshTodayCount() {
        lifecycleScope.launch(Dispatchers.IO) {
            val record = db.jumpRecordDao().getRecordByDate(todayStr)
            todayCount = record?.count ?: 0
            launch(Dispatchers.Main) {
                countText.text = "今日累计：$todayCount"
            }
        }
    }

    private fun refreshTodayEntries() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.jumpEntryDao().getEntriesByDate(todayStr)
            val limited = if (list.size > 10) list.subList(0, 10) else list
            launch(Dispatchers.Main) { adapter.submitList(ArrayList(limited)) }
        }
    }

    private fun onAddClick() {
        val parsed = parseAndValidateInput() ?: return
        val addVal = parsed
        val newCount = todayCount + addVal
        if (!validateTotal(newCount)) return
        saveTodayCountAndEntry(prev = todayCount, type = "add", inputVal = addVal, finalCount = newCount)
    }

    private fun onCoverClick() {
        val coverVal = parseAndValidateInput() ?: return
        if (!validateTotal(coverVal)) return
        saveTodayCountAndEntry(prev = todayCount, type = "cover", inputVal = coverVal, finalCount = coverVal)
    }

    private fun parseAndValidateInput(): Int? {
        val raw = inputEdit.text?.toString()?.trim() ?: ""
        if (raw.isEmpty()) {
            inputEdit.error = "请输入跳绳次数"
            inputEdit.requestFocus()
            return null
        }
        
        // 检查是否包含加减运算符
        val hasOperators = raw.contains('+') || raw.contains('-')
        
        val num = if (hasOperators) {
            // 使用计算器解析表达式
            SimpleCalculator.calculate(raw)
        } else {
            // 处理前导零 - 更安全的处理方式
            val normalized = normalizeNumberString(raw)
            val parsedNum = normalized.toIntOrNull()
            
            // 更新输入框内容，移除前导零
            if (parsedNum != null && normalized != raw) {
                inputEdit.setText(normalized)
                inputEdit.setSelection(inputEdit.text?.length ?: 0)
            }
            
            parsedNum
        }
        
        // 验证结果
        if (num == null) {
            inputEdit.error = if (hasOperators) "表达式格式不正确" else "请输入有效数字"
            inputEdit.requestFocus()
            return null
        }
        
        if (num <= 0) {
            inputEdit.error = "跳绳次数必须大于0"
            inputEdit.requestFocus()
            return null
        }
        
        // 如果是计算表达式，显示计算结果
        if (hasOperators) {
            inputEdit.setText(num.toString())
            inputEdit.setSelection(inputEdit.text?.length ?: 0)
            Toast.makeText(this, "计算结果: $num", Toast.LENGTH_SHORT).show()
        }
        
        val MAX_INPUT = 100_000
        if (num > MAX_INPUT) {
            Toast.makeText(this, "输入过大，已限制为 $MAX_INPUT", Toast.LENGTH_SHORT).show()
            inputEdit.setText(MAX_INPUT.toString())
            inputEdit.setSelection(inputEdit.text?.length ?: 0)
            return MAX_INPUT
        }
        
        return num
    }

    /**
     * 安全地标准化数字字符串，正确处理前导零
     */
    private fun normalizeNumberString(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "0"

        // 如果全是0，返回单个0
        if (trimmed.all { it == '0' }) return "0"

        // 移除前导零，但至少保留一个数字
        val normalized = trimmed.dropWhile { it == '0' }
        return if (normalized.isEmpty()) "0" else normalized
    }

    private fun validateTotal(total: Int): Boolean {
        val MAX_TOTAL = 1_000_000
        if (total > MAX_TOTAL) {
            Toast.makeText(this, "累计过大，最大支持 $MAX_TOTAL", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun onUndoLatest() {
        lifecycleScope.launch(Dispatchers.IO) {
            val entryDao = db.jumpEntryDao()
            val latest = entryDao.getLatestByDate(todayStr) ?: run {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@TodayCountActivity, "无可撤销记录", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // 安全地获取前一个total值：如果有多个记录则获取前一个，否则重置为0
            val entries = entryDao.getEntriesByDate(todayStr)
            val prevTotal = if (entries.size > 1) {
                val prevEntry = entries[1] // 第二个最新的记录
                prevEntry.totalAfter
            } else {
                0 // 只有一条记录时，撤销后重置为0
            }

            db.jumpRecordDao().insertRecord(JumpRecord(todayStr, prevTotal))
            entryDao.deleteById(latest.id)
            todayCount = prevTotal
            val updatedList = entryDao.getEntriesByDate(todayStr)
            val limited = if (updatedList.size > 10) updatedList.subList(0, 10) else updatedList
            launch(Dispatchers.Main) {
                countText.text = "今日累计：$todayCount"
                adapter.setHighlightKey(null)
                adapter.submitList(ArrayList(limited)) {
                    rvToday.scrollToPosition(0)
                }
                Toast.makeText(this@TodayCountActivity, "已撤销最近一次操作", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTodayCountAndEntry(prev: Int, type: String, inputVal: Int, finalCount: Int) {
        // 立即更新内存中的计数，防止快速点击导致的竞态条件
        todayCount = finalCount
        countText.text = "今日累计：$todayCount"
        
        val endTime = System.currentTimeMillis()
        lifecycleScope.launch(Dispatchers.IO) {
            db.jumpRecordDao().insertRecord(JumpRecord(todayStr, finalCount))
            val newId = db.jumpEntryDao().insert(
                JumpEntry(
                    id = 0,
                    date = todayStr,
                    type = type,
                    value = inputVal,
                    totalAfter = finalCount,
                    timestamp = endTime,
                    startTime = inputStartTime,
                    endTime = endTime
                )
            )
            
            // 记录跳绳数据并启动提醒计时器（如果需要）
            ReminderService.recordJump(this@TodayCountActivity)
            
            val updatedList = db.jumpEntryDao().getEntriesByDate(todayStr)
            val limited = if (updatedList.size > 10) updatedList.subList(0, 10) else updatedList
            launch(Dispatchers.Main) {
                // countText.text 已在前面更新
                inputEdit.setText("")
                animateCount()
                maybeCelebrate(prev, finalCount)
                adapter.setHighlightKey(newId)
                adapter.submitList(ArrayList(limited)) {
                    rvToday.scrollToPosition(0)
                }
            }
        }
    }

    private fun anchorCenterToFireworks(): Pair<Float, Float> {
        val cardLoc = IntArray(2)
        val fwLoc = IntArray(2)
        countText.getLocationOnScreen(cardLoc)
        fireworksView.getLocationOnScreen(fwLoc)
        val cx = (cardLoc[0] - fwLoc[0]) + countText.width / 2f
        val cy = (cardLoc[1] - fwLoc[1]) + countText.height / 2f
        return cx to cy
    }

    private fun maybeCelebrate(prev: Int, now: Int) {
        if (now <= 0) return
        val crossed1000 = (prev / 1000) != (now / 1000)
        val crossed500 = (prev / 500) != (now / 500)
        if (!crossed1000 && !crossed500) return
        fireworksView.post {
            val (cx, cy) = anchorCenterToFireworks()
            when {
                crossed1000 -> fireworksView.startBigAt(cx, cy)
                crossed500 -> fireworksView.startSmallAt(cx, cy)
            }
        }
    }

    private fun vibrateClick(view: View) {
        // 优先使用系统触觉反馈（无需权限）
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        // 作为备选，使用振动反馈（如果权限允许）
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(15)
                }
            }
        } catch (_: SecurityException) {
            // 无权限时忽略，系统触觉反馈已足够
        }
    }

    private fun animateCount() {
        countText.scaleX = 1f
        countText.scaleY = 1f
        countText.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(120)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                countText.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            .start()
    }
}
