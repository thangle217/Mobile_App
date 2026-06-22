package com.example.myapplication.data.remote

import com.example.myapplication.domain.model.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class CloudDataSource {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun login(usernameOrEmail: String, password: String, role: UserRole): UserSession {
        val inputStr = usernameOrEmail.trim()
        val usersRef = db.collection("users")
        val query = usersRef.whereEqualTo("username", inputStr).get().await()
        val emailToLogin = if (!query.isEmpty) {
            query.documents.first().getString("email") ?: inputStr
        } else {
            inputStr
        }

        val authResult = auth.signInWithEmailAndPassword(emailToLogin, password).await()
        val userRecord = usersRef.document(authResult.user!!.uid).get().await()
        val actualRoleStr = userRecord.getString("role") ?: UserRole.NguoiDung.name
        val actualRole = UserRole.from(actualRoleStr)
        
        if (actualRole != role) {
            auth.signOut()
            error("Tài khoản này không có quyền $role.")
        }
        
        return UserSession(
            token = authResult.user!!.uid,
            username = userRecord.getString("username") ?: "",
            role = actualRole,
            displayName = userRecord.getString("fullName") ?: ""
        )
    }

    suspend fun register(payload: JSONObject): String {
        val username = payload.optString("tenDangNhap").trim()
        val email = payload.optString("email").trim()
        val password = payload.optString("matKhau")
        val confirmPassword = payload.optString("xacNhanMatKhau", password)
        val role = UserRole.from(payload.optString("vaiTro"))
        
        require(username.isNotBlank()) { "Tên đăng nhập không được để trống." }
        require(email.isNotBlank()) { "Email không được để trống." }
        require(password.length >= 6) { "Mật khẩu phải có ít nhất 6 ký tự." }
        require(password == confirmPassword) { "Mật khẩu nhập lại không khớp." }

        val query = db.collection("users").whereEqualTo("username", username).get().await()
        require(query.isEmpty) { "Tên đăng nhập đã tồn tại." }

        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user!!.uid
        
        val userMap = hashMapOf(
            "id" to uid,
            "username" to username,
            "role" to role.name,
            "fullName" to payload.optString("hoTen").ifBlank { username },
            "email" to email,
            "phone" to payload.optString("soDienThoai"),
            "cccd" to payload.optString("cccd"),
            "cccdFrontUrl" to payload.optString("anhCccdMatTruoc"),
            "cccdBackUrl" to payload.optString("anhCccdMatSau"),
            "address" to "",
            "workplace" to "",
            "bankName" to "",
            "bankAccount" to "",
            "bankOwner" to "",
            "transferContent" to ""
        )
        
        db.collection("users").document(uid).set(userMap).await()
        return "Đăng ký thành công. Bạn có thể đăng nhập ngay."
    }

    suspend fun forgotPassword(email: String): String {
        auth.sendPasswordResetEmail(email).await()
        return "Vui lòng kiểm tra hộp thư email của bạn để lấy link đặt lại mật khẩu."
    }

    suspend fun resetPassword(email: String, token: String, newPassword: String, confirmPassword: String): String {
        error("Vui lòng dùng link trong email để đặt lại mật khẩu.")
    }

    suspend fun createTestAccounts(): String {
        val testAccounts = listOf(
            mapOf("username" to "Admin", "password" to "Admin123", "email" to "admin@quanlynhatro.com", "role" to UserRole.Admin),
            mapOf("username" to "chutro", "password" to "123456", "email" to "chutro@quanlynhatro.com", "role" to UserRole.ChuTro),
            mapOf("username" to "nguoithue", "password" to "123456", "email" to "nguoithue@quanlynhatro.com", "role" to UserRole.NguoiDung)
        )
        
        var createdCount = 0
        for (acc in testAccounts) {
            val username = acc["username"] as String
            val password = acc["password"] as String
            val email = acc["email"] as String
            val role = acc["role"] as UserRole
            
            val query = db.collection("users").whereEqualTo("username", username).get().await()
            if (query.isEmpty) {
                try {
                    var uid: String? = null
                    try {
                        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                        uid = authResult.user?.uid
                    } catch (e: Exception) {
                        val loginResult = auth.signInWithEmailAndPassword(email, password).await()
                        uid = loginResult.user?.uid
                    }
                    
                    if (uid != null) {
                        val userMap = hashMapOf(
                            "id" to uid,
                            "username" to username,
                            "role" to role.name,
                            "fullName" to "$username Test",
                            "email" to email,
                            "phone" to "0123456789",
                            "cccd" to "012345678912",
                            "cccdFrontUrl" to "",
                            "cccdBackUrl" to "",
                            "address" to "",
                            "workplace" to "",
                            "bankName" to "",
                            "bankAccount" to "",
                            "bankOwner" to "",
                            "transferContent" to ""
                        )
                        db.collection("users").document(uid).set(userMap).await()
                        createdCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        auth.signOut()
        return "Đã kiểm tra và tạo $createdCount tài khoản mẫu."
    }

    suspend fun account(session: UserSession): AccountProfile {
        val query = db.collection("users").whereEqualTo("username", session.username).get().await()
        if (query.isEmpty) error("Không tìm thấy tài khoản.")
        val doc = query.documents.first()
        return AccountProfile(
            fullName = doc.getString("fullName") ?: "",
            email = doc.getString("email") ?: "",
            phone = doc.getString("phone") ?: "",
            cccd = doc.getString("cccd") ?: "",
            cccdFrontUrl = doc.getString("cccdFrontUrl") ?: "",
            cccdBackUrl = doc.getString("cccdBackUrl") ?: "",
            address = doc.getString("address") ?: "",
            workplace = doc.getString("workplace") ?: "",
            bankName = doc.getString("bankName") ?: "",
            bankAccount = doc.getString("bankAccount") ?: "",
            bankOwner = doc.getString("bankOwner") ?: "",
            transferContent = doc.getString("transferContent") ?: ""
        )
    }

    suspend fun updateAccount(session: UserSession, profile: AccountProfile): AccountProfile {
        val query = db.collection("users").whereEqualTo("username", session.username).get().await()
        if (query.isEmpty) error("Không tìm thấy tài khoản.")
        val docId = query.documents.first().id
        
        val updateMap = mapOf(
            "fullName" to profile.fullName,
            "email" to profile.email,
            "phone" to profile.phone,
            "cccd" to profile.cccd,
            "cccdFrontUrl" to profile.cccdFrontUrl,
            "cccdBackUrl" to profile.cccdBackUrl,
            "address" to profile.address,
            "workplace" to profile.workplace,
            "bankName" to profile.bankName,
            "bankAccount" to profile.bankAccount,
            "bankOwner" to profile.bankOwner,
            "transferContent" to profile.transferContent
        )
        
        db.collection("users").document(docId).update(updateMap).await()
        return profile
    }

    suspend fun changePassword(session: UserSession, old: String, new: String, confirm: String): String {
        require(new == confirm) { "Mật khẩu mới không khớp." }
        val user = auth.currentUser ?: error("Bạn chưa đăng nhập.")
        user.updatePassword(new).await()
        return "Đổi mật khẩu thành công."
    }

    suspend fun saveImage(fileName: String, mimeType: String, bytes: ByteArray): String {
        return "https://dummyimage.com/600x400/000/fff&text=Cccd"
    }

    suspend fun list(screen: AppScreen, session: UserSession?): List<RentalItem> {
        val collectionName = getCollectionName(screen)
        var query: com.google.firebase.firestore.Query = db.collection(collectionName)
        
        if (session != null && session.role == UserRole.NguoiDung) {
            query = query.whereEqualTo("tenantUsername", session.username)
        }
        
        val snapshot = query.get().await()
        return snapshot.documents.map { doc ->
            val data = doc.data ?: emptyMap<String, Any>()
            val id = doc.id
            val title = data["title"] as? String ?: ""
            val status = data["status"] as? String ?: ""
            val value = data["value"] as? String ?: ""
            val note = data["note"] as? String ?: ""
            val detailsList = data["details"] as? List<Map<String, String>> ?: emptyList()
            val details = detailsList.map { it["key"]!! to it["value"]!! }
            RentalItem(id, title, status, value, note, details)
        }
    }

    suspend fun upsert(screen: AppScreen, item: RentalItem) {
        val collectionName = getCollectionName(screen)
        val docRef = if (item.id.isEmpty()) {
            db.collection(collectionName).document()
        } else {
            db.collection(collectionName).document(item.id)
        }
        
        val detailsList = item.details.map { mapOf("key" to it.first, "value" to it.second) }
        val data = hashMapOf(
            "id" to docRef.id,
            "title" to item.title,
            "status" to item.status,
            "value" to item.value,
            "note" to item.note,
            "details" to detailsList
        )
        
        item.details.firstOrNull { it.first == "tenantUsername" }?.let {
            data["tenantUsername"] = it.second
        }
        
        docRef.set(data).await()
    }

    private suspend fun runCronJobs() {
        val today = java.util.Calendar.getInstance().time
        val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val contracts = list(AppScreen.Contracts, null).filter { it.status == "Đang hiệu lực" }
        for (contract in contracts) {
            val endDateStr = contract.detail("endDate")
            if (endDateStr.isNotBlank()) {
                try {
                    val endDate = format.parse(endDateStr)
                    if (endDate != null && endDate.before(today)) {
                        upsert(AppScreen.Contracts, contract.copy(status = "Kết thúc"))
                        val room = list(AppScreen.Rooms, null).firstOrNull { it.id == contract.detail("roomId") }
                        if (room != null) {
                            upsert(
                                AppScreen.Rooms, 
                                room.copy(
                                    status = "Còn trống",
                                    details = room.details.removeDetail("tenantUsername").removeDetail("tenantName").removeDetail("contractId")
                                )
                            )
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    suspend fun dashboard(): DashboardSummary {
        runCronJobs()
        return DashboardSummary()
    }

    suspend fun nextId(screen: AppScreen): String {
        return db.collection(getCollectionName(screen)).document().id
    }
    
    suspend fun delete(screen: AppScreen, id: String) {
        when (screen) {
            AppScreen.Houses -> {
                val rooms = list(AppScreen.Rooms, null).filter { it.detail("houseId") == id || it.note.contains(id) }
                require(rooms.isEmpty()) { "Không thể xóa nhà trọ đang có phòng." }
            }
            AppScreen.Rooms -> {
                val hasContracts = list(AppScreen.Contracts, null).any { it.detail("roomId") == id }
                val hasInvoices = list(AppScreen.Invoices, null).any { it.detail("roomId") == id }
                if (hasContracts || hasInvoices) {
                    val room = list(AppScreen.Rooms, null).firstOrNull { it.id == id } ?: return
                    upsert(AppScreen.Rooms, room.copy(status = "Ngưng hoạt động"))
                    return
                }
            }
            AppScreen.Contracts -> {
                val contract = list(AppScreen.Contracts, null).firstOrNull { it.id == id } ?: return
                require(contract.status != "Đang hiệu lực") { "Không thể xóa hợp đồng đang hiệu lực." }
            }
            AppScreen.RentRequests -> {
                val req = list(AppScreen.RentRequests, null).firstOrNull { it.id == id } ?: return
                require(req.status.contains("Chờ", true)) { "Chỉ được phép xóa yêu cầu thuê đang chờ duyệt." }
            }
            AppScreen.Invoices -> {
                val invoice = list(AppScreen.Invoices, null).firstOrNull { it.id == id } ?: return
                require(invoice.status != "Đã thanh toán") { "Không thể xóa hóa đơn đã thanh toán." }
            }
            else -> {}
        }
        db.collection(getCollectionName(screen)).document(id).delete().await()
    }

    suspend fun createRentRequest(roomId: String, session: UserSession, duration: String, note: String): RentalItem {
        val exists = list(AppScreen.RentRequests, session).any {
            it.detail("roomId") == roomId &&
                it.detail("tenantUsername").equals(session.username, true) &&
                it.status.contains("Chờ", true)
        }
        require(!exists) { "Bạn đã có yêu cầu thuê phòng này đang chờ xử lý." }

        val docRef = db.collection("rentRequests").document()
        val details = listOf("roomId" to roomId, "tenantUsername" to session.username, "duration" to duration)
        val item = RentalItem(docRef.id, "Yêu cầu thuê phòng $roomId", "Chờ duyệt", duration, note, details)
        upsert(AppScreen.RentRequests, item)
        return item
    }
    suspend fun decideRentRequest(requestId: String, approve: Boolean, session: UserSession): RentalItem {
        val status = if (approve) "Đã duyệt" else "Từ chối"
        val reqDoc = db.collection("rentRequests").document(requestId).get().await()
        val detailsList = reqDoc.get("details") as? List<Map<String, String>> ?: emptyList()
        val details = detailsList.map { it["key"]!! to it["value"]!! }
        val item = RentalItem(requestId, reqDoc.getString("title") ?: "", status, reqDoc.getString("value") ?: "", reqDoc.getString("note") ?: "", details)
        upsert(AppScreen.RentRequests, item)
        return item
    }

    suspend fun tenantConfirmRentRequest(requestId: String, session: UserSession): RentalItem {
        val reqDoc = db.collection("rentRequests").document(requestId).get().await()
        require(reqDoc.exists()) { "Không tìm thấy yêu cầu thuê." }
        
        val detailsList = reqDoc.get("details") as? List<Map<String, String>> ?: emptyList()
        val detailsMap = detailsList.associate { it["key"]!! to it["value"]!! }
        val roomId = detailsMap["roomId"] ?: error("Lỗi dữ liệu: Không có roomId")
        
        // Update request status to Hoan tat
        val updatedRequest = RentalItem(requestId, reqDoc.getString("title") ?: "", "Hoàn tất", reqDoc.getString("value") ?: "", reqDoc.getString("note") ?: "", detailsList.map { it["key"]!! to it["value"]!! })
        upsert(AppScreen.RentRequests, updatedRequest)

        // Create Contract
        val contractId = db.collection("contracts").document().id
        val cal = java.util.Calendar.getInstance()
        val startDate = String.format("%02d/%02d/%04d", cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
        cal.add(java.util.Calendar.MONTH, 6) // Default 6 months
        val endDate = String.format("%02d/%02d/%04d", cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
        
        return saveContract(contractId, roomId, session.username, startDate, endDate, "0", "Hợp đồng tự động tạo từ Yêu cầu thuê", "Đang hiệu lực", session)
    }
    suspend fun confirmContract(contractId: String, approve: Boolean, session: UserSession): RentalItem {
        val status = if (approve) "Đang hiệu lực" else "Hủy"
        val item = RentalItem(contractId, "Hợp đồng $contractId", status, "", "", listOf())
        upsert(AppScreen.Contracts, item)
        return item
    }
    suspend fun saveContract(contractId: String, roomId: String, tenantUsername: String, startDate: String, endDate: String, deposit: String, note: String, status: String, session: UserSession): RentalItem {
        val id = contractId.ifBlank { db.collection("contracts").document().id }
        // get room price
        var roomPrice = "0"
        val roomDoc = db.collection("rooms").document(roomId).get().await()
        if (roomDoc.exists()) {
            roomPrice = roomDoc.getString("value") ?: "0"
        }
        val details = listOf("roomId" to roomId, "tenantUsername" to tenantUsername, "startDate" to startDate, "endDate" to endDate, "deposit" to deposit)
        val item = RentalItem(id, "HĐ Thuê phòng $roomId", status, roomPrice, note, details)
        upsert(AppScreen.Contracts, item)
        return item
    }
    suspend fun closeContract(contractId: String, cancel: Boolean, session: UserSession): RentalItem {
        val status = if (cancel) "Hủy" else "Đã thanh lý"
        val item = RentalItem(contractId, "Thanh lý HĐ $contractId", status, "", "", listOf())
        upsert(AppScreen.Contracts, item)
        return item
    }
    suspend fun createRenewRequest(contractId: String, session: UserSession, newEndDate: String, note: String): RentalItem {
        val docRef = db.collection("renewRequests").document()
        val details = listOf("contractId" to contractId, "tenantUsername" to session.username, "newEndDate" to newEndDate)
        val item = RentalItem(docRef.id, "Gia hạn HĐ $contractId", "Chờ duyệt", newEndDate, note, details)
        upsert(AppScreen.RenewRequests, item)
        return item
    }
    suspend fun decideRenewRequest(requestId: String, approve: Boolean, session: UserSession): RentalItem {
        val status = if (approve) "Đã duyệt" else "Từ chối"
        val item = RentalItem(requestId, "Phản hồi GH $requestId", status, "", "", listOf())
        upsert(AppScreen.RenewRequests, item)
        return item
    }
    suspend fun getLatestUtilityIndex(screen: AppScreen, roomId: String): Double { return 0.0 }
    suspend fun saveUtilityReading(screen: AppScreen, roomId: String, period: String, oldIndex: Double, newIndex: Double, price: Double): RentalItem {
        val id = db.collection("misc").document().id
        val total = (newIndex - oldIndex) * price
        val item = RentalItem(id, "Chỉ số phòng $roomId kỳ $period", "Đã ghi", total.toString(), "", listOf("roomId" to roomId, "period" to period))
        upsert(screen, item)
        return item
    }
    suspend fun createInvoice(roomId: String, period: String, otherCost: Double, otherNote: String): RentalItem {
        val id = db.collection("invoices").document().id
        var roomPrice = 0.0
        val roomDoc = db.collection("rooms").document(roomId).get().await()
        if (roomDoc.exists()) {
            roomPrice = roomDoc.getString("value")?.toDoubleOrNull() ?: 0.0
        }
        val total = roomPrice + otherCost
        val details = listOf("roomId" to roomId, "period" to period, "otherCost" to otherCost.toString(), "otherNote" to otherNote)
        val item = RentalItem(id, "Hóa đơn phòng $roomId ($period)", "Chưa thanh toán", total.toString(), otherNote, details)
        upsert(AppScreen.Invoices, item)
        return item
    }
    suspend fun submitPayment(invoiceId: String, transactionId: String, receiptImage: String, note: String, session: UserSession): RentalItem {
        val id = db.collection("payments").document().id
        val details = listOf("invoiceId" to invoiceId, "transactionId" to transactionId, "receiptImage" to receiptImage, "tenantUsername" to session.username)
        val item = RentalItem(id, "Biên lai hóa đơn $invoiceId", "Chờ xác nhận", transactionId, note, details)
        upsert(AppScreen.Payments, item)
        return item
    }
    suspend fun decidePayment(paymentId: String, approve: Boolean, rejectReason: String?, session: UserSession): RentalItem {
        val status = if (approve) "Đã xác nhận" else "Từ chối"
        val item = RentalItem(paymentId, "Phản hồi BL $paymentId", status, "", rejectReason ?: "", listOf())
        upsert(AppScreen.Payments, item)
        return item
    }
    suspend fun respondToIncident(incidentId: String, response: String, newStatus: String, session: UserSession): RentalItem {
        val item = RentalItem(incidentId, "Sự cố $incidentId", newStatus, "", response, listOf())
        upsert(AppScreen.Incidents, item)
        return item
    }
    suspend fun markNoticeAsRead(noticeId: String, username: String): RentalItem {
        return RentalItem(id = noticeId, title = "", status = "Đã xem", value = "", note = "")
    }
    suspend fun createNotice(title: String, content: String, targetType: String, session: UserSession): RentalItem {
        val id = db.collection("notices").document().id
        val item = RentalItem(id, title, "Mới", targetType, content, listOf())
        upsert(AppScreen.Notices, item)
        return item
    }
    fun unreadNoticeCount(username: String): Int { return 0 }
    
    private fun getCollectionName(screen: AppScreen): String {
        return when (screen) {
            AppScreen.Houses -> "houses"
            AppScreen.RoomTypes -> "roomTypes"
            AppScreen.Rooms -> "rooms"
            AppScreen.Tenants -> "tenants"
            AppScreen.Services -> "services"
            AppScreen.Contracts -> "contracts"
            AppScreen.Invoices -> "invoices"
            AppScreen.Payments -> "payments"
            AppScreen.Incidents -> "incidents"
            AppScreen.RentRequests -> "rentRequests"
            AppScreen.RenewRequests -> "renewRequests"
            AppScreen.Notices -> "notices"
            else -> "misc"
        }
    }
}

private fun RentalItem.detail(key: String): String = details.firstOrNull { it.first == key }?.second.orEmpty()
private fun List<Pair<String, String>>.removeDetail(key: String) = filter { it.first != key }
