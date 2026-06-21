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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.util.moneyValue

@Composable
internal fun SpecializedEditDialog(
    screen: AppScreen,
    item: RentalItem,
    houses: List<RentalItem>,
    rooms: List<RentalItem>,
    invoices: List<RentalItem>,
    onDismiss: () -> Unit,
    onSaveUtility: (AppScreen, String, String, Double, Double, Double) -> Unit,
    onSaveInvoice: (String, String, Double, String) -> Unit,
    onSavePayment: (String, String, String, String) -> Unit,
    onSave: (RentalItem) -> Unit,
    repository: RentalRepository
) {
    when (screen) {
        AppScreen.Houses -> HouseFormDialog(item, onDismiss, onSave)
        AppScreen.RoomTypes -> RoomTypeFormDialog(item, onDismiss, onSave)
        AppScreen.Rooms -> RoomFormDialog(item, houses, onDismiss, onSave)
        AppScreen.Services -> ServiceFormDialog(item, onDismiss, onSave)
        AppScreen.Electric, AppScreen.Water -> UtilityReadingFormDialog(screen, item, rooms, onDismiss, onSaveUtility, repository)
        AppScreen.Invoices -> InvoiceFormDialog(item, rooms, onDismiss, onSaveInvoice)
        AppScreen.Payments -> SubmitPaymentFormDialog(item, invoices, onDismiss, onSavePayment)
        AppScreen.Incidents -> IncidentFormDialog(item, onDismiss, onSave)
        else -> EditItemDialog(screen, item, onDismiss, onSave)
    }
}

@Composable
internal fun ChangePasswordDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF34D399),
        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
        cursorColor = Color(0xFF34D399),
        focusedLabelColor = Color(0xFF34D399),
        unfocusedLabelColor = Color.White.copy(alpha = 0.55f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi mật khẩu", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword, onValueChange = { oldPassword = it },
                    label = { Text("Mật khẩu cũ") }, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = CutCornerShape(8.dp), colors = fieldColors
                )
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("Mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = CutCornerShape(8.dp), colors = fieldColors
                )
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Nhập lại mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = CutCornerShape(8.dp), colors = fieldColors
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable(enabled = !saving) { onSubmit(oldPassword, newPassword, confirmPassword) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(if (saving) "Đang lưu..." else "Đổi mật khẩu", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) }
        },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun ContractEditorDialog(
    item: RentalItem,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var roomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var tenantUsername by remember(item) { mutableStateOf(item.detail("tenantUsername")) }
    var startDate by remember(item) { mutableStateOf(item.detail("startDate")) }
    var endDate by remember(item) { mutableStateOf(item.detail("endDate")) }
    var deposit by remember(item) { mutableStateOf(item.detail("deposit")) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Chờ người thuê xác nhận" }) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Tạo hợp đồng" else "Sửa hợp đồng",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = roomId, onValueChange = { roomId = it },
                        label = { Text("Mã phòng *") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = tenantUsername, onValueChange = { tenantUsername = it },
                        label = { Text("Tên đăng nhập người thuê *") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = startDate, onValueChange = { startDate = it },
                            label = { Text("Bắt đầu *") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = endDate, onValueChange = { endDate = it },
                            label = { Text("Kết thúc *") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = deposit, onValueChange = { deposit = it },
                        label = { Text("Tiền đặt cọc") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = status, onValueChange = { status = it },
                        label = { Text("Trạng thái") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text("Ghi chú / điều khoản") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                localError?.let {
                    item { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomId.isBlank() || tenantUsername.isBlank() || startDate.isBlank() || endDate.isBlank()) {
                        localError = "Vui lòng nhập đủ mã phòng, người thuê, ngày bắt đầu và ngày kết thúc."
                    } else {
                        onSave(roomId.trim(), tenantUsername.trim(), startDate.trim(), endDate.trim(), deposit.trim(), note.trim(), status.trim())
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Lưu hợp đồng", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun RenewRequestDialog(contract: RentalItem, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var newEndDate by remember(contract) { mutableStateOf(contract.detail("endDate")) }
    var note by remember(contract) { mutableStateOf("Mình muốn gia hạn hợp đồng này.") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Gửi yêu cầu gia hạn", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Hợp đồng", contract.title)
                DetailRow("Ngày kết thúc hiện tại", contract.detail("endDate").ifBlank { contract.value })
                OutlinedTextField(
                    value = newEndDate, onValueChange = { newEndDate = it },
                    label = { Text("Ngày kết thúc mới") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
                localError?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newEndDate.isBlank()) localError = "Vui lòng nhập ngày kết thúc mới." else onSubmit(newEndDate.trim(), note.trim())
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
            ) {
                Text("Gửi gia hạn", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun RentRequestDialog(room: RentalItem, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var duration by remember(room) { mutableStateOf("6 tháng") }
    var note by remember(room) { mutableStateOf("Mình muốn thuê phòng này.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Gửi yêu cầu thuê phòng", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Phòng cần thuê", room.title)
                DetailRow("Giá thuê", room.value)
                OutlinedTextField(
                    value = duration, onValueChange = { duration = it },
                    label = { Text("Thời hạn mong muốn") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú thêm") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(duration, note) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text("Gửi yêu cầu", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun HouseFormDialog(item: RentalItem, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item.title) }
    var address by remember(item) { mutableStateOf(item.note) }
    var roomCount by remember(item) { mutableStateOf(item.value.ifBlank { "0 phòng" }) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Đang hoạt động" }) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Thêm nhà trọ" else "Sửa nhà trọ",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên nhà trọ *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("Địa chỉ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = roomCount, onValueChange = { roomCount = it },
                    label = { Text("Số phòng (VD: 20 phòng)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("Trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Đang hoạt động", "Tạm dừng")) { s ->
                        FilterChip(
                            selected = status == s, onClick = { status = s }, label = { Text(s) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                error?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) error = "Tên nhà trọ không được để trống."
                    else onSave(item.copy(title = name.trim(), status = status, value = roomCount.trim(), note = address.trim()))
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
            ) { Text("Lưu nhà trọ", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun RoomTypeFormDialog(item: RentalItem, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item.title) }
    var priceRange by remember(item) { mutableStateOf(item.value) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Đang dùng" }) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Thêm loại phòng" else "Sửa loại phòng",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên loại phòng *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = priceRange, onValueChange = { priceRange = it },
                    label = { Text("Khoảng giá (VD: 2.000.000đ - 3.500.000đ)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("Trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Đang dùng", "Ngừng dùng")) { s ->
                        FilterChip(
                            selected = status == s, onClick = { status = s }, label = { Text(s) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
                error?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) error = "Tên loại phòng không được để trống."
                    else onSave(item.copy(title = name.trim(), status = status, value = priceRange.trim(), note = note.trim()))
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
            ) { Text("Lưu loại phòng", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun RoomFormDialog(
    item: RentalItem,
    houses: List<RentalItem>,
    onDismiss: () -> Unit,
    onSave: (RentalItem) -> Unit
) {
    var name by remember(item) { mutableStateOf(item.title) }
    var houseId by remember(item) { mutableStateOf(item.detail("houseId")) }
    var price by remember(item) { mutableStateOf(item.value) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Còn trống" }) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Thêm phòng" else "Sửa phòng",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Tên phòng * (VD: Phòng A01)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    if (houses.isNotEmpty()) {
                        Text("Nhà trọ *", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(houses) { house ->
                                FilterChip(
                                    selected = houseId == house.id,
                                    onClick = { houseId = house.id },
                                    label = { Text("${house.title} (${house.id})") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = houseId, onValueChange = { houseId = it },
                            label = { Text("Mã nhà trọ * (VD: NT001)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = price, onValueChange = { price = it },
                        label = { Text("Giá thuê * (VD: 3.200.000đ/tháng)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    Text("Trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Còn trống", "Đã thuê", "Đang giữ chỗ", "Đang sửa chữa")) { s ->
                            FilterChip(
                                selected = status == s, onClick = { status = s }, label = { Text(s) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text("Ghi chú (Tầng, tiện ích, ...)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                error?.let { err ->
                    item { Text(err, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        name.isBlank() -> error = "Tên phòng không được để trống."
                        houseId.isBlank() -> error = "Vui lòng chọn nhà trọ."
                        price.isBlank() -> error = "Giá thuê không được để trống."
                        else -> {
                            val detailsWithHouse = item.details
                                .filterNot { it.first == "houseId" }
                                .toMutableList().also { it.add(0, "houseId" to houseId) }
                            onSave(item.copy(
                                title = name.trim(),
                                status = status,
                                value = price.trim(),
                                note = note.trim(),
                                details = detailsWithHouse
                            ))
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2))
            ) { Text("Lưu phòng", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun ServiceFormDialog(item: RentalItem, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item.title) }
    var unitPrice by remember(item) { mutableStateOf(item.value) }
    var unit by remember(item) { mutableStateOf(item.detail("unit").ifBlank { "tháng" }) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Tính phí" }) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Thêm dịch vụ" else "Sửa dịch vụ",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên dịch vụ * (VD: Internet, Rác...)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = unitPrice, onValueChange = { unitPrice = it },
                    label = { Text("Đơn giá *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("Đơn vị tính", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("tháng", "phòng", "người", "kWh", "m3")) { u ->
                        FilterChip(
                            selected = unit == u, onClick = { unit = u }, label = { Text(u) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                Text("Trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Tính phí", "Miễn phí", "Tạm dừng")) { s ->
                        FilterChip(
                            selected = status == s, onClick = { status = s }, label = { Text(s) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
                error?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        name.isBlank() -> error = "Tên dịch vụ không được để trống."
                        unitPrice.isBlank() -> error = "Đơn giá không được để trống."
                        else -> {
                            val newDetails = item.details.filterNot { it.first == "unit" } + ("unit" to unit)
                            onSave(item.copy(
                                title = name.trim(), status = status,
                                value = unitPrice.trim(), note = note.trim(),
                                details = newDetails
                            ))
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6))
            ) { Text("Lưu dịch vụ", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun UtilityReadingFormDialog(
    screen: AppScreen,
    item: RentalItem,
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSaveUtility: (AppScreen, String, String, Double, Double, Double) -> Unit,
    repository: RentalRepository
) {
    var roomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var period by remember(item) { mutableStateOf(item.detail("period").ifBlank { "2026-06" }) }
    var oldIndex by remember(item) { mutableStateOf(item.detail("oldIndex").toDoubleOrNull() ?: 0.0) }
    var newIndexStr by remember(item) { mutableStateOf(item.detail("newIndex").ifBlank { "" }) }
    val defaultPrice = if (screen == AppScreen.Electric) 3500.0 else 15000.0
    var priceStr by remember(item) { mutableStateOf(item.detail("price").ifBlank { defaultPrice.toLong().toString() }) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadingOldIndex by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) {
        if (roomId.isNotBlank() && item.id.isBlank()) {
            loadingOldIndex = true
            repository.getLatestUtilityIndex(screen, roomId)
                .onSuccess { oldIndex = it }
                .onFailure { /* fallback */ }
            loadingOldIndex = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Ghi chỉ số ${screen.label.lowercase()}" else "Sửa chỉ số ${screen.label.lowercase()}",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (rooms.isNotEmpty()) {
                        Text("Chọn phòng *", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rooms) { room ->
                                FilterChip(
                                    selected = roomId == room.id,
                                    onClick = { roomId = room.id },
                                    label = { Text(room.title) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = roomId, onValueChange = { roomId = it },
                            label = { Text("Mã phòng * (VD: P101)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = period, onValueChange = { period = it },
                        label = { Text("Kỳ ghi chỉ số * (VD: 2026-06)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (loadingOldIndex) "Đang tải..." else oldIndex.toString(),
                            onValueChange = {},
                            label = { Text("Chỉ số cũ") },
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newIndexStr,
                            onValueChange = { newIndexStr = it },
                            label = { Text("Chỉ số mới *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = priceStr, onValueChange = { priceStr = it },
                        label = { Text("Đơn giá (${if (screen == AppScreen.Electric) "đ/kWh" else "đ/m3"})") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                error?.let { err ->
                    item { Text(err, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newIndex = newIndexStr.toDoubleOrNull()
                    val price = priceStr.toDoubleOrNull()
                    when {
                        roomId.isBlank() -> error = "Vui lòng chọn phòng."
                        period.isBlank() -> error = "Vui lòng nhập kỳ ghi."
                        newIndex == null -> error = "Vui lòng nhập chỉ số mới hợp lệ."
                        newIndex < oldIndex -> error = "Chỉ số mới không được nhỏ hơn chỉ số cũ."
                        price == null || price <= 0 -> error = "Vui lòng nhập đơn giá hợp lệ."
                        else -> {
                            onSaveUtility(screen, roomId, period.trim(), oldIndex, newIndex, price)
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = screenAccent(screen))
            ) { Text("Lưu chỉ số", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun InvoiceFormDialog(
    item: RentalItem,
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSaveInvoice: (String, String, Double, String) -> Unit
) {
    var roomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var period by remember(item) { mutableStateOf(item.detail("period").ifBlank { "2026-06" }) }
    var otherCostStr by remember(item) { mutableStateOf(item.detail("otherCost").ifBlank { "0" }) }
    var otherNote by remember(item) { mutableStateOf(item.detail("otherNote")) }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedRoom = rooms.firstOrNull { it.id == roomId }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Tạo hóa đơn" else "Sửa hóa đơn",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (rooms.isNotEmpty()) {
                        val rentedRooms = rooms.filter { it.status.equals("Đã thuê", true) || it.detail("tenantUsername").isNotBlank() }
                        Text("Chọn phòng thuê *", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rentedRooms) { room ->
                                FilterChip(
                                    selected = roomId == room.id,
                                    onClick = { roomId = room.id },
                                    label = { Text("${room.title} (${room.id})") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                        if (rentedRooms.isEmpty()) {
                            Text("Không có phòng nào đang được thuê.", color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        OutlinedTextField(
                            value = roomId, onValueChange = { roomId = it },
                            label = { Text("Mã phòng * (VD: P101)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = period, onValueChange = { period = it },
                        label = { Text("Kỳ hóa đơn * (VD: 2026-06)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = otherCostStr, onValueChange = { otherCostStr = it },
                        label = { Text("Chi phí phát sinh (nếu có)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = otherNote, onValueChange = { otherNote = it },
                        label = { Text("Lý do phát sinh / Ghi chú") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                
                selectedRoom?.let { room ->
                    val rPrice = moneyValue(room.value)
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Thông tin tạm tính:", fontWeight = FontWeight.Bold, color = Color(0xFF166534), style = MaterialTheme.typography.bodyMedium)
                                Text("• Tiền phòng: ${com.example.myapplication.domain.util.formatMoney(rPrice)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                                Text("• Tiền điện/nước/dịch vụ: Sẽ tự động đối chiếu theo kỳ $period", style = MaterialTheme.typography.bodySmall, color = Color(0xFF166534))
                                val extra = otherCostStr.toDoubleOrNull() ?: 0.0
                                if (extra > 0) {
                                    Text("• Phát sinh: ${com.example.myapplication.domain.util.formatMoney(extra.toLong())}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                                }
                            }
                        }
                    }
                }
                
                error?.let { err ->
                    item { Text(err, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val otherCost = otherCostStr.toDoubleOrNull() ?: 0.0
                    when {
                        roomId.isBlank() -> error = "Vui lòng chọn phòng."
                        period.isBlank() -> error = "Vui lòng nhập kỳ hóa đơn."
                        otherCostStr.isNotBlank() && otherCostStr.toDoubleOrNull() == null -> error = "Chi phí phát sinh phải là số hợp lệ."
                        else -> {
                            onSaveInvoice(roomId, period.trim(), otherCost, otherNote.trim())
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) { Text("Tạo hóa đơn", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun SubmitPaymentFormDialog(
    item: RentalItem,
    invoices: List<RentalItem>,
    onDismiss: () -> Unit,
    onSavePayment: (String, String, String, String) -> Unit
) {
    var invoiceId by remember(item) { mutableStateOf(item.detail("invoiceId")) }
    var transactionId by remember(item) { mutableStateOf(item.detail("transactionId")) }
    var note by remember(item) { mutableStateOf(item.note) }
    var receiptImage by remember(item) { mutableStateOf(item.detail("receiptImage")) }
    var error by remember { mutableStateOf<String?>(null) }

    val unpaidInvoices = invoices.filter { it.status != "Đã thanh toán" }
    val selectedInvoice = invoices.firstOrNull { it.id == invoiceId }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Gửi biên lai thanh toán",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (unpaidInvoices.isNotEmpty()) {
                        Text("Chọn hóa đơn thanh toán *", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(unpaidInvoices) { inv ->
                                FilterChip(
                                    selected = invoiceId == inv.id,
                                    onClick = { invoiceId = inv.id },
                                    label = { Text("${inv.title} (${inv.value})") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = invoiceId, onValueChange = { invoiceId = it },
                            label = { Text("Mã hóa đơn * (VD: HD001)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                
                selectedInvoice?.let { inv ->
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                            border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Thông tin thanh toán:", fontWeight = FontWeight.Bold, color = Color(0xFF9A3412), style = MaterialTheme.typography.bodyMedium)
                                Text("• Số tiền cần nộp: ${inv.value}", fontWeight = FontWeight.Bold, color = Color(0xFFC2410C), style = MaterialTheme.typography.bodyMedium)
                                Text("• Chi tiết: ${inv.note}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = transactionId, onValueChange = { transactionId = it },
                        label = { Text("Mã giao dịch ngân hàng / Ref *") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text("Nội dung chuyển khoản") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    Text("Ảnh biên lai (Mô phỏng đính kèm)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { receiptImage = "data:image/png;base64,iVBORw0KGgoAAA..." },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B))
                        ) {
                            Text(if (receiptImage.isBlank()) "Đính kèm ảnh" else "Đã đính kèm ảnh", fontWeight = FontWeight.Bold)
                        }
                        if (receiptImage.isNotBlank()) {
                            Text("✓ Đã chọn file ảnh", color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                
                error?.let { err ->
                    item { Text(err, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        invoiceId.isBlank() -> error = "Vui lòng chọn hóa đơn."
                        transactionId.isBlank() -> error = "Vui lòng nhập mã giao dịch ngân hàng."
                        else -> {
                            onSavePayment(invoiceId, transactionId.trim(), receiptImage, note.trim())
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
            ) { Text("Gửi biên lai", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun RejectPaymentDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Từ chối thanh toán", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vui lòng nhập lý do từ chối thanh toán để gửi đến người thuê:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("Lý do từ chối *") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                )
                error?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        error = "Lý do không được để trống."
                    } else {
                        onSubmit(reason.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Gửi từ chối", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
internal fun EditItemDialog(screen: AppScreen, item: RentalItem, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var title by remember(item) { mutableStateOf(item.title) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { defaultStatus(screen) }) }
    var value by remember(item) { mutableStateOf(item.value) }
    var note by remember(item) { mutableStateOf(item.note) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item.id.isBlank()) "Thêm ${screen.label.lowercase()}" else "Sửa ${screen.label.lowercase()}",
                fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Tên / nội dung") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = status, onValueChange = { status = it },
                    label = { Text("Trạng thái") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = value, onValueChange = { value = it },
                    label = { Text("Giá trị / số tiền / thời hạn") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
                localError?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        localError = "Vui lòng nhập tên hoặc nội dung."
                    } else {
                        onSave(
                            item.copy(
                                title = title.trim(),
                                status = status.trim().ifBlank { defaultStatus(screen) },
                                value = value.trim(),
                                note = note.trim()
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = screenAccent(screen))
            ) {
                Text("Lưu", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
internal fun DetailDialog(item: RentalItem, screen: AppScreen, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text(item.title, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DetailRow("Module", screen.label) }
                item { DetailRow("Mã", item.id) }
                item { DetailRow("Trạng thái", item.status) }
                item { DetailRow("Giá trị", item.value) }
                item { DetailRow("Ghi chú", item.note) }
                items(item.details) { DetailRow(it.first, it.second) }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Đóng", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
internal fun IncidentFormDialog(
    item: RentalItem,
    onDismiss: () -> Unit,
    onSave: (RentalItem) -> Unit
) {
    var title by remember(item) { mutableStateOf(item.title) }
    var description by remember(item) { mutableStateOf(item.note) }
    var urgency by remember(item) { mutableStateOf(item.value.ifBlank { "Bình thường" }) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                if (item.id.isBlank()) "Báo sự cố mới" else "Sửa sự cố",
                fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Tiêu đề sự cố *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("Mức độ khẩn cấp", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Bình thường", "Gấp", "Rất gấp")) { u ->
                        FilterChip(
                            selected = urgency == u,
                            onClick = { urgency = u },
                            label = { Text(u) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Mô tả chi tiết sự cố *") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
                error?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        title.isBlank() -> error = "Vui lòng nhập tiêu đề sự cố."
                        description.isBlank() -> error = "Vui lòng mô tả chi tiết sự cố."
                        else -> onSave(
                            item.copy(
                                title = title.trim(),
                                status = "Mới",
                                value = urgency,
                                note = description.trim()
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = screenAccent(AppScreen.Incidents))
            ) { Text("Gửi báo cáo", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun RespondIncidentDialog(
    incident: RentalItem,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var response by remember(incident) { mutableStateOf("") }
    var newStatus by remember(incident) { mutableStateOf("Đang xử lý") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Phản hồi sự cố", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(incident.title, fontWeight = FontWeight.Bold, color = Color(0xFF7F1D1D), style = MaterialTheme.typography.bodyMedium)
                        Text("Mức độ: ${incident.value}", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(incident.note, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                }
                Text("Cập nhật trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Đang xử lý", "Đã khắc phục", "Không thể xử lý")) { s ->
                        FilterChip(
                            selected = newStatus == s, onClick = { newStatus = s }, label = { Text(s) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = response, onValueChange = { response = it },
                    label = { Text("Nội dung phản hồi *") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
                error?.let { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (response.isBlank()) error = "Vui lòng nhập nội dung phản hồi."
                    else onSubmit(response.trim(), newStatus)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) { Text("Gửi phản hồi", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
internal fun NoticeFormDialog(
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("all") }
    var selectedRoomId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Tạo thông báo mới", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A5F))
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Tiêu đề thông báo *") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = content, onValueChange = { content = it },
                        label = { Text("Nội dung thông báo *") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    Text("Gửi đến", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("all" to "Tất cả mọi người", "room" to "Theo phòng")) { (k, v) ->
                            FilterChip(
                                selected = targetType == k || (targetType == "room" && k == "room"),
                                onClick = { targetType = k; selectedRoomId = "" },
                                label = { Text(v) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
                if (targetType == "room" && rooms.isNotEmpty()) {
                    item {
                        Text("Chọn phòng nhận *", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rooms) { room ->
                                FilterChip(
                                    selected = selectedRoomId == room.id,
                                    onClick = { selectedRoomId = room.id },
                                    label = { Text(room.title) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
                error?.let { item { Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall) } }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTarget = when {
                        targetType == "room" && selectedRoomId.isNotBlank() -> "room:$selectedRoomId"
                        targetType == "room" && selectedRoomId.isBlank() -> { error = "Vui lòng chọn phòng cần gửi."; return@Button }
                        else -> "all"
                    }
                    when {
                        title.isBlank() -> error = "Vui lòng nhập tiêu đề."
                        content.isBlank() -> error = "Vui lòng nhập nội dung."
                        else -> onSave(title.trim(), content.trim(), finalTarget)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = screenAccent(AppScreen.Notices))
            ) { Text("Gửi thông báo", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
