package com.example.myapplication.ui.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.SessionStore
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.AccountProfile
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import kotlinx.coroutines.launch

@Composable
fun RentalManagerApp() {
    val context = LocalContext.current
    val repository = remember(context) { RentalRepository(context) }
    val sessionStore = remember { SessionStore(context) }
    val scope = rememberCoroutineScope()
    var restored by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<UserSession?>(null) }
    var screen by remember { mutableStateOf(AppScreen.Dashboard) }

    LaunchedEffect(Unit) {
        sessionStore.session.collect {
            session = it
            restored = true
        }
    }

    if (!restored) {
        FullScreenLoading("Đang khởi động ứng dụng...")
    } else if (session == null) {
        LoginScreen(
            repository = repository,
            onLoggedIn = {
                session = it
                screen = AppScreen.Dashboard
                if (it.token.isNotBlank()) {
                    scope.launch { sessionStore.save(it) }
                }
            }
        )
    } else {
        MainShell(
            repository = repository,
            session = session,
            screen = screen,
            onScreenChange = { screen = it },
            onLogout = {
                scope.launch { sessionStore.clear() }
                session = null
                screen = AppScreen.Dashboard
            }
        )
    }
}

@Composable
internal fun MainShell(
    repository: RentalRepository,
    session: UserSession?,
    screen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    val role = session?.role ?: UserRole.ChuTro
    val screens = screensForRole(role)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        AppLogo(size = 40)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(session?.displayName ?: role.label, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), style = MaterialTheme.typography.bodyLarge)
                            Text(role.label, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Divider(color = Color(0xFFE2E8F0))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(screens) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label, fontWeight = FontWeight.Medium) },
                                selected = item == screen,
                                onClick = {
                                    onScreenChange(item)
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    Surface(
                                        color = screenAccent(item).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = item.shortCode,
                                            color = screenAccent(item),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppHeader(
                    title = screen.label,
                    sourceLabel = "Dữ liệu thiết bị",
                    onMenu = { scope.launch { drawerState.open() } },
                    onLogout = onLogout
                )
            },
            bottomBar = {
                AppBottomBar(
                    items = bottomScreens(role),
                    selected = screen,
                    onSelected = onScreenChange
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (screen) {
                    AppScreen.Dashboard -> DashboardScreen(repository, session, onScreenChange, onLogout)
                    AppScreen.Account -> AccountScreen(repository, session, onLogout)
                    else -> ModuleScreen(repository, session, screen, onLogout)
                }
            }
        }
    }
}

@Composable
internal fun ModuleScreen(
    repository: RentalRepository,
    session: UserSession?,
    screen: AppScreen,
    onSessionExpired: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember(screen, session) { mutableStateOf<UiState<List<RentalItem>>>(UiState.Loading) }
    var query by remember(screen) { mutableStateOf("") }
    var status by remember(screen) { mutableStateOf("Tất cả") }
    var selected by remember { mutableStateOf<RentalItem?>(null) }
    var editing by remember { mutableStateOf<RentalItem?>(null) }
    var contractEditing by remember { mutableStateOf<RentalItem?>(null) }
    var rentRequestRoom by remember { mutableStateOf<RentalItem?>(null) }
    var renewContract by remember { mutableStateOf<RentalItem?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var confirmData by remember { mutableStateOf<ConfirmData?>(null) }
    var houses by remember(screen) { mutableStateOf<List<RentalItem>>(emptyList()) }
    var rooms by remember(screen) { mutableStateOf<List<RentalItem>>(emptyList()) }
    var invoices by remember(screen) { mutableStateOf<List<RentalItem>>(emptyList()) }
    var paymentForInvoice by remember { mutableStateOf<RentalItem?>(null) }
    var rejectingPayment by remember { mutableStateOf<RentalItem?>(null) }
    var respondingIncident by remember { mutableStateOf<RentalItem?>(null) }
    var noticeFormOpen by remember { mutableStateOf(false) }
    val role = session?.role ?: UserRole.NguoiDung
    val canManage = canManageScreen(role, screen)

    fun load() {
        state = UiState.Loading
    }

    LaunchedEffect(screen, session, state) {
        if (state is UiState.Loading) {
            state = repository.list(screen, session)
        }
    }

    LaunchedEffect(screen) {
        if (screen == AppScreen.Rooms) {
            val result = repository.list(AppScreen.Houses, null)
            if (result is UiState.Content) houses = result.data
        }
        if (screen in setOf(AppScreen.Electric, AppScreen.Water, AppScreen.Invoices, AppScreen.Payments, AppScreen.Notices)) {
            val result = repository.list(AppScreen.Rooms, null)
            if (result is UiState.Content) rooms = result.data
        }
        if (screen == AppScreen.Payments) {
            val result = repository.list(AppScreen.Invoices, session)
            if (result is UiState.Content) invoices = result.data
        }
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { items, source ->
        val statuses = listOf("Tất cả") + items.map { it.status }.distinct()
        val filtered = items.filter {
            val textMatch = it.title.contains(query, true) || it.id.contains(query, true) || it.note.contains(query, true) || it.value.contains(query, true)
            val statusMatch = status == "Tất cả" || it.status == status
            textMatch && statusMatch
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ModuleHeader(screen, items.size, source) }
            if (canManage) item {
                Button(
                    onClick = {
                        actionError = null
                        if (screen == AppScreen.Contracts) {
                            contractEditing = RentalItem("", "", "Chờ người thuê xác nhận", "", "")
                        } else if (screen == AppScreen.Notices) {
                            noticeFormOpen = true
                        } else {
                            editing = RentalItem(
                                id = "",
                                title = "",
                                status = defaultStatus(screen),
                                value = "",
                                note = ""
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = screenAccent(screen)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Thêm ${screen.label.lowercase()}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
            actionError?.let {
                item { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
            if (items.isNotEmpty()) {
                item {
                    SearchPanel(query, { query = it }, statuses, status, { status = it })
                }
            }
            if (items.isEmpty()) {
                item {
                    EmptyState(
                        if (canManage) "Chưa có dữ liệu. Hãy bấm nút Thêm phía trên để tạo mới."
                        else "Chưa có dữ liệu ${screen.label.lowercase()}."
                    )
                }
            } else if (filtered.isEmpty()) {
                item { EmptyState("Không tìm thấy kết quả phù hợp với bộ lọc.") }
            } else {
                items(filtered, key = { it.id }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RentalListCard(
                            item = item,
                            canManage = canManage,
                            onOpen = { selected = item },
                            onEdit = {
                                actionError = null
                                if (screen == AppScreen.Contracts) contractEditing = item else editing = item
                            },
                            onDelete = {
                                actionError = null
                                confirmData = ConfirmData(
                                    title = "Xác nhận xóa",
                                    message = "Bạn có chắc chắn muốn xóa bản ghi này (${item.id})? Hành động này không thể hoàn tác.",
                                    onConfirm = {
                                        scope.launch {
                                            repository.deleteItem(screen, item.id, session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể xóa dữ liệu." }
                                        }
                                    }
                                )
                            }
                        )
                        ModuleActionBar(
                            screen = screen,
                            item = item,
                            role = role,
                            onRentRoom = { rentRequestRoom = item },
                            onApproveRequest = {
                                confirmData = ConfirmData(
                                    title = "Duyệt yêu cầu thuê",
                                    message = "Bạn có chắc chắn muốn duyệt yêu cầu thuê này không?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.decideRentRequest(item.id, approve = true, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể duyệt yêu cầu." }
                                        }
                                    }
                                )
                            },
                            onRejectRequest = {
                                confirmData = ConfirmData(
                                    title = "Từ chối yêu cầu thuê",
                                    message = "Bạn có chắc chắn muốn từ chối yêu cầu thuê này không?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.decideRentRequest(item.id, approve = false, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể từ chối yêu cầu." }
                                        }
                                    }
                                )
                            },
                            onConfirmContract = {
                                confirmData = ConfirmData(
                                    title = "Xác nhận hợp đồng",
                                    message = "Bạn có chắc chắn muốn ký xác nhận hợp đồng này không?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.confirmContract(item.id, approve = true, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể xác nhận hợp đồng." }
                                        }
                                    }
                                )
                            },
                            onRejectContract = {
                                confirmData = ConfirmData(
                                    title = "Từ chối hợp đồng",
                                    message = "Bạn có từ chối ký hợp đồng này không?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.confirmContract(item.id, approve = false, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể từ chối hợp đồng." }
                                        }
                                    }
                                )
                            },
                            onCloseContract = {
                                confirmData = ConfirmData(
                                    title = "Kết thúc hợp đồng",
                                    message = "Bạn có chắc muốn kết thúc hợp đồng này sớm?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.closeContract(item.id, cancel = false, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể kết thúc hợp đồng." }
                                        }
                                    }
                                )
                            },
                            onCancelContract = {
                                confirmData = ConfirmData(
                                    title = "Hủy hợp đồng",
                                    message = "Bạn có chắc chắn muốn hủy bỏ hợp đồng này?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.closeContract(item.id, cancel = true, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể hủy hợp đồng." }
                                        }
                                    }
                                )
                            },
                            onRenewContract = { renewContract = item },
                            onApproveRenew = {
                                confirmData = ConfirmData(
                                    title = "Duyệt gia hạn",
                                    message = "Bạn đồng ý gia hạn hợp đồng này chứ?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.decideRenewRequest(item.id, approve = true, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể duyệt gia hạn." }
                                        }
                                    }
                                )
                            },
                            onRejectRenew = {
                                confirmData = ConfirmData(
                                    title = "Từ chối gia hạn",
                                    message = "Bạn không đồng ý gia hạn hợp đồng này?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.decideRenewRequest(item.id, approve = false, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể từ chối gia hạn." }
                                        }
                                    }
                                )
                            },
                            onPayInvoice = { paymentForInvoice = item },
                            onApprovePayment = {
                                confirmData = ConfirmData(
                                    title = "Xác nhận thanh toán",
                                    message = "Bạn đã nhận được tiền và muốn duyệt thanh toán này chứ?",
                                    onConfirm = {
                                        scope.launch {
                                            repository.decidePayment(item.id, approve = true, rejectReason = null, session = session)
                                                .onSuccess { load() }
                                                .onFailure { actionError = it.message ?: "Không thể duyệt thanh toán." }
                                        }
                                    }
                                )
                            },
                            onRejectPayment = { rejectingPayment = item },
                            onRespondIncident = { respondingIncident = item },
                            onMarkNoticeRead = {
                                scope.launch {
                                    repository.markNoticeAsRead(item.id, session)
                                        .onSuccess { load() }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    selected?.let { DetailDialog(it, screen, onDismiss = { selected = null }) }

    confirmData?.let {
        AlertDialog(
            onDismissRequest = { confirmData = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text(it.title, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
            text = { Text(it.message, color = Color(0xFF475569)) },
            confirmButton = {
                Button(
                    onClick = {
                        it.onConfirm()
                        confirmData = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) { Text("Xác nhận", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmData = null }) { Text("Hủy") }
            }
        )
    }

    editing?.let { item ->
        SpecializedEditDialog(
            screen = screen,
            item = item,
            houses = houses,
            rooms = rooms,
            invoices = invoices,
            onDismiss = { editing = null },
            onSaveUtility = { scr, rId, prd, oldVal, newVal, unitPr ->
                actionError = null
                scope.launch {
                    repository.saveUtilityReading(scr, rId, prd, oldVal, newVal, unitPr, session)
                        .onSuccess { editing = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể lưu chỉ số." }
                }
            },
            onSaveInvoice = { rId, prd, extra, note ->
                actionError = null
                scope.launch {
                    repository.createInvoice(rId, prd, extra, note, session)
                        .onSuccess { editing = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể tạo hóa đơn." }
                }
            },
            onSavePayment = { invId, txId, img, note ->
                actionError = null
                scope.launch {
                    repository.submitPayment(invId, txId, img, note, session)
                        .onSuccess { editing = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể gửi biên lai." }
                }
            },
            onSave = { updated ->
                actionError = null
                scope.launch {
                    repository.saveItem(screen, updated, session)
                        .onSuccess { editing = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể lưu dữ liệu." }
                }
            },
            repository = repository
        )
    }

    contractEditing?.let { item ->
        ContractEditorDialog(
            item = item,
            onDismiss = { contractEditing = null },
            onSave = { rId, tenant, start, end, dep, note, status ->
                actionError = null
                val updated = item.copy(
                    title = "Hợp đồng phòng $rId",
                    status = status,
                    value = dep,
                    note = note,
                    details = listOf(
                        "roomId" to rId,
                        "tenantUsername" to tenant,
                        "startDate" to start,
                        "endDate" to end,
                        "deposit" to dep
                    )
                )
                scope.launch {
                    repository.saveItem(AppScreen.Contracts, updated, session)
                        .onSuccess { contractEditing = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể lưu hợp đồng." }
                }
            }
        )
    }

    renewContract?.let { contract ->
        RenewRequestDialog(
            contract = contract,
            onDismiss = { renewContract = null },
            onSubmit = { newEnd, note ->
                actionError = null
                scope.launch {
                    repository.createRenewRequest(contract.id, session, newEnd, note)
                        .onSuccess { renewContract = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể gửi yêu cầu gia hạn." }
                }
            }
        )
    }

    rentRequestRoom?.let { room ->
        RentRequestDialog(
            room = room,
            onDismiss = { rentRequestRoom = null },
            onSubmit = { duration, note ->
                actionError = null
                scope.launch {
                    repository.requestRoom(room.id, session, duration, note)
                        .onSuccess { rentRequestRoom = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể gửi yêu cầu thuê phòng." }
                }
            }
        )
    }

    paymentForInvoice?.let { invoice ->
        SubmitPaymentFormDialog(
            item = RentalItem(
                id = "",
                title = "",
                status = "Chờ xác nhận",
                value = invoice.value,
                note = "",
                details = listOf("invoiceId" to invoice.id)
            ),
            invoices = listOf(invoice),
            onDismiss = { paymentForInvoice = null },
            onSavePayment = { invId, txId, img, note ->
                actionError = null
                scope.launch {
                    repository.submitPayment(invId, txId, img, note, session)
                        .onSuccess { paymentForInvoice = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể gửi biên lai." }
                }
            }
        )
    }

    rejectingPayment?.let { payment ->
        RejectPaymentDialog(
            onDismiss = { rejectingPayment = null },
            onSubmit = { reason ->
                actionError = null
                scope.launch {
                    repository.decidePayment(payment.id, approve = false, rejectReason = reason, session = session)
                        .onSuccess { rejectingPayment = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể từ chối thanh toán." }
                }
            }
        )
    }

    respondingIncident?.let { incident ->
        RespondIncidentDialog(
            incident = incident,
            onDismiss = { respondingIncident = null },
            onSubmit = { response, newStatus ->
                actionError = null
                scope.launch {
                    repository.respondToIncident(incident.id, response, newStatus, session)
                        .onSuccess { respondingIncident = null; load() }
                        .onFailure { actionError = it.message ?: "Không thể phản hồi sự cố." }
                }
            }
        )
    }

    if (noticeFormOpen && canManage) {
        NoticeFormDialog(
            rooms = rooms,
            onDismiss = { noticeFormOpen = false },
            onSave = { title, content, targetType ->
                actionError = null
                scope.launch {
                    repository.createNotice(title, content, targetType, session)
                        .onSuccess { noticeFormOpen = false; load() }
                        .onFailure { actionError = it.message ?: "Không thể tạo thông báo." }
                }
            }
        )
    }
}

@Composable
internal fun AccountScreen(
    repository: RentalRepository,
    session: UserSession?,
    onSessionExpired: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var state by remember(session) { mutableStateOf<UiState<AccountProfile>>(UiState.Loading) }
    var editing by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(AccountProfile()) }

    fun load() {
        state = UiState.Loading
    }

    fun uploadFromUri(uri: Uri, front: Boolean) {
        val currentSession = session ?: return
        saving = true
        error = null
        scope.launch {
            val result = runCatching {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Không đọc được ảnh CCCD.")
                repository.uploadCccdImage("cccd_${System.currentTimeMillis()}.jpg", mime, bytes, currentSession).getOrThrow()
            }
            result.onSuccess {
                draft = if (front) draft.copy(cccdFrontUrl = it) else draft.copy(cccdBackUrl = it)
                message = "Upload ảnh CCCD thành công. Hãy bấm Lưu hồ sơ để cập nhật."
            }.onFailure {
                error = it.message ?: "Upload ảnh CCCD thất bại."
            }
            saving = false
        }
    }

    val frontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadFromUri(it, front = true) }
    }
    val backPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadFromUri(it, front = false) }
    }

    LaunchedEffect(session, state) {
        if (state is UiState.Loading && session != null) {
            state = repository.account(session)
            (state as? UiState.Content<AccountProfile>)?.let { draft = it.data }
        }
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { profile, _ ->
        if (!editing) draft = profile
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppLogo(size = 48)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hồ sơ cá nhân", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text(profile.role.label, color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        message?.let { Text(it, color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
                        error?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
                        
                        Divider(color = Color(0xFFF1F5F9))

                        if (editing) {
                            ProfileEditor(
                                profile = draft,
                                onChange = { draft = it },
                                onPickFront = { frontPicker.launch("image/*") },
                                onPickBack = { backPicker.launch("image/*") }
                            )
                        } else {
                            listOf(
                                "Họ tên" to profile.fullName,
                                "Tên đăng nhập" to profile.username,
                                "Email" to profile.email,
                                "Số điện thoại" to profile.phone,
                                "CCCD" to profile.cccd,
                                "Địa chỉ" to profile.address,
                                "Ngân hàng" to profile.bankName,
                                "Số tài khoản" to profile.bankAccount
                            ).forEach { DetailRow(it.first, it.second.ifBlank { "Chưa cập nhật" }) }
                        }
                        
                        Divider(color = Color(0xFFF1F5F9))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                enabled = !saving,
                                onClick = {
                                    if (editing) {
                                        val currentSession = session ?: return@Button
                                        saving = true
                                        scope.launch {
                                            repository.updateAccount(currentSession, draft)
                                                .onSuccess {
                                                    draft = it
                                                    state = UiState.Content(it, DataSource.Local)
                                                    editing = false
                                                    message = "Cập nhật hồ sơ thành công."
                                                }
                                                .onFailure { error = it.message ?: "Không thể cập nhật hồ sơ." }
                                            saving = false
                                        }
                                    } else {
                                        editing = true
                                        message = null
                                        error = null
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) { Text(if (editing) "Lưu hồ sơ" else "Cập nhật", fontWeight = FontWeight.Bold) }
                            
                            OutlinedButton(
                                onClick = { changingPassword = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Đổi mật khẩu", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (editing) {
                            TextButton(
                                onClick = { editing = false; draft = profile },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Hủy chỉnh sửa", color = Color(0xFF64748B), fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }
        }
    }

    if (changingPassword) {
        ChangePasswordDialog(
            saving = saving,
            onDismiss = { changingPassword = false },
            onSubmit = { oldPassword, newPassword, confirmPassword ->
                val currentSession = session ?: return@ChangePasswordDialog
                saving = true
                scope.launch {
                    repository.changePassword(currentSession, oldPassword, newPassword, confirmPassword)
                        .onSuccess {
                            message = it
                            changingPassword = false
                        }
                        .onFailure { error = it.message ?: "Không thể đổi mật khẩu." }
                    saving = false
                }
            }
        )
    }
}

@Composable
internal fun ProfileEditor(
    profile: AccountProfile,
    onChange: (AccountProfile) -> Unit,
    onPickFront: () -> Unit,
    onPickBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(profile.fullName, { onChange(profile.copy(fullName = it)) }, label = { Text("Họ tên") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.email, { onChange(profile.copy(email = it)) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.phone, { onChange(profile.copy(phone = it)) }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.cccd, { onChange(profile.copy(cccd = it)) }, label = { Text("CCCD/CMND") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(profile.dateOfBirth.take(10), { onChange(profile.copy(dateOfBirth = it)) }, label = { Text("Ngày sinh yyyy-MM-dd") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(profile.gender, { onChange(profile.copy(gender = it)) }, label = { Text("Giới tính") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
        }
        OutlinedTextField(profile.address, { onChange(profile.copy(address = it)) }, label = { Text("Địa chỉ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.workplace, { onChange(profile.copy(workplace = it)) }, label = { Text("Nơi công tác") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPickFront, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text(if (profile.cccdFrontUrl.isBlank()) "Ảnh CCCD trước" else "Đổi ảnh trước")
            }
            OutlinedButton(onClick = onPickBack, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text(if (profile.cccdBackUrl.isBlank()) "Ảnh CCCD sau" else "Đổi ảnh sau")
            }
        }
        OutlinedTextField(profile.bankName, { onChange(profile.copy(bankName = it)) }, label = { Text("Tên ngân hàng") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(profile.bankAccount, { onChange(profile.copy(bankAccount = it)) }, label = { Text("Số tài khoản") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(profile.bankOwner, { onChange(profile.copy(bankOwner = it)) }, label = { Text("Chủ tài khoản") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
        }
        OutlinedTextField(profile.transferContent, { onChange(profile.copy(transferContent = it)) }, label = { Text("Nội dung chuyển khoản mặc định") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
    }
}

@Composable
internal fun ModuleHeader(screen: AppScreen, count: Int, source: DataSource) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val accent = screenAccent(screen)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = screen.shortCode,
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = screen.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "$count bản ghi • ${sourceLabel(source)}",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    statuses: List<String>,
    status: String,
    onStatusChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Tìm kiếm theo từ khóa...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statuses) { item ->
                    FilterChip(
                        selected = status == item,
                        onClick = { onStatusChange(item) },
                        label = { Text(item) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun RentalListCard(
    item: RentalItem,
    canManage: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 4.dp, height = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor(item.status))
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor(item.status).copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            item.id.takeLast(3),
                            color = statusColor(item.status),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        item.id,
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusPill(item.status)
            }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.value,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note,
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor(item.status)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Chi tiết", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (canManage) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Sửa", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Xóa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ModuleActionBar(
    screen: AppScreen,
    item: RentalItem,
    role: UserRole,
    onRentRoom: () -> Unit,
    onApproveRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onConfirmContract: () -> Unit,
    onRejectContract: () -> Unit,
    onCloseContract: () -> Unit,
    onCancelContract: () -> Unit,
    onRenewContract: () -> Unit,
    onApproveRenew: () -> Unit,
    onRejectRenew: () -> Unit,
    onPayInvoice: () -> Unit = {},
    onApprovePayment: () -> Unit = {},
    onRejectPayment: () -> Unit = {},
    onRespondIncident: () -> Unit = {},
    onMarkNoticeRead: () -> Unit = {}
) {
    val showRent = role == UserRole.NguoiDung && screen == AppScreen.Rooms && item.status.equals("Còn trống", true)
    val showDecision = role != UserRole.NguoiDung && screen == AppScreen.RentRequests && item.status.contains("Chờ", true)
    val showConfirm = role == UserRole.NguoiDung && screen == AppScreen.Contracts && item.status == "Chờ người thuê xác nhận"
    val showRenew = role == UserRole.NguoiDung && screen == AppScreen.Contracts && item.status == "Đang hiệu lực"
    val showClose = role != UserRole.NguoiDung && screen == AppScreen.Contracts && item.status in setOf("Chờ người thuê xác nhận", "Đang hiệu lực")
    val showRenewDecision = role != UserRole.NguoiDung && screen == AppScreen.RenewRequests && item.status.contains("Chờ", true)
    val showPayInvoice = role == UserRole.NguoiDung && screen == AppScreen.Invoices && item.status == "Chưa thanh toán"
    val showPaymentDecision = role != UserRole.NguoiDung && screen == AppScreen.Payments && item.status == "Chờ xác nhận"
    val showRespondIncident = role != UserRole.NguoiDung && screen == AppScreen.Incidents && !item.status.equals("Đã khắc phục", true)
    val showMarkRead = screen == AppScreen.Notices && item.status == "Mới"
    if (!showRent && !showDecision && !showConfirm && !showRenew && !showClose && !showRenewDecision
        && !showPayInvoice && !showPaymentDecision && !showRespondIncident && !showMarkRead) return

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showRent) {
                Button(
                    onClick = onRentRoom,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Gửi yêu cầu thuê", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (showDecision) {
                Button(
                    onClick = onApproveRequest,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Duyệt", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRejectRequest,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Từ chối", fontWeight = FontWeight.Bold)
                }
            }
            if (showConfirm) {
                Button(
                    onClick = onConfirmContract,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Xác nhận", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRejectContract,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Từ chối", fontWeight = FontWeight.Bold)
                }
            }
            if (showRenew) {
                Button(
                    onClick = onRenewContract,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Gửi yêu cầu gia hạn", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (showClose) {
                Button(
                    onClick = onCloseContract,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Kết thúc", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCancelContract,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Hủy hợp đồng", fontWeight = FontWeight.Bold)
                }
            }
            if (showRenewDecision) {
                Button(
                    onClick = onApproveRenew,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Duyệt gia hạn", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRejectRenew,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Từ chối", fontWeight = FontWeight.Bold)
                }
            }
            if (showPayInvoice) {
                Button(
                    onClick = onPayInvoice,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Thanh toán ngay", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (showPaymentDecision) {
                Button(
                    onClick = onApprovePayment,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Xác nhận", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRejectPayment,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Từ chối", fontWeight = FontWeight.Bold)
                }
            }
            if (showRespondIncident) {
                Button(
                    onClick = onRespondIncident,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Phản hồi sự cố", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (showMarkRead) {
                OutlinedButton(
                    onClick = onMarkNoticeRead,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6)),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Đánh dấu đã đọc", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
