package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.*
import org.json.JSONObject

interface IRentalRepository {
    suspend fun login(username: String, password: String, role: UserRole): Result<UserSession>
    suspend fun register(payload: JSONObject): Result<String>
    suspend fun forgotPassword(email: String): Result<String>
    suspend fun resetPassword(email: String, token: String, newPassword: String, confirmPassword: String): Result<String>
    suspend fun account(session: UserSession): UiState<AccountProfile>
    suspend fun updateAccount(session: UserSession, profile: AccountProfile): Result<AccountProfile>
    suspend fun sendPasswordResetEmail(email: String): Result<String>
    suspend fun uploadCccdImage(fileName: String, mimeType: String, bytes: ByteArray, session: UserSession? = null): Result<String>
    suspend fun dashboard(session: UserSession?): UiState<DashboardSummary>
    suspend fun list(screen: AppScreen, session: UserSession?): UiState<List<RentalItem>>
    suspend fun saveItem(screen: AppScreen, item: RentalItem, session: UserSession?): Result<RentalItem>
    suspend fun deleteItem(screen: AppScreen, id: String, session: UserSession?): Result<Unit>
    suspend fun requestRoom(roomId: String, session: UserSession?, moveInDate: String, expectedMoveOutDate: String, note: String): Result<RentalItem>
    suspend fun decideRentRequest(requestId: String, approve: Boolean, session: UserSession?): Result<RentalItem>
    suspend fun tenantConfirmRentRequest(requestId: String, session: UserSession?): Result<RentalItem>
    suspend fun confirmContract(contractId: String, approve: Boolean, session: UserSession?): Result<RentalItem>
    suspend fun saveContract(
        contractId: String,
        roomId: String,
        tenantUsername: String,
        startDate: String,
        endDate: String,
        deposit: String,
        note: String,
        status: String,
        session: UserSession?
    ): Result<RentalItem>
    suspend fun closeContract(contractId: String, cancel: Boolean, session: UserSession?): Result<RentalItem>
    suspend fun createRenewRequest(contractId: String, session: UserSession?, newEndDate: String, note: String): Result<RentalItem>
    suspend fun decideRenewRequest(requestId: String, approve: Boolean, session: UserSession?, newEndDateOverride: String? = null, newDepositOverride: String? = null): Result<RentalItem>
    suspend fun getLatestUtilityIndex(screen: AppScreen, roomId: String): Result<Double>
    suspend fun saveUtilityReading(
        screen: AppScreen,
        roomId: String,
        period: String,
        oldIndex: Double,
        newIndex: Double,
        price: Double,
        session: UserSession?
    ): Result<RentalItem>
    suspend fun createInvoice(
        roomId: String,
        period: String,
        otherCost: Double,
        otherNote: String,
        session: UserSession?,
        roomRentOverride: Double? = null
    ): Result<RentalItem>
    suspend fun submitPayment(
        invoiceId: String,
        transactionId: String,
        receiptImage: String,
        note: String,
        session: UserSession?
    ): Result<RentalItem>
    suspend fun decidePayment(
        paymentId: String,
        approve: Boolean,
        rejectReason: String?,
        session: UserSession?
    ): Result<RentalItem>
    suspend fun respondToIncident(
        incidentId: String,
        response: String,
        newStatus: String,
        session: UserSession?
    ): Result<RentalItem>
    suspend fun markNoticeAsRead(noticeId: String, session: UserSession?): Result<RentalItem>
    suspend fun createNotice(
        title: String,
        content: String,
        targetType: String,
        session: UserSession?
    ): Result<RentalItem>
    fun unreadNoticeCount(session: UserSession?): Int
    fun demoSession(roleName: String): UserSession
}
