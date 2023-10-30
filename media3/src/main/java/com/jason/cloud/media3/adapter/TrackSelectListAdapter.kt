package com.jason.cloud.media3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.jason.cloud.media3.R
import com.jason.cloud.media3.model.TrackSelectEntity

class TrackSelectListAdapter : BaseAdapter() {
    private val items = ArrayList<TrackSelectEntity>()
    private var selectedPosition = 0
    private var onSelectionChangedListener: ((Int, TrackSelectEntity) -> Unit)? = null

    private fun notifyPositionSelected(position: Int) {
        onSelectionChangedListener?.invoke(position, items[position])
    }

    fun setSelectedPosition(position: Int) {
        this.selectedPosition = position
    }

    fun setOnSelectionChangedListener(listener: (Int, TrackSelectEntity) -> Unit) {
        this.onSelectionChangedListener = listener
    }

    fun setData(data: List<TrackSelectEntity>) {
        items.clear()
        items.addAll(data)
    }

    fun getSelectedItem(): TrackSelectEntity {
        return items[selectedPosition]
    }

    fun getSelectionPosition(): Int {
        return selectedPosition
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(p0: Int): Any {
        return items[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, convertView: View?, parent: ViewGroup): View {
        val itemView: View
        val holder: ViewHolder
        if (convertView == null) {
            itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_media3_track_select, parent, false
            )
            holder = ViewHolder(itemView)
            itemView.tag = holder
        } else {
            itemView = convertView
            holder = itemView.tag as ViewHolder
        }

        holder.tvName.text = items[p0].name
        holder.tvName.isSelected = p0 == selectedPosition
        holder.checkbox.isChecked = p0 == selectedPosition
        holder.checkbox.isClickable = p0 != selectedPosition
        holder.checkbox.setOnCheckedChangeListener { buttonView, _ ->
            if (buttonView.isPressed) {
                selectedPosition = p0
                notifyPositionSelected(p0)
            }
        }
        itemView.setOnClickListener {
            selectedPosition = p0
            notifyPositionSelected(p0)
        }
        return itemView
    }

    inner class ViewHolder(itemView: View) {
        val tvName: TextView by lazy { itemView.findViewById(R.id.tv_name) }
        val checkbox: CheckBox by lazy { itemView.findViewById(R.id.checkbox) }
    }
}