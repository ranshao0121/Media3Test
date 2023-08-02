package com.jason.cloud.media3.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jason.cloud.media3.R
import com.jason.cloud.media3.model.Media3VideoItem
import com.jason.cloud.media3.utils.PlayerUtils
import com.jason.cloud.media3.widget.Media3PlayerView
import java.io.Serializable

class VideoPlayActivity : AppCompatActivity() {
    private val playerView: Media3PlayerView by lazy { findViewById(R.id.player_view) }
    private var isPausedByUser = true

    companion object {
        fun open(context: Context?, title: String, url: String, useCache: Boolean = false) {
            context?.startActivity(Intent(context, VideoPlayActivity::class.java).apply {
                putExtra("url", url)
                putExtra("title", title)
                putExtra("useCache", useCache)
            })
        }

        fun open(context: Context?, item: Media3VideoItem) {
            context?.startActivity(Intent(context, VideoPlayActivity::class.java).apply {
                putExtra("item", item)
            })
        }

        fun open(context: Context?, videoData: List<Media3VideoItem>, position: Int = 0) {
            context?.startActivity(Intent(context, VideoPlayActivity::class.java).apply {
                putExtra("videoData", videoData as Serializable)
                putExtra("position", position)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_play)
        playerView.getStatusView().layoutParams.height =
            PlayerUtils.getStatusBarHeight(this).toInt()
        playerView.setOnPlayStateListener {
            when (it) {
                Media3PlayerView.STATE_PLAYING -> isPausedByUser = true
                Media3PlayerView.STATE_PAUSED -> isPausedByUser = true
            }
        }

        val url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")
        val useCache = intent.getBooleanExtra("useCache", false)
        if (url?.isNotBlank() == true && title?.isNotBlank() == true) {
            playerView.setDataSource(Media3VideoItem.create(title, url, useCache))
            playerView.prepare()
            playerView.start()
            return
        }

        val item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("item", Media3VideoItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("item")?.let {
                it as Media3VideoItem
            }
        }
        if (item != null) {
            playerView.setDataSource(item)
            playerView.prepare()
            playerView.start()
            return
        }

        val position = intent.getIntExtra("position", 0)
        val videoData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("UNCHECKED_CAST")
            intent.getSerializableExtra("videoData", Serializable::class.java)
                ?.let { it as List<Media3VideoItem> } ?: emptyList()
        } else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            intent.getSerializableExtra("videoData")?.let {
                it as List<Media3VideoItem>
            }
        }
        if (videoData?.isNotEmpty() == true) {
            playerView.setDataSource(videoData)
            playerView.seekToItem(position, 0)
            playerView.prepare()
            playerView.start()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isPausedByUser.not()) {
            playerView.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (playerView.isPlaying()) {
            isPausedByUser = false
            playerView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.release()
    }
}