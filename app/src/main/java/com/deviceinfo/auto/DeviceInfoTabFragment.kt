package com.deviceinfo.auto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeviceInfoTabFragment : Fragment() {

    interface Host {
        fun onTabListReady(sectionKey: String, list: RecyclerView)
        fun tabRecycledViewPool(): RecyclerView.RecycledViewPool
    }

    private var listAdapter: InfoListAdapter? = null
    private var listView: RecyclerView? = null

    private val sectionKey: String
        get() = requireArguments().getString(ARG_SECTION_KEY).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_device_info_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = view.findViewById<RecyclerView>(R.id.list)
        listView = list
        if (listAdapter == null) {
            list.setHasFixedSize(true)
            list.layoutManager = LinearLayoutManager(requireContext())
            list.itemAnimator = null
            (activity as? Host)?.tabRecycledViewPool()?.let { list.setRecycledViewPool(it) }
            listAdapter = InfoListAdapter()
            list.adapter = listAdapter
        } else {
            list.adapter = listAdapter
        }
        (activity as? Host)?.onTabListReady(sectionKey, list)
    }

    fun bindItems(items: List<InfoItem>) {
        listAdapter?.submitList(items.toList())
    }

    fun bindRows(rows: List<InfoItem.Row>) {
        bindItems(rows)
    }

    fun recyclerView(): RecyclerView? = listView

    companion object {
        private const val ARG_SECTION_KEY = "section_key"

        fun newInstance(sectionKey: String): DeviceInfoTabFragment =
            DeviceInfoTabFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SECTION_KEY, sectionKey)
                }
            }
    }
}
