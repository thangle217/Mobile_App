package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RentalManagerApp()
            }
        }
    }
}

data class RentalItem(
    val code: String,
    val title: String,
    val status: String,
    val value: String,
    val note: String,
    val details: List<Pair<String, String>> = emptyList()
)

enum class AppScreen(val label: String) {
    Dashboard("Tong quan"),
    Houses("Nha tro"),
    RoomTypes("Loai phong"),
    Rooms("Phong"),
    Tenants("Khach thue"),
    Contracts("Hop dong"),
    Invoices("Hoa don"),
    Payments("Thanh toan"),
    Services("Dich vu"),
    ServiceRegs("Dang ky DV"),
    Electric("Chi so dien"),
    Water("Chi so nuoc"),
    RentRequests("Yeu cau thue"),
    RenewRequests("Gia han"),
    Incidents("Su co"),
    Notices("Thong bao"),
    Users("Nguoi dung"),
    Account("Tai khoan")
}

class RentalStore {
    private val emptyItems = mutableStateListOf<RentalItem>()

    val houses = mutableStateListOf(
        RentalItem("NT01", "Nha tro An Binh", "Dang hoat dong", "20 phong", "Quan 9, TP.HCM", listOf("Chu tro" to "Nguyen Minh Quan", "Dia chi" to "100 Le Loi", "Mo ta" to "An ninh, gan tram xe buyt")),
        RentalItem("NT02", "Ky tuc xa Mini Hoa Sen", "Dang hoat dong", "16 phong", "Thu Duc, TP.HCM", listOf("Chu tro" to "Tran Thi Thu Ha", "Dia chi" to "107 Nguyen Van Cu", "Mo ta" to "Gan DHQG, co camera")),
        RentalItem("NT03", "Studio Green Home", "Tam dung", "12 phong", "Tan Phu, TP.HCM", listOf("Chu tro" to "Le Hoang Phuc", "Dia chi" to "114 Cong Hoa"))
    )
    val roomTypes = mutableStateListOf(
        RentalItem("LP01", "Phong thuong", "Dang dung", "2.300.000d - 3.000.000d", "Phong co ban, chi phi hop ly", listOf("Suc chua" to "1-2 nguoi", "Dien tich" to "18-24 m2")),
        RentalItem("LP02", "Phong gac lung", "Dang dung", "3.200.000d - 4.000.000d", "Co gac, toi uu khong gian", listOf("Suc chua" to "2-3 nguoi", "Tien nghi" to "Gac, bep nho")),
        RentalItem("LP03", "Studio", "Dang dung", "4.500.000d - 5.500.000d", "Rong, co bep va noi that co ban", listOf("Suc chua" to "2 nguoi", "Tien nghi" to "May lanh, bep rieng"))
    )
    val rooms = mutableStateListOf(
        RentalItem("P101", "Phong A01", "Da thue", "3.200.000d/thang", "Tang 1 - Nha tro An Binh", listOf("Loai phong" to "Phong gac lung", "Dien tich" to "24 m2", "Suc chua" to "2", "Nguoi thue" to "Nguoi Thue Demo")),
        RentalItem("P102", "Phong A02", "Con trong", "2.750.000d/thang", "Tang 1 - Nha tro An Binh", listOf("Loai phong" to "Phong thuong", "Dien tich" to "20 m2", "Suc chua" to "2", "Mo ta" to "Phong trong san sang cho thue")),
        RentalItem("P203", "Phong B03", "Dang sua", "3.600.000d/thang", "Tang 2 - Ky tuc xa Mini Hoa Sen", listOf("Loai phong" to "Studio", "Dien tich" to "28 m2", "Suc chua" to "2", "Mo ta" to "Dang sua chua nhe")),
        RentalItem("P204", "Phong B04", "Con trong", "4.200.000d/thang", "Tang 2 - Ky tuc xa Mini Hoa Sen", listOf("Loai phong" to "Studio", "Dien tich" to "30 m2", "Suc chua" to "2"))
    )
    val tenants = mutableStateListOf(
        RentalItem("KT001", "Nguoi Thue Demo", "Dang thue", "0910000001", "Phong P101 tu 01/05/2026", listOf("Email" to "nguoithue@example.com", "CCCD" to "079200000001", "Noi cong tac" to "Sinh vien")),
        RentalItem("KT002", "Tran Thi Mai", "Dang thue", "0910000002", "Phong P203 tu 15/03/2026", listOf("Email" to "nguoithue2@example.com", "CCCD" to "079200000002")),
        RentalItem("KT003", "Le Van Nam", "Cho cap nhat", "0910000003", "Ho so vua tao tu yeu cau thue", listOf("Email" to "nguoithue3@example.com"))
    )
    val contracts = mutableStateListOf(
        RentalItem("HDONG01", "Hop dong P101", "Dang hieu luc", "01/05/2026 - 01/05/2027", "Tien coc 3.200.000d", listOf("Nguoi thue" to "Nguoi Thue Demo", "Phong" to "P101", "Noi dung" to "Hop dong thue phong mau")),
        RentalItem("HDONG02", "Hop dong P203", "Sap het han", "15/03/2026 - 20/06/2026", "Con 16 ngay", listOf("Nguoi thue" to "Tran Thi Mai", "Phong" to "P203", "Tien coc" to "3.600.000d")),
        RentalItem("HDONG03", "Hop dong tu yeu cau P102", "Cho nguoi thue xac nhan", "10/06/2026 - 10/12/2026", "Tao sau khi chu tro duyet", listOf("Nguoi thue" to "Le Van Nam", "Phong" to "P102"))
    )
    val invoices = mutableStateListOf(
        RentalItem("H0526-101", "Hoa don P101 ky 2026-05", "Chua thanh toan", "3.815.000d", "Tien dien + nuoc + dich vu", listOf("Phong" to "P101", "Ky hoa don" to "2026-05", "Tien dien" to "245.000d", "Tien nuoc" to "90.000d", "Dich vu" to "Internet, giu xe", "QR" to "VietQR theo tai khoan chu tro")),
        RentalItem("H0526-203", "Hoa don P203 ky 2026-05", "Thanh toan mot phan", "4.020.000d", "Da xac nhan 2.000.000d", listOf("Phong" to "P203", "Ky hoa don" to "2026-05", "Con lai" to "2.020.000d")),
        RentalItem("H0626-101", "Hoa don thue phong P101", "Da thanh toan", "3.200.000d", "Hoa don thue phong", listOf("Loai" to "ThuePhong", "Ngay lap" to "01/06/2026"))
    )
    val payments = mutableStateListOf(
        RentalItem("TT001", "Bien lai P203", "Cho xac nhan", "2.020.000d", "Ma GD VCB2030526", listOf("Hoa don" to "H0526-203", "Hinh thuc" to "Chuyen khoan", "Anh bien lai" to "Da gui")),
        RentalItem("TT002", "Tien mat P101", "Da xac nhan", "3.200.000d", "Chu tro nhap truc tiep", listOf("Hoa don" to "H0626-101", "Nguoi xac nhan" to "Nguyen Minh Quan")),
        RentalItem("TT003", "Bien lai loi", "Tu choi", "500.000d", "Sai noi dung chuyen khoan", listOf("Ly do" to "Khong khop ma hoa don"))
    )
    val services = mutableStateListOf(
        RentalItem("DV01", "Internet", "Tinh phi", "100.000d/thang", "Tinh theo phong", listOf("Nha tro" to "Nha tro An Binh", "Lich su gia" to "Cap nhat 01/05/2026")),
        RentalItem("DV02", "Giu xe may", "Tinh phi", "90.000d/thang", "Tinh theo xe", listOf("Nha tro" to "Nha tro An Binh")),
        RentalItem("DV03", "Camera an ninh", "Tien ich", "0d", "Hien thi tien ich phong", listOf("Nha tro" to "Ky tuc xa Mini Hoa Sen"))
    )
    val serviceRegs = mutableStateListOf(
        RentalItem("DKDV01", "P101 dang dung Internet", "Dang su dung", "100.000d/thang", "Dang ky ky 2026-05", listOf("Nguoi thue" to "Nguoi Thue Demo", "Phong" to "P101", "Dich vu" to "Internet")),
        RentalItem("DKDV02", "P101 dang dung Giu xe", "Dang su dung", "90.000d/thang", "Dang ky ky 2026-05", listOf("Nguoi thue" to "Nguoi Thue Demo", "Phong" to "P101", "Dich vu" to "Giu xe may")),
        RentalItem("DKDV03", "P203 huy May giat", "Da huy", "70.000d/thang", "Nguoi thue da huy", listOf("Phong" to "P203"))
    )
    val electric = mutableStateListOf(
        RentalItem("DIEN-P101", "Dien P101 ky 2026-05", "Da ghi", "70 kWh x 3.500d", "245.000d", listOf("Chi so cu" to "120", "Chi so moi" to "190", "Ngay ghi" to "31/05/2026")),
        RentalItem("DIEN-P203", "Dien P203 ky 2026-05", "Da ghi", "64 kWh x 3.500d", "224.000d", listOf("Chi so cu" to "80", "Chi so moi" to "144")),
        RentalItem("DIEN-P204", "Dien P204 ky 2026-06", "Can ghi", "Chua co chi so", "Can ghi truoc khi tao hoa don", listOf("Phong" to "P204"))
    )
    val water = mutableStateListOf(
        RentalItem("NUOC-P101", "Nuoc P101 ky 2026-05", "Da ghi", "6 m3 x 15.000d", "90.000d", listOf("Chi so cu" to "30", "Chi so moi" to "36")),
        RentalItem("NUOC-P203", "Nuoc P203 ky 2026-05", "Da ghi", "7 m3 x 15.000d", "105.000d", listOf("Chi so cu" to "20", "Chi so moi" to "27")),
        RentalItem("NUOC-P204", "Nuoc P204 ky 2026-06", "Can ghi", "Chua co chi so", "Can ghi truoc khi tao hoa don", listOf("Phong" to "P204"))
    )
    val rentRequests = mutableStateListOf(
        RentalItem("YCT01", "Le Van Nam muon thue P102", "Cho duyet", "6 thang", "Muon vao ngay 10/06/2026", listOf("Phong" to "P102", "Ngay bat dau" to "10/06/2026", "Ghi chu" to "Can xem phong truoc")),
        RentalItem("YCT02", "Hoang Gia Huy muon thue P204", "Da duyet", "12 thang", "Da tao hop dong cho nguoi thue xac nhan", listOf("Phong" to "P204")),
        RentalItem("YCT03", "Bui Khanh Vy muon thue P203", "Tu choi", "3 thang", "Phong dang sua", listOf("Phong" to "P203"))
    )
    val renewRequests = mutableStateListOf(
        RentalItem("GH001", "Gia han hop dong P203", "Cho duyet", "Them 6 thang", "Nguoi thue muon giu gia cu", listOf("Hop dong" to "HDONG02", "Ngay gui" to "01/06/2026")),
        RentalItem("GH002", "Gia han hop dong P101", "Da chap nhan", "Them 12 thang", "Chu tro da chap nhan", listOf("Hop dong" to "HDONG01"))
    )
    val incidents = mutableStateListOf(
        RentalItem("SC001", "Ro nuoc trong phong P101", "Moi", "Rat gap", "Nguoi thue vua bao cao", listOf("Phong" to "P101", "Noi dung" to "Nuoc ro tu tran nha tam")),
        RentalItem("SC002", "Wifi yeu", "Dang xu ly", "Binh thuong", "Da hen ky thuat", listOf("Phong" to "P203", "Phan hoi" to "Chu tro dang xu ly")),
        RentalItem("SC003", "Den hanh lang hong", "Da xu ly", "Binh thuong", "Da thay bong den", listOf("Phong" to "Tang 2"))
    )
    val notices = mutableStateListOf(
        RentalItem("TB01", "Hoa don thang 05 da duoc tao", "Moi", "HoaDon", "Nguoi thue vui long thanh toan truoc ngay 10", listOf("Nguoi nhan" to "Tat ca phong dang thue")),
        RentalItem("TB02", "Hop dong P203 sap het han", "Moi", "HopDong", "Con duoi 30 ngay", listOf("Nguoi nhan" to "Tran Thi Mai")),
        RentalItem("TB03", "Bao tri he thong nuoc", "Da doc", "He thong", "Bao tri ngay 08/06/2026", listOf("Nguoi nhan" to "Tat ca"))
    )
    val users = mutableStateListOf(
        RentalItem("U01", "Admin he thong", "Admin", "admin@demo.local", "Quan ly toan bo he thong", listOf("Ten dang nhap" to "Admin")),
        RentalItem("U02", "Nguyen Minh Quan", "ChuTro", "chutro@example.com", "Chu tro Nha tro An Binh", listOf("Ngan hang" to "VCB 1020304001")),
        RentalItem("U03", "Nguoi Thue Demo", "NguoiDung", "nguoithue@example.com", "Nguoi thue phong P101", listOf("CCCD" to "079200000001"))
    )

    fun listFor(screen: AppScreen): MutableList<RentalItem> = when (screen) {
        AppScreen.Houses -> houses
        AppScreen.RoomTypes -> roomTypes
        AppScreen.Rooms -> rooms
        AppScreen.Tenants -> tenants
        AppScreen.Contracts -> contracts
        AppScreen.Invoices -> invoices
        AppScreen.Payments -> payments
        AppScreen.Services -> services
        AppScreen.ServiceRegs -> serviceRegs
        AppScreen.Electric -> electric
        AppScreen.Water -> water
        AppScreen.RentRequests -> rentRequests
        AppScreen.RenewRequests -> renewRequests
        AppScreen.Incidents -> incidents
        AppScreen.Notices -> notices
        AppScreen.Users -> users
        else -> emptyItems
    }

    fun addTo(screen: AppScreen, item: RentalItem) {
        listFor(screen).add(0, item)
    }

    fun update(screen: AppScreen, code: String, item: RentalItem) {
        val list = listFor(screen)
        val index = list.indexOfFirst { it.code == code }
        if (index >= 0) list[index] = item
    }

    fun softDelete(screen: AppScreen, item: RentalItem) {
        update(screen, item.code, item.copy(status = deletedStatus(screen), note = "Da an/ngung theo luong xoa mem cua web cu"))
    }
}

@Composable
fun RentalManagerApp() {
    val store = remember { RentalStore() }
    var loggedIn by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("ChuTro") }
    var screen by remember { mutableStateOf(AppScreen.Dashboard) }

    if (!loggedIn) {
        LoginScreen(role = role, onRoleChange = { role = it }, onLogin = { loggedIn = true })
    } else {
        MainShell(
            store = store,
            role = role,
            screen = screen,
            onScreenChange = { screen = it },
            onLogout = {
                loggedIn = false
                screen = AppScreen.Dashboard
            }
        )
    }
}

@Composable
fun LoginScreen(role: String, onRoleChange: (String) -> Unit, onLogin: () -> Unit) {
    var username by remember { mutableStateOf(if (role == "Admin") "Admin" else if (role == "NguoiDung") "nguoithue" else "chutro") }
    var password by remember { mutableStateOf(if (role == "Admin") "Admin123" else "123456") }
    val roles = listOf("Admin", "ChuTro", "NguoiDung")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F766E), Color(0xFF0F172A))))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f))
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Quan Ly Phong Tro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Ban mobile offline bam theo API va luong web cu.", color = Color(0xFF64748B))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Ten dang nhap hoac email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mat khau") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(roles) { item ->
                        FilterChip(
                            selected = role == item,
                            onClick = {
                                onRoleChange(item)
                                username = if (item == "Admin") "Admin" else if (item == "NguoiDung") "nguoithue" else "chutro"
                                password = if (item == "Admin") "Admin123" else "123456"
                            },
                            label = { Text(item) }
                        )
                    }
                }
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("Dang nhap")
                }
                Text("Tai khoan demo nhu web: Admin/Admin123, chutro/123456, nguoithue/123456", color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun MainShell(
    store: RentalStore,
    role: String,
    screen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    val screens = screensForRole(role)
    var addFor by remember { mutableStateOf<AppScreen?>(null) }
    var selected by remember { mutableStateOf<Pair<AppScreen, RentalItem>?>(null) }
    var editing by remember { mutableStateOf<Pair<AppScreen, RentalItem>?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        AppHeader(title = screen.label, onLogout = onLogout)
        NavigationStrip(screens = screens, selected = screen, onSelected = onScreenChange)
        when (screen) {
            AppScreen.Dashboard -> DashboardScreen(store, role, onScreenChange)
            AppScreen.Account -> AccountScreen(role)
            else -> ModuleScreen(
                screen = screen,
                store = store,
                role = role,
                onAddClick = { addFor = screen },
                onOpen = { selected = screen to it },
                onEdit = { editing = screen to it },
                onDelete = { store.softDelete(screen, it) },
                onAction = { item -> store.update(screen, item.code, item.copy(status = nextStatus(screen, item.status), note = actionNote(screen, item.status))) }
            )
        }
    }

    addFor?.let { target ->
        EditItemDialog(
            title = "Them ${target.label}",
            screen = target,
            item = null,
            onDismiss = { addFor = null },
            onSave = {
                store.addTo(target, it)
                addFor = null
            }
        )
    }
    editing?.let { (target, item) ->
        EditItemDialog(
            title = "Sua ${target.label}",
            screen = target,
            item = item,
            onDismiss = { editing = null },
            onSave = {
                store.update(target, item.code, it.copy(code = item.code))
                editing = null
            }
        )
    }
    selected?.let { (target, item) ->
        DetailDialog(
            screen = target,
            item = item,
            role = role,
            onDismiss = { selected = null },
            onEdit = {
                selected = null
                editing = target to item
            }
        )
    }
}

@Composable
fun AppHeader(title: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F766E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onLogout) { Text("Thoat", color = Color.White) }
    }
}

fun screensForRole(role: String): List<AppScreen> = when (role) {
    "Admin" -> AppScreen.values().toList()
    "NguoiDung" -> listOf(
        AppScreen.Dashboard, AppScreen.Rooms, AppScreen.Contracts, AppScreen.Invoices,
        AppScreen.Payments, AppScreen.Services, AppScreen.ServiceRegs, AppScreen.Electric,
        AppScreen.Water, AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents,
        AppScreen.Notices, AppScreen.Account
    )
    else -> listOf(
        AppScreen.Dashboard, AppScreen.Houses, AppScreen.RoomTypes, AppScreen.Rooms,
        AppScreen.Tenants, AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments,
        AppScreen.Services, AppScreen.ServiceRegs, AppScreen.Electric, AppScreen.Water,
        AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents,
        AppScreen.Notices, AppScreen.Account
    )
}

@Composable
fun NavigationStrip(screens: List<AppScreen>, selected: AppScreen, onSelected: (AppScreen) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(screens) { item ->
            FilterChip(selected = selected == item, onClick = { onSelected(item) }, label = { Text(item.label) })
        }
    }
}

@Composable
fun DashboardScreen(store: RentalStore, role: String, onOpen: (AppScreen) -> Unit) {
    val unpaid = store.invoices.count { it.status.contains("Chua", true) || it.status.contains("mot phan", true) }
    val pending = store.rentRequests.count { it.status == "Cho duyet" } + store.renewRequests.count { it.status == "Cho duyet" } + store.payments.count { it.status == "Cho xac nhan" }
    val revenue = store.payments.filter { it.status == "Da xac nhan" }.fold(0L) { total, item -> total + moneyValue(item.value) }
    val tenantMode = role == "NguoiDung"
    val recentItems = listOfNotNull(
        store.rentRequests.firstOrNull { it.status == "Cho duyet" },
        store.renewRequests.firstOrNull { it.status == "Cho duyet" },
        store.payments.firstOrNull { it.status == "Cho xac nhan" },
        store.incidents.firstOrNull { it.status == "Moi" || it.status == "Dang xu ly" },
        store.notices.firstOrNull { it.status == "Moi" }
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { HeroCard(role) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Phong", store.rooms.size.toString(), "${store.rooms.count { it.status == "Con trong" }} con trong", Modifier.weight(1f))
                StatCard(if (tenantMode) "Hoa don cua toi" else "Hoa don", unpaid.toString(), "Can thanh toan/thu", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Can xu ly", pending.toString(), "Yeu cau va bien lai", Modifier.weight(1f))
                StatCard("Da thu", formatMoney(revenue), "Demo offline", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Tac vu nhanh") }
        item { QuickActions(role, onOpen) }
        item { SectionTitle(if (tenantMode) "Viec cua toi" else "Can xu ly hom nay") }
        items(recentItems) { item ->
            RentalListCard(item = item, role = role, onOpen = {}, onEdit = null, onDelete = null, onPrimaryAction = null)
        }
    }
}

@Composable
fun HeroCard(role: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Xin chao, $role", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Quan ly phong, hop dong, hoa don, thanh toan, dich vu, dien nuoc va su co theo luong web cu.", color = Color(0xFFCCFBF1))
            AssistChip(onClick = {}, label = { Text("Offline demo - san sang noi API ASP.NET") })
        }
    }
}

@Composable
fun StatCard(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFF0F766E), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun QuickActions(role: String, onOpen: (AppScreen) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onOpen(AppScreen.Rooms) }, modifier = Modifier.weight(1f)) { Text(if (role == "NguoiDung") "Tim phong" else "Phong") }
            Button(onClick = { onOpen(AppScreen.Invoices) }, modifier = Modifier.weight(1f)) { Text("Hoa don") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onOpen(if (role == "NguoiDung") AppScreen.Payments else AppScreen.Electric) }, modifier = Modifier.weight(1f)) { Text(if (role == "NguoiDung") "Gui bien lai" else "Ghi dien") }
            OutlinedButton(onClick = { onOpen(AppScreen.Incidents) }, modifier = Modifier.weight(1f)) { Text("Bao su co") }
        }
    }
}

@Composable
fun ModuleScreen(
    screen: AppScreen,
    store: RentalStore,
    role: String,
    onAddClick: () -> Unit,
    onOpen: (RentalItem) -> Unit,
    onEdit: (RentalItem) -> Unit,
    onDelete: (RentalItem) -> Unit,
    onAction: (RentalItem) -> Unit
) {
    var query by remember(screen) { mutableStateOf("") }
    var status by remember(screen) { mutableStateOf("Tat ca") }
    val allItems = store.listFor(screen)
    val statuses = listOf("Tat ca") + allItems.map { it.status }.distinct()
    val canAdd = canAdd(role, screen)
    val canManage = role != "NguoiDung" || screen in listOf(AppScreen.Payments, AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents, AppScreen.ServiceRegs)
    val filtered = allItems.filter {
        val textMatch = it.title.contains(query, true) || it.code.contains(query, true) || it.note.contains(query, true) || it.value.contains(query, true)
        val statusMatch = status == "Tat ca" || it.status == status
        textMatch && statusMatch
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ModuleHeader(screen, allItems.size, canAdd, onAddClick) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Tim theo ma, ten, ghi chu hoac so tien") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statuses) { item ->
                    FilterChip(selected = status == item, onClick = { status = item }, label = { Text(item) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyState(screen) }
        } else {
            items(filtered, key = { it.code }) { item ->
                RentalListCard(
                    item = item,
                    role = role,
                    onOpen = { onOpen(item) },
                    onEdit = if (canManage) ({ onEdit(item) }) else null,
                    onDelete = if (canManage && role != "NguoiDung") ({ onDelete(item) }) else null,
                    onPrimaryAction = primaryActionAllowed(role, screen, item).takeIf { it }?.let { { onAction(item) } }
                )
            }
        }
    }
}

@Composable
fun ModuleHeader(screen: AppScreen, count: Int, canAdd: Boolean, onAddClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(screen.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$count ban ghi", color = Color(0xFF64748B))
        }
        if (canAdd) {
            Button(onClick = onAddClick, shape = RoundedCornerShape(8.dp)) { Text("Them") }
        }
    }
}

@Composable
fun RentalListCard(
    item: RentalItem,
    role: String,
    onOpen: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onPrimaryAction: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(statusColor(item.status).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) { Text(item.code.takeLast(2), color = statusColor(item.status), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.code, color = Color(0xFF64748B))
                }
                StatusPill(item.status)
            }
            Text(item.value, fontWeight = FontWeight.Medium)
            Text(item.note, color = Color(0xFF64748B), maxLines = 2, overflow = TextOverflow.Ellipsis)
            ActionButtons(
                item = item,
                role = role,
                onOpen = onOpen,
                onEdit = onEdit,
                onDelete = onDelete,
                onPrimaryAction = onPrimaryAction
            )
        }
    }
}

@Composable
fun ActionButtons(
    item: RentalItem,
    role: String,
    onOpen: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onPrimaryAction: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            onPrimaryAction?.let {
                Button(
                    onClick = it,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(actionLabel(item.status), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text(if (role == "NguoiDung") "Xem" else "Chi tiet", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onEdit != null || onDelete != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                onEdit?.let {
                    OutlinedButton(onClick = it, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("Sua")
                    }
                }
                onDelete?.let {
                    OutlinedButton(onClick = it, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("An")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    Surface(color = statusColor(status).copy(alpha = 0.12f), shape = RoundedCornerShape(999.dp)) {
        Text(status, color = statusColor(status), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun DetailDialog(screen: AppScreen, item: RentalItem, role: String, onDismiss: () -> Unit, onEdit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DetailRow("Module", screen.label) }
                item { DetailRow("Ma", item.code) }
                item { DetailRow("Trang thai", item.status) }
                item { DetailRow("Gia tri", item.value) }
                item { DetailRow("Ghi chu", item.note) }
                items(item.details) { detail -> DetailRow(detail.first, detail.second) }
                item {
                    Text(
                        moduleHint(screen, role),
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Dong") } },
        dismissButton = {
            if (role != "NguoiDung" || screen in listOf(AppScreen.Payments, AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents, AppScreen.ServiceRegs)) {
                TextButton(onClick = onEdit) { Text("Sua") }
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EditItemDialog(title: String, screen: AppScreen, item: RentalItem?, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var itemTitle by remember { mutableStateOf(item?.title.orEmpty()) }
    var status by remember { mutableStateOf(item?.status ?: defaultStatus(screen)) }
    var value by remember { mutableStateOf(item?.value.orEmpty()) }
    var note by remember { mutableStateOf(item?.note.orEmpty()) }
    val code = item?.code ?: "${screen.name.take(3).uppercase()}${System.currentTimeMillis().toString().takeLast(4)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = itemTitle, onValueChange = { itemTitle = it }, label = { Text(primaryFieldLabel(screen)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Trang thai") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(valueFieldLabel(screen)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chu") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    RentalItem(
                        code = code,
                        title = itemTitle.ifBlank { "Ban ghi ${screen.label}" },
                        status = status.ifBlank { defaultStatus(screen) },
                        value = value.ifBlank { defaultValue(screen) },
                        note = note.ifBlank { "Tao tu app mobile offline" },
                        details = item?.details ?: defaultDetails(screen)
                    )
                )
            }) { Text("Luu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huy") } }
    )
}

@Composable
fun AccountScreen(role: String) {
    val account = when (role) {
        "Admin" -> listOf("Ho ten" to "Admin he thong", "Email" to "admin@demo.local", "Vai tro" to "Admin")
        "NguoiDung" -> listOf("Ho ten" to "Nguoi Thue Demo", "Email" to "nguoithue@example.com", "Phong dang thue" to "P101", "CCCD" to "079200000001")
        else -> listOf("Ho ten" to "Nguyen Minh Quan", "Email" to "chutro@example.com", "Ngan hang" to "VCB 1020304001", "Noi dung CK" to "Thanh toan hoa don {MaHoaDon} phong {TenPhong}")
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tai khoan cua toi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    account.forEach { DetailRow(it.first, it.second) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {}) { Text("Cap nhat") }
                        OutlinedButton(onClick = {}) { Text("Doi mat khau") }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)), modifier = Modifier.fillMaxWidth()) {
                Text("Cac man hinh dang dung du lieu mau trong bo nho. Khi co base URL, co the thay RentalStore bang repository goi API web cu.", modifier = Modifier.padding(16.dp), color = Color(0xFF075985))
            }
        }
    }
}

@Composable
fun EmptyState(screen: AppScreen) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Chua co du lieu ${screen.label.lowercase()}", fontWeight = FontWeight.Bold)
            Text("Thu doi bo loc hoac them ban ghi moi.", color = Color(0xFF64748B))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
}

fun statusColor(status: String): Color = when {
    status.contains("Da", true) || status.contains("Dang hieu luc", true) || status.contains("Con trong", true) || status.contains("Dang su dung", true) -> Color(0xFF059669)
    status.contains("Cho", true) || status.contains("Chua", true) || status.contains("Mot phan", true) || status.contains("Sap", true) || status.contains("Can", true) -> Color(0xFFD97706)
    status.contains("Tu choi", true) || status.contains("Huy", true) || status.contains("Ngung", true) || status.contains("Tam dung", true) -> Color(0xFFDC2626)
    status.contains("Dang", true) || status.contains("Moi", true) -> Color(0xFF2563EB)
    else -> Color(0xFF475569)
}

fun canAdd(role: String, screen: AppScreen): Boolean = when (role) {
    "NguoiDung" -> screen in listOf(AppScreen.Payments, AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents, AppScreen.ServiceRegs)
    else -> screen !in listOf(AppScreen.Users)
}

fun primaryActionAllowed(role: String, screen: AppScreen, item: RentalItem): Boolean = when {
    role == "NguoiDung" && screen == AppScreen.Rooms && item.status == "Con trong" -> true
    role == "NguoiDung" && screen == AppScreen.Invoices && item.status != "Da thanh toan" -> true
    role == "NguoiDung" && screen in listOf(AppScreen.Notices, AppScreen.ServiceRegs) -> true
    role != "NguoiDung" && screen in listOf(AppScreen.Payments, AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents, AppScreen.Contracts, AppScreen.Invoices, AppScreen.Electric, AppScreen.Water, AppScreen.Notices) -> true
    else -> false
}

fun nextStatus(screen: AppScreen, status: String): String = when (screen) {
    AppScreen.Rooms -> if (status == "Con trong") "Da thue" else status
    AppScreen.Contracts -> when (status) {
        "Cho nguoi thue xac nhan" -> "Dang hieu luc"
        "Dang hieu luc", "Sap het han" -> "Ket thuc"
        else -> "Dang hieu luc"
    }
    AppScreen.Invoices -> when (status) {
        "Chua thanh toan" -> "Thanh toan mot phan"
        "Thanh toan mot phan" -> "Da thanh toan"
        else -> status
    }
    AppScreen.Payments -> if (status == "Cho xac nhan") "Da xac nhan" else status
    AppScreen.RentRequests -> if (status == "Cho duyet") "Cho nguoi thue xac nhan" else status
    AppScreen.RenewRequests -> if (status == "Cho duyet") "Da chap nhan" else status
    AppScreen.Incidents -> when (status) {
        "Moi" -> "Dang xu ly"
        "Dang xu ly" -> "Da xu ly"
        else -> status
    }
    AppScreen.Electric, AppScreen.Water -> "Da ghi"
    AppScreen.Notices -> "Da doc"
    AppScreen.ServiceRegs -> if (status == "Dang su dung") "Da huy" else "Dang su dung"
    else -> "Da cap nhat"
}

fun actionLabel(status: String): String = when (status) {
    "Cho xac nhan" -> "Xac nhan"
    "Cho duyet" -> "Duyet"
    "Cho nguoi thue xac nhan" -> "Xac nhan HD"
    "Moi" -> "Tiep nhan"
    "Dang xu ly" -> "Hoan tat"
    "Chua thanh toan" -> "Ghi nhan"
    "Thanh toan mot phan" -> "Thu du"
    "Con trong" -> "Gui yeu cau"
    "Can ghi" -> "Ghi chi so"
    "Dang su dung" -> "Huy DV"
    else -> "Cap nhat"
}

fun actionNote(screen: AppScreen, oldStatus: String): String = when (screen) {
    AppScreen.RentRequests -> if (oldStatus == "Cho duyet") "Da duyet va tao hop dong cho nguoi thue xac nhan" else "Da cap nhat yeu cau"
    AppScreen.Payments -> "Da xac nhan bien lai, hoa don se duoc cap nhat trang thai"
    AppScreen.Invoices -> "Trang thai hoa don duoc tinh theo tong thanh toan da xac nhan"
    AppScreen.Electric, AppScreen.Water -> "Da ghi chi so moi va san sang tinh hoa don"
    AppScreen.Notices -> "Da danh dau la da doc"
    else -> "Da cap nhat theo luong xu ly cua web cu"
}

fun defaultStatus(screen: AppScreen): String = when (screen) {
    AppScreen.Rooms -> "Con trong"
    AppScreen.Contracts -> "Dang hieu luc"
    AppScreen.Invoices -> "Chua thanh toan"
    AppScreen.Payments -> "Cho xac nhan"
    AppScreen.RentRequests, AppScreen.RenewRequests -> "Cho duyet"
    AppScreen.Incidents, AppScreen.Notices -> "Moi"
    AppScreen.Electric, AppScreen.Water -> "Da ghi"
    AppScreen.ServiceRegs -> "Dang su dung"
    AppScreen.Services -> "Tinh phi"
    else -> "Dang hoat dong"
}

fun deletedStatus(screen: AppScreen): String = when (screen) {
    AppScreen.Rooms, AppScreen.Houses, AppScreen.RoomTypes, AppScreen.Services -> "Ngung hoat dong"
    AppScreen.Invoices -> "Huy"
    AppScreen.Notices -> "An"
    AppScreen.ServiceRegs -> "Da huy"
    else -> "Da an"
}

fun primaryFieldLabel(screen: AppScreen): String = when (screen) {
    AppScreen.Incidents -> "Tieu de su co"
    AppScreen.Notices -> "Tieu de thong bao"
    AppScreen.RentRequests -> "Nguoi thue va phong"
    AppScreen.RenewRequests -> "Hop dong can gia han"
    AppScreen.Electric, AppScreen.Water -> "Phong va ky ghi chi so"
    else -> "Ten / noi dung"
}

fun valueFieldLabel(screen: AppScreen): String = when (screen) {
    AppScreen.Invoices, AppScreen.Payments -> "So tien"
    AppScreen.Contracts, AppScreen.RentRequests, AppScreen.RenewRequests -> "Ky han"
    AppScreen.Electric -> "Chi so dien"
    AppScreen.Water -> "Chi so nuoc"
    else -> "Gia tri"
}

fun defaultValue(screen: AppScreen): String = when (screen) {
    AppScreen.Invoices, AppScreen.Payments -> "0d"
    AppScreen.Contracts -> "01/06/2026 - 01/12/2026"
    AppScreen.Electric -> "0 kWh x 3.500d"
    AppScreen.Water -> "0 m3 x 15.000d"
    else -> "Dang cap nhat"
}

fun defaultDetails(screen: AppScreen): List<Pair<String, String>> = when (screen) {
    AppScreen.Invoices -> listOf("Loai" to "HangThang", "Ky hoa don" to "2026-06")
    AppScreen.Payments -> listOf("Hinh thuc" to "Chuyen khoan", "Trang thai xac nhan" to "ChoXacNhan")
    AppScreen.Electric, AppScreen.Water -> listOf("Chi so cu" to "0", "Chi so moi" to "0")
    else -> emptyList()
}

fun moduleHint(screen: AppScreen, role: String): String = when (screen) {
    AppScreen.Invoices -> "Web cu co tao hoa don theo phong/ky, tinh tien dien nuoc/dich vu, xuat PDF/CSV va tao QR thanh toan."
    AppScreen.Payments -> if (role == "NguoiDung") "Nguoi thue gui bien lai; chu tro/admin xac nhan hoac tu choi." else "Chu tro/admin xem bien lai cho xac nhan, chap nhan de cap nhat hoa don."
    AppScreen.RentRequests -> "Duyet yeu cau thue se tao hop dong cho nguoi thue xac nhan."
    AppScreen.RenewRequests -> "Chu tro co the chap nhan hoac tu choi gia han hop dong."
    AppScreen.Electric, AppScreen.Water -> "Chi so dung de tinh hoa don hang thang theo ky yyyy-MM."
    AppScreen.Services -> "Dich vu co loai TinhPhi/TienIch/TienNghi va lich su cap nhat gia."
    else -> "Man hinh nay mo phong CRUD, loc, chi tiet va xoa mem theo quyen trong web cu."
}

fun moneyValue(value: String): Long {
    val digits = value.filter { it.isDigit() }
    return digits.toLongOrNull() ?: 0L
}

fun formatMoney(value: Long): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}.${(value % 1_000_000) / 10_000}M"
    value >= 1_000 -> "${value / 1_000}K"
    else -> "${value}d"
}
