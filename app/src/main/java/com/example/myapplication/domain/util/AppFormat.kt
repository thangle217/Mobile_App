package com.example.myapplication.domain.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val viLocale = Locale.forLanguageTag("vi-VN")
private val moneyFormatter = NumberFormat.getCurrencyInstance(viLocale)
private val inputDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val outputDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun formatMoney(value: Long): String = moneyFormatter.format(value)

fun formatCompactMoney(value: Long): String = when {
    value >= 1_000_000 -> "${value / 1_000_000},${(value % 1_000_000) / 100_000} triệu"
    value >= 1_000 -> "${value / 1_000} nghìn"
    else -> formatMoney(value)
}

fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "Chưa cập nhật"
    return runCatching { LocalDate.parse(value.take(10), inputDate).format(outputDate) }.getOrDefault(value)
}

fun moneyValue(value: String): Long = value.filter { it.isDigit() }.toLongOrNull() ?: 0L

fun normalizeStatus(status: String): String = when (status.trim()) {
    "Dang hoat dong" -> "Đang hoạt động"
    "Tam dung" -> "Tạm dừng"
    "Dang dung" -> "Đang dùng"
    "Da thue" -> "Đã thuê"
    "Con trong" -> "Còn trống"
    "Dang sua" -> "Đang sửa"
    "Dang thue" -> "Đang thuê"
    "Cho cap nhat" -> "Chờ cập nhật"
    "Dang hieu luc" -> "Đang hiệu lực"
    "Sap het han" -> "Sắp hết hạn"
    "Cho nguoi thue xac nhan" -> "Chờ người thuê xác nhận"
    "Chua thanh toan" -> "Chưa thanh toán"
    "Thanh toan mot phan" -> "Thanh toán một phần"
    "Da thanh toan" -> "Đã thanh toán"
    "Cho xac nhan" -> "Chờ xác nhận"
    "Da xac nhan" -> "Đã xác nhận"
    "Tu choi" -> "Từ chối"
    "Tinh phi" -> "Tính phí"
    "Tien ich" -> "Tiện ích"
    "Dang su dung" -> "Đang sử dụng"
    "Da huy" -> "Đã hủy"
    "Da ghi" -> "Đã ghi"
    "Can ghi" -> "Cần ghi"
    "Cho duyet" -> "Chờ duyệt"
    "Da duyet" -> "Đã duyệt"
    "Da chap nhan" -> "Đã chấp nhận"
    "Moi" -> "Mới"
    "Dang xu ly" -> "Đang xử lý"
    "Da xu ly" -> "Đã xử lý"
    "Rat gap" -> "Rất gấp"
    "Binh thuong" -> "Bình thường"
    else -> status
}

fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    return email.trim().matches(emailRegex)
}

fun isValidPhone(phone: String): Boolean {
    val phoneRegex = "^(0|\\+84)[35789][0-9]{8}$".toRegex()
    return phone.trim().matches(phoneRegex)
}

