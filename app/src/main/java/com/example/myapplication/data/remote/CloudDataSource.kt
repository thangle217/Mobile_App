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

    suspend fun dashboard(): DashboardSummary {
        return DashboardSummary()
    }

    suspend fun nextId(screen: AppScreen): String {
        return db.collection(getCollectionName(screen)).document().id
    }
    
    suspend fun delete(screen: AppScreen, id: String) {
        db.collection(getCollectionName(screen)).document(id).delete().await()
    }

    suspend fun createRentRequest(roomId: String, session: UserSession, duration: String, note: String): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun decideRentRequest(requestId: String, approve: Boolean, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun confirmContract(contractId: String, approve: Boolean, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun saveContract(contractId: String, roomId: String, tenantUsername: String, startDate: String, endDate: String, deposit: String, note: String, status: String, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun closeContract(contractId: String, cancel: Boolean, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun createRenewRequest(contractId: String, session: UserSession, newEndDate: String, note: String): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun decideRenewRequest(requestId: String, approve: Boolean, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun getLatestUtilityIndex(screen: AppScreen, roomId: String): Double { return 0.0 }
    suspend fun saveUtilityReading(screen: AppScreen, roomId: String, period: String, oldIndex: Double, newIndex: Double, price: Double): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun createInvoice(roomId: String, period: String, otherCost: Double, otherNote: String): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun submitPayment(invoiceId: String, transactionId: String, receiptImage: String, note: String, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun decidePayment(paymentId: String, approve: Boolean, rejectReason: String?, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun respondToIncident(incidentId: String, response: String, newStatus: String, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun markNoticeAsRead(noticeId: String, username: String): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
    }
    suspend fun createNotice(title: String, content: String, targetType: String, session: UserSession): RentalItem {
        return RentalItem(id = "", title = "", status = "", value = "", note = "")
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
            AppScreen.Payments -> "payments"
            AppScreen.Incidents -> "incidents"
            else -> "misc"
        }
    }
}
