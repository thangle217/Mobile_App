package com.example.myapplication.data.remote

import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.AccountProfile
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import com.example.myapplication.domain.util.moneyValue
import com.example.myapplication.domain.util.normalizeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:5253/"
}

class ApiClient(
    private val baseUrl: String = ApiConfig.BASE_URL,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(username: String, password: String, role: UserRole): UserSession = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("tenDangNhap", username)
            .put("matKhau", password)
            .put("vaiTro", role.name)
            .toString()
            .toRequestBody(jsonType)
        val json = requestJson("api/Auth/dang-nhap", method = "POST", body = body, token = null)
        val payload = json.unwrapObject()
        val token = payload.optString("token").ifBlank { payload.optString("Token").ifBlank { payload.optString("accessToken") } }
        if (token.isBlank()) error("API không trả về token đăng nhập.")
        val resolvedRole = UserRole.from(payload.optString("vaiTro").ifBlank { payload.optString("VaiTro").ifBlank { payload.optString("role") } })
        UserSession(
            token = token,
            role = resolvedRole,
            displayName = payload.optString("hoTen")
                .ifBlank { payload.optString("HoTen") }
                .ifBlank { payload.optString("tenDangNhap") }
                .ifBlank { payload.optString("TenDangNhap") }
                .ifBlank { username },
            username = username
        )
    }

    suspend fun register(payload: JSONObject): String = withContext(Dispatchers.IO) {
        val json = requestJson("api/Auth/dang-ky", method = "POST", body = payload.toString().toRequestBody(jsonType), token = null)
        json.optString("thongBao").ifBlank { json.optString("message").ifBlank { "Đăng ký thành công." } }
    }

    suspend fun forgotPassword(email: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email).toString().toRequestBody(jsonType)
        val json = requestJson("api/Account/quen-mat-khau", method = "POST", body = body, token = null)
        json.optString("thongBao").ifBlank { json.optString("message").ifBlank { "Nếu email tồn tại, hệ thống đã gửi mã OTP." } }
    }

    suspend fun resetPassword(email: String, token: String, newPassword: String, confirmPassword: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email)
            .put("token", token)
            .put("matKhauMoi", newPassword)
            .put("nhapLaiMatKhau", confirmPassword)
            .toString()
            .toRequestBody(jsonType)
        val json = requestJson("api/Account/reset-mat-khau", method = "POST", body = body, token = null)
        json.optString("thongBao").ifBlank { json.optString("message").ifBlank { "Đặt lại mật khẩu thành công." } }
    }

    suspend fun account(token: String): AccountProfile = withContext(Dispatchers.IO) {
        requestJson("api/Account/thong-tin", token = token).unwrapObject().toAccountProfile()
    }

    suspend fun updateAccount(token: String, profile: AccountProfile): AccountProfile = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("hoTen", profile.fullName)
            .put("email", profile.email)
            .put("soDienThoai", profile.phone)
            .put("cccd", profile.cccd)
            .put("ngaySinh", profile.dateOfBirth.ifBlank { JSONObject.NULL })
            .put("gioiTinh", profile.gender)
            .put("quocTich", profile.nationality)
            .put("diaChi", profile.address)
            .put("noiCongTac", profile.workplace)
            .put("anhCccdMatTruoc", profile.cccdFrontUrl)
            .put("anhCccdMatSau", profile.cccdBackUrl)
            .put("tenNganHang", profile.bankName)
            .put("maNganHang", profile.bankCode)
            .put("soTaiKhoan", profile.bankAccount)
            .put("tenChuTaiKhoan", profile.bankOwner)
            .put("noiDungChuyenKhoanMacDinh", profile.transferContent)
            .toString()
            .toRequestBody(jsonType)
        requestJson("api/Account/cap-nhat", method = "PUT", body = body, token = token).unwrapObject().toAccountProfile()
    }

    suspend fun changePassword(token: String, oldPassword: String, newPassword: String, confirmPassword: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("matKhauCu", oldPassword)
            .put("matKhauMoi", newPassword)
            .put("nhapLaiMatKhau", confirmPassword)
            .toString()
            .toRequestBody(jsonType)
        val json = requestJson("api/Account/doi-mat-khau", method = "POST", body = body, token = token)
        json.optString("thongBao").ifBlank { json.optString("message").ifBlank { "Đổi mật khẩu thành công." } }
    }

    suspend fun uploadCccdImage(fileName: String, mimeType: String, bytes: ByteArray, token: String? = null): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("File", fileName, bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        val json = requestJson("api/Auth/upload-cccd-image", method = "POST", body = body, token = token)
        val data = json.unwrapObject()
        data.optString("url").ifBlank { error("API upload không trả về URL ảnh.") }
    }

    suspend fun dashboard(token: String): DashboardSummary = withContext(Dispatchers.IO) {
        val data = requestJson("api/Dashboard/overview", token = token).unwrapObject()
        DashboardSummary(
            totalRooms = data.optIntAny("tongPhong", "totalRooms", "soPhong"),
            emptyRooms = data.optIntAny("phongTrong", "emptyRooms", "soPhongTrong"),
            unpaidInvoices = data.optIntAny("hoaDonChuaThanhToan", "unpaidInvoices", "soHoaDonChuaThanhToan"),
            pendingTasks = data.optIntAny("canXuLy", "pendingTasks", "yeuCauChoDuyet"),
            revenue = data.optLongAny("doanhThu", "revenue", "tongDoanhThu")
        )
    }

    suspend fun list(screen: AppScreen, token: String): List<RentalItem> = withContext(Dispatchers.IO) {
        val endpoint = endpointFor(screen) ?: return@withContext emptyList()
        val root = requestJson(endpoint, token = token)
        val array = root.unwrapArray()
        List(array.length()) { index -> array.getJSONObject(index).toRentalItem(screen) }
    }

    private fun endpointFor(screen: AppScreen): String? = when (screen) {
        AppScreen.Houses -> "api/NhaTro"
        AppScreen.Rooms -> "api/Phong"
        AppScreen.Tenants -> "api/NguoiThue"
        AppScreen.Contracts -> "api/HopDong"
        AppScreen.Invoices -> "api/HoaDon"
        AppScreen.Payments -> "api/ThanhToan"
        AppScreen.Services -> "api/DichVu"
        AppScreen.ServiceRegs -> "api/DangKyDichVu"
        AppScreen.Electric -> "api/ChiSoDien"
        AppScreen.Water -> "api/ChiSoNuoc"
        AppScreen.RentRequests -> "api/YeuCauThue"
        AppScreen.RenewRequests -> "api/YeuCauGiaHan"
        AppScreen.Incidents -> "api/BaoCaoSuCo"
        AppScreen.Notices -> "api/ThongBao"
        AppScreen.Users -> "api/User"
        AppScreen.Dashboard, AppScreen.Account -> null
    }

    private fun requestJson(
        path: String,
        method: String = "GET",
        body: okhttp3.RequestBody? = null,
        token: String?
    ): JSONObject {
        val url = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
        val builder = Request.Builder().url(url)
            .header("Accept", "application/json")
            .method(method, body)
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, raw.ifBlank { response.message })
            return if (raw.trim().startsWith("[")) JSONObject().put("duLieu", JSONArray(raw)) else JSONObject(raw)
        }
    }
}

class ApiException(val statusCode: Int, message: String) : Exception(message)

private fun JSONObject.unwrapObject(): JSONObject {
    val data = opt("duLieu") ?: opt("data") ?: this
    return data as? JSONObject ?: this
}

private fun JSONObject.unwrapArray(): JSONArray {
    val data = opt("duLieu") ?: opt("data") ?: opt("items") ?: this
    return when (data) {
        is JSONArray -> data
        is JSONObject -> data.optJSONArray("items") ?: JSONArray().put(data)
        else -> JSONArray()
    }
}

private fun JSONObject.toRentalItem(screen: AppScreen): RentalItem {
    val id = firstString("ma${screen.shortCode}", "id", "maNhaTro", "maPhong", "maHoaDon", "maHopDong", "maNguoiThue", "maThanhToan", "maYeuCau", "maDichVu")
        .ifBlank { optString("ma").ifBlank { "#${hashCode().toString().takeLast(5)}" } }
    val title = firstString(
        "tenNhaTro", "tenPhong", "tenLoaiPhong", "tenDichVu", "tieuDe", "hoTen",
        "tenNguoiThue", "tenDangNhap", "noiDung", "maHoaDon", "maHopDong"
    ).ifBlank { "${screen.label} $id" }
    val status = normalizeStatus(firstString("trangThaiText", "trangThai", "tenTrangThai", "daDoc").ifBlank { "Đang cập nhật" })
    val value = firstString("tongTien", "giaPhong", "giaDichVu", "soTien", "tienDien", "tienNuoc", "email", "soDienThoai")
        .ifBlank { "Đang cập nhật" }
    val note = firstString("diaChi", "moTa", "ghiChu", "noiDung", "kyHoaDon", "ngayTao", "ngayGui")
        .ifBlank { "Dữ liệu đồng bộ từ API ${screen.label.lowercase()}." }
    val details = keys().asSequence()
        .take(8)
        .mapNotNull { key ->
            val valueText = opt(key)?.toString().orEmpty()
            if (valueText == "null" || valueText.length > 80) null else key to valueText
        }
        .toList()
    return RentalItem(id = id, title = title, status = status, value = value, note = note, details = details)
}

private fun JSONObject.toAccountProfile(): AccountProfile {
    return AccountProfile(
        userId = optInt("maNguoiDung"),
        username = optString("tenDangNhap"),
        fullName = optString("hoTen"),
        email = optString("email"),
        phone = optString("soDienThoai"),
        cccd = optString("cccd").ifBlank { optString("CCCD") },
        dateOfBirth = optString("ngaySinh"),
        gender = optString("gioiTinh"),
        nationality = optString("quocTich"),
        address = optString("diaChi"),
        workplace = optString("noiCongTac"),
        cccdFrontUrl = optString("anhCccdMatTruoc"),
        cccdBackUrl = optString("anhCccdMatSau"),
        bankName = optString("tenNganHang"),
        bankCode = optString("maNganHang"),
        bankAccount = optString("soTaiKhoan"),
        bankOwner = optString("tenChuTaiKhoan"),
        transferContent = optString("noiDungChuyenKhoanMacDinh"),
        role = UserRole.from(optString("vaiTro"))
    )
}

private fun JSONObject.firstString(vararg keys: String): String {
    return keys.firstNotNullOfOrNull { key ->
        val value = opt(key)
        when (value) {
            null, JSONObject.NULL -> null
            is Number -> value.toLong().let { if (key.lowercase().contains("tien") || key.lowercase().contains("gia")) "${it}đ" else it.toString() }
            is Boolean -> if (value) "Đã đọc" else "Chưa đọc"
            else -> value.toString().takeIf { it.isNotBlank() }
        }
    }.orEmpty()
}

private fun JSONObject.optIntAny(vararg keys: String): Int = keys.firstNotNullOfOrNull { key ->
    if (has(key)) optInt(key) else null
} ?: 0

private fun JSONObject.optLongAny(vararg keys: String): Long = keys.firstNotNullOfOrNull { key ->
    if (!has(key)) null else when (val value = opt(key)) {
        is Number -> value.toLong()
        is String -> moneyValue(value)
        else -> null
    }
} ?: 0L

