package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AbsListView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Space
import android.widget.TextView
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import com.jason.cloud.media3.R
import com.jason.cloud.media3.adapter.TrackSelectListAdapter
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.model.Media3Track
import com.jason.cloud.media3.model.TrackSelectEntity
import com.jason.cloud.media3.utils.Media3VideoScaleMode

class Media3TrackView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val rvSelection: ListView by lazy { findViewById(R.id.rv_selection) }
    private val outside: View by lazy { findViewById(R.id.touch_outside) }

    private lateinit var playerView: Media3PlayerView
    private val adapter = TrackSelectListAdapter()
    private val tvTitle: TextView

    init {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.media3_track_view, this)

        val headerView = View.inflate(context, R.layout.item_media3_track_select_header, null)
        tvTitle = headerView.findViewById(R.id.tv_title)
        rvSelection.addHeaderView(headerView)
        rvSelection.addFooterView(Space(context).apply {
            layoutParams = AbsListView.LayoutParams(0, 24)
        })

        rvSelection.adapter = adapter

        outside.setOnClickListener {
            hide()
        }
    }

    fun attachPlayerView(playerView: Media3PlayerView) {
        this.playerView = playerView
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showEpisodeSelector() {
        tvTitle.setText(R.string.media3_select_episode)
        val player = playerView.internalPlayer
        var selectedPosition = 0
        val trackList = ArrayList<TrackSelectEntity>().apply {
            for (i in 0 until player.mediaItemCount) {
                val tag = player.getMediaItemAt(i).localConfiguration?.tag
                if (tag is Media3Item) {
                    if (tag.url == player.getCurrentMedia3Item()?.url) {
                        selectedPosition = i
                    }
                    add(
                        TrackSelectEntity(i,
                            buildString {
                                append(tag.title)
                                if (tag.subtitle.isNotBlank()) {
                                    append(" / ")
                                    append(tag.subtitle)
                                }
                            }
                        )
                    )
                }
            }
        }

        adapter.setSelectedPosition(selectedPosition)
        adapter.setData(trackList)
        adapter.notifyDataSetChanged()

        (outside.layoutParams as LayoutParams).weight = 0.35f
        (rvSelection.layoutParams as LayoutParams).weight = 0.65f

        rvSelection.smoothScrollToPosition(selectedPosition.coerceAtLeast(0))
        adapter.setOnSelectionChangedListener { position, item ->
            hide()
            if (selectedPosition != position) {
                playerView.seekToItem(item.tag as Int, 0)
                playerView.prepare()
                playerView.start()
            }
        }

        show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showRatioSelector() {
        tvTitle.setText(R.string.media3_select_scale_mode)
        val trackList = ArrayList<TrackSelectEntity>().apply {
            add(TrackSelectEntity(Media3VideoScaleMode.FIT, "自动适应"))
            add(TrackSelectEntity(Media3VideoScaleMode.ZOOM, "居中裁剪"))
            add(TrackSelectEntity(Media3VideoScaleMode.FILL, "填充屏幕"))
            add(TrackSelectEntity(Media3VideoScaleMode.FIXED_WIDTH, "宽度固定"))
            add(TrackSelectEntity(Media3VideoScaleMode.FIXED_HEIGHT, "高度固定"))
        }

        val selectedPosition = trackList.indexOfFirst {
            it.tag == playerView.getScaleMode()
        }

        adapter.setSelectedPosition(selectedPosition)
        adapter.setData(trackList)
        adapter.notifyDataSetChanged()

        (outside.layoutParams as LayoutParams).weight = 0.25f
        (rvSelection.layoutParams as LayoutParams).weight = 0.75f

        rvSelection.smoothScrollToPosition(selectedPosition.coerceAtLeast(0))
        adapter.setOnSelectionChangedListener { position, item ->
            hide()
            if (selectedPosition != position) {
                playerView.setScaleMode(item.tag as Media3VideoScaleMode)
            }
        }

        show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showSpeedSelector() {
        tvTitle.setText(R.string.media3_select_play_speed)
        val trackList = ArrayList<TrackSelectEntity>().apply {
            add(TrackSelectEntity(0.25f, "×0.25"))
            add(TrackSelectEntity(0.5f, "×0.5"))
            add(TrackSelectEntity(1.0f, "×1.0"))
            add(TrackSelectEntity(1.25f, "×1.25"))
            add(TrackSelectEntity(1.5f, "×1.5"))
            add(TrackSelectEntity(2.0f, "×2.0"))
            add(TrackSelectEntity(3.0f, "×3.0"))
            add(TrackSelectEntity(4.0f, "×4.0"))
            add(TrackSelectEntity(8.0f, "×8.0"))
        }

        val selectedPosition = trackList.indexOfFirst {
            it.tag as Float == playerView.internalPlayer.playbackParameters.speed
        }

        adapter.setSelectedPosition(selectedPosition)
        adapter.setData(trackList)
        adapter.notifyDataSetChanged()

        (outside.layoutParams as LayoutParams).weight = 0.2f
        (rvSelection.layoutParams as LayoutParams).weight = 0.8f

        rvSelection.smoothScrollToPosition(selectedPosition.coerceAtLeast(0))
        adapter.setOnSelectionChangedListener { position, item ->
            hide()
            if (selectedPosition != position) {
                playerView.internalPlayer.playbackParameters = PlaybackParameters(item.tag as Float)
            }
        }

        show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showAudioTrackSelector() {
        tvTitle.setText(R.string.media3_select_audio_track)
        val audioTracks = playerView.internalPlayer.getTrackList(context, C.TRACK_TYPE_AUDIO)
        val selectedPosition = audioTracks.indexOfFirst { it.selected }
        val trackList = audioTracks.map { TrackSelectEntity(it, it.name) }

        adapter.setSelectedPosition(selectedPosition)
        adapter.setData(trackList)
        adapter.notifyDataSetChanged()

        (outside.layoutParams as LayoutParams).weight = 0.3f
        (rvSelection.layoutParams as LayoutParams).weight = 0.7f

        rvSelection.smoothScrollToPosition(selectedPosition.coerceAtLeast(0))
        adapter.setOnSelectionChangedListener { position, item ->
            hide()
            if (selectedPosition != position) {
                playerView.internalPlayer.selectTrack(item.tag as Media3Track)
            }
        }
        show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showSubtitleSelector() {
        tvTitle.setText(R.string.media3_select_subtitles)
        val subtitles = playerView.internalPlayer.getTrackList(context, C.TRACK_TYPE_TEXT)
        val selectedPosition = subtitles.indexOfFirst { it.selected }
        val trackList = subtitles.map { TrackSelectEntity(it, it.name) }

        adapter.setSelectedPosition(selectedPosition)
        adapter.setData(trackList)
        adapter.notifyDataSetChanged()

        (outside.layoutParams as LayoutParams).weight = 0.3f
        (rvSelection.layoutParams as LayoutParams).weight = 0.7f

        rvSelection.smoothScrollToPosition(selectedPosition.coerceAtLeast(0))
        adapter.setOnSelectionChangedListener { position, item ->
            hide()
            if (selectedPosition != position) {
                playerView.internalPlayer.selectTrack(item.tag as Media3Track)
            }
        }

        show()
    }

    fun show() {
        playerView.hideControlView {
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(240).start()
        }

        playerView.addOnStateChangeListener(object : OnStateChangeListener {
            override fun onStateChanged(state: Int) {
                hide()
            }
        })
    }

    fun hide() {
        if (visibility != View.GONE) {
            alpha = 1f
            animate().alpha(0f).setDuration(240).withEndAction {
                visibility = View.GONE
            }
        }
    }
}