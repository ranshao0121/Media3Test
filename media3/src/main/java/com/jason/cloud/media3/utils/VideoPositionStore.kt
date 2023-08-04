package com.jason.cloud.media3.utils

interface VideoPositionStore {
    fun get(url: String): Long

    fun save(url: String, position: Long)

    fun remove(url: String)
}