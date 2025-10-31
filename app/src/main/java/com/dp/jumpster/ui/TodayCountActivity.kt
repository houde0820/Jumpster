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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TodayCountActivity : AppCompatActivity() {
    private lateinit var countText: TextView
    private lateinit var inputEdit: EditText
    private lateinit var addButton: Button
    private lateinit var coverButton: Button
    private lateinit var rvToday: RecyclerView
    private lateinit var fireworksView: FireworksView
    private val adapter = TodayEntryAdapter()

    private var todayCount = 0
    private val todayStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshTodayCount() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@TodayCountActivity).jumpRecordDao()
            val record = dao.getRecordByDate(todayStr)
            todayCount = record?.count ?: 0
            launch(Dispatchers.Main) {
                countText.text = "今日累计：$todayCount"
            }
        }
    }

    private fun refreshTodayEntries() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@TodayCountActivity).jumpEntryDao()
            val list = dao.getEntriesByDate(todayStr)
            val limited = if (list.size > 10) list.subList(0, 10) else list
            launch(Dispatchers.Main) { adapter.submitList(ArrayList(limited)) }
        }
    }

    private fun onAddClick() {
        val addVal = inputEdit.text.toString().toIntOrNull() ?: 0
        if (addVal == 0) {
            inputEdit.setText("")
            return
        }
        val newCount = todayCount + addVal
        saveTodayCountAndEntry(prev = todayCount, type = "add", inputVal = addVal, finalCount = newCount)
    }

    private fun onCoverClick() {
        val coverVal = inputEdit.text.toString().toIntOrNull() ?: 0
        if (coverVal == 0) {
            inputEdit.setText("")
            return
        }
        saveTodayCountAndEntry(prev = todayCount, type = "cover", inputVal = coverVal, finalCount = coverVal)
    }

    private fun saveTodayCountAndEntry(prev: Int, type: String, inputVal: Int, finalCount: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@TodayCountActivity)
            db.jumpRecordDao().insertRecord(JumpRecord(todayStr, finalCount))
            val newId = db.jumpEntryDao().insert(
                JumpEntry(
                    id = 0,
                    date = todayStr,
                    type = type,
                    value = inputVal,
                    totalAfter = finalCount,
                    timestamp = System.currentTimeMillis()
                )
            )
            todayCount = finalCount
            val updatedList = db.jumpEntryDao().getEntriesByDate(todayStr)
            val limited = if (updatedList.size > 10) updatedList.subList(0, 10) else updatedList
            launch(Dispatchers.Main) {
                countText.text = "今日累计：$todayCount"
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
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (_: SecurityException) {
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
