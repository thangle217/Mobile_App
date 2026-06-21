package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.CloudDataSource
import com.example.myapplication.domain.model.AccountProfile
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import org.json.JSONObject

import com.example.myapplication.data.local.LocalAppStore

class RentalRepository(context: Context) : IRentalRepository {
    private val store = LocalAppStore(context)

    override suspend fun login(username: String, password: String, role: UserRole): Result<UserSession> = runCatching {
        try {
            store.login(username, password, role)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("incorrect, malformed or has expired", ignoreCase = true) || msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true)) {
                throw Exception("Sai mật khẩu hoặc tài khoản không tồn tại.")
            } else if (msg.contains("badly formatted", ignoreCase = true)) {
                throw Exception("Email không đúng định dạng.")
            } else {
                throw e
            }
        }
    }

    override suspend fun register(payload: JSONObject): Result<String> = runCatching {
        store.register(payload)
    }

    suspend fun createTestAccounts(): Result<String> = runCatching {
        "Đã khôi phục dữ liệu mẫu (local seed)."
    }

    override suspend fun forgotPassword(email: String): Result<String> = runCatching {
        store.forgotPassword(email)
    }

    override suspend fun resetPassword(email: String, token: String, newPassword: String, confirmPassword: String): Result<String> = runCatching {
        store.resetPassword(email, token, newPassword, confirmPassword)
    }

    override suspend fun account(session: UserSession): UiState<AccountProfile> = runCatching {
        store.account(session)
    }.fold(
        onSuccess = { UiState.Content(it, DataSource.Local) },
        onFailure = { UiState.Error(it.message ?: "Không tải được thông tin tài khoản.") }
    )

    override suspend fun updateAccount(session: UserSession, profile: AccountProfile): Result<AccountProfile> = runCatching {
        store.updateAccount(session, profile)
    }

    override suspend fun changePassword(session: UserSession, oldPassword: String, newPassword: String, confirmPassword: String): Result<String> = runCatching {
        store.changePassword(session, oldPassword, newPassword, confirmPassword)
    }

    override suspend fun uploadCccdImage(fileName: String, mimeType: String, bytes: ByteArray, session: UserSession?): Result<String> = runCatching {
        store.saveImage(fileName, mimeType, bytes)
    }

    override suspend fun dashboard(session: UserSession?): UiState<DashboardSummary> {
        return UiState.Content(store.dashboard(), DataSource.Local)
    }

    override suspend fun list(screen: AppScreen, session: UserSession?): UiState<List<RentalItem>> {
        val items = store.list(screen, session)
        // Luôn trả về Content (kể cả khi rỗng) để ModuleScreen vẫn render và hiện nút "Thêm"
        return UiState.Content(items, DataSource.Local)
    }

    override suspend fun saveItem(screen: AppScreen, item: RentalItem, session: UserSession?): Result<RentalItem> = runCatching {
        require(canManage(session?.role, screen)) { "Tài khoản này không có quyền sửa ${screen.label.lowercase()}." }
        validateItemForScreen(screen, item)
        val id = item.id.ifBlank { store.nextId(screen) }
        val baseDetails = item.details.toMutableList()
        if (session?.role == UserRole.NguoiDung && baseDetails.none { it.first == "tenantUsername" }) {
            baseDetails.add("tenantUsername" to session.username)
        }
        if (session != null && baseDetails.none { it.first == "createdBy" }) {
            baseDetails.add("createdBy" to session.username)
        }
        val finalItem = item.copy(details = baseDetails)
        store.upsert(screen, finalItem.copy(id = id))
        finalItem.copy(id = id)
    }

    private fun validateItemForScreen(screen: AppScreen, item: RentalItem) {
        when (screen) {
            AppScreen.Houses -> {
                require(item.title.isNotBlank()) { "Tên nhà trọ không được để trống." }
            }
            AppScreen.RoomTypes -> {
                require(item.title.isNotBlank()) { "Tên loại phòng không được để trống." }
            }
            AppScreen.Rooms -> {
                require(item.title.isNotBlank()) { "Tên phòng không được để trống." }
                require(item.value.isNotBlank()) { "Vui lòng nhập giá thuê." }
            }
            AppScreen.Services -> {
                require(item.title.isNotBlank()) { "Tên dịch vụ không được để trống." }
                require(item.value.isNotBlank()) { "Vui lòng nhập đơn giá." }
            }
            AppScreen.Incidents -> {
                require(item.title.isNotBlank()) { "Vui lòng nhập tiêu đề sự cố." }
            }
            AppScreen.Payments -> {
                require(item.title.isNotBlank()) { "Vui lòng nhập tiêu đề biên lai." }
                require(item.value.isNotBlank()) { "Vui lòng nhập số tiền." }
            }
            else -> {
                require(item.title.isNotBlank()) { "Tên/nội dung không được để trống." }
            }
        }
    }

    override suspend fun deleteItem(screen: AppScreen, id: String, session: UserSession?): Result<Unit> = runCatching {
        require(canManage(session?.role, screen)) { "Tài khoản này không có quyền xóa ${screen.label.lowercase()}." }
        store.delete(screen, id)
    }

    override suspend fun requestRoom(roomId: String, session: UserSession?, duration: String, note: String): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.NguoiDung) { "Chỉ Người thuê được gửi yêu cầu thuê phòng." }
        store.createRentRequest(roomId, session!!, duration, note)
    }

    override suspend fun decideRentRequest(requestId: String, approve: Boolean, session: UserSession?): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ được duyệt yêu cầu thuê." }
        store.decideRentRequest(requestId, approve, session!!)
    }

    override suspend fun confirmContract(contractId: String, approve: Boolean, session: UserSession?): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.NguoiDung) { "Chỉ Người thuê được xác nhận hợp đồng của mình." }
        store.confirmContract(contractId, approve, session!!)
    }

    override suspend fun saveContract(
        contractId: String,
        roomId: String,
        tenantUsername: String,
        startDate: String,
        endDate: String,
        deposit: String,
        note: String,
        status: String,
        session: UserSession?
    ): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ được lưu hợp đồng." }
        store.saveContract(contractId, roomId, tenantUsername, startDate, endDate, deposit, note, status, session!!)
    }

    override suspend fun closeContract(contractId: String, cancel: Boolean, session: UserSession?): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ được kết thúc/hủy hợp đồng." }
        store.closeContract(contractId, cancel, session!!)
    }

    override suspend fun createRenewRequest(contractId: String, session: UserSession?, newEndDate: String, note: String): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.NguoiDung) { "Chỉ Người thuê được gửi yêu cầu gia hạn." }
        store.createRenewRequest(contractId, session!!, newEndDate, note)
    }

    override suspend fun decideRenewRequest(requestId: String, approve: Boolean, session: UserSession?): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ được duyệt gia hạn." }
        store.decideRenewRequest(requestId, approve, session!!)
    }

    override suspend fun getLatestUtilityIndex(screen: AppScreen, roomId: String): Result<Double> = runCatching {
        store.getLatestUtilityIndex(screen, roomId)
    }

    override suspend fun saveUtilityReading(
        screen: AppScreen,
        roomId: String,
        period: String,
        oldIndex: Double,
        newIndex: Double,
        price: Double,
        session: UserSession?
    ): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ có quyền ghi chỉ số điện nước." }
        store.saveUtilityReading(screen, roomId, period, oldIndex, newIndex, price)
    }

    override suspend fun createInvoice(
        roomId: String,
        period: String,
        otherCost: Double,
        otherNote: String,
        session: UserSession?
    ): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ có quyền lập hóa đơn." }
        store.createInvoice(roomId, period, otherCost, otherNote)
    }

    override suspend fun submitPayment(
        invoiceId: String,
        transactionId: String,
        receiptImage: String,
        note: String,
        session: UserSession?
    ): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.NguoiDung) { "Chỉ người thuê có quyền gửi biên lai thanh toán." }
        store.submitPayment(invoiceId, transactionId, receiptImage, note, session!!)
    }

    override suspend fun decidePayment(
        paymentId: String,
        approve: Boolean,
        rejectReason: String?,
        session: UserSession?
    ): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ có quyền duyệt thanh toán." }
        store.decidePayment(paymentId, approve, rejectReason, session!!)
    }

    override suspend fun respondToIncident(
        incidentId: String,
        response: String,
        newStatus: String,
        session: UserSession?
    ): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) {
            "Chỉ Admin hoặc Chủ trọ có quyền phản hồi sự cố."
        }
        store.respondToIncident(incidentId, response, newStatus, session!!)
    }

    override suspend fun markNoticeAsRead(noticeId: String, session: UserSession?): Result<RentalItem> = runCatching {
        val username = session?.username ?: error("Vui lòng đăng nhập.")
        store.markNoticeAsRead(noticeId, username)
    }

    override suspend fun createNotice(
        title: String,
        content: String,
        targetType: String,
        session: UserSession?
    ): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) {
            "Chỉ Admin hoặc Chủ trọ có quyền tạo thông báo."
        }
        store.createNotice(title, content, targetType, session!!)
    }

    override fun unreadNoticeCount(session: UserSession?): Int {
        val username = session?.username ?: return 0
        return store.unreadNoticeCount(username)
    }

    override fun demoSession(roleName: String): UserSession {
        val role = UserRole.from(roleName)
        val username = when (role) {
            UserRole.Admin -> "Admin"
            UserRole.ChuTro -> "chutro"
            UserRole.NguoiDung -> "nguoithue"
        }
        val password = if (role == UserRole.Admin) "Admin123" else "123456"
        return UserSession(token = "dummy", role = role, displayName = username, username = username)
    }

    private fun canManage(role: UserRole?, screen: AppScreen): Boolean = when (role) {
        UserRole.Admin -> screen != AppScreen.Account
        UserRole.ChuTro -> screen in setOf(
            AppScreen.Houses,
            AppScreen.RoomTypes,
            AppScreen.Rooms,
            AppScreen.Tenants,
            AppScreen.Contracts,
            AppScreen.Invoices,
            AppScreen.Payments,
            AppScreen.Services,
            AppScreen.ServiceRegs,
            AppScreen.Electric,
            AppScreen.Water,
            AppScreen.RentRequests,
            AppScreen.RenewRequests,
            AppScreen.Incidents,
            AppScreen.Notices
        )
        UserRole.NguoiDung -> screen in setOf(
            AppScreen.RentRequests,
            AppScreen.RenewRequests,
            AppScreen.Payments,
            AppScreen.Incidents
        )
        null -> false
    }
}
