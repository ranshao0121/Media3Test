package com.jason.cloud.media3.model

import android.util.Log
import java.io.File
import java.io.Serializable

open class Media3Item : Serializable {
    var url: String = ""
    var title: String = ""
    var image: String = ""
    var subtitle: String = ""
    var cacheEnabled: Boolean = false
    var externalSubtitles: ArrayList<Media3ExternalSubtitle> = arrayListOf()
    var headers: HashMap<String, String> = hashMapOf()
    var tag: Any? = null

    companion object {
        fun create(title: String, url: String, cache: Boolean = false): Media3Item {
            return Media3Item().apply {
                this.title = title
                this.url = url
                this.cacheEnabled = cache
            }
        }

        fun create(
            title: String,
            subtitle: String,
            url: String,
            cache: Boolean = false
        ): Media3Item {
            return Media3Item().apply {
                this.title = title
                this.subtitle = subtitle
                this.url = url
                this.cacheEnabled = cache
            }
        }

        fun create(
            title: String,
            url: String,
            subtitles: List<File>
        ): Media3Item {
            subtitles.forEach {
                Log.e("Subtitles", it.absolutePath)
            }
            return Media3Item().apply {
                this.title = title
                this.url = url
                this.cacheEnabled = false
                this.externalSubtitles.addAll(subtitles.filter {
                    it.exists() && it.length() > 0
                }.map {
                    Media3ExternalSubtitle.createFromFile(it)
                })
            }
        }
    }
}