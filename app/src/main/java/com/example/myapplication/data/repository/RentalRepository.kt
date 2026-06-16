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
        val items = store.list(screen)
        return if (items.isEmpty()) {
            UiState.Empty("Chưa có dữ liệu ${screen.label.lowercase()}.")
        } else {
            UiState.Content(items, DataSource.Local)
        }
    }

    suspend fun saveItem(screen: AppScreen, item: RentalItem): Result<RentalItem> = runCatching {
        val id = item.id.ifBlank { store.nextId(screen) }
        store.upsert(screen, item.copy(id = id))
    }

    suspend fun deleteItem(screen: AppScreen, id: String): Result<Unit> = runCatching {
        store.delete(screen, id)
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
}
