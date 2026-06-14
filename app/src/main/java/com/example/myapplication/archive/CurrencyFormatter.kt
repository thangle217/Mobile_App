package com.example.myapplication.archive

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun formatVND(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
}