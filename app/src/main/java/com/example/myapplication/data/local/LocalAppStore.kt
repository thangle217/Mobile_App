package com.example.myapplication.data.local

import android.content.Context
import android.util.Base64
import com.example.myapplication.domain.model.AccountProfile
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import com.example.myapplication.domain.util.moneyValue
import org.json.JSONArray
import org.json.JSONObject

class LocalAppStore(context: Context) {
    private val prefs = context.getSharedPreferences("rental_local_store", Context.MODE_PRIVATE)

    init {
        seedIfNeeded()
    }

    fun login(usernameOrEmail: String, password: String, role: UserRole): UserSession {
        val user = usersArray().objects().firstOrNull {
            val matchIdentity = it.optString("username").equals(usernameOrEmail, true) ||
                it.optString("email").equals(usernameOrEmail, true)
            matchIdentity && it.optString("password") == password && it.optString("role") == role.name
        } ?: error("Sai tài khoản, mật khẩu hoặc vai trò.")
        return user.toSession()
    }

    fun register(payload: JSONObject): String {
        val username = payload.optString("tenDangNhap").trim()
        val email = payload.optString("email").trim()
        val password = payload.optString("matKhau")
        val confirmPassword = payload.optString("xacNhanMatKhau", password)
        val role = UserRole.from(payload.optString("vaiTro"))
        require(username.isNotBlank()) { "Tên đăng nhập không được để trống." }
        require(email.isNotBlank()) { "Email không được để trống." }
        require(password.length >= 6) { "Mật khẩu phải có ít nhất 6 ký tự." }
        require(password == confirmPassword) { "Mật khẩu nhập lại không khớp." }
        val users = usersArray()
        require(users.objects().none { it.optString("username").equals(username, true) || it.optString("email").equals(email, true) }) {
            "Tên đăng nhập hoặc email đã tồn tại."
        }
        val nextId = users.length() + 1
        users.put(
            JSONObject()
                .put("id", nextId)
                .put("username", username)
                .put("password", password)
                .put("role", role.name)
                .put("fullName", payload.optString("hoTen").ifBlank { username })
                .put("email", email)
                .put("phone", payload.optString("soDienThoai"))
                .put("cccd", payload.optString("cccd"))
                .put("cccdFrontUrl", payload.optString("anhCccdMatTruoc"))
                .put("cccdBackUrl", payload.optString("anhCccdMatSau"))
                .put("address", "")
                .put("workplace", "")
                .put("bankName", "")
                .put("bankAccount", "")
                .put("bankOwner", "")
                .put("transferContent", "")
        )
        saveUsers(users)
        addUserListItem(nextId, username, role, email)
        return "Đăng ký thành công. Bạn có thể đăng nhập bằng tài khoản vừa tạo."
    }

    fun forgotPassword(email: String): String {
        val users = usersArray()
        val user = users.objects().firstOrNull { it.optString("email").equals(email, true) }
        if (user != null) {
            val token = "123456"
            user.put("resetToken", token)
            saveUsers(users)
        }
        return "Nếu email tồn tại, mã đặt lại trong bản local là 123456."
    }

    fun resetPassword(email: String, token: String, newPassword: String, confirmPassword: String): String {
        require(newPassword == confirmPassword) { "Mật khẩu nhập lại không khớp." }
        require(newPassword.length >= 6) { "Mật khẩu mới phải có ít nhất 6 ký tự." }
        val users = usersArray()
        val user = users.objects().firstOrNull { it.optString("email").equals(email, true) } ?: error("Email không tồn tại.")
        require(user.optString("resetToken") == token) { "Mã OTP/Token không đúng." }
        user.put("password", newPassword).remove("resetToken")
        saveUsers(users)
        return "Đặt lại mật khẩu thành công."
    }

    fun account(session: UserSession): AccountProfile {
        val user = findUser(session.username) ?: error("Không tìm thấy tài khoản.")
        return user.toProfile()
    }

    fun updateAccount(session: UserSession, profile: AccountProfile): AccountProfile {
        val users = usersArray()
        val user = users.objects().firstOrNull { it.optString("username").equals(session.username, true) } ?: error("Không tìm thấy tài khoản.")
        user.put("fullName", profile.fullName)
            .put("email", profile.email)
            .put("phone", profile.phone)
            .put("cccd", profile.cccd)
            .put("dateOfBirth", profile.dateOfBirth)
            .put("gender", profile.gender)
            .put("nationality", profile.nationality)
            .put("address", profile.address)
            .put("workplace", profile.workplace)
            .put("cccdFrontUrl", profile.cccdFrontUrl)
            .put("cccdBackUrl", profile.cccdBackUrl)
            .put("bankName", profile.bankName)
            .put("bankCode", profile.bankCode)
            .put("bankAccount", profile.bankAccount)
            .put("bankOwner", profile.bankOwner)
            .put("transferContent", profile.transferContent)
        saveUsers(users)
        return user.toProfile()
    }

    fun changePassword(session: UserSession, oldPassword: String, newPassword: String, confirmPassword: String): String {
        require(newPassword == confirmPassword) { "Mật khẩu nhập lại không khớp." }
        val users = usersArray()
        val user = users.objects().firstOrNull { it.optString("username").equals(session.username, true) } ?: error("Không tìm thấy tài khoản.")
        require(user.optString("password") == oldPassword) { "Mật khẩu cũ không đúng." }
        user.put("password", newPassword)
        saveUsers(users)
        return "Đổi mật khẩu thành công."
    }

    fun saveImage(fileName: String, mimeType: String, bytes: ByteArray): String {
        return "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    fun dashboard(): DashboardSummary {
        val rooms = list(AppScreen.Rooms)
        val invoices = list(AppScreen.Invoices)
        val payments = list(AppScreen.Payments)
        val pending = list(AppScreen.RentRequests).count { it.status.contains("Chờ", true) } +
            list(AppScreen.RenewRequests).count { it.status.contains("Chờ", true) } +
            payments.count { it.status.contains("Chờ", true) }
        return DashboardSummary(
            totalRooms = rooms.size,
            emptyRooms = rooms.count { it.status == "Còn trống" },
            unpaidInvoices = invoices.count { it.status != "Đã thanh toán" },
            pendingTasks = pending,
            revenue = payments.filter { it.status == "Đã xác nhận" }.sumOf { moneyValue(it.value) }
        )
    }

    fun list(screen: AppScreen): List<RentalItem> = itemsObject().optJSONArray(screen.name)?.toItems().orEmpty()

    fun list(screen: AppScreen, session: UserSession?): List<RentalItem> {
        val items = list(screen)
        return when (session?.role) {
            UserRole.NguoiDung -> filterTenantItems(screen, items, session.username)
            else -> items
        }
    }

    fun upsert(screen: AppScreen, item: RentalItem): RentalItem {
        val items = itemsObject()
        val array = items.optJSONArray(screen.name) ?: JSONArray()
        val existingIndex = array.objects().indexOfFirst { it.optString("id") == item.id }
        val json = item.toJson()
        if (existingIndex >= 0) {
            array.put(existingIndex, json)
        } else {
            array.put(0, json)
        }
        items.put(screen.name, array)
        saveItems(items)
        return item
    }

    fun delete(screen: AppScreen, id: String) {
        val items = itemsObject()
        val next = JSONArray()
        items.optJSONArray(screen.name)?.objects().orEmpty()
            .filterNot { it.optString("id") == id }
            .forEach { next.put(it) }
        items.put(screen.name, next)
        saveItems(items)
    }

    fun nextId(screen: AppScreen): String {
        val prefix = screen.shortCode.filter { it.isLetterOrDigit() }.ifBlank { "ID" }
        val next = list(screen).size + 1
        return "$prefix${next.toString().padStart(3, '0')}"
    }

    fun createRentRequest(roomId: String, session: UserSession, duration: String, note: String): RentalItem {
        val room = list(AppScreen.Rooms).firstOrNull { it.id == roomId } ?: error("Không tìm thấy phòng.")
        require(room.status.equals("Còn trống", true)) { "Phòng này hiện không còn trống." }
        val exists = list(AppScreen.RentRequests).any {
            it.detail("roomId") == roomId &&
                it.detail("tenantUsername").equals(session.username, true) &&
                it.status.contains("Chờ", true)
        }
        require(!exists) { "Bạn đã có yêu cầu thuê phòng này đang chờ xử lý." }
        val request = RentalItem(
            id = nextId(AppScreen.RentRequests),
            title = "${session.displayName} muốn thuê ${room.title}",
            status = "Chờ duyệt",
            value = duration.ifBlank { "6 tháng" },
            note = note.ifBlank { "Yêu cầu được gửi từ ứng dụng mobile." },
            details = listOf(
                "tenantUsername" to session.username,
                "tenantName" to session.displayName,
                "roomId" to room.id,
                "roomName" to room.title
            )
        )
        upsert(AppScreen.RentRequests, request)
        upsert(
            AppScreen.Notices,
            RentalItem(
                id = nextId(AppScreen.Notices),
                title = "Yêu cầu thuê mới từ ${session.displayName}",
                status = "Mới",
                value = "Yêu cầu thuê",
                note = "${session.displayName} muốn thuê ${room.title}",
                details = listOf("targetRole" to UserRole.ChuTro.name, "requestId" to request.id)
            )
        )
        return request
    }

    fun decideRentRequest(requestId: String, approve: Boolean, session: UserSession): RentalItem {
        val request = list(AppScreen.RentRequests).firstOrNull { it.id == requestId } ?: error("Không tìm thấy yêu cầu thuê.")
        require(request.status.contains("Chờ", true)) { "Yêu cầu này đã được xử lý." }
        val roomId = request.detail("roomId")
        val room = list(AppScreen.Rooms).firstOrNull { it.id == roomId } ?: error("Không tìm thấy phòng trong yêu cầu.")
        if (approve) {
            require(room.status.equals("Còn trống", true)) { "Phòng này không còn trống nên không thể duyệt yêu cầu." }
        }
        val nextRequest = request.copy(
            status = if (approve) "Đã duyệt" else "Từ chối",
            note = if (approve) "${request.note}\nĐã duyệt bởi ${session.displayName}." else "${request.note}\nĐã từ chối bởi ${session.displayName}."
        )
        upsert(AppScreen.RentRequests, nextRequest)
        if (!approve) return nextRequest

        val contract = RentalItem(
            id = nextId(AppScreen.Contracts),
            title = "Hợp đồng ${room.title} - ${request.detail("tenantName").ifBlank { request.detail("tenantUsername") }}",
            status = "Chờ người thuê xác nhận",
            value = request.value,
            note = "Tạo từ yêu cầu ${request.id}. Người thuê cần xác nhận để hoàn tất.",
            details = listOf(
                "requestId" to request.id,
                "tenantUsername" to request.detail("tenantUsername"),
                "tenantName" to request.detail("tenantName"),
                "roomId" to room.id,
                "roomName" to room.title
            )
        )
        upsert(AppScreen.Contracts, contract)
        upsert(
            AppScreen.Rooms,
            room.copy(
                status = "Đang giữ chỗ",
                note = "${room.note}\nĐang chờ ${request.detail("tenantName").ifBlank { request.detail("tenantUsername") }} xác nhận hợp đồng.",
                details = room.details
                    .replaceDetail("tenantUsername", request.detail("tenantUsername"))
                    .replaceDetail("tenantName", request.detail("tenantName"))
                    .replaceDetail("contractId", contract.id)
            )
        )
        upsert(
            AppScreen.Notices,
            RentalItem(
                id = nextId(AppScreen.Notices),
                title = "Hợp đồng đang chờ xác nhận",
                status = "Mới",
                value = "Hợp đồng",
                note = "Yêu cầu thuê ${room.title} đã được duyệt. Vui lòng xác nhận hợp đồng.",
                details = listOf("targetUser" to request.detail("tenantUsername"), "contractId" to contract.id)
            )
        )
        return nextRequest
    }

    fun confirmContract(contractId: String, approve: Boolean, session: UserSession): RentalItem {
        val contract = list(AppScreen.Contracts).firstOrNull { it.id == contractId } ?: error("Không tìm thấy hợp đồng.")
        require(contract.detail("tenantUsername").equals(session.username, true)) { "Bạn chỉ được xác nhận hợp đồng của mình." }
        require(contract.status == "Chờ người thuê xác nhận") { "Hợp đồng này không còn chờ xác nhận." }
        val nextContract = contract.copy(
            status = if (approve) "Đang hiệu lực" else "Người thuê từ chối",
            note = if (approve) "${contract.note}\nNgười thuê đã xác nhận." else "${contract.note}\nNgười thuê đã từ chối."
        )
        upsert(AppScreen.Contracts, nextContract)
        val roomId = contract.detail("roomId")
        val room = list(AppScreen.Rooms).firstOrNull { it.id == roomId } ?: error("Không tìm thấy phòng trong hợp đồng.")
        if (!approve) {
            upsert(
                AppScreen.Rooms,
                room.copy(
                    status = "Còn trống",
                    note = "${room.note}\nNgười thuê đã từ chối hợp đồng ${contract.id}.",
                    details = room.details
                        .removeDetail("tenantUsername")
                        .removeDetail("tenantName")
                        .removeDetail("contractId")
                )
            )
            return nextContract
        }
        upsert(
            AppScreen.Rooms,
            room.copy(
                status = "Đã thuê",
                note = "${room.note}\nĐang thuê bởi ${session.displayName}.",
                details = room.details
                    .replaceDetail("tenantUsername", session.username)
                    .replaceDetail("tenantName", session.displayName)
                    .replaceDetail("contractId", contract.id)
            )
        )
        val tenantExists = list(AppScreen.Tenants).any { it.detail("tenantUsername").equals(session.username, true) }
        if (!tenantExists) {
            upsert(
                AppScreen.Tenants,
                RentalItem(
                    id = nextId(AppScreen.Tenants),
                    title = session.displayName,
                    status = "Đang thuê",
                    value = session.username,
                    note = "Thuê ${room.title}",
                    details = listOf("tenantUsername" to session.username, "roomId" to room.id, "contractId" to contract.id)
                )
            )
        }
        return nextContract
    }

    fun saveContract(
        contractId: String,
        roomId: String,
        tenantUsername: String,
        startDate: String,
        endDate: String,
        deposit: String,
        note: String,
        status: String,
        session: UserSession
    ): RentalItem {
        val room = list(AppScreen.Rooms).firstOrNull { it.id == roomId } ?: error("Vui lòng chọn phòng hợp lệ.")
        val tenant = usersArray().objects().firstOrNull { it.optString("username").equals(tenantUsername, true) }
            ?: error("Không tìm thấy tài khoản người thuê.")
        require(UserRole.from(tenant.optString("role")) == UserRole.NguoiDung) { "Tài khoản được chọn không phải Người thuê." }
        require(startDate.isNotBlank()) { "Vui lòng nhập ngày bắt đầu." }
        require(endDate.isNotBlank()) { "Vui lòng nhập ngày kết thúc." }
        val id = contractId.ifBlank { nextId(AppScreen.Contracts) }
        val existing = list(AppScreen.Contracts).firstOrNull { it.id == id }
        val activeConflict = list(AppScreen.Contracts).any {
            it.id != id &&
                it.detail("roomId") == roomId &&
                it.status in setOf("Chờ người thuê xác nhận", "Đang hiệu lực")
        }
        require(!activeConflict) { "Phòng này đã có hợp đồng đang hiệu lực hoặc chờ xác nhận." }

        val tenantName = tenant.optString("fullName").ifBlank { tenantUsername }
        val contract = RentalItem(
            id = id,
            title = "Hợp đồng ${room.title} - $tenantName",
            status = status.ifBlank { "Chờ người thuê xác nhận" },
            value = "$startDate - $endDate",
            note = note.ifBlank { "Tiền cọc: $deposit" },
            details = listOf(
                "roomId" to room.id,
                "roomName" to room.title,
                "tenantUsername" to tenantUsername,
                "tenantName" to tenantName,
                "startDate" to startDate,
                "endDate" to endDate,
                "deposit" to deposit,
                "createdBy" to session.username
            )
        )
        upsert(AppScreen.Contracts, contract)
        if (contract.status == "Chờ người thuê xác nhận") {
            upsert(
                AppScreen.Rooms,
                room.copy(
                    status = "Đang giữ chỗ",
                    note = "${room.note}\nĐang chờ $tenantName xác nhận hợp đồng.",
                    details = room.details
                        .replaceDetail("tenantUsername", tenantUsername)
                        .replaceDetail("tenantName", tenantName)
                        .replaceDetail("contractId", contract.id)
                    )
            )
        }
        if (contract.status == "Đang hiệu lực") {
            upsert(
                AppScreen.Rooms,
                room.copy(
                    status = "Đã thuê",
                    note = "${room.note}\nĐang thuê bởi $tenantName.",
                    details = room.details
                        .replaceDetail("tenantUsername", tenantUsername)
                        .replaceDetail("tenantName", tenantName)
                        .replaceDetail("contractId", contract.id)
                )
            )
            val tenantExists = list(AppScreen.Tenants).any { it.detail("contractId") == contract.id }
            if (!tenantExists) {
                upsert(
                    AppScreen.Tenants,
                    RentalItem(
                        id = nextId(AppScreen.Tenants),
                        title = tenantName,
                        status = "Đang thuê",
                        value = tenantUsername,
                        note = "Thuê ${room.title}",
                        details = listOf("tenantUsername" to tenantUsername, "roomId" to room.id, "contractId" to contract.id)
                    )
                )
            }
        }
        return contract
    }

    fun closeContract(contractId: String, cancel: Boolean, session: UserSession): RentalItem {
        val contract = list(AppScreen.Contracts).firstOrNull { it.id == contractId } ?: error("Không tìm thấy hợp đồng.")
        require(contract.status in setOf("Chờ người thuê xác nhận", "Đang hiệu lực")) { "Chỉ xử lý được hợp đồng đang chờ hoặc đang hiệu lực." }
        val next = contract.copy(
            status = if (cancel) "Đã hủy" else "Đã kết thúc",
            note = "${contract.note}\n${if (cancel) "Đã hủy" else "Đã kết thúc"} bởi ${session.displayName}."
        )
        upsert(AppScreen.Contracts, next)
        val room = list(AppScreen.Rooms).firstOrNull { it.id == contract.detail("roomId") }
        if (room != null) {
            upsert(
                AppScreen.Rooms,
                room.copy(
                    status = "Còn trống",
                    note = "${room.note}\nPhòng đã được mở lại sau hợp đồng ${contract.id}.",
                    details = room.details
                        .removeDetail("tenantUsername")
                        .removeDetail("tenantName")
                        .removeDetail("contractId")
                )
            )
        }
        val tenant = list(AppScreen.Tenants).firstOrNull { it.detail("contractId") == contract.id }
        if (tenant != null) {
            upsert(AppScreen.Tenants, tenant.copy(status = if (cancel) "Đã hủy" else "Đã rời phòng"))
        }
        return next
    }

    fun createRenewRequest(contractId: String, session: UserSession, newEndDate: String, note: String): RentalItem {
        val contract = list(AppScreen.Contracts).firstOrNull { it.id == contractId } ?: error("Không tìm thấy hợp đồng.")
        require(contract.detail("tenantUsername").equals(session.username, true)) { "Bạn chỉ được gia hạn hợp đồng của mình." }
        require(contract.status == "Đang hiệu lực") { "Chỉ hợp đồng đang hiệu lực mới được gửi yêu cầu gia hạn." }
        require(newEndDate.isNotBlank()) { "Vui lòng nhập ngày kết thúc mới." }
        val exists = list(AppScreen.RenewRequests).any {
            it.detail("contractId") == contract.id && it.status.contains("Chờ", true)
        }
        require(!exists) { "Hợp đồng này đã có yêu cầu gia hạn đang chờ duyệt." }
        val request = RentalItem(
            id = nextId(AppScreen.RenewRequests),
            title = "Gia hạn ${contract.title}",
            status = "Chờ duyệt",
            value = "Đến $newEndDate",
            note = note.ifBlank { "Người thuê muốn gia hạn hợp đồng." },
            details = listOf(
                "contractId" to contract.id,
                "tenantUsername" to session.username,
                "tenantName" to session.displayName,
                "roomId" to contract.detail("roomId"),
                "oldEndDate" to contract.detail("endDate"),
                "newEndDate" to newEndDate
            )
        )
        upsert(AppScreen.RenewRequests, request)
        return request
    }

    fun decideRenewRequest(requestId: String, approve: Boolean, session: UserSession): RentalItem {
        val request = list(AppScreen.RenewRequests).firstOrNull { it.id == requestId } ?: error("Không tìm thấy yêu cầu gia hạn.")
        require(request.status.contains("Chờ", true)) { "Yêu cầu gia hạn này đã được xử lý." }
        val nextRequest = request.copy(
            status = if (approve) "Đã duyệt" else "Từ chối",
            note = if (approve) "${request.note}\nĐã duyệt bởi ${session.displayName}." else "${request.note}\nĐã từ chối bởi ${session.displayName}."
        )
        upsert(AppScreen.RenewRequests, nextRequest)
        if (approve) {
            val contract = list(AppScreen.Contracts).firstOrNull { it.id == request.detail("contractId") }
                ?: error("Không tìm thấy hợp đồng cần gia hạn.")
            upsert(
                AppScreen.Contracts,
                contract.copy(
                    value = "${contract.detail("startDate")} - ${request.detail("newEndDate")}",
                    note = "${contract.note}\nĐã gia hạn đến ${request.detail("newEndDate")}.",
                    details = contract.details.replaceDetail("endDate", request.detail("newEndDate"))
                )
            )
        }
        return nextRequest
    }

    private fun seedIfNeeded() {
        if (prefs.getBoolean("seeded", false)) {
            migrateDemoLinksIfNeeded()
            return
        }
        saveUsers(
            JSONArray()
                .put(seedUser(1, "Admin", "Admin123", UserRole.Admin, "Admin hệ thống", "admin@demo.local"))
                .put(seedUser(2, "chutro", "123456", UserRole.ChuTro, "Nguyễn Minh Quân", "chutro@example.com"))
                .put(seedUser(3, "nguoithue", "123456", UserRole.NguoiDung, "Người Thuê Demo", "nguoithue@example.com"))
        )
        saveItems(seedItems())
        prefs.edit().putBoolean("seeded", true).apply()
    }

    private fun migrateDemoLinksIfNeeded() {
        val contract = list(AppScreen.Contracts).firstOrNull { it.id == "HD001" } ?: return
        if (contract.detail("tenantUsername").isNotBlank()) return
        val room = list(AppScreen.Rooms).firstOrNull { it.id == "P101" }
        if (room != null) {
            upsert(
                AppScreen.Rooms,
                room.copy(
                    status = "Đã thuê",
                    details = room.details
                        .replaceDetail("tenantUsername", "nguoithue")
                        .replaceDetail("tenantName", "Người Thuê Demo")
                        .replaceDetail("contractId", "HD001")
                )
            )
        }
        upsert(
            AppScreen.Contracts,
            contract.copy(
                title = "Hợp đồng P101 - Người Thuê Demo",
                status = "Đang hiệu lực",
                value = "01/05/2026 - 01/05/2027",
                details = contract.details
                    .replaceDetail("tenantUsername", "nguoithue")
                    .replaceDetail("tenantName", "Người Thuê Demo")
                    .replaceDetail("roomId", "P101")
                    .replaceDetail("roomName", "Phòng A01")
                    .replaceDetail("startDate", "01/05/2026")
                    .replaceDetail("endDate", "01/05/2027")
                    .replaceDetail("deposit", "3.200.000đ")
            )
        )
    }

    private fun seedUser(id: Int, username: String, password: String, role: UserRole, fullName: String, email: String): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("username", username)
            .put("password", password)
            .put("role", role.name)
            .put("fullName", fullName)
            .put("email", email)
            .put("phone", "")
            .put("cccd", "")
    }

    private fun seedItems(): JSONObject {
        val obj = JSONObject()
        obj.put(AppScreen.Houses.name, JSONArray().put(item("NT01", "Nhà trọ An Bình", "Đang hoạt động", "20 phòng", "Quận 9, TP.HCM")))
        obj.put(AppScreen.RoomTypes.name, JSONArray().put(item("LP01", "Phòng thường", "Đang dùng", "2.300.000đ - 3.000.000đ", "Phòng cơ bản, chi phí hợp lý")))
        obj.put(AppScreen.Rooms.name, JSONArray()
            .put(itemWithDetails("P101", "Phòng A01", "Đã thuê", "3.200.000đ/tháng", "Tầng 1 - Nhà trọ An Bình", listOf("tenantUsername" to "nguoithue", "tenantName" to "Người Thuê Demo", "contractId" to "HD001")))
            .put(item("P102", "Phòng A02", "Còn trống", "2.750.000đ/tháng", "Sẵn sàng cho thuê")))
        obj.put(AppScreen.Tenants.name, JSONArray().put(itemWithDetails("KT001", "Người Thuê Demo", "Đang thuê", "nguoithue", "Phòng P101", listOf("tenantUsername" to "nguoithue", "roomId" to "P101", "contractId" to "HD001"))))
        obj.put(AppScreen.Contracts.name, JSONArray().put(itemWithDetails("HD001", "Hợp đồng P101 - Người Thuê Demo", "Đang hiệu lực", "01/05/2026 - 01/05/2027", "Tiền cọc 3.200.000đ", listOf("tenantUsername" to "nguoithue", "tenantName" to "Người Thuê Demo", "roomId" to "P101", "roomName" to "Phòng A01", "startDate" to "01/05/2026", "endDate" to "01/05/2027", "deposit" to "3.200.000đ"))))
        obj.put(AppScreen.Invoices.name, JSONArray().put(item("H001", "Hóa đơn P101 kỳ 2026-05", "Chưa thanh toán", "3.815.000đ", "Tiền phòng + điện + nước")))
        obj.put(AppScreen.Payments.name, JSONArray().put(item("TT001", "Biên lai P101", "Chờ xác nhận", "3.815.000đ", "Chờ chủ trọ xác nhận")))
        obj.put(AppScreen.Services.name, JSONArray().put(item("DV01", "Internet", "Tính phí", "100.000đ/tháng", "Tính theo phòng")))
        obj.put(AppScreen.ServiceRegs.name, JSONArray().put(item("DK001", "P101 dùng Internet", "Đang sử dụng", "100.000đ/tháng", "Đăng ký kỳ 2026-05")))
        obj.put(AppScreen.Electric.name, JSONArray().put(item("D001", "Điện P101 kỳ 2026-05", "Đã ghi", "70 kWh x 3.500đ", "245.000đ")))
        obj.put(AppScreen.Water.name, JSONArray().put(item("N001", "Nước P101 kỳ 2026-05", "Đã ghi", "6 m3 x 15.000đ", "90.000đ")))
        obj.put(AppScreen.RentRequests.name, JSONArray().put(item("YT001", "Lê Văn Nam muốn thuê P102", "Chờ duyệt", "6 tháng", "Muốn vào ngày 10/06/2026")))
        obj.put(AppScreen.RenewRequests.name, JSONArray().put(item("GH001", "Gia hạn hợp đồng P101", "Chờ duyệt", "Thêm 6 tháng", "Người thuê muốn giữ phòng")))
        obj.put(AppScreen.Incidents.name, JSONArray().put(item("SC001", "Rò nước trong phòng P101", "Mới", "Rất gấp", "Người thuê vừa báo cáo")))
        obj.put(AppScreen.Notices.name, JSONArray().put(item("TB001", "Hóa đơn tháng 05 đã được tạo", "Mới", "Hóa đơn", "Vui lòng thanh toán trước ngày 10")))
        obj.put(AppScreen.Users.name, JSONArray()
            .put(item("U001", "Admin hệ thống", "Admin", "admin@demo.local", "Quản lý toàn bộ hệ thống"))
            .put(item("U002", "Nguyễn Minh Quân", "Chủ trọ", "chutro@example.com", "Chủ trọ Nhà trọ An Bình"))
            .put(item("U003", "Người Thuê Demo", "Người thuê", "nguoithue@example.com", "Người thuê phòng P101")))
        return obj
    }

    private fun addUserListItem(id: Int, username: String, role: UserRole, email: String) {
        upsert(AppScreen.Users, RentalItem("U${id.toString().padStart(3, '0')}", username, role.label, email, "Tài khoản đăng ký trong app"))
    }

    private fun item(id: String, title: String, status: String, value: String, note: String): JSONObject = RentalItem(id, title, status, value, note).toJson()
    private fun itemWithDetails(id: String, title: String, status: String, value: String, note: String, details: List<Pair<String, String>>): JSONObject {
        return RentalItem(id, title, status, value, note, details).toJson()
    }
    private fun usersArray(): JSONArray = JSONArray(prefs.getString("users", "[]") ?: "[]")
    private fun itemsObject(): JSONObject = JSONObject(prefs.getString("items", "{}") ?: "{}")
    private fun saveUsers(users: JSONArray) = prefs.edit().putString("users", users.toString()).apply()
    private fun saveItems(items: JSONObject) = prefs.edit().putString("items", items.toString()).apply()
    private fun findUser(username: String): JSONObject? = usersArray().objects().firstOrNull { it.optString("username").equals(username, true) }

    private fun filterTenantItems(screen: AppScreen, items: List<RentalItem>, username: String): List<RentalItem> = when (screen) {
        AppScreen.Rooms -> items.filter { it.status.equals("Còn trống", true) || it.detail("tenantUsername").equals(username, true) }
        AppScreen.Houses, AppScreen.RoomTypes, AppScreen.Services, AppScreen.Notices -> items.filter {
            it.detail("targetUser").isBlank() || it.detail("targetUser").equals(username, true)
        }
        AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments, AppScreen.ServiceRegs,
        AppScreen.Electric, AppScreen.Water, AppScreen.RentRequests, AppScreen.RenewRequests,
        AppScreen.Incidents, AppScreen.Tenants -> items.filter {
            it.detail("tenantUsername").equals(username, true) ||
                it.note.contains(username, true) ||
                it.value.equals(username, true)
        }
        AppScreen.Users -> emptyList()
        else -> items
    }
}

private fun RentalItem.detail(key: String): String = details.firstOrNull { it.first == key }?.second.orEmpty()
private fun List<Pair<String, String>>.replaceDetail(key: String, value: String): List<Pair<String, String>> {
    val withoutKey = filterNot { it.first == key }
    return if (value.isBlank()) withoutKey else withoutKey + (key to value)
}

private fun List<Pair<String, String>>.removeDetail(key: String): List<Pair<String, String>> = filterNot { it.first == key }

private fun JSONObject.toSession(): UserSession = UserSession(
    token = "local:${optString("username")}:${System.currentTimeMillis()}",
    role = UserRole.from(optString("role")),
    displayName = optString("fullName").ifBlank { optString("username") },
    username = optString("username")
)

private fun JSONObject.toProfile(): AccountProfile = AccountProfile(
    userId = optInt("id"),
    username = optString("username"),
    fullName = optString("fullName"),
    email = optString("email"),
    phone = optString("phone"),
    cccd = optString("cccd"),
    dateOfBirth = optString("dateOfBirth"),
    gender = optString("gender"),
    nationality = optString("nationality"),
    address = optString("address"),
    workplace = optString("workplace"),
    cccdFrontUrl = optString("cccdFrontUrl"),
    cccdBackUrl = optString("cccdBackUrl"),
    bankName = optString("bankName"),
    bankCode = optString("bankCode"),
    bankAccount = optString("bankAccount"),
    bankOwner = optString("bankOwner"),
    transferContent = optString("transferContent"),
    role = UserRole.from(optString("role"))
)

private fun RentalItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("status", status)
    .put("value", value)
    .put("note", note)
    .put("details", JSONArray(details.map { JSONObject().put("label", it.first).put("value", it.second) }))

private fun JSONObject.toRentalItem(): RentalItem = RentalItem(
    id = optString("id"),
    title = optString("title"),
    status = optString("status"),
    value = optString("value"),
    note = optString("note"),
    details = optJSONArray("details")?.objects().orEmpty().map { it.optString("label") to it.optString("value") }
)

private fun JSONArray.toItems(): List<RentalItem> = objects().map { it.toRentalItem() }
private fun JSONArray.objects(): List<JSONObject> = List(length()) { index -> getJSONObject(index) }
