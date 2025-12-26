package com.dp.jumpster.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.dp.jumpster.R
import com.dp.jumpster.data.AppDatabase
import com.dp.jumpster.data.JumpEntry
import com.dp.jumpster.data.JumpRecord
import com.dp.jumpster.data.JumpRepository
import com.dp.jumpster.databinding.ActivityTodayCountBinding
import com.dp.jumpster.service.ReminderService
import com.dp.jumpster.ui.viewmodel.TodayCountViewModel
import com.dp.jumpster.ui.viewmodel.ViewModelFactory
import com.dp.jumpster.util.ShareCardGenerator
import com.dp.jumpster.util.SimpleCalculator
import com.dp.jumpster.util.vibrateClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class TodayCountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTodayCountBinding
    private val adapter = TodayEntryAdapter()

    private var todayCount = 0
    private lateinit var viewModel: TodayCountViewModel
    
    // 记录开始时间
    private var inputStartTime: Long = 0
    private var loadedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 ViewModel
        val db = AppDatabase.getInstance(this)
        val repository = JumpRepository(db.jumpRecordDao(), db.jumpEntryDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TodayCountViewModel::class.java]
        
        // 使用 View Binding
        binding = ActivityTodayCountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        binding.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_settings_gear)
        binding.toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.rvTodayEntries.layoutManager = LinearLayoutManager(this)
        binding.rvTodayEntries.adapter = adapter
        (binding.rvTodayEntries.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // 默认聚焦并弹出数字键盘
        binding.editInput.requestFocus()
        binding.editInput.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editInput, InputMethodManager.SHOW_IMPLICIT)
        }
        
        // 键盘回车默认追加
        binding.editInput.setOnEditorActionListener { _, _, _ ->
            onAddClick()
            true
        }
        
        // 记录输入开始时间
        binding.editInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                inputStartTime = System.currentTimeMillis()
            }
        }
        inputStartTime = System.currentTimeMillis() // 初始化时记录开始时间

        adapter.onItemClickListener = object : TodayEntryAdapter.OnItemClickListener {
            override fun onItemClick(entry: JumpEntry) {
                val i = Intent(this@TodayCountActivity, DayEntriesActivity::class.java)
                i.putExtra("date", loadedDate)
                startActivity(i)
            }
        }

        binding.btnAdd.setOnClickListener {
            it.vibrateClick()
            onAddClick()
        }
        binding.btnCover.setOnClickListener {
            it.vibrateClick()
            onCoverClick()
            // ReminderService logic to be handled via observation or side effect in Activity
        }


        // 如果提醒已开启，显示提醒状态
        if (ReminderService.isReminderActive(this)) {
            showReminderStatus()
        }

        checkRealDisplayMetrics()
        
        setupObservers()
    }

    private fun setupObservers() {
        // 观察总次数
        viewModel.todayCount.observe(this) { count ->
            binding.textCountTotal.text = getString(R.string.fmt_today_total, count)
        }
        
        // 观察记录列表
        viewModel.entries.observe(this) { list ->
           adapter.submitList(ArrayList(list))
        }
        
        // 观察高亮条目
        viewModel.highlightEntryId.observe(this) { id ->
            adapter.setHighlightKey(id)
            if (id != null) {
                binding.rvTodayEntries.scrollToPosition(0)
                // Clear highlight after scroll? Usually kept for a while.
                // viewModel.clearHighlight() can be called if needed.
            }
        }
        
        // 观察 Toast 消息
        viewModel.toastMessage.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                viewModel.resetToast()
            }
        }
        
        // 观察动画事件
        viewModel.animateCountEvent.observe(this) { animate ->
            if (animate) {
                animateCount()
                 // Reset event logic if using SingleLiveEvent, or just handle boolean state
            }
        }
        
        // 观察庆祝事件
        viewModel.celebrateEvent.observe(this) { (prev, curr) ->
             maybeCelebrate(prev, curr)
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val currentToday = LocalDate.now().toString()
        if (loadedDate != currentToday) {
            loadedDate = currentToday
            supportActionBar?.subtitle = currentToday
        }
        viewModel.loadData()
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
                viewModel.undoLatest()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun shareToday() {
        val count = viewModel.todayCount.value ?: 0
        if (count > 0) {
            ShareCardGenerator(this).shareToday(count, loadedDate)
        } else {
            Toast.makeText(this, getString(R.string.msg_no_records_today), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示提醒状态信息
     */
    private fun showReminderStatus() {
        val isActive = ReminderService.isReminderActive(this)
        val isTimerStarted = ReminderService.isTimerStarted(this)
        
        if (isActive) {
            if (isTimerStarted) {
                val message = getString(R.string.msg_reminder_active_interval)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                val message = getString(R.string.msg_reminder_active_first)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // refreshTodayCount and refreshTodayEntries removed - logic moved to ViewModel


    private fun onAddClick() {
        val parsed = parseAndValidateInput() ?: return
        if (!validateTotal((viewModel.todayCount.value ?: 0) + parsed)) return
        
        viewModel.addJump(parsed, inputStartTime) {
            Toast.makeText(this, "日期已变更，数据已刷新", Toast.LENGTH_SHORT).show()
        }
        
        binding.editInput.setText("")
    }

    private fun onCoverClick() {
        val parsed = parseAndValidateInput() ?: return
        if (!validateTotal(parsed)) return

        viewModel.coverJump(parsed, inputStartTime) {
             Toast.makeText(this, "日期已变更，数据已刷新", Toast.LENGTH_SHORT).show()
        }
        
        binding.editInput.setText("")
    }

    private fun parseAndValidateInput(): Int? {
        val raw = binding.editInput.text?.toString()?.trim() ?: ""
        if (raw.isEmpty()) {
            binding.editInput.error = getString(R.string.error_enter_jumps)
            binding.editInput.requestFocus()
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
                binding.editInput.setText(normalized)
                binding.editInput.setSelection(binding.editInput.text?.length ?: 0)
            }
            
            parsedNum
        }
        
        // 验证结果
        if (num == null) {
            binding.editInput.error = if (hasOperators) getString(R.string.error_invalid_format) else getString(R.string.error_invalid_number)
            binding.editInput.requestFocus()
            return null
        }
        
        if (num <= 0) {
            binding.editInput.error = getString(R.string.error_must_be_positive)
            binding.editInput.requestFocus()
            return null
        }
        
        // 如果是计算表达式，显示计算结果
        if (hasOperators) {
            binding.editInput.setText(num.toString())
            binding.editInput.setSelection(binding.editInput.text?.length ?: 0)
            Toast.makeText(this, getString(R.string.msg_calc_result, num), Toast.LENGTH_SHORT).show()
        }
        
        val MAX_INPUT = 100_000
        if (num > MAX_INPUT) {
            Toast.makeText(this, getString(R.string.msg_input_too_large, MAX_INPUT), Toast.LENGTH_SHORT).show()
            binding.editInput.setText(MAX_INPUT.toString())
            binding.editInput.setSelection(binding.editInput.text?.length ?: 0)
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
            Toast.makeText(this, getString(R.string.msg_total_too_large, MAX_TOTAL), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // onUndoLatest logic moved to ViewModel

    // saveTodayCountAndEntry logic moved to ViewModel

    private fun anchorCenterToFireworks(): Pair<Float, Float> {
        val cardLoc = IntArray(2)
        val fwLoc = IntArray(2)
        binding.textCountTotal.getLocationOnScreen(cardLoc)
        binding.fireworks.getLocationOnScreen(fwLoc)
        val cx = (cardLoc[0] - fwLoc[0]) + binding.textCountTotal.width / 2f
        val cy = (cardLoc[1] - fwLoc[1]) + binding.textCountTotal.height / 2f
        return cx to cy
    }

    private fun maybeCelebrate(prev: Int, now: Int) {
        if (now <= 0) return
        val crossed1000 = (prev / 1000) != (now / 1000)
        val crossed500 = (prev / 500) != (now / 500)
        if (!crossed1000 && !crossed500) return
        binding.fireworks.post {
            val (cx, cy) = anchorCenterToFireworks()
            when {
                crossed1000 -> binding.fireworks.startBigAt(cx, cy)
                crossed500 -> binding.fireworks.startSmallAt(cx, cy)
            }
        }
    }

    // vibrateClick has been extracted to ViewExtensions.kt


    private fun animateCount() {
        binding.textCountTotal.scaleX = 1f
        binding.textCountTotal.scaleY = 1f
        binding.textCountTotal.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(120)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.textCountTotal.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            .start()
    }

    /**
     * 打印当前 Activity 实际占用的屏幕参数
     */
    fun Activity.checkRealDisplayMetrics() {
        // 获取当前资源中的显示指标
        val metrics = resources.displayMetrics

        // 1. 获取 App 实际可用绘图区域的宽/高 (已自动减去状态栏、侧边栏等系统占用)
        val widthPixels = metrics.widthPixels
        val heightPixels = metrics.heightPixels

        // 2. 获取逻辑密度
        val density = metrics.density
        val densityDpi = metrics.densityDpi

        // 3. 计算 App 感知到的 dp 宽度 (宽度像素 / density)
        val widthDp = widthPixels / density
        val heightDp = heightPixels / density

        Log.d("DisplayCheck", "=========== 屏幕参数检查 (Kotlin) ===========")
        Log.d("DisplayCheck", "1. 物理全屏 (wm size): 2240 x 1260 ")
        Log.d("DisplayCheck", "2. App可用分辨率 (px): $widthPixels x $heightPixels")
        Log.d("DisplayCheck", "3. Density: $density (DPI: $densityDpi)")
        Log.d("DisplayCheck", "4. App dp: ${widthDp}dp x ${heightDp}dp")
        Log.d("DisplayCheck", "=========================================")
    }
}
