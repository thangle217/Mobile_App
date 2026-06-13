package com.example.myapplication.archive

import java.text.SimpleDateFormat
import java.util.Date

object DateUtils {
    fun formatDate(date: Date): String = SimpleDateFormat("dd/MM/yyyy").format(date)
}