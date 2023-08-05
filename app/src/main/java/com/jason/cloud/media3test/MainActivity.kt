package com.jason.cloud.media3test

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.jason.cloud.media3.activity.VideoPlayActivity
import com.jason.cloud.media3.model.Media3Item
import com.tencent.mmkv.MMKV
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var fileSelectLauncher: ActivityResultLauncher<String>

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MMKV.initialize(applicationContext)

        fileSelectLauncher = registerForActivityResult(SelectFilesContract()) { uriList ->
            if (uriList.isNotEmpty()) {
                VideoPlayActivity.open(this, ArrayList<Media3Item>().apply {
                    uriList.forEach { uri ->
                        val file = DocumentFile.fromSingleUri(this@MainActivity, uri)
                        val name = file?.name ?: uri.toString()
                        add(Media3Item.create(name, uri.toString()))
                    }
                })
            }
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            VideoPlayActivity.positionStore = PositionStore()
            VideoPlayActivity.open(this, loadSourceList(), 0)
        }
        findViewById<Button>(R.id.btn_select).setOnClickListener {
            fileSelectLauncher.launch("video/*")
        }
    }

    private fun loadSourceList(): List<Media3Item> {
        return ArrayList<Media3Item>().apply {
            assets.open("sources.json").use {
                it.readBytes().decodeToString()
            }.let {
                val list = JSONObject(it).getJSONArray("list")
                for (i in 0 until list.length()) {
                    val obj = list.getJSONObject(i)
                    add(
                        Media3Item.create(
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