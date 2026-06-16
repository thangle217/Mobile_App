package com.example.myapplication.domain.model

enum class UserRole(val label: String) {
    Admin("Quản trị viên"),
    ChuTro("Chủ trọ"),
    NguoiDung("Người thuê");

    companion object {
        fun from(value: String?): UserRole = entries.firstOrNull { it.name.equals(value, true) } ?: ChuTro
    }
}

enum class AppScreen(val label: String, val shortCode: String) {
    Dashboard("Tổng quan", "DB"),
    Houses("Nhà trọ", "NT"),
    RoomTypes("Loại phòng", "LP"),
    Rooms("Phòng", "P"),
    Tenants("Khách thuê", "KT"),
    Contracts("Hợp đồng", "HD"),
    Invoices("Hóa đơn", "HĐ"),
    Payments("Thanh toán", "TT"),
    Services("Dịch vụ", "DV"),
    ServiceRegs("Đăng ký DV", "DK"),
    Electric("Chỉ số điện", "Đ"),
    Water("Chỉ số nước", "N"),
    RentRequests("Yêu cầu thuê", "YT"),
    RenewRequests("Gia hạn", "GH"),
    Incidents("Sự cố", "SC"),
    Notices("Thông báo", "TB"),
    Users("Người dùng", "ND"),
    Account("Tài khoản", "TK")
}

data class RentalItem(
    val id: String,
    val title: String,
    val status: String,
    val value: String,
    val note: String,
    val details: List<Pair<String, String>> = emptyList()
)

data class DashboardSummary(
    val totalRooms: Int = 0,
    val emptyRooms: Int = 0,
    val unpaidInvoices: Int = 0,
    val pendingTasks: Int = 0,
    val revenue: Long = 0L
)

data class UserSession(
    val token: String,
    val role: UserRole,
    val displayName: String,
    val username: String
)

data class AccountProfile(
    val userId: Int = 0,
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val cccd: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val nationality: String = "",
    val address: String = "",
    val workplace: String = "",
    val cccdFrontUrl: String = "",
    val cccdBackUrl: String = "",
    val bankName: String = "",
    val bankCode: String = "",
    val bankAccount: String = "",
    val bankOwner: String = "",
    val transferContent: String = "",
    val role: UserRole = UserRole.NguoiDung
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T, val source: DataSource = DataSource.Local) : UiState<T>
    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String, val canRetry: Boolean = true) : UiState<Nothing>
}

enum class DataSource {
    Api,
    Demo,
    Local
}
