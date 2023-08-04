package com.jason.cloud.media3test

import android.util.Log
import com.jason.cloud.media3.utils.PlayerUtils
import com.jason.cloud.media3.utils.VideoPositionStore
import com.tencent.mmkv.MMKV

open class PositionStore : VideoPositionStore {
    private val mmkv by lazy { MMKV.mmkvWithID("PositionStore") }

    override fun get(url: String): Long {
        return mmkv.decodeLong(url).also {
            Log.i("PositionStore", "get: $url >> ${PlayerUtils.stringForTime(it)}")
        }
    }

    override fun save(url: String, position: Long) {
        Log.i("PositionStore", "save: $url >> $position")
        mmkv.encode(url, position)
    }

    override fun remove(url: String) {
        Log.i("PositionStore", "remove: $url")
        mmkv.remove(url)
    }
}