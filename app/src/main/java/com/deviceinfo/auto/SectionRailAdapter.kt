package com.deviceinfo.auto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SectionRailAdapter(
    private val onSectionClick: (Int) -> Unit,
) : RecyclerView.Adapter<SectionRailAdapter.ViewHolder>() {

    private var sectionKeys: List<String> = emptyList()
    private var selectedPosition = RecyclerView.NO_POSITION

    init {
        setHasStableIds(true)
    }

    fun submitSections(keys: List<String>, selected: Int) {
        sectionKeys = keys.toList()
        selectedPosition = selected.takeIf { it in sectionKeys.indices } ?: RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun select(position: Int) {
        if (position !in sectionKeys.indices || position == selectedPosition) return
        val previous = selectedPosition
        selectedPosition = position
        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
        notifyItemChanged(position)
    }

    override fun getItemId(position: Int): Long = sectionKeys[position].hashCode().toLong()

    override fun getItemCount(): Int = sectionKeys.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section_rail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sectionKey = sectionKeys[position]
        val context = holder.itemView.context
        val title = DeviceInfoUiShared.sectionTitle(context, sectionKey)
        holder.icon.setImageResource(DeviceInfoUiShared.sectionIcon(sectionKey))
        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.contentDescription = title
        ViewCompat.setTooltipText(holder.itemView, title)
        ViewCompat.setAccessibilityDelegate(
            holder.itemView,
            SectionItemAccessibilityDelegate(holder, position == selectedPosition),
        )
        holder.itemView.setOnClickListener { view ->
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            DeviceInfoHaptics.lightTabTap(view)
            onSectionClick(adapterPosition)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.section_rail_icon)
    }

    private class SectionItemAccessibilityDelegate(
        private val holder: ViewHolder,
        private val selected: Boolean,
    ) : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(
            host: View,
            info: AccessibilityNodeInfoCompat,
        ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.className = android.widget.Button::class.java.name
            info.isSelected = selected
            val navigation = host.parent as? RecyclerView ?: return
            val layoutManager = navigation.layoutManager as? LinearLayoutManager ?: return
            val position = holder.bindingAdapterPosition.takeIf {
                it != RecyclerView.NO_POSITION
            } ?: return
            val isHorizontal = layoutManager.orientation == RecyclerView.HORIZONTAL
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                if (isHorizontal) 0 else position,
                1,
                if (isHorizontal) position else 0,
                1,
                false,
                selected,
            ))
        }
    }
}
