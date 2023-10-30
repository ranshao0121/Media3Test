package com.jason.cloud.media3.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jason.cloud.media3.R
import com.jason.cloud.media3.adapter.TrackSelectDialogAdapter
import com.jason.cloud.media3.model.TrackSelectEntity

class TrackSelectDialog(context: Context) {
    private val view = View.inflate(context, R.layout.media3_view_track_select_dialog, null)

    private var lastSelection = 0
    private val adapter = TrackSelectDialogAdapter()
    private val builder = MaterialAlertDialogBuilder(context).setView(view)
    private val rvSelection by lazy { view.findViewById<RecyclerView>(R.id.rv_selection) }

    init {
        rvSelection.adapter = adapter
        adapter.setOnSelectionChangedListener { i, _ ->
            doSelection(i)
        }
    }

    private fun doSelection(position: Int) {
        Log.e("TrackSelectDialog", "doSelection: $position")
        for (i in 0 until adapter.itemCount) {
            val holder = rvSelection?.findViewHolderForAdapterPosition(i)
            if (holder is TrackSelectDialogAdapter.ViewHolder) {
                holder.checkbox.isChecked = i == position
            }
        }
    }

    fun setTitle(title: CharSequence?) {
        builder.setTitle(title)
    }

    fun setTitle(titleId: Int) {
        builder.setTitle(titleId)
    }

    fun setTitle(title: CharSequence): TrackSelectDialog {
        builder.setTitle(title)
        return this
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionData(list: List<TrackSelectEntity>, selected: Int): TrackSelectDialog {
        lastSelection = selected
        adapter.setSelectedPosition(selected)
        adapter.setData(list)
        adapter.notifyDataSetChanged()
        rvSelection?.scrollToPosition(selected)
        return this
    }

    fun onPositive(
        text: CharSequence? = null,
        block: ((selection: TrackSelectEntity) -> Unit)? = null
    ): TrackSelectDialog {
        builder.setPositiveButton(text) { _, _ ->
            if (lastSelection != adapter.getSelectionPosition()) {
                block?.invoke(adapter.getSelectedItem())
            }
        }
        return this
    }

    fun onNegative(text: CharSequence? = null, block: (() -> Unit)? = null): TrackSelectDialog {
        builder.setNegativeButton(text) { _, _ ->
            block?.invoke()
        }
        return this
    }

    fun onNeutral(text: CharSequence? = null, block: (() -> Unit)? = null): TrackSelectDialog {
        builder.setNeutralButton(text) { _, _ ->
            block?.invoke()
        }
        return this
    }

    fun show() {
        val dialog = builder.create()
        dialog.setOnShowListener { onShowListener?.invoke() }
        dialog.show()
    }

    private var onShowListener: (() -> Unit?)? = null
    fun setOnShowListener(onShowListener: () -> Unit?) {
        this.onShowListener = onShowListener
    }

    fun setOnDismissListener(function: () -> Unit?) {
        builder.setOnDismissListener {
            function.invoke()
        }
    }
}