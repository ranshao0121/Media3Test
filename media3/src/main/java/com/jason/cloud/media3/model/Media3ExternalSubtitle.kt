package com.jason.cloud.media3.model

import androidx.media3.common.MimeTypes
import java.io.File
import java.io.Serializable

class Media3ExternalSubtitle(
    val uri: String,
    val title: String,
    val language: String?,
    val mimeType: String?,
) : Serializable {
    companion object {
        //VideoName.Language.srt
        fun createFromFile(file: File): Media3ExternalSubtitle {
            val title = file.nameWithoutExtension
            val language = getSubtitleLanguage(file.absolutePath.lowercase())
            val mimeType = getSubtitleMimeType(file.name)
            return Media3ExternalSubtitle(file.absolutePath, title, language, mimeType)
        }

        fun findSubtitles(video: File): List<File> {
            val name = video.nameWithoutExtension
            return video.parentFile?.listFiles()?.filter {
                it.name.startsWith(name, true) && it.length() > 0 && isSubtitle(it.name)
            } ?: emptyList()
        }

        fun isSubtitle(name: String): Boolean {
            return name.matches(
                "^.+\\.(srt|ass|ssa|vtt|ttml)\$".toRegex(
                    RegexOption.IGNORE_CASE
                )
            )
        }

        fun getSubtitleLanguage(fileName: String): String? {
            if (fileName.contains(".")) {
                val name = fileName.substringBeforeLast(".")
                val lang = name.substringAfterLast(".")
                if (lang.length in 2..6) {
                    return lang
                }
            }
            return null
        }

        fun getSubtitleMimeType(name: String): String {
            return if (name.endsWith(".ssa") || name.endsWith(".ass")) {
                MimeTypes.TEXT_SSA
            } else if (name.endsWith(".vtt")) {
                MimeTypes.TEXT_VTT
            } else if (name.endsWith(".ttml") || name.endsWith(".xml") || name.endsWith(".dfxp")) {
                MimeTypes.APPLICATION_TTML
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
        }
    }
}
