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

    fun getLatestUtilityIndex(screen: AppScreen, roomId: String): Double {
        val readings = list(screen)
        val roomReadings = readings.filter { it.detail("roomId") == roomId }
        if (roomReadings.isEmpty()) return 0.0
        val sorted = roomReadings.sortedByDescending { it.detail("period") }
        return sorted.first().detail("newIndex").toDoubleOrNull() ?: 0.0
    }

    fun saveUtilityReading(
        screen: AppScreen,
        roomId: String,
        period: String,
        oldIndex: Double,
        newIndex: Double,
        price: Double
    ): RentalItem {
        val room = list(AppScreen.Rooms).firstOrNull { it.id == roomId } ?: error("Không tìm thấy phòng.")
        val roomName = room.title
        val tenantUsername = room.detail("tenantUsername")
        val consumption = (newIndex - oldIndex).coerceAtLeast(0.0)
        val amount = consumption * price
        val unit = if (screen == AppScreen.Electric) "kWh" else "m3"

        val id = nextId(screen)
        val title = "${if (screen == AppScreen.Electric) "Điện" else "Nước"} $roomName kỳ $period"

        val reading = RentalItem(
            id = id,
            title = title,
            status = "Đã ghi",
            value = "${consumption.toLong()} $unit x ${com.example.myapplication.domain.util.formatMoney(price.toLong())}",
            note = com.example.myapplication.domain.util.formatMoney(amount.toLong()),
            details = listOf(
                "roomId" to roomId,
                "roomName" to roomName,
                "period" to period,
                "oldIndex" to oldIndex.toString(),
                "newIndex" to newIndex.toString(),
                "consumption" to consumption.toString(),
                "price" to price.toString(),
                "amount" to amount.toString(),
                "tenantUsername" to tenantUsername
            )
        )

        upsert(screen, reading)
        return reading
    }

    fun createInvoice(
        roomId: String,
        period: String,
        otherCost: Double,
        otherNote: String
    ): RentalItem {
        val room = list(AppScreen.Rooms).firstOrNull { it.id == roomId } ?: error("Không tìm thấy phòng.")
        val tenantUsername = room.detail("tenantUsername")
        require(tenantUsername.isNotBlank()) { "Phòng này hiện chưa có người thuê." }

        // Chặn trùng hóa đơn
        val exists = list(AppScreen.Invoices).any {
            it.detail("roomId") == roomId && it.detail("period") == period
        }
        require(!exists) { "Hóa đơn phòng ${room.title} kỳ $period đã tồn tại." }

        val roomPrice = moneyValue(room.value)

        // Lấy tiền điện
        val electricItem = list(AppScreen.Electric).firstOrNull {
            it.detail("roomId") == roomId && it.detail("period") == period
        }
        val electricCost = electricItem?.detail("amount")?.toDoubleOrNull()?.toLong()
            ?: electricItem?.let { moneyValue(it.note) }
            ?: 0L

        // Lấy tiền nước
        val waterItem = list(AppScreen.Water).firstOrNull {
            it.detail("roomId") == roomId && it.detail("period") == period
        }
        val waterCost = waterItem?.detail("amount")?.toDoubleOrNull()?.toLong()
            ?: waterItem?.let { moneyValue(it.note) }
            ?: 0L

        // Tiền dịch vụ
        val serviceRegs = list(AppScreen.ServiceRegs).filter {
            it.detail("roomId") == roomId ||
            it.title.contains(roomId, true) ||
            it.title.contains(room.title, true)
        }
        val servicesCost = serviceRegs.sumOf { moneyValue(it.value) }

        val totalAmount = roomPrice + electricCost + waterCost + servicesCost + otherCost.toLong()

        val id = nextId(AppScreen.Invoices)
        val title = "Hóa đơn ${room.title} kỳ $period"

        val summaryNote = buildString {
            append("Tiền phòng: ${com.example.myapplication.domain.util.formatMoney(roomPrice)}")
            if (electricCost > 0) append(" + Điện: ${com.example.myapplication.domain.util.formatMoney(electricCost)}")
            if (waterCost > 0) append(" + Nước: ${com.example.myapplication.domain.util.formatMoney(waterCost)}")
            if (servicesCost > 0) append(" + Dịch vụ: ${com.example.myapplication.domain.util.formatMoney(servicesCost)}")
            if (otherCost > 0) {
                append(" + Phát sinh: ${com.example.myapplication.domain.util.formatMoney(otherCost.toLong())}")
                if (otherNote.isNotBlank()) append(" (${otherNote})")
            }
        }

        val invoice = RentalItem(
            id = id,
            title = title,
            status = "Chưa thanh toán",
            value = com.example.myapplication.domain.util.formatMoney(totalAmount),
            note = summaryNote,
            details = listOf(
                "roomId" to roomId,
                "roomName" to room.title,
                "period" to period,
                "roomPrice" to roomPrice.toString(),
                "electricCost" to electricCost.toString(),
                "waterCost" to waterCost.toString(),
                "servicesCost" to servicesCost.toString(),
                "otherCost" to otherCost.toString(),
                "totalAmount" to totalAmount.toString(),
                "tenantUsername" to tenantUsername,
                "otherNote" to otherNote
            )
        )

        upsert(AppScreen.Invoices, invoice)

        // Tạo thông báo cho người thuê
        upsert(
            AppScreen.Notices,
            RentalItem(
                id = nextId(AppScreen.Notices),
                title = "Hóa đơn mới kỳ $period",
                status = "Mới",
                value = "Hóa đơn",
                note = "Hóa đơn cho ${room.title} kỳ $period đã được lập. Số tiền: ${com.example.myapplication.domain.util.formatMoney(totalAmount)}.",
                details = listOf(
                    "targetUser" to tenantUsername,
                    "invoiceId" to invoice.id
                )
            )
        )

        return invoice
    }

    fun submitPayment(
        invoiceId: String,
        transactionId: String,
        receiptImage: String,
        note: String,
        session: UserSession
    ): RentalItem {
        val invoice = list(AppScreen.Invoices).firstOrNull { it.id == invoiceId } ?: error("Không tìm thấy hóa đơn.")
        val roomId = invoice.detail("roomId")
        val room = list(AppScreen.Rooms).firstOrNull { it.id == roomId } ?: error("Không tìm thấy phòng.")

        val id = nextId(AppScreen.Payments)
        val title = "Biên lai ${room.title} kỳ ${invoice.detail("period")}"

        val payment = RentalItem(
            id = id,
            title = title,
            status = "Chờ xác nhận",
            value = invoice.value,
            note = "Mã GD: $transactionId${if (note.isNotBlank()) " | $note" else ""}",
            details = listOf(
                "invoiceId" to invoiceId,
                "roomId" to roomId,
                "roomName" to room.title,
                "period" to invoice.detail("period"),
                "tenantUsername" to session.username,
                "tenantName" to session.displayName,
                "transactionId" to transactionId,
                "receiptImage" to receiptImage
            )
        )

        upsert(AppScreen.Payments, payment)

        // Cập nhật trạng thái hóa đơn sang "Chờ xác nhận"
        upsert(AppScreen.Invoices, invoice.copy(status = "Chờ xác nhận"))

        // Thông báo cho chủ trọ
        upsert(
            AppScreen.Notices,
            RentalItem(
                id = nextId(AppScreen.Notices),
                title = "Biên lai thanh toán mới từ ${session.displayName}",
                status = "Mới",
                value = "Thanh toán",
                note = "${session.displayName} đã gửi biên lai cho ${room.title}. Số tiền: ${invoice.value}.",
                details = listOf(
                    "targetRole" to UserRole.ChuTro.name,
                    "paymentId" to payment.id
                )
            )
        )

        return payment
    }

    fun decidePayment(
        paymentId: String,
        approve: Boolean,
        rejectReason: String?,
        session: UserSession
    ): RentalItem {
        val payment = list(AppScreen.Payments).firstOrNull { it.id == paymentId } ?: error("Không tìm thấy biên lai thanh toán.")
        require(payment.status == "Chờ xác nhận") { "Biên lai này đã được xử lý." }

        val invoiceId = payment.detail("invoiceId")
        val invoice = list(AppScreen.Invoices).firstOrNull { it.id == invoiceId } ?: error("Không tìm thấy hóa đơn liên kết.")

        val nextPayment = payment.copy(
            status = if (approve) "Đã xác nhận" else "Từ chối",
            note = payment.note + (if (approve) "\nĐã duyệt bởi ${session.displayName}." else "\nBị từ chối bởi ${session.displayName}. Lý do: ${rejectReason.orEmpty()}")
        )
        upsert(AppScreen.Payments, nextPayment)

        val nextInvoice = invoice.copy(
            status = if (approve) "Đã thanh toán" else "Chưa thanh toán",
            note = invoice.note + (if (approve) "" else " (Bị từ chối thanh toán: ${rejectReason.orEmpty()})")
        )
        upsert(AppScreen.Invoices, nextInvoice)

        // Thông báo cho người thuê
        val tenantUsername = payment.detail("tenantUsername")
        upsert(
            AppScreen.Notices,
            RentalItem(
                id = nextId(AppScreen.Notices),
                title = if (approve) "Thanh toán hóa đơn thành công" else "Biên lai thanh toán bị từ chối",
                status = "Mới",
                value = "Thanh toán",
                note = if (approve) "Hóa đơn kỳ ${payment.detail("period")} đã được xác nhận thanh toán." else "Lý do: ${rejectReason.orEmpty()}",
                details = listOf(
                    "targetUser" to tenantUsername,
                    "paymentId" to payment.id
                )
            )
        )

        return payment
    }

    // ─── Mốc D: Sự cố & Thông báo ────────────────────────────────────────────

    fun respondToIncident(
        incidentId: String,
        response: String,
        newStatus: String,
        session: UserSession
    ): RentalItem {
        val incident = list(AppScreen.Incidents).firstOrNull { it.id == incidentId }
            ?: error("Không tìm thấy sự cố.")
        val now = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val updatedNote = incident.note + "\n[${session.displayName} - $now]: $response"
        val updated = incident.copy(
            status = newStatus,
            note = updatedNote,
            details = incident.details
                .replaceDetail("responseBy", session.displayName)
                .replaceDetail("responseAt", now)
                .replaceDetail("response", response)
        )
        upsert(AppScreen.Incidents, updated)

        // Thông báo cho người thuê
        val tenantUsername = incident.detail("tenantUsername")
        if (tenantUsername.isNotBlank()) {
            upsert(
                AppScreen.Notices,
                RentalItem(
                    id = nextId(AppScreen.Notices),
                    title = "Cập nhật sự cố: ${incident.title}",
                    status = "Mới",
                    value = "Sự cố",
                    note = "Phản hồi từ ${session.displayName}: $response (Trạng thái: $newStatus)",
                    details = listOf(
                        "targetUser" to tenantUsername,
                        "incidentId" to incidentId
                    )
                )
            )
        }
        return updated
    }

    fun markNoticeAsRead(noticeId: String, username: String): RentalItem {
        val notice = list(AppScreen.Notices).firstOrNull { it.id == noticeId }
            ?: error("Không tìm thấy thông báo.")
        val readKey = "readBy_$username"
        if (notice.detail(readKey) == "true") return notice
        val updated = notice.copy(
            status = if (notice.detail("targetUser").isBlank()) notice.status else "Đã đọc",
            details = notice.details.replaceDetail(readKey, "true")
        )
        upsert(AppScreen.Notices, updated)
        return updated
    }

    fun createNotice(
        title: String,
        content: String,
        targetType: String, // "all", "room:<roomId>", "user:<username>"
        session: UserSession
    ): RentalItem {
        require(title.isNotBlank()) { "Tiêu đề thông báo không được để trống." }
        require(content.isNotBlank()) { "Nội dung thông báo không được để trống." }

        val targetUser = if (targetType.startsWith("user:")) targetType.removePrefix("user:") else ""
        val targetRoom = if (targetType.startsWith("room:")) targetType.removePrefix("room:") else ""

        val notice = RentalItem(
            id = nextId(AppScreen.Notices),
            title = title,
            status = "Mới",
            value = "Thông báo",
            note = content,
            details = listOf(
                "targetUser" to targetUser,
                "targetRoom" to targetRoom,
                "targetType" to targetType,
                "createdBy" to session.displayName
            )
        )
        upsert(AppScreen.Notices, notice)
        return notice
    }

    fun unreadNoticeCount(username: String): Int {
        val items = list(AppScreen.Notices)
        return items.count { notice ->
            val forUser = notice.detail("targetUser").let { it.isBlank() || it == username }
            val notRead = notice.detail("readBy_$username") != "true"
            forUser && notRead
        }
    }

    private fun seedIfNeeded() {
        val currentVersion = prefs.getInt("seed_version", 0)
        val targetVersion = 4
        if (currentVersion >= targetVersion) {
            migrateDemoLinksIfNeeded()
            return
        }
        // Xóa dữ liệu cũ và seed lại khi version thay đổi
        prefs.edit().clear().apply()
        saveUsers(
            JSONArray()
                .put(seedUser(1, "Admin", "Admin123", UserRole.Admin, "Admin hệ thống", "admin@demo.local"))
                .put(seedUser(2, "chutro", "123456", UserRole.ChuTro, "Nguyễn Minh Quân", "chutro@example.com"))
                .put(seedUser(3, "nguoithue", "123456", UserRole.NguoiDung, "Người Thuê Demo", "nguoithue@example.com"))
                .put(seedUser(4, "chutro1", "123456", UserRole.ChuTro, "Trần Quốc Tuấn", "tuantq@gmail.com", "0912345678", "012345678901", "15/08/1985", "Nam", "Việt Nam", "123 Đường Láng, Đống Đa, Hà Nội", "Tự do", "Vietcombank", "VCB", "1011121314", "TRAN QUOC TUAN", "chutro1 thanh toan"))
                .put(seedUser(5, "chutro2", "123456", UserRole.ChuTro, "Lê Thị Hồng", "honglt@gmail.com", "0987654321", "098765432109", "20/11/1990", "Nữ", "Việt Nam", "456 Điện Biên Phủ, Quận 3, TP.HCM", "Kinh doanh", "Techcombank", "TCB", "190220330440", "LE THI HONG", "chutro2 thanh toan"))
                .put(seedUser(6, "nguoithue1", "123456", UserRole.NguoiDung, "Phạm Văn Nam", "nampv@gmail.com", "0905123456", "034567890123", "10/02/1998", "Nam", "Việt Nam", "789 Cách Mạng Tháng 8, Quận 10, TP.HCM", "FPT Software", "MB Bank", "MBB", "999988887777", "PHAM VAN NAM", "nguoithue1 chuyen khoan"))
                .put(seedUser(7, "nguoithue2", "123456", UserRole.NguoiDung, "Nguyễn Thu Thảo", "thaont@gmail.com", "0934567890", "079876543210", "05/05/2001", "Nữ", "Việt Nam", "321 Lê Lợi, Hải Châu, Đà Nẵng", "Đại học Bách Khoa", "VietinBank", "CTG", "108888777666", "NGUYEN THU THAO", "nguoithue2 chuyen khoan"))
        )
        saveItems(seedItems())
        prefs.edit()
            .putBoolean("seeded", true)
            .putInt("seed_version", targetVersion)
            .apply()
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

    private fun seedUser(
        id: Int,
        username: String,
        password: String,
        role: UserRole,
        fullName: String,
        email: String,
        phone: String = "",
        cccd: String = "",
        dateOfBirth: String = "",
        gender: String = "",
        nationality: String = "",
        address: String = "",
        workplace: String = "",
        bankName: String = "",
        bankCode: String = "",
        bankAccount: String = "",
        bankOwner: String = "",
        transferContent: String = ""
    ): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("username", username)
            .put("password", password)
            .put("role", role.name)
            .put("fullName", fullName)
            .put("email", email)
            .put("phone", phone)
            .put("cccd", cccd)
            .put("dateOfBirth", dateOfBirth)
            .put("gender", gender)
            .put("nationality", nationality)
            .put("address", address)
            .put("workplace", workplace)
            .put("bankName", bankName)
            .put("bankCode", bankCode)
            .put("bankAccount", bankAccount)
            .put("bankOwner", bankOwner)
            .put("transferContent", transferContent)
    }

    private fun seedItems(): JSONObject {
        val obj = JSONObject()
        obj.put(AppScreen.Houses.name, JSONArray()
            .put(item("NT01", "Nhà trọ An Bình", "Đang hoạt động", "20 phòng", "Quận 9, TP.HCM"))
            .put(item("NT02", "Nhà trọ Bình Minh", "Đang hoạt động", "10 phòng", "Quận Thủ Đức, TP.HCM"))
            .put(item("NT03", "Nhà trọ Hồng Hà", "Đang hoạt động", "15 phòng", "Quận Bình Thạnh, TP.HCM")))
        obj.put(AppScreen.RoomTypes.name, JSONArray()
            .put(item("LP01", "Phòng thường", "Đang dùng", "2.300.000đ - 3.000.000đ", "Phòng cơ bản, chi phí hợp lý"))
            .put(item("LP02", "Phòng VIP", "Đang dùng", "4.000.000đ - 5.000.000đ", "Phòng máy lạnh, đầy đủ tiện nghi")))
        obj.put(AppScreen.Rooms.name, JSONArray()
            .put(itemWithDetails("P101", "Phòng A01", "Đã thuê", "3.200.000đ/tháng", "Tầng 1 - Nhà trọ An Bình", listOf("tenantUsername" to "nguoithue", "tenantName" to "Người Thuê Demo", "contractId" to "HD001")))
            .put(item("P102", "Phòng A02", "Còn trống", "2.750.000đ/tháng", "Sẵn sàng cho thuê"))
            .put(itemWithDetails("P201", "Phòng B01", "Đã thuê", "3.500.000đ/tháng", "Tầng 2 - Nhà trọ Bình Minh", listOf("tenantUsername" to "nguoithue1", "tenantName" to "Phạm Văn Nam", "contractId" to "HD002")))
            .put(itemWithDetails("P202", "Phòng B02", "Đã thuê", "4.500.000đ/tháng", "Tầng 2 - Nhà trọ Bình Minh", listOf("tenantUsername" to "nguoithue2", "tenantName" to "Nguyễn Thu Thảo", "contractId" to "HD003")))
            .put(item("P203", "Phòng C01", "Còn trống", "3.000.000đ/tháng", "Sẵn sàng cho thuê - Nhà trọ Hồng Hà"))
            .put(item("P204", "Phòng C02", "Còn trống", "3.200.000đ/tháng", "Sẵn sàng cho thuê - Nhà trọ Hồng Hà")))
        obj.put(AppScreen.Tenants.name, JSONArray()
            .put(itemWithDetails("KT001", "Người Thuê Demo", "Đang thuê", "nguoithue", "Phòng P101", listOf("tenantUsername" to "nguoithue", "roomId" to "P101", "contractId" to "HD001")))
            .put(itemWithDetails("KT002", "Phạm Văn Nam", "Đang thuê", "nguoithue1", "Phòng P201", listOf("tenantUsername" to "nguoithue1", "roomId" to "P201", "contractId" to "HD002")))
            .put(itemWithDetails("KT003", "Nguyễn Thu Thảo", "Đang thuê", "nguoithue2", "Phòng P202", listOf("tenantUsername" to "nguoithue2", "roomId" to "P202", "contractId" to "HD003"))))
        obj.put(AppScreen.Contracts.name, JSONArray().put(itemWithDetails("HD001", "Hợp đồng P101 - Người Thuê Demo", "Đang hiệu lực", "01/05/2026 - 01/05/2027", "Tiền cọc 3.200.000đ", listOf("tenantUsername" to "nguoithue", "tenantName" to "Người Thuê Demo", "roomId" to "P101", "roomName" to "Phòng A01", "startDate" to "01/05/2026", "endDate" to "01/05/2027", "deposit" to "3.200.000đ"))))
        obj.put(AppScreen.Invoices.name, JSONArray().put(item("H001", "Hóa đơn P101 kỳ 2026-05", "Chưa thanh toán", "3.815.000đ", "Tiền phòng + điện + nước")))
        obj.put(AppScreen.Payments.name, JSONArray().put(item("TT001", "Biên lai P101", "Chờ xác nhận", "3.815.000đ", "Chờ chủ trọ xác nhận")))
        obj.put(AppScreen.Services.name, JSONArray().put(item("DV01", "Internet", "Tính phí", "100.000đ/tháng", "Tính theo phòng")))
        obj.put(AppScreen.ServiceRegs.name, JSONArray().put(item("DK001", "P101 dùng Internet", "Đang sử dụng", "100.000đ/tháng", "Đăng ký kỳ 2026-05")))
        obj.put(AppScreen.Electric.name, JSONArray().put(item("D001", "Điện P101 kỳ 2026-05", "Đã ghi", "70 kWh x 3.500đ", "245.000đ")))
        obj.put(AppScreen.Water.name, JSONArray().put(item("N001", "Nước P101 kỳ 2026-05", "Đã ghi", "6 m3 x 15.000đ", "90.000đ")))
        obj.put(AppScreen.RentRequests.name, JSONArray().put(itemWithDetails("YT001", "Lê Văn Nam muốn thuê P102", "Chờ duyệt", "6 tháng", "Muốn vào ngày 10/06/2026", listOf("tenantUsername" to "nguoithue", "roomId" to "P102"))))
        obj.put(AppScreen.RenewRequests.name, JSONArray().put(itemWithDetails("GH001", "Gia hạn hợp đồng P101", "Chờ duyệt", "Thêm 6 tháng", "Người thuê muốn giữ phòng", listOf("tenantUsername" to "nguoithue", "contractId" to "HD001"))))
        obj.put(AppScreen.Incidents.name, JSONArray()
            .put(itemWithDetails("SC001", "Rò nước trong phòng P101", "Mới", "Rất gấp", "Người thuê vừa báo cáo, cần xử lý gấp.", listOf("tenantUsername" to "nguoithue", "roomId" to "P101")))
            .put(itemWithDetails("SC002", "Điện phòng bị mất ở gắn điện", "Đang xử lý", "Gấp", "Bóng đèn bị cháy, cần thay mới.\n[Chủ trọ - 18/06/2026 09:00]: Đã sắp xếp thợ vào sửa ngày mai.", listOf("tenantUsername" to "nguoithue", "roomId" to "P101", "responseBy" to "Nguyễn Minh Quân"))))
        obj.put(AppScreen.Notices.name, JSONArray()
            .put(itemWithDetails("TB001", "Hóa đơn tháng 05 đã được tạo", "Mới", "Thông báo", "Vui lòng thanh toán trước ngày 10", listOf("targetUser" to "nguoithue", "createdBy" to "Chủ trọ")))
            .put(itemWithDetails("TB002", "Thông báo nội quy nhà trọ", "Mới", "Thông báo", "Xin nhắc nhở quý khách không được nuôi vật nuôi trong nhà trọ. Tất cả khách ra vào phải quét mã QR ở cổng chính.", listOf("targetUser" to "", "targetType" to "all", "createdBy" to "Chủ trọ"))))
        obj.put(AppScreen.Users.name, JSONArray()
            .put(item("U001", "Admin hệ thống", "Admin", "admin@demo.local", "Quản lý toàn bộ hệ thống"))
            .put(item("U002", "Nguyễn Minh Quân", "Chủ trọ", "chutro@example.com", "Chủ trọ Nhà trọ An Bình"))
            .put(item("U003", "Người Thuê Demo", "Người thuê", "nguoithue@example.com", "Người thuê phòng P101"))
            .put(item("U004", "Trần Quốc Tuấn", "Chủ trọ", "tuantq@gmail.com", "Chủ trọ Nhà trọ Bình Minh"))
            .put(item("U005", "Lê Thị Hồng", "Chủ trọ", "honglt@gmail.com", "Chủ trọ Nhà trọ Hồng Hà"))
            .put(item("U006", "Phạm Văn Nam", "Người thuê", "nampv@gmail.com", "Người thuê phòng P201"))
            .put(item("U007", "Nguyễn Thu Thảo", "Người thuê", "thaont@gmail.com", "Người thuê phòng P202")))
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
