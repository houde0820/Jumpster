package com.dp.jumpster.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dp.jumpster.R
import com.dp.jumpster.data.JumpEntry
import java.text.SimpleDateFormat
import java.util.*

class TodayEntryAdapter : ListAdapter<JumpEntry, TodayEntryAdapter.EntryVH>(Diff()) {
    init { setHasStableIds(true) }

    interface OnItemClickListener { fun onItemClick(entry: JumpEntry) }
    var onItemClickListener: OnItemClickListener? = null

    // 用于只对最新插入项做一次淡入动画
    var highlightKey: Long? = null
        private set

    fun setHighlightKey(key: Long?) { highlightKey = key }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return if (item.id != 0L) item.id else item.timestamp
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_today_entry, parent, false)
        return EntryVH(v)
    }

    override fun onBindViewHolder(holder: EntryVH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClickListener?.onItemClick(item) }
        val key = getItemId(position)
        if (highlightKey != null && highlightKey == key) {
            if (holder.itemView.alpha == 1f) holder.itemView.alpha = 0f
            holder.itemView.animate().alpha(1f).setDuration(180).withEndAction { highlightKey = null }.start()
        } else {
            holder.itemView.alpha = 1f
        }
    }

    class Diff : DiffUtil.ItemCallback<JumpEntry>() {
        override fun areItemsTheSame(oldItem: JumpEntry, newItem: JumpEntry): Boolean =
            (oldItem.id != 0L && newItem.id != 0L && oldItem.id == newItem.id) || oldItem.timestamp == newItem.timestamp
        override fun areContentsTheSame(oldItem: JumpEntry, newItem: JumpEntry): Boolean =
            oldItem.type == newItem.type && oldItem.value == newItem.value && oldItem.totalAfter == newItem.totalAfter && oldItem.timestamp == newItem.timestamp
    }

    class EntryVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.text_title)
        private val subtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        fun bind(e: JumpEntry) {
            val typeLabel = if (e.type == "add") "追加" else "覆盖"
            val valueStr = if (e.type == "add") "+${e.value}" else "→ ${e.totalAfter}"
            title.text = "$typeLabel $valueStr"
            val colorId = if (e.type == "add") R.color.sport_primary else R.color.sport_secondary
            title.setTextColor(ContextCompat.getColor(itemView.context, colorId))
            
            // 显示时间范围
            val timeInfo = if (e.startTime > 0 && e.endTime > 0 && e.endTime > e.startTime) {
                val duration = (e.endTime - e.startTime) / 1000 // 秒
                val durationStr = if (duration < 60) "${duration}秒" else "${duration/60}分${duration%60}秒"
                "${timeFmt.format(Date(e.timestamp))} | 用时 $durationStr | 累计 ${e.totalAfter}"
            } else {
                "${timeFmt.format(Date(e.timestamp))} | 累计 ${e.totalAfter}"
            }
            subtitle.text = timeInfo
        }
    }
}
