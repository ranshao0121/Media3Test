package com.jason.cloud.media3.utils

import android.content.Context
import android.content.res.Configuration

fun Context.getCurrentOrientation(): Int {
    val orientation = resources.configuration.orientation
    if (orientation != Configuration.ORIENTATION_UNDEFINED) {
        return orientation
    }

    val width = resources.displayMetrics.widthPixels
    val height = resources.displayMetrics.heightPixels
    return if (width > height) {
        Configuration.ORIENTATION_LANDSCAPE
    } else {
        Configuration.ORIENTATION_PORTRAIT
    }
}

fun Configuration.getCurrentOrientation(): Int {
    val orientation = this.orientation
    if (orientation != Configuration.ORIENTATION_UNDEFINED) {
        return orientation
    }

    val width = screenWidthDp
    val height = screenHeightDp
    return if (width > height) {
        Configuration.ORIENTATION_LANDSCAPE
    } else {
        Configuration.ORIENTATION_PORTRAIT
    }
}