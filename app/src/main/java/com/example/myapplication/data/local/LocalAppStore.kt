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
        } ?: error("Sai tĂ i khoáº£n, máº­t kháº©u hoáº·c vai trĂ².")
        return user.toSession()
    }

    fun register(payload: JSONObject): String {
        val username = payload.optString("tenDangNhap").trim()
        val email = payload.optString("email").trim()
        val password = payload.optString("matKhau")
        val confirmPassword = payload.optString("xacNhanMatKhau", password)
        val role = UserRole.from(payload.optString("vaiTro"))
        require(username.isNotBlank()) { "TĂªn Ä‘Äƒng nháº­p khĂ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng." }
        require(email.isNotBlank()) { "Email khĂ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng." }
        require(password.length >= 6) { "Máº­t kháº©u pháº£i cĂ³ Ă­t nháº¥t 6 kĂ½ tá»±." }
        require(password == confirmPassword) { "Máº­t kháº©u nháº­p láº¡i khĂ´ng khá»›p." }
        val users = usersArray()
        require(users.objects().none { it.optString("username").equals(username, true) || it.optString("email").equals(email, true) }) {
            "TĂªn Ä‘Äƒng nháº­p hoáº·c email Ä‘Ă£ tá»“n táº¡i."
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
        return "ÄÄƒng kĂ½ thĂ nh cĂ´ng. Báº¡n cĂ³ thá»ƒ Ä‘Äƒng nháº­p báº±ng tĂ i khoáº£n vá»«a táº¡o."
    }

    fun forgotPassword(email: String): String {
        val users = usersArray()
        val user = users.objects().firstOrNull { it.optString("email").equals(email, true) }
        if (user != null) {
            val token = "123456"
            user.put("resetToken", token)
            saveUsers(users)
        }
        return "Náº¿u email tá»“n táº¡i, mĂ£ Ä‘áº·t láº¡i trong báº£n local lĂ  123456."
    }

    fun resetPassword(email: String, token: String, newPassword: String, confirmPassword: String): String {
        require(newPassword == confirmPassword) { "Máº­t kháº©u nháº­p láº¡i khĂ´ng khá»›p." }
        require(newPassword.length >= 6) { "Máº­t kháº©u má»›i pháº£i cĂ³ Ă­t nháº¥t 6 kĂ½ tá»±." }
        val users = usersArray()
        val user = users.objects().firstOrNull { it.optString("email").equals(email, true) } ?: error("Email khĂ´ng tá»“n táº¡i.")
        require(user.optString("resetToken") == token) { "MĂ£ OTP/Token khĂ´ng Ä‘Ăºng." }
        user.put("password", newPassword).remove("resetToken")
        saveUsers(users)
        return "Äáº·t láº¡i máº­t kháº©u thĂ nh cĂ´ng."
    }

    fun account(session: UserSession): AccountProfile {
        val user = findUser(session.username) ?: error("KhĂ´ng tĂ¬m tháº¥y tĂ i khoáº£n.")
        return user.toProfile()
    }

    fun updateAccount(session: UserSession, profile: AccountProfile): AccountProfile {
        val users = usersArray()
        val user = users.objects().firstOrNull { it.optString("username").equals(session.username, true) } ?: error("KhĂ´ng tĂ¬m tháº¥y tĂ i khoáº£n.")
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
        // Use ASCII-safe prefix check: pending statuses start with "Ch" (Chờ/Chưa), completed start with non-ASCII (Đã/Từ)
        val pending = list(AppScreen.RentRequests).count { it.status.startsWith("Ch") } +
            list(AppScreen.RenewRequests).count { it.status.startsWith("Ch") } +
            payments.count { it.status.startsWith("Ch") }
        return DashboardSummary(
            totalRooms = rooms.size,
            emptyRooms = rooms.count { it.detail("tenantUsername").isBlank() },
            unpaidInvoices = invoices.count { it.status.startsWith("Ch") },
            pendingTasks = pending,
            revenue = payments.filter { it.status.isNotBlank() && !it.status.startsWith("Ch") }.sumOf { moneyValue(it.value) }
        )
    }

    fun list(screen: AppScreen): List<RentalItem> = itemsObject().optJSONArray(screen.name)?.toItems().orEmpty()

    fun list(screen: AppScreen, session: UserSession?): List<RentalItem> {
        val items = list(screen)
        return when (session?.role) {
            UserRole.Admin -> items
            UserRole.ChuTro -> filterLandlordItems(screen, items, session.username)
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

    // ——— Mốc D: Sự cố & Thông báo ————————————————————————————————————————————

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
        try {
            // Only check version - do NOT call list() here (could fail on restored/corrupt prefs)
            val currentVersion = prefs.getInt("seed_version", 0)
            val targetVersion = 8
            if (currentVersion >= targetVersion) return

            // Wipe all existing (possibly corrupted or backed-up) data
            prefs.edit().clear().commit()

            val usersArr = JSONArray()
            usersArr.put(seedUser(1, "Admin", "Admin123", UserRole.Admin, "Admin he thong", "admin@demo.local"))

            val chuTroData = listOf(
                Triple("chutro", "Nguyen Minh Quan", "chutro@example.com"),
                Triple("chutro2", "Tran Thi Thu Ha", "chutro2@example.com"),
                Triple("chutro3", "Le Hoang Phuc", "chutro3@example.com"),
                Triple("chutro4", "Pham Gia Han", "chutro4@example.com"),
                Triple("chutro5", "Do Thanh Dat", "chutro5@example.com")
            )
            chuTroData.forEachIndexed { i, d ->
                usersArr.put(seedUser(i + 2, d.first, "123456", UserRole.ChuTro, d.second, d.third))
            }

            val tenantNames = listOf(
                "Nguoi Thue Demo", "Tran Thi Mai", "Le Van Nam", "Vo Thi Hanh", "Do Quoc Bao",
                "Pham Ngoc Linh", "Hoang Gia Huy", "Nguyen Hoai An", "Bui Khanh Vy", "Dang Minh Khang",
                "Phan Tuan Kiet", "Vu Thanh Tam", "Mai Phuong Anh", "Cao Nhat Minh", "Ta Hong Nhung",
                "Lam Duc Anh", "Trinh Bao Chau", "Ho Quang Vinh", "Ngo My Duyen", "Duong Hai Dang"
            )
            tenantNames.forEachIndexed { i, name ->
                val uname = if (i == 0) "nguoithue" else "nguoithue${i + 1}"
                val email = if (i == 0) "nguoithue@example.com" else "nguoithue${i + 1}@example.com"
                usersArr.put(seedUser(i + 7, uname, "123456", UserRole.NguoiDung, name, email))
            }

            prefs.edit().putString("users", usersArr.toString()).commit()

            val seedItems = generateSeedItems(chuTroData, tenantNames)
            prefs.edit().putString("items", seedItems.toString()).commit()
            prefs.edit().putBoolean("seeded", true).putInt("seed_version", targetVersion).commit()
        } catch (e: Exception) {
            android.util.Log.e("LocalAppStore", "Seed failed: ${e.message}", e)
        }
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

    private fun generateSeedItems(
        chuTroData: List<Triple<String, String, String>>,
        tenantNames: List<String>
    ): JSONObject {
        // Simple money formatter - no locale dependency, cannot throw
        fun fmt(v: Long): String {
            val millions = v / 1_000_000
            val hundreds = (v % 1_000_000) / 100_000
            return if (hundreds == 0L) "${millions} trieu/thang" else "${millions},${hundreds} trieu/thang"
        }
        fun fmtAmt(v: Long): String {
            val millions = v / 1_000_000
            val hundreds = (v % 1_000_000) / 100_000
            return if (hundreds == 0L) "${millions} trieu" else "${millions},${hundreds} trieu"
        }
        val obj = JSONObject()
        val housesArr = JSONArray()
        val roomTypesArr = JSONArray()
        val roomsArr = JSONArray()
        val servicesArr = JSONArray()
        val tenantsArr = JSONArray()
        val contractsArr = JSONArray()
        val invoicesArr = JSONArray()
        val paymentsArr = JSONArray()
        val serviceRegsArr = JSONArray()
        val electricArr = JSONArray()
        val waterArr = JSONArray()
        val rentRequestsArr = JSONArray()
        val renewRequestsArr = JSONArray()
        val incidentsArr = JSONArray()
        val noticesArr = JSONArray()
        val usersArr = JSONArray()

        usersArr.put(item("U001", "Admin hệ thống", "Admin", "admin@demo.local", "Quản lý toàn bộ hệ thống"))
        chuTroData.forEachIndexed { i, d ->
            usersArr.put(item("U${(i+2).toString().padStart(3, '0')}", d.second, "Chủ trọ", d.third, "Tài khoản đăng ký trong app"))
        }
        tenantNames.forEachIndexed { i, name ->
            val uname = if (i == 0) "nguoithue" else "nguoithue${i+1}"
            val email = if (i == 0) "nguoithue@example.com" else "nguoithue${i+1}@example.com"
            usersArr.put(item("U${(i+7).toString().padStart(3, '0')}", name, "Người thuê", email, "Tài khoản đăng ký trong app"))
        }

        val houseNames = listOf("Nhà trọ An Bình", "Ký túc xá Mini Hoa Sen", "Căn hộ dịch vụ Minh Quân", "Nhà trọ Bình Minh", "Studio Green Home", "Nhà trọ Tân Phú", "Căn hộ Blue Sky", "Nhà trọ Gần Đại Học", "Khu phòng trọ Sunrise", "Nhà trọ Mộc Lan")
        val addresses = listOf("Lê Lợi", "Nguyễn Văn Cừ", "Phạm Văn Đồng", "Cộng Hòa", "Điện Biên Phủ")

        var roomIndex = 1
        var tenantIndex = 0

        houseNames.forEachIndexed { i, houseName ->
            val houseId = "NT${(i+1).toString().padStart(2, '0')}"
            val chuTroUsername = chuTroData[i % chuTroData.size].first
            housesArr.put(itemWithDetails(houseId, houseName, "Đang hoạt động", "4 phòng", "${100 + i * 7} ${addresses[i % 5]}, TP.HCM", listOf("createdBy" to chuTroUsername)))

            roomTypesArr.put(itemWithDetails("LP${i*3 + 1}", "Phòng thường", "Đang dùng", "2.000.000đ - 3.000.000đ", "Phòng cơ bản, chi phí hợp lý", listOf("houseId" to houseId, "createdBy" to chuTroUsername)))
            roomTypesArr.put(itemWithDetails("LP${i*3 + 2}", "Phòng gác lửng", "Đang dùng", "3.000.000đ - 4.000.000đ", "Có gác, tối ưu không gian", listOf("houseId" to houseId, "createdBy" to chuTroUsername)))
            roomTypesArr.put(itemWithDetails("LP${i*3 + 3}", "Studio", "Đang dùng", "4.000.000đ - 6.000.000đ", "Rộng, có bếp và nội thất cơ bản", listOf("houseId" to houseId, "createdBy" to chuTroUsername)))

            servicesArr.put(itemWithDetails("DV${i*4 + 1}", "Internet", "Tính phí", "100.000đ/tháng", "Tính theo phòng", listOf("houseId" to houseId, "createdBy" to chuTroUsername)))
            servicesArr.put(itemWithDetails("DV${i*4 + 2}", "Vệ sinh", "Tính phí", "60.000đ/tháng", "Tính theo phòng", listOf("houseId" to houseId, "createdBy" to chuTroUsername)))
            servicesArr.put(itemWithDetails("DV${i*4 + 3}", "Giữ xe máy", "Tính phí", "90.000đ/tháng", "Tính theo phòng", listOf("houseId" to houseId, "createdBy" to chuTroUsername)))
            servicesArr.put(itemWithDetails("DV${i*4 + 4}", "Máy giặt chung", "Tính phí", "70.000đ/tháng", "Tính theo phòng", listOf("houseId" to houseId, "createdBy" to chuTroUsername)))

            for (j in 1..4) {
                val roomId = "P${roomIndex.toString().padStart(3, '0')}"
                val roomName = "${('A' + i % 5)}${j.toString().padStart(2, '0')}"
                val isRented = j <= 2
                val isRepair = !isRented && (roomIndex % 3 == 0)
                val status = if (isRented) "Đã thuê" else if (isRepair) "Đang sửa chữa" else "Còn trống"
                val price = 1_800_000L + (j.toLong() * 450_000L) + ((roomIndex.toLong() % 4L) * 250_000L)
                val priceStr = fmt(price)
                
                var details = listOf("houseId" to houseId, "createdBy" to chuTroUsername)

                if (isRented && tenantIndex < tenantNames.size) {
                    val tenantUsername = if (tenantIndex == 0) "nguoithue" else "nguoithue${tenantIndex+1}"
                    val tName = tenantNames[tenantIndex]
                    val contractId = "HD${tenantIndex.toString().padStart(3, '0')}"
                    
                    details = details + listOf("tenantUsername" to tenantUsername, "tenantName" to tName, "contractId" to contractId)
                    
                    tenantsArr.put(itemWithDetails("KT${tenantIndex.toString().padStart(3, '0')}", tName, "Đang thuê", tenantUsername, "Phòng $roomName", listOf("tenantUsername" to tenantUsername, "roomId" to roomId, "contractId" to contractId, "createdBy" to chuTroUsername)))
                    
                    contractsArr.put(itemWithDetails(contractId, "HopDong $roomName - $tName", "Dang hieu luc", "01/01/2026 - 31/12/2026", "Tien coc ${fmtAmt(price)}", listOf("tenantUsername" to tenantUsername, "tenantName" to tName, "roomId" to roomId, "roomName" to roomName, "startDate" to "01/01/2026", "endDate" to "31/12/2026", "deposit" to fmtAmt(price), "createdBy" to chuTroUsername)))
                    
                    val elecOld = 50.0 + roomIndex * 3 % 180
                    val elecUse = 18.0 + roomIndex % 70
                    val elecNew = elecOld + elecUse
                    electricArr.put(itemWithDetails("D${tenantIndex.toString().padStart(3, '0')}", "Dien $roomName ky 2026-06", "Da ghi", "${elecUse.toInt()} kWh x 3500d", fmtAmt((elecUse * 3500).toLong()), listOf("roomId" to roomId, "roomName" to roomName, "period" to "2026-06", "oldIndex" to elecOld.toString(), "newIndex" to elecNew.toString(), "consumption" to elecUse.toString(), "price" to "3500.0", "amount" to (elecUse * 3500).toString(), "tenantUsername" to tenantUsername, "createdBy" to chuTroUsername)))

                    val waterOld = 10.0 + roomIndex % 45
                    val waterUse = 4.0 + roomIndex % 12
                    val waterNew = waterOld + waterUse
                    waterArr.put(itemWithDetails("N${tenantIndex.toString().padStart(3, '0')}", "Nuoc $roomName ky 2026-06", "Da ghi", "${waterUse.toInt()} m3 x 15000d", fmtAmt((waterUse * 15000).toLong()), listOf("roomId" to roomId, "roomName" to roomName, "period" to "2026-06", "oldIndex" to waterOld.toString(), "newIndex" to waterNew.toString(), "consumption" to waterUse.toString(), "price" to "15000.0", "amount" to (waterUse * 15000).toString(), "tenantUsername" to tenantUsername, "createdBy" to chuTroUsername)))

                    serviceRegsArr.put(itemWithDetails("DK${tenantIndex.toString().padStart(3, '0')}A", "$roomName dùng Internet", "Đang sử dụng", "100.000đ/tháng", "Đăng ký kỳ 2026-06", listOf("tenantUsername" to tenantUsername, "roomId" to roomId, "createdBy" to chuTroUsername)))
                    serviceRegsArr.put(itemWithDetails("DK${tenantIndex.toString().padStart(3, '0')}B", "$roomName dùng Vệ sinh", "Đang sử dụng", "60.000đ/tháng", "Đăng ký kỳ 2026-06", listOf("tenantUsername" to tenantUsername, "roomId" to roomId, "createdBy" to chuTroUsername)))
                    
                    val totalInv = price + (elecUse * 3500).toLong() + (waterUse * 15000).toLong() + 160_000L
                    val invId = "H${tenantIndex.toString().padStart(3, '0')}"
                    val invStatus = if (tenantIndex % 2 == 0) "Da thanh toan" else "Chua thanh toan"
                    invoicesArr.put(itemWithDetails(invId, "HoaDon $roomName ky 2026-06", invStatus, fmtAmt(totalInv), "Tien phong + Dien + Nuoc + DichVu", listOf("tenantUsername" to tenantUsername, "roomId" to roomId, "roomName" to roomName, "period" to "2026-06", "totalAmount" to totalInv.toString(), "createdBy" to chuTroUsername)))

                    if (tenantIndex % 2 == 0) {
                        paymentsArr.put(itemWithDetails("TT${tenantIndex.toString().padStart(3, '0')}", "BienLai $roomName ky 2026-06", "Da xac nhan", fmtAmt(totalInv), "Ma GD: GD${20260600 + tenantIndex}", listOf("tenantUsername" to tenantUsername, "roomId" to roomId, "invoiceId" to invId, "createdBy" to tenantUsername)))
                    }

                    if (tenantIndex % 3 == 0) {
                        val title = listOf("Rò nước trong phòng", "Mất điện khu vực", "Khóa cửa bị kẹt", "Máy lạnh không mát", "Wifi yếu")[tenantIndex % 5]
                        incidentsArr.put(itemWithDetails("SC${tenantIndex.toString().padStart(3, '0')}", "$title $roomName", "Mới", "Bình thường", "Mô tả: $title", listOf("tenantUsername" to tenantUsername, "roomId" to roomId, "createdBy" to tenantUsername)))
                    }

                    tenantIndex++
                }

                roomsArr.put(itemWithDetails(roomId, roomName, status, priceStr, "Tầng $j - $houseName", details))
                roomIndex++
            }
        }
        
        obj.put(AppScreen.Houses.name, housesArr)
        obj.put(AppScreen.RoomTypes.name, roomTypesArr)
        obj.put(AppScreen.Rooms.name, roomsArr)
        obj.put(AppScreen.Tenants.name, tenantsArr)
        obj.put(AppScreen.Contracts.name, contractsArr)
        obj.put(AppScreen.Invoices.name, invoicesArr)
        obj.put(AppScreen.Payments.name, paymentsArr)
        obj.put(AppScreen.Services.name, servicesArr)
        obj.put(AppScreen.ServiceRegs.name, serviceRegsArr)
        obj.put(AppScreen.Electric.name, electricArr)
        obj.put(AppScreen.Water.name, waterArr)
        obj.put(AppScreen.RentRequests.name, rentRequestsArr)
        obj.put(AppScreen.RenewRequests.name, renewRequestsArr)
        obj.put(AppScreen.Incidents.name, incidentsArr)
        obj.put(AppScreen.Notices.name, noticesArr)
        obj.put(AppScreen.Users.name, usersArr)
        return obj
    }


    private fun addUserListItem(id: Int, username: String, role: UserRole, email: String) {
        upsert(AppScreen.Users, RentalItem("U${id.toString().padStart(3, '0')}", username, role.label, email, "Tài khoản đăng ký trong app"))
    }

    private fun item(id: String, title: String, status: String, value: String, note: String): JSONObject = RentalItem(id, title, status, value, note).toJson()
    private fun itemWithDetails(id: String, title: String, status: String, value: String, note: String, details: List<Pair<String, String>>): JSONObject {
        return RentalItem(id, title, status, value, note, details).toJson()
    }
    private fun usersArray(): JSONArray = try { JSONArray(prefs.getString("users", "[]") ?: "[]") } catch (e: Exception) { JSONArray() }
    private fun itemsObject(): JSONObject = try { JSONObject(prefs.getString("items", "{}") ?: "{}") } catch (e: Exception) { JSONObject() }
    private fun saveUsers(users: JSONArray) = prefs.edit().putString("users", users.toString()).apply()
    private fun saveItems(items: JSONObject) = prefs.edit().putString("items", items.toString()).apply()
    private fun findUser(username: String): JSONObject? = usersArray().objects().firstOrNull { it.optString("username").equals(username, true) }

    private fun filterLandlordItems(screen: AppScreen, items: List<RentalItem>, username: String): List<RentalItem> = when (screen) {
        AppScreen.Houses, AppScreen.RoomTypes, AppScreen.Rooms, AppScreen.Services -> items.filter {
            it.detail("createdBy").equals(username, true)
        }
        AppScreen.Tenants, AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments, AppScreen.ServiceRegs,
        AppScreen.Electric, AppScreen.Water, AppScreen.RentRequests, AppScreen.RenewRequests,
        AppScreen.Incidents -> items.filter {
            val myRooms = list(AppScreen.Rooms).filter { r -> r.detail("createdBy").equals(username, true) }.map { r -> r.id }
            it.detail("createdBy").equals(username, true) || myRooms.contains(it.detail("roomId")) || it.detail("tenantUsername").equals(username, true)
        }
        AppScreen.Notices -> items.filter {
            it.detail("targetUser").isBlank() || it.detail("targetUser").equals(username, true) || it.detail("createdBy").equals(username, true)
        }
        AppScreen.Users -> emptyList()
        else -> items
    }

    private fun filterTenantItems(screen: AppScreen, items: List<RentalItem>, username: String): List<RentalItem> = when (screen) {
        // Check tenantUsername detail instead of garbled Vietnamese status literal
        AppScreen.Rooms -> items.filter { it.detail("tenantUsername").isBlank() || it.detail("tenantUsername").equals(username, true) || it.detail("createdBy").equals(username, true) }
        AppScreen.Houses, AppScreen.RoomTypes, AppScreen.Services -> items.filter {
            it.detail("createdBy").equals(username, true) || true 
        }
        AppScreen.Notices -> items.filter {
            it.detail("targetUser").isBlank() || it.detail("targetUser").equals(username, true) || it.detail("createdBy").equals(username, true)
        }
        AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments, AppScreen.ServiceRegs,
        AppScreen.Electric, AppScreen.Water, AppScreen.RentRequests, AppScreen.RenewRequests,
        AppScreen.Incidents, AppScreen.Tenants -> items.filter {
            it.detail("tenantUsername").equals(username, true) ||
                it.detail("createdBy").equals(username, true) ||
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
