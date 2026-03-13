package com.deviceinfo.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val items: MutableList<InfoItem> = mutableListOf()
    private var adapter: InfoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContentView(R.layout.activity_main)
        applyStatusBarInset()
        showDeviceInfoList()
    }

    private fun applyStatusBarInset() {
        val root = findViewById<View>(R.id.root)
        val initialPaddingTop = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sysBars = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(v.paddingLeft, initialPaddingTop + sysBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        registerBatteryReceiver()
        handler.post(memoryRefreshRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(memoryRefreshRunnable)
        unregisterReceiver(batteryReceiver)
        super.onPause()
    }

    private fun showDeviceInfoList() {
        val info = DeviceInfoProvider.get(this)

        fun healthIconRes(health: String): Int =
            when (health) {
                "Good" -> R.drawable.ic_row_health
                "Unknown" -> R.drawable.ic_row_unknown
                else -> R.drawable.ic_row_error
            }

        fun levelIconRes(level: Int, isPowerSaveMode: Boolean): Int =
            when {
                level < 13 -> R.drawable.ic_row_battery_0   // Step 1: 0–12.5%
                level < 25 -> R.drawable.ic_row_battery_1   // Step 2: 12.5–25%
                level < 38 -> R.drawable.ic_row_battery_2   // Step 3: 25–37.5%
                level < 50 -> R.drawable.ic_row_battery_3   // Step 4: 37.5–50%
                level < 63 -> R.drawable.ic_row_battery_4   // Step 5: 50–62.5%
                level < 75 -> R.drawable.ic_row_battery_5   // Step 6: 62.5–75%
                level < 88 -> R.drawable.ic_row_battery_6   // Step 7: 75–87.5%
                else -> R.drawable.ic_row_battery_full      // Step 8: 87.5–100%
            }

        // Rough ranges based on typical Li-ion battery guidance:
        // < 10°C: cold / reduced performance
        // 10–35°C: normal
        // 35–45°C: warm (still safe but high)
        // > 45°C: hot / risky
        fun temperatureIconRes(tempC: Float): Int =
            when {
                tempC < 10f -> R.drawable.ic_row_temp_cold
                tempC >= 45f -> R.drawable.ic_row_temp_hot
                tempC >= 35f -> R.drawable.ic_row_temp_warm
                else -> R.drawable.ic_row_temp_normal
            }

        fun levelText(info: DeviceInfo): String {
            val base = "${info.batteryLevel}% • ${info.chargingStatus}"
            return if (info.isPowerSaveMode) "$base • Saver on" else base
        }

        if (adapter == null) {
            // Build static + initial dynamic content once
            items.clear()
            items += listOf(
                InfoItem.Section("Battery"),
                InfoItem.Row("Level", levelText(info), levelIconRes(info.batteryLevel, info.isPowerSaveMode)),
                InfoItem.Row("Health", info.health, healthIconRes(info.health)),
                InfoItem.Row("Temperature", String.format("%.1f °C", info.batteryTemperatureCelsius), temperatureIconRes(info.batteryTemperatureCelsius)),
                InfoItem.Row("Voltage", String.format("%.2f V", info.batteryVoltageV), R.drawable.ic_row_voltage),
                InfoItem.Section("Storage & memory"),
                InfoItem.Row("Storage", info.storageSummary, R.drawable.ic_row_storage),
                InfoItem.Row("RAM", info.ramSummary, R.drawable.ic_row_memory),
                InfoItem.Section("Device"),
                InfoItem.Row("Manufacturer", info.manufacturer, R.drawable.ic_row_manufacturer),
                InfoItem.Row("Model", info.model, R.drawable.ic_row_model),
                InfoItem.Row("System", info.androidVersion, R.drawable.ic_row_android),
                InfoItem.Row("Security patch", info.securityPatch, R.drawable.ic_row_shield),
                InfoItem.Row("Build", info.buildId, R.drawable.ic_row_build)
            )
            val list = findViewById<RecyclerView>(R.id.list)
            list.layoutManager = LinearLayoutManager(this)
            list.itemAnimator = null
            adapter = InfoAdapter(items)
            list.adapter = adapter
        } else {
            // Use full update path only when explicitly requested
            updateDynamic(info, updateBattery = true, updateMemory = true)
        }
    }

    private fun updateDynamic(info: DeviceInfo, updateBattery: Boolean, updateMemory: Boolean) {
        if (adapter == null) return

        fun healthIconRes(health: String): Int =
            when (health) {
                "Good" -> R.drawable.ic_row_health
                "Unknown" -> R.drawable.ic_row_unknown
                else -> R.drawable.ic_row_error
            }

        fun levelIconRes(level: Int, isPowerSaveMode: Boolean): Int =
            when {
                level < 13 -> R.drawable.ic_row_battery_0
                level < 25 -> R.drawable.ic_row_battery_1
                level < 38 -> R.drawable.ic_row_battery_2
                level < 50 -> R.drawable.ic_row_battery_3
                level < 63 -> R.drawable.ic_row_battery_4
                level < 75 -> R.drawable.ic_row_battery_5
                level < 88 -> R.drawable.ic_row_battery_6
                else -> R.drawable.ic_row_battery_full
            }

        fun temperatureIconRes(tempC: Float): Int =
            when {
                tempC < 10f -> R.drawable.ic_row_temp_cold
                tempC >= 45f -> R.drawable.ic_row_temp_hot
                tempC >= 35f -> R.drawable.ic_row_temp_warm
                else -> R.drawable.ic_row_temp_normal
            }

        fun levelText(info: DeviceInfo): String {
            val base = "${info.batteryLevel}% • ${info.chargingStatus}"
            return if (info.isPowerSaveMode) "$base • Saver on" else base
        }

        for ((index, item) in items.withIndex()) {
            if (item is InfoItem.Row) {
                when (item.title) {
                    "Level", "Health", "Temperature", "Voltage" ->
                        if (updateBattery) {
                            when (item.title) {
                                "Level" -> {
                                    items[index] = item.copy(
                                        value = levelText(info),
                                        iconResId = levelIconRes(info.batteryLevel, info.isPowerSaveMode)
                                    )
                                }

                                "Health" -> {
                                    items[index] = item.copy(
                                        value = info.health,
                                        iconResId = healthIconRes(info.health)
                                    )
                                }

                                "Temperature" -> {
                                    items[index] = item.copy(
                                        value = String.format("%.1f °C", info.batteryTemperatureCelsius),
                                        iconResId = temperatureIconRes(info.batteryTemperatureCelsius)
                                    )
                                }

                                "Voltage" -> {
                                    items[index] = item.copy(
                                        value = String.format("%.2f V", info.batteryVoltageV),
                                        iconResId = R.drawable.ic_row_voltage
                                    )
                                }
                            }
                            adapter?.notifyItemChanged(index)
                        }

                    "Storage", "RAM" ->
                        if (updateMemory) {
                            when (item.title) {
                                "Storage" -> {
                                    items[index] = item.copy(
                                        value = info.storageSummary,
                                        iconResId = R.drawable.ic_row_storage
                                    )
                                }

                                "RAM" -> {
                                    items[index] = item.copy(
                                        value = info.ramSummary,
                                        iconResId = R.drawable.ic_row_memory
                                    )
                                }
                            }
                            adapter?.notifyItemChanged(index)
                        }
                }
            }
        }
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(batteryReceiver, filter)
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED,
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val info = DeviceInfoProvider.get(this@MainActivity)
                    updateDynamic(info, updateBattery = true, updateMemory = false)
                }
            }
        }
    }

    private val memoryRefreshRunnable = object : Runnable {
        override fun run() {
            val info = DeviceInfoProvider.get(this@MainActivity)
            updateDynamic(info, updateBattery = false, updateMemory = true)
            handler.postDelayed(this, MEMORY_REFRESH_MS)
        }
    }

    companion object {
        private const val MEMORY_REFRESH_MS = 5_000L
    }
}

private sealed class InfoItem {
    data class Section(val title: String) : InfoItem()
    data class Row(val title: String, val value: String, val iconResId: Int) : InfoItem()
}

private class InfoAdapter(
    private val items: List<InfoItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SECTION = 0
        const val VIEW_TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
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
        when (val item = items[position]) {
            is InfoItem.Section -> (holder as SectionViewHolder).bind(item.title)
            is InfoItem.Row -> {
                val isFirstInGroup = position > 0 && items[position - 1] is InfoItem.Section
                val isLastInGroup = position == items.size - 1 || items[position + 1] is InfoItem.Section
                val bgRes = when {
                    isFirstInGroup && isLastInGroup -> R.drawable.bg_group_item_single
                    isFirstInGroup -> R.drawable.bg_group_item_top
                    isLastInGroup -> R.drawable.bg_group_item_bottom
                    else -> R.drawable.bg_group_item_middle
                }
                (holder as RowViewHolder).bind(item.title, item.value, item.iconResId, bgRes)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.section_title)
        fun bind(text: String) { title.text = text }
    }

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val title: TextView = view.findViewById(R.id.title)
        private val value: TextView = view.findViewById(R.id.value)
        fun bind(t: String, v: String, iconResId: Int, bgResId: Int) {
            itemView.setBackgroundResource(bgResId)
            icon.setImageResource(iconResId)
            title.text = t
            value.text = v
        }
    }
}
