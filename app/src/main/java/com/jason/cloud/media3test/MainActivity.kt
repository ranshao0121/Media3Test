package com.jason.cloud.media3test

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.jason.cloud.media3.activity.VideoPlayActivity
import com.jason.cloud.media3.model.Media3VideoItem
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var fileSelectLauncher: ActivityResultLauncher<String>

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fileSelectLauncher = registerForActivityResult(SelectFilesContract()) { uriList ->
            if (uriList.isNotEmpty()) {
                VideoPlayActivity.open(this, ArrayList<Media3VideoItem>().apply {
                    uriList.forEach { uri ->
                        val file = DocumentFile.fromSingleUri(this@MainActivity, uri)
                        val name = file?.name ?: uri.toString()
                        add(Media3VideoItem.create(name, uri.toString()))
                    }
                })
            }
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            VideoPlayActivity.open(this, loadSourceList())
        }
        findViewById<Button>(R.id.btn_select).setOnClickListener {
            fileSelectLauncher.launch("video/*")
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