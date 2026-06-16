package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.local.LocalAppStore
import com.example.myapplication.domain.model.AccountProfile
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import org.json.JSONObject

class RentalRepository(context: Context) {
    private val store = LocalAppStore(context.applicationContext)

    suspend fun login(username: String, password: String, role: UserRole): Result<UserSession> = runCatching {
        store.login(username, password, role)
    }

    suspend fun register(payload: JSONObject): Result<String> = runCatching {
        store.register(payload)
    }

    suspend fun forgotPassword(email: String): Result<String> = runCatching {
        store.forgotPassword(email)
    }

    suspend fun resetPassword(email: String, token: String, newPassword: String, confirmPassword: String): Result<String> = runCatching {
        store.resetPassword(email, token, newPassword, confirmPassword)
    }

    suspend fun account(session: UserSession): UiState<AccountProfile> = runCatching {
        store.account(session)
    }.fold(
        onSuccess = { UiState.Content(it, DataSource.Local) },
        onFailure = { UiState.Error(it.message ?: "Không tải được thông tin tài khoản.") }
    )

    suspend fun updateAccount(session: UserSession, profile: AccountProfile): Result<AccountProfile> = runCatching {
        store.updateAccount(session, profile)
    }

    suspend fun changePassword(session: UserSession, oldPassword: String, newPassword: String, confirmPassword: String): Result<String> = runCatching {
        store.changePassword(session, oldPassword, newPassword, confirmPassword)
    }

    suspend fun uploadCccdImage(fileName: String, mimeType: String, bytes: ByteArray, session: UserSession? = null): Result<String> = runCatching {
        store.saveImage(fileName, mimeType, bytes)
    }

    suspend fun dashboard(session: UserSession?): UiState<DashboardSummary> {
        return UiState.Content(store.dashboard(), DataSource.Local)
    }

    suspend fun list(screen: AppScreen, session: UserSession?): UiState<List<RentalItem>> {
        val items = store.list(screen, session)
        return if (items.isEmpty()) {
            UiState.Empty("Chưa có dữ liệu ${screen.label.lowercase()}.")
        } else {
            UiState.Content(items, DataSource.Local)
        }
    }

    suspend fun saveItem(screen: AppScreen, item: RentalItem, session: UserSession?): Result<RentalItem> = runCatching {
        require(canManage(session?.role, screen)) { "Tài khoản này không có quyền sửa ${screen.label.lowercase()}." }
        val id = item.id.ifBlank { store.nextId(screen) }
        store.upsert(screen, item.copy(id = id))
    }

    suspend fun deleteItem(screen: AppScreen, id: String, session: UserSession?): Result<Unit> = runCatching {
        require(canManage(session?.role, screen)) { "Tài khoản này không có quyền xóa ${screen.label.lowercase()}." }
        store.delete(screen, id)
    }

    suspend fun requestRoom(roomId: String, session: UserSession?, duration: String, note: String): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.NguoiDung) { "Chỉ Người thuê được gửi yêu cầu thuê phòng." }
        store.createRentRequest(roomId, session, duration, note)
    }

    suspend fun decideRentRequest(requestId: String, approve: Boolean, session: UserSession?): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.Admin || session?.role == UserRole.ChuTro) { "Chỉ Admin hoặc Chủ trọ được duyệt yêu cầu thuê." }
        store.decideRentRequest(requestId, approve, session)
    }

    suspend fun confirmContract(contractId: String, approve: Boolean, session: UserSession?): Result<RentalItem> = runCatching {
        require(session?.role == UserRole.NguoiDung) { "Chỉ Người thuê được xác nhận hợp đồng của mình." }
        store.confirmContract(contractId, approve, session)
    }

    fun demoSession(roleName: String): UserSession {
        val role = UserRole.from(roleName)
        val username = when (role) {
            UserRole.Admin -> "Admin"
            UserRole.ChuTro -> "chutro"
            UserRole.NguoiDung -> "nguoithue"
        }
        val password = if (role == UserRole.Admin) "Admin123" else "123456"
        return store.login(username, password, role)
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
