package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextClock
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.Util
import com.jason.cloud.media3.R
import com.jason.cloud.media3.dialog.TrackSelectDialog
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.model.Media3Track
import com.jason.cloud.media3.model.TrackSelectEntity
import com.jason.cloud.media3.utils.CutoutArea
import com.jason.cloud.media3.utils.CutoutUtil
import com.jason.cloud.media3.utils.Media3VideoScaleMode
import com.jason.cloud.media3.utils.PlayerUtils
import com.jason.cloud.media3.utils.TimeUtil
import com.jason.cloud.media3.utils.getCurrentOrientation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Formatter
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint(
    "UnsafeOptInUsageError",
    "ClickableViewAccessibility",
    "SourceLockedOrientationActivity",
    "SetTextI18n"
)
class Media3PlayerControlView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {
    lateinit var statusView: View

    lateinit var ibBackspace: ImageButton
    lateinit var tvTitle: MarqueeTextView
    lateinit var ivBattery: ImageView
    lateinit var tvBattery: TextView
    lateinit var tvClock: TextClock
    lateinit var timeLayout: LinearLayout
    lateinit var titleBarLayout: LinearLayout
    private var showTitleBarInPortrait = false

    lateinit var ibLock: ImageButton
    lateinit var tvBottomTitle: TextView
    lateinit var tvBottomSubtitle: TextView
    lateinit var bottomTitle: LinearLayout

    lateinit var tvPosition: TextView
    lateinit var tvDuration: TextView
    lateinit var tvEndTime: TextView
    lateinit var videoSeekBar: SeekBar
    lateinit var bottomSeekLayout: LinearLayout

    lateinit var ibPlay: ImageButton
    lateinit var ibNext: ImageButton
    lateinit var ibList: ImageButton
    lateinit var ibSubtitle: ImageButton
    lateinit var ibAudioTrack: ImageButton
    lateinit var ibPlaySpeed: ImageButton
    lateinit var ibRatio: ImageButton
    lateinit var ibRotation: ImageButton
    lateinit var bottomBar: LinearLayout
    lateinit var ibLargerLock: ImageButton

    private lateinit var batteryReceiver: BatteryReceiver

    private var playerView: Media3PlayerView? = null
    lateinit var tvSourceInfo: TextView

    private val scope = CoroutineScope(Dispatchers.Main)
    private var videoPositionJob: Job? = null

    private var isBatteryReceiverRegister = false
    private var onBackPressedListener: (() -> Unit)? = null
    private var cutoutArea: CutoutArea = CutoutArea.UNKNOWN
    internal var isDragging = false

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isInEditMode) return
        if (isBatteryReceiverRegister) {
            context.unregisterReceiver(batteryReceiver)
            isBatteryReceiverRegister = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode) return
        if (!isBatteryReceiverRegister) {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            isBatteryReceiverRegister = true
        }
    }

    private class BatteryReceiver(val imageView: ImageView, val textView: TextView) :
        BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            imageView.drawable.level = extras.getInt("level") * 100 / extras.getInt("scale")
            textView.text = (extras.getInt("level") * 100 / extras.getInt("scale")).toString() + "%"
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.media3_player_control_view, this)
        if (isInEditMode.not()) {
            bindViews()

            setOnTouchListener { _, _ ->
                false
            }
            batteryReceiver = BatteryReceiver(ivBattery, tvBattery)
            ibBackspace.setOnClickListener {
                onBackPressedListener?.invoke()
            }
            ibList.setOnClickListener {
                showEpisodeSelector()
            }
            ibSubtitle.isVisible = false
            ibSubtitle.setOnClickListener {
                showSubtitleSelector()
            }
            ibAudioTrack.isVisible = false
            ibAudioTrack.setOnClickListener {
                showAudioTrackSelector()
            }
            ibPlaySpeed.setOnClickListener {
                showSpeedSelector()
            }
            ibRatio.setOnClickListener {
                showRatioSelector()
            }
            ibLock.setOnClickListener {
                toggleLockState()
            }
            ibLargerLock.setOnClickListener {
                toggleLockState()
            }
            ibRotation.setOnClickListener {
                toggleFullScreen()
            }
        }
    }

    private fun bindViews() {
        statusView = findViewById(R.id.status_view)
        statusView.isVisible = false

        ibBackspace = findViewById(R.id.media3_ib_backspace)
        tvTitle = findViewById(R.id.media3_tv_title)
        ivBattery = findViewById(R.id.media3_iv_battery)
        tvBattery = findViewById(R.id.media3_tv_battery)
        tvClock = findViewById(R.id.media3_tv_clock)
        timeLayout = findViewById(R.id.time_layout)
        titleBarLayout = findViewById(R.id.media3_title_bar)

        ibLock = findViewById(R.id.media3_ib_lock)
        tvBottomTitle = findViewById(R.id.media3_tv_bottom_title)
        tvBottomSubtitle = findViewById(R.id.media3_tv_bottom_subtitle)
        bottomTitle = findViewById(R.id.media3_bottom_title)

        tvPosition = findViewById(R.id.media3_tv_video_position)
        tvDuration = findViewById(R.id.media3_tv_video_duration)
        tvEndTime = findViewById(R.id.media3_tv_end_time)
        videoSeekBar = findViewById(R.id.media3_video_seek_bar)
        bottomSeekLayout = findViewById(R.id.media3_bottom_seek_layout)

        ibPlay = findViewById(R.id.media3_ib_play)
        ibNext = findViewById(R.id.media3_ib_next)
        ibList = findViewById(R.id.media3_ib_list)
        ibSubtitle = findViewById(R.id.media3_ib_subtitle)
        ibAudioTrack = findViewById(R.id.media3_ib_audio_track)
        ibPlaySpeed = findViewById(R.id.media3_ib_play_speed)
        ibRatio = findViewById(R.id.media3_ib_ratio)
        ibRotation = findViewById(R.id.media3_ib_rotation)
        bottomBar = findViewById(R.id.media3_bottom_bar)
        ibLargerLock = findViewById(R.id.media3_ib_larger_lock)
        tvSourceInfo = findViewById(R.id.tv_source_info)

        val orientation = context.getCurrentOrientation()
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            statusView.isVisible = true
            tvEndTime.isVisible = false
            if (showTitleBarInPortrait) {
                titleBarLayout.visibility = View.VISIBLE
                bottomTitle.visibility = View.GONE
            } else {
                titleBarLayout.visibility = View.INVISIBLE
                bottomTitle.visibility = View.VISIBLE
            }
        }
    }

    fun setCutoutArea(cutoutRect: Rect) {
        if (cutoutRect.isEmpty) {
            this.cutoutArea = CutoutArea.UNKNOWN
        } else {
            val width = cutoutRect.right - cutoutRect.left
            val center = cutoutRect.right - width / 2
            if (cutoutRect.right < context.resources.displayMetrics.widthPixels / 2) {
                this.cutoutArea = CutoutArea.LEFT
            } else if (cutoutRect.left > context.resources.displayMetrics.widthPixels / 2) {
                this.cutoutArea = CutoutArea.RIGHT
            } else if (center == context.resources.displayMetrics.widthPixels / 2) {
                this.cutoutArea = CutoutArea.CENTER
            } else {
                this.cutoutArea = CutoutArea.UNKNOWN
            }
        }
    }

    private fun toggleFullScreen() {
        if (context.getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            ibRotation.isEnabled = false
            ibRotation.animate().rotationBy(-360f).setDuration(240).withEndAction {
                ibRotation.isEnabled = true
                playerView?.cancelFullScreen()
            }
        } else {
            ibRotation.isEnabled = false
            ibRotation.animate().rotationBy(360f).setDuration(240).withEndAction {
                ibRotation.isEnabled = true
                playerView?.startFullScreen()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val statusHeight = PlayerUtils.getStatusBarHeightPortrait(context).toInt()
        val newOrientation = newConfig.getCurrentOrientation()
        statusView.layoutParams.height = statusHeight
        statusView.isVisible = newOrientation == Configuration.ORIENTATION_PORTRAIT

        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            tvEndTime.isVisible = true
            bottomTitle.visibility = View.GONE
            titleBarLayout.visibility = View.VISIBLE
        }
        if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            tvEndTime.isVisible = false
            if (showTitleBarInPortrait) {
                titleBarLayout.visibility = View.VISIBLE
                bottomTitle.visibility = View.GONE
            } else {
                titleBarLayout.visibility = View.INVISIBLE
                bottomTitle.visibility = View.VISIBLE
            }
        }

        val activity = PlayerUtils.scanForActivity(context) ?: return
        if (CutoutUtil.hasCutout(activity).not()) { //没有刘海的情况下
            if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) { //非全屏情况下
                titleBarLayout.setPadding(0, 0, 0, 0)
                bottomBar.setPadding(0, 0, 0, 0)
            } else {
                titleBarLayout.setPadding(0, 0, 0, 0)
                bottomBar.setPadding(0, 0, 0, 0)
            }
        } else { //有刘海
            if (newOrientation == Configuration.ORIENTATION_PORTRAIT) { //非全屏情况下
                titleBarLayout.setPadding(0, 0, 0, 0)
                bottomBar.setPadding(0, 0, 0, 0)
            } else {
                if (cutoutArea == CutoutArea.CENTER) {
                    titleBarLayout.setPadding(0, 0, 0, 0)
                    bottomBar.setPadding(0, 0, 0, 0)
                } else {
                    titleBarLayout.setPadding(statusHeight, 0, 0, 0)
                    bottomBar.setPadding(statusHeight, 0, 0, 0)
                }
            }
        }
    }

    private fun toggleLockState() {
        if (playerView?.isLocked != true) {
            playerView?.isLocked = true
            ibLargerLock.isVisible = true

            ibLock.isSelected = true
            ibLock.animate().alpha(0f).setDuration(240).withEndAction {
                ibLock.visibility = View.INVISIBLE
                ibLock.alpha = 1f
            }

            hideTitleBar(true)
            hideBottomBar(true)
        } else {
            playerView?.isLocked = false
            ibLargerLock.isVisible = false

            if (context.getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
                showTitleBar(isVisible)
            }

            if (isVisible.not()) {
                ibLock.isSelected = false
                ibLock.visibility = View.VISIBLE
                hideBottomBar(false)
            } else {
                ibLock.isSelected = false
                ibLock.visibility = View.VISIBLE
                ibLock.alpha = 0f
                ibLock.animate().alpha(1f).setDuration(240).withEndAction {
                    ibLock.visibility = View.VISIBLE
                }
                showBottomBar(true)
            }
        }
    }

    fun attachPlayerView(playerView: Media3PlayerView) {
        this.playerView = playerView
        this.playerView?.internalPlayer?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                updateNextButtonAction()
                val tag = mediaItem?.localConfiguration?.tag
                if (tag is Media3Item) {
                    Log.e("Media3PlayerControlView", "onMediaItemTransition: ${tag.title}")
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                super.onTracksChanged(tracks)
                ibSubtitle.isVisible =
                    playerView.internalPlayer.getTrackList(context, C.TRACK_TYPE_TEXT).isNotEmpty()
                ibAudioTrack.isVisible =
                    playerView.internalPlayer.getTrackList(context, C.TRACK_TYPE_AUDIO).size > 1
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                ibPlay.isSelected = isPlaying
                ibPlay.setOnClickListener {
                    if (isPlaying) {
                        playerView.pause()
                    } else {
                        playerView.start()
                    }
                }

                val formatBuilder = StringBuilder()
                val formatter = Formatter(formatBuilder, Locale.getDefault())
                if (isPlaying.not()) {
                    videoPositionJob?.cancel()
                    videoSeekBar.setOnSeekBarChangeListener(null)
                } else {
                    startVideoPositionObserver()
                    val player = playerView.internalPlayer
                    videoSeekBar.isEnabled = player.duration > 0
                    videoSeekBar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar, progress: Int, fromUser: Boolean
                        ) {
                            if (fromUser) {
                                tvPosition.text = Util.getStringForTime(
                                    formatBuilder,
                                    formatter,
                                    player.duration * progress / seekBar.max
                                )
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            isDragging = true
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            isDragging = false
                            if (seekBar.progress == seekBar.max) {
                                player.seekTo(player.duration - 1)
                            } else {
                                var newPosition = player.duration * seekBar.progress / seekBar.max
                                if (newPosition == player.duration) {
                                    newPosition = player.duration - 1
                                }
                                player.seekTo(newPosition)
                            }
                        }
                    })
                }
            }
        })
        updateNextButtonAction()
    }

    private fun startVideoPositionObserver() {
        val player = playerView?.internalPlayer ?: return
        videoPositionJob?.cancel()
        videoPositionJob = scope.launch {
            var progress: Float
            var bufferedProgress: Float
            val formatBuilder = StringBuilder()
            val formatter = Formatter(formatBuilder, Locale.getDefault())
            while (isActive) {
                delay(500)
                if (isDragging) {
                    continue
                }
                progress = player.currentPosition / player.duration.toFloat() * 100
                bufferedProgress =
                    player.contentBufferedPosition / player.contentDuration.toFloat() * 100
                videoSeekBar.max = 100
                videoSeekBar.progress = progress.roundToInt()
                videoSeekBar.secondaryProgress = bufferedProgress.roundToInt()
                tvPosition.text = Util.getStringForTime(
                    formatBuilder, formatter, player.currentPosition
                )
                tvDuration.text = Util.getStringForTime(
                    formatBuilder, formatter, player.duration
                )

                tvEndTime.text = TimeUtil.getEndTimeString(
                    player.duration - player.currentPosition,
                    "将于 %s 播放完毕"
                )
            }
        }
    }

    fun show(block: (() -> Unit)? = null) {
        if (isVisible.not()) {
            alpha = 0f
            isVisible = true
            animate().alpha(1f).setDuration(240).withEndAction {
                block?.invoke()
            }
        }
    }

    fun hide(block: (() -> Unit)? = null) {
        if (isVisible) {
            alpha = 1f
            animate().alpha(0f).setDuration(240).withEndAction {
                isVisible = false
                block?.invoke()
            }
        }
    }

    fun hideTitleBar(animated: Boolean = false) {
        if (titleBarLayout.visibility != View.INVISIBLE) {
            if (animated.not()) {
                titleBarLayout.visibility = View.INVISIBLE
                titleBarLayout.alpha = 1f
            } else {
                titleBarLayout.animate().alpha(0f).setDuration(240).withEndAction {
                    titleBarLayout.visibility = View.INVISIBLE
                    titleBarLayout.alpha = 1f
                }
            }
        }
    }

    fun setShowTitleBarInPortrait(show: Boolean) {
        showTitleBarInPortrait = show
        if (show) {
            titleBarLayout.visibility = View.VISIBLE
            bottomTitle.visibility = View.GONE
        } else {
            titleBarLayout.visibility = View.INVISIBLE
            bottomTitle.visibility = View.VISIBLE
        }
    }

    fun showTitleBar(animated: Boolean = false) {
        if (titleBarLayout.visibility != View.VISIBLE) {
            titleBarLayout.visibility = View.VISIBLE
            titleBarLayout.alpha = 0f
            if (animated) {
                titleBarLayout.animate().alpha(1f).setDuration(240).start()
            }
        }
    }

    fun hideBottomBar(animated: Boolean = false) {
        if (bottomBar.visibility != View.INVISIBLE) {
            if (animated.not()) {
                bottomBar.visibility = View.INVISIBLE
                bottomBar.alpha = 1f
            } else {
                bottomBar.animate().alpha(0f).setDuration(240).withEndAction {
                    bottomBar.visibility = View.INVISIBLE
                    bottomBar.alpha = 1f
                }
            }
        }
    }

    fun showBottomBar(animated: Boolean = false) {
        if (bottomBar.visibility != View.VISIBLE) {
            bottomBar.visibility = View.VISIBLE
            bottomBar.alpha = 0f
            if (animated) {
                bottomBar.animate().alpha(1f).setDuration(240).start()
            }
        }
    }

    fun onBackPressed(listener: () -> Unit) {
        onBackPressedListener = listener
    }

    @SuppressLint("SetTextI18n")
    private fun updateNextButtonAction() {
        val player = playerView?.internalPlayer ?: return
        ibList.isVisible = player.mediaItemCount > 1
        if (player.hasNextMediaItem().not()) {
            ibNext.alpha = 0.5f
            ibNext.isEnabled = false
        } else {
            ibNext.alpha = 1.0f
            ibNext.isEnabled = true
            ibNext.setOnClickListener {
                playerView?.seekToNext()
                playerView?.prepare()
                playerView?.start()
            }
        }

        val tag = player.currentMediaItem?.localConfiguration?.tag
        if (tag is Media3Item) {
            tvBottomTitle.text = tag.title
            tvBottomTitle.isVisible = tag.title.isNotBlank()
            tvBottomSubtitle.text = tag.subtitle
            tvBottomSubtitle.isVisible = tag.subtitle.isNotBlank()
            tvTitle.text =
                tag.title + if (tag.subtitle.isNotBlank()) " / " + tag.subtitle else ""
        }
    }

    private fun showSubtitleSelector() {
        if (context.getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            playerView?.trackView?.showSubtitleSelector()
            return
        }
        val player = playerView?.internalPlayer ?: return
        val subtitles = player.getTrackList(context, C.TRACK_TYPE_TEXT)
        val selectedPosition = subtitles.indexOfFirst { it.selected }
        val list = subtitles.map { TrackSelectEntity(it, it.name) }
        TrackSelectDialog(context).apply {
            setTitle("字幕选择")
            setOnShowListener { playerView?.pause() }
            setOnDismissListener { playerView?.start() }
            setSelectionData(list, selectedPosition)
            onNegative("取消")
            onPositive("确定") {
                player.selectTrack(it.tag as Media3Track)
            }
            show()
        }
    }

    private fun showAudioTrackSelector() {
        if (context.getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            playerView?.trackView?.showAudioTrackSelector()
            return
        }
        val player = playerView?.internalPlayer ?: return
        val audioTracks = player.getTrackList(context, C.TRACK_TYPE_AUDIO)
        val selectedPosition = audioTracks.indexOfFirst { it.selected }
        val list = audioTracks.map { TrackSelectEntity(it, it.name) }
        TrackSelectDialog(context).apply {
            setTitle("音轨选择")
            setOnShowListener { playerView?.pause() }
            setOnDismissListener { playerView?.start() }
            setSelectionData(list, selectedPosition)
            onNegative("取消")
            onPositive("确定") {
                player.selectTrack(it.tag as Media3Track)
            }
            show()
        }
    }

    private fun showSpeedSelector() {
        if (context.getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            playerView?.trackView?.showSpeedSelector()
            return
        }
        val player = playerView?.internalPlayer ?: return
        val list = ArrayList<TrackSelectEntity>().apply {
            add(TrackSelectEntity(0.25f, "0.25x"))
            add(TrackSelectEntity(0.5f, "0.5x"))
            add(TrackSelectEntity(1.0f, "1.0x"))
            add(TrackSelectEntity(1.25f, "1.25x"))
            add(TrackSelectEntity(1.5f, "1.5x"))
            add(TrackSelectEntity(2.0f, "2.0x"))
            add(TrackSelectEntity(3.0f, "3.0x"))
            add(TrackSelectEntity(4.0f, "4.0x"))
            add(TrackSelectEntity(8.0f, "8.0x"))
        }

        val selectedPosition = list.indexOfFirst {
            it.tag as Float == player.playbackParameters.speed
        }

        TrackSelectDialog(context).apply {
            setTitle("倍速播放")
            setOnShowListener { playerView?.pause() }
            setOnDismissListener { playerView?.start() }
            setSelectionData(list, selectedPosition)
            onNegative("取消")
            onPositive("确定") {
                player.playbackParameters = PlaybackParameters(it.tag as Float)
            }
            show()
        }
    }

    private fun showRatioSelector() {
        if (context.getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            playerView?.trackView?.showRatioSelector()
            return
        }

        playerView?.let { view ->
            val list = ArrayList<TrackSelectEntity>().apply {
                add(TrackSelectEntity(Media3VideoScaleMode.FIT, "自动适应"))
                add(TrackSelectEntity(Media3VideoScaleMode.ZOOM, "居中裁剪"))
                add(TrackSelectEntity(Media3VideoScaleMode.FILL, "填充屏幕"))
                add(TrackSelectEntity(Media3VideoScaleMode.FIXED_WIDTH, "宽度固定"))
                add(TrackSelectEntity(Media3VideoScaleMode.FIXED_HEIGHT, "高度固定"))
            }

            val selectedPosition = list.indexOfFirst {
                it.tag == view.getScaleMode()
            }

            TrackSelectDialog(context).apply {
                setTitle("画面缩放")
                setOnShowListener { view.pause() }
                setOnDismissListener { view.start() }
                setSelectionData(list, selectedPosition)
                onNegative("取消")
                onPositive("确定") {
                    view.setScaleMode(it.tag as Media3VideoScaleMode)
                }
                show()
            }
        }
    }

    private fun showEpisodeSelector() {
        if (context.getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            playerView?.trackView?.showEpisodeSelector()
            return
        }

        val player = playerView?.internalPlayer ?: return
        var selectedPosition = 0
        val list = ArrayList<TrackSelectEntity>().apply {
            for (i in 0 until player.mediaItemCount) {
                val tag = player.getMediaItemAt(i).localConfiguration?.tag
                if (tag is Media3Item) {
                    val name = if (tag.subtitle.isBlank()) tag.title else {
                        tag.title + " / " + tag.subtitle
                    }
                    if (tag.url == player.getCurrentMedia3Item()?.url) {
                        selectedPosition = i
                    }
                    add(TrackSelectEntity(i, name))
                }
            }
        }

        TrackSelectDialog(context).apply {
            setTitle("选择剧集")
            setOnShowListener { playerView!!.pause() }
            setOnDismissListener { playerView!!.start() }
            setSelectionData(list, selectedPosition)
            onNegative("取消")
            onPositive("确定") {
                playerView?.seekToItem(it.tag as Int, 0)
                playerView?.prepare()
                playerView?.start()
            }
            show()
        }
    }
}