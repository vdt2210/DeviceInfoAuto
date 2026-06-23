package com.deviceinfo.auto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

sealed class InfoItem {
    data class Section(val key: String) : InfoItem()
    data class Row(
        val key: String,
        val title: String,
        val value: CharSequence,
        val iconResId: Int,
    ) : InfoItem()
}

private class InfoItemDiffCallback : DiffUtil.ItemCallback<InfoItem>() {
    override fun areItemsTheSame(oldItem: InfoItem, newItem: InfoItem): Boolean =
        when {
            oldItem is InfoItem.Section && newItem is InfoItem.Section ->
                oldItem.key == newItem.key
            oldItem is InfoItem.Row && newItem is InfoItem.Row ->
                oldItem.key == newItem.key
            else -> false
        }

    override fun areContentsTheSame(oldItem: InfoItem, newItem: InfoItem): Boolean =
        oldItem == newItem
}

class InfoListAdapter : ListAdapter<InfoItem, RecyclerView.ViewHolder>(InfoItemDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    companion object {
        const val VIEW_TYPE_SECTION = 0
        const val VIEW_TYPE_ROW = 1
    }

    override fun getItemId(position: Int): Long =
        when (val item = getItem(position)) {
            is InfoItem.Section -> item.key.hashCode().toLong()
            is InfoItem.Row -> item.key.hashCode().toLong()
        }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is InfoItem.Section -> VIEW_TYPE_SECTION
            is InfoItem.Row -> VIEW_TYPE_ROW
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_info_row, parent, false)
                RowViewHolder(view)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is InfoItem.Section -> {
                val ctx = holder.itemView.context
                val title = DeviceInfoUiShared.sectionTitle(ctx, item.key)
                (holder as SectionViewHolder).bind(title)
                holder.itemView.contentDescription =
                    ctx.getString(R.string.info_section_content_description, title)
            }
            is InfoItem.Row -> {
                val isFirstInGroup = position > 0 && getItem(position - 1) is InfoItem.Section
                val isLastInGroup = position == itemCount - 1 || getItem(position + 1) is InfoItem.Section
                val bgRes = when {
                    isFirstInGroup && isLastInGroup -> R.drawable.bg_group_item_single
                    isFirstInGroup -> R.drawable.bg_group_item_top
                    isLastInGroup -> R.drawable.bg_group_item_bottom
                    else -> R.drawable.bg_group_item_middle
                }
                val ctx = holder.itemView.context
                (holder as RowViewHolder).bind(item.title, item.value, item.iconResId, bgRes)
                holder.itemView.contentDescription =
                    ctx.getString(R.string.info_row_content_description, item.title, item.value)
                holder.itemView.findViewById<ImageView>(R.id.icon).importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        }
    }

    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.section_title)
        fun bind(text: String) {
            title.text = text
        }
    }

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val title: TextView = view.findViewById(R.id.title)
        private val value: TextView = view.findViewById(R.id.value)
        fun bind(t: String, v: CharSequence, iconResId: Int, bgResId: Int) {
            itemView.setBackgroundResource(bgResId)
            icon.setImageResource(iconResId)
            title.text = t
            value.text = v
        }
    }
}
