package com.jason.cloud.media3.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar

object TimeUtil {
    @SuppressLint("SimpleDateFormat")
    fun getEndTimeString(duration: Long, formatter: String): String {
        val currentTime = System.currentTimeMillis()
        val endTime = currentTime + duration

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endTime

        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val amPm = when {
            hourOfDay < 6 -> "凌晨"
            hourOfDay < 12 -> "上午"
            hourOfDay < 18 -> "下午"
            else -> "晚上"
        }

        val dateFormat = SimpleDateFormat("$amPm hh:mm")
        val endTimeText = dateFormat.format(calendar.time)
        return String.format(formatter, endTimeText)
    }
}