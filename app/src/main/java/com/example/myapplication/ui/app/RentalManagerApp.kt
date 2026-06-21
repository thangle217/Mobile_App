package com.example.myapplication.ui.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

val LocalAppThemeIsLight = compositionLocalOf { false }

fun AppScreen.getIcon(): ImageVector {
    return when (this) {
        AppScreen.Dashboard -> Icons.Default.Home
        AppScreen.Account, AppScreen.Tenants, AppScreen.Users -> Icons.Default.Person
        AppScreen.Notices -> Icons.Default.Notifications
        AppScreen.Incidents -> Icons.Default.Warning
        AppScreen.Electric, AppScreen.Water -> Icons.Default.Info
        else -> Icons.Default.List
    }
}

private val AppGreenDark  = Color(0xFF064E3B)
private val AppGreenMid   = Color(0xFF0F766E)
private val AppGreenLight = Color(0xFF34D399)
private val AppBgGradient = Brush.verticalGradient(listOf(Color(0xFF0F766E), Color(0xFF064E3B)))
private val AppCardBg     = Color.White.copy(alpha = 0.15f)
private val AppCardBorder = Color.White.copy(alpha = 0.28f)

@Composable
fun RentalManagerApp() {
    val context = LocalContext.current
    val repository = remember(context) { RentalRepository(context) }
    val sessionStore = remember { SessionStore(context) }
    val scope = rememberCoroutineScope()
    var restored by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<UserSession?>(null) }
    var screen by remember { mutableStateOf(AppScreen.Dashboard) }
    var isLightMode by remember { mutableStateOf(false) }

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
        CompositionLocalProvider(LocalAppThemeIsLight provides isLightMode) {
            MainShell(
                repository = repository,
                session = session,
                screen = screen,
                onScreenChange = { screen = it },
                onLogout = {
                    scope.launch { sessionStore.clear() }
                    session = null
                    screen = AppScreen.Dashboard
                },
                onToggleTheme = { isLightMode = !isLightMode }
            )
        }
    }
}

@Composable
internal fun MainShell(
    repository: RentalRepository,
    session: UserSession?,
    screen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val role = session?.role ?: UserRole.ChuTro
    val screens = screensForRole(role)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AppGreenDark,
                drawerContentColor = Color.White
            ) {
                Column(modifier = Modifier.background(AppBgGradient).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                        AppLogo(size = 44)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(session?.displayName ?: role.label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text(role.label, color = AppGreenLight, fontSize = 12.sp)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(AppGreenLight.copy(alpha = 0.25f)))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(screens) { item ->
                            val active = item == screen
                            val accent = screenAccent(item)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CutCornerShape(10.dp))
                                    .background(if (active) AppGreenLight.copy(alpha = 0.18f) else Color.Transparent)
                                    .border(1.dp, if (active) AppGreenLight.copy(alpha = 0.5f) else Color.Transparent, CutCornerShape(10.dp))
                                    .clickable {
                                        onScreenChange(item)
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = CutCornerShape(6.dp), color = accent.copy(alpha = if (active) 0.3f else 0.15f)) {
                                        Icon(
                                            item.getIcon(), contentDescription = item.label,
                                            tint = if (active) AppGreenLight else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(6.dp).size(18.dp)
                                        )
                                    }
                                    Text(item.label, color = if (active) Color.White else Color.White.copy(alpha = 0.75f), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
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
                    onMenu = { scope.launch { drawerState.open() } },
                    onLogout = onLogout,
                    onToggleTheme = onToggleTheme
                )
            },
            bottomBar = {
                AppBottomBar(
                    items = bottomScreens(role),
                    selected = screen,
                    onSelected = onScreenChange
                )
            },
            containerColor = AppGreenDark
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
                .background(AppBgGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ModuleHeader(screen, items.size, source) }
            if (canManage) item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CutCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(AppGreenMid, Color(0xFF0369A1))))
                        .clickable {
                            actionError = null
                            if (screen == AppScreen.Contracts) {
                                contractEditing = RentalItem("", "", "Chờ người thuê xác nhận", "", "")
                            } else if (screen == AppScreen.Notices) {
                                noticeFormOpen = true
                            } else {
                                editing = RentalItem(id = "", title = "", status = defaultStatus(screen), value = "", note = "")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Thêm ${screen.label.lowercase()}", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            actionError?.let {
                item { Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
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
            title = { Text(it.title, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(it.message, color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(AppGreenMid, Color(0xFF0369A1))))
                        .clickable { it.onConfirm(); confirmData = null }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) { Text("Xác nhận", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmData = null }) { Text("Hủy", color = AppGreenLight) }
            },
            containerColor = AppGreenDark,
            titleContentColor = Color.White,
            shape = CutCornerShape(20.dp)
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
                .background(AppBgGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .clip(CutCornerShape(24.dp))
                        .background(AppCardBg)
                        .border(1.dp, AppCardBorder, CutCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(size = 48)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hồ sơ cá nhân", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(profile.role.label, color = AppGreenLight, fontSize = 12.sp)
                        }
                    }
                    message?.let { Text(it, color = AppGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    error?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(AppGreenLight.copy(alpha = 0.25f)))

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

                    Box(Modifier.fillMaxWidth().height(1.dp).background(AppGreenLight.copy(alpha = 0.25f)))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f).height(48.dp)
                                .clip(CutCornerShape(12.dp))
                                .background(if (!saving) Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0284C7))) else Brush.horizontalGradient(listOf(Color.Gray, Color.Gray)))
                                .clickable(enabled = !saving) {
                                    if (editing) {
                                        val currentSession = session ?: return@clickable
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
                                        editing = true; message = null; error = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(if (editing) "Lưu hồ sơ" else "Cập nhật", color = Color.White, fontWeight = FontWeight.Bold) }

                        Box(
                            modifier = Modifier
                                .weight(1f).height(48.dp)
                                .clip(CutCornerShape(12.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CutCornerShape(12.dp))
                                .clickable { changingPassword = true },
                            contentAlignment = Alignment.Center
                        ) { Text("Đổi mật khẩu", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) }
                    }
                    if (editing) {
                        TextButton(
                            onClick = { editing = false; draft = profile },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Hủy chỉnh sửa", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium) }
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
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color.White.copy(alpha = 0.1f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
        focusedBorderColor = AppGreenLight,
        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
        cursorColor = AppGreenLight,
        focusedLabelColor = AppGreenLight,
        unfocusedLabelColor = Color.White.copy(alpha = 0.8f)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(profile.fullName, { onChange(profile.copy(fullName = it)) }, label = { Text("Họ tên") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        OutlinedTextField(profile.email, { onChange(profile.copy(email = it)) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        OutlinedTextField(profile.phone, { onChange(profile.copy(phone = it)) }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        OutlinedTextField(profile.cccd, { onChange(profile.copy(cccd = it)) }, label = { Text("CCCD/CMND") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(profile.dateOfBirth.take(10), { onChange(profile.copy(dateOfBirth = it)) }, label = { Text("Ngày sinh") }, modifier = Modifier.weight(1f), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
            OutlinedTextField(profile.gender, { onChange(profile.copy(gender = it)) }, label = { Text("Giới tính") }, modifier = Modifier.weight(1f), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        }
        OutlinedTextField(profile.address, { onChange(profile.copy(address = it)) }, label = { Text("Địa chỉ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        OutlinedTextField(profile.workplace, { onChange(profile.copy(workplace = it)) }, label = { Text("Nơi công tác") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.weight(1f).height(42.dp).clip(CutCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, AppGreenLight.copy(alpha = 0.4f), CutCornerShape(8.dp))
                    .clickable { onPickFront() },
                contentAlignment = Alignment.Center
            ) { Text(if (profile.cccdFrontUrl.isBlank()) "Ảnh CCCD trước" else "Đổi ảnh trước", color = AppGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            Box(
                modifier = Modifier.weight(1f).height(42.dp).clip(CutCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, AppGreenLight.copy(alpha = 0.4f), CutCornerShape(8.dp))
                    .clickable { onPickBack() },
                contentAlignment = Alignment.Center
            ) { Text(if (profile.cccdBackUrl.isBlank()) "Ảnh CCCD sau" else "Đổi ảnh sau", color = AppGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
        OutlinedTextField(profile.bankName, { onChange(profile.copy(bankName = it)) }, label = { Text("Tên ngân hàng") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(profile.bankAccount, { onChange(profile.copy(bankAccount = it)) }, label = { Text("Số tài khoản") }, modifier = Modifier.weight(1f), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
            OutlinedTextField(profile.bankOwner, { onChange(profile.copy(bankOwner = it)) }, label = { Text("Chủ tài khoản") }, modifier = Modifier.weight(1f), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
        }
        OutlinedTextField(profile.transferContent, { onChange(profile.copy(transferContent = it)) }, label = { Text("Nội dung chuyển khoản") }, modifier = Modifier.fillMaxWidth(), shape = CutCornerShape(8.dp), colors = fieldColors)
    }
}

@Composable
internal fun ModuleHeader(screen: AppScreen, count: Int, source: DataSource) {
    val accent = screenAccent(screen)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(14.dp))
            .background(AppCardBg)
            .border(1.dp, accent.copy(alpha = 0.4f), CutCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CutCornerShape(10.dp), color = accent.copy(alpha = 0.2f), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(screen.shortCode, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(screen.label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("$count bản ghi • ${sourceLabel(source)}", color = AppGreenLight.copy(alpha = 0.8f), fontSize = 11.sp)
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
    Column(
        modifier = Modifier
            .clip(CutCornerShape(16.dp))
            .background(AppCardBg)
            .border(1.dp, AppCardBorder, CutCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Tìm kiếm...", color = Color.White.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CutCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AppGreenLight,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = AppGreenLight
            )
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(statuses) { item ->
                val active = status == item
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(8.dp))
                        .background(if (active) AppGreenLight.copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (active) AppGreenLight else Color.White.copy(alpha = 0.3f), CutCornerShape(8.dp))
                        .clickable { onStatusChange(item) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(item, color = if (active) AppGreenLight else Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
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
    val accent = statusColor(item.status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(16.dp))
            .background(AppCardBg)
            .border(1.dp, accent.copy(alpha = 0.35f), CutCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(width = 4.dp, height = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(10.dp))
            Surface(shape = CutCornerShape(10.dp), color = accent.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(item.id.takeLast(3), color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Text(item.id, color = AppGreenLight.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            StatusPill(item.status)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                if (item.note.isNotBlank()) {
                    Text(item.note, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f).height(40.dp)
                    .clip(CutCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.25f))
                    .border(1.dp, accent.copy(alpha = 0.5f), CutCornerShape(10.dp))
                    .clickable { onOpen() },
                contentAlignment = Alignment.Center
            ) { Text("Chi tiết", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            if (canManage) {
                Box(
                    modifier = Modifier
                        .weight(1f).height(40.dp)
                        .clip(CutCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CutCornerShape(10.dp))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) { Text("Sửa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                Box(
                    modifier = Modifier
                        .weight(1f).height(40.dp)
                        .clip(CutCornerShape(10.dp))
                        .background(Color(0xFFF87171).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFF87171).copy(alpha = 0.4f), CutCornerShape(10.dp))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) { Text("Xóa", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
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
        shape = CutCornerShape(14.dp),
        color = AppCardBg,
        border = BorderStroke(1.dp, AppCardBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showRent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(44.dp)
                        .clip(CutCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(AppGreenMid, Color(0xFF0369A1))))
                        .clickable { onRentRoom() },
                    contentAlignment = Alignment.Center
                ) { Text("Gửi yêu cầu thuê", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
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
