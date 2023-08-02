package com.jason.cloud.media3test

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.jason.cloud.media3.activity.VideoPlayActivity
import com.jason.cloud.media3.model.Media3VideoItem
import com.jason.cloud.media3.widget.Media3PlayerView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            VideoPlayActivity.open(this, loadSourceList())
        }
    }

    private fun loadSourceList(): List<Media3VideoItem> {
        return ArrayList<Media3VideoItem>().apply {
            assets.open("sources.json").use {
                it.readBytes().decodeToString()
            }.let {
                val list = JSONObject(it).getJSONArray("list")
                for (i in 0 until list.length()) {
                    val obj = list.getJSONObject(i)
                    add(
                        Media3VideoItem.create(
                            obj.getString("name"),
                            obj.getString("uri"),
                            obj.getBoolean("useCache")
                        )
                    )
                }
            }
        }
    }
}