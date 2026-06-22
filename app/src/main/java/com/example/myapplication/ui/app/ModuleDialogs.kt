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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.util.moneyValue

class CurrencyVisualTransformation(val suffix: String = "") : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val original = text.text.filter { it.isDigit() }
        if (original.isEmpty()) return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        
        val formatted = try {
            val number = original.toLong()
            java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(number) + suffix
        } catch (e: Exception) {
            original + suffix
        }
        
        return androidx.compose.ui.text.input.TransformedText(
            androidx.compose.ui.text.AnnotatedString(formatted),
            object : androidx.compose.ui.text.input.OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    var transformedOffset = 0
                    var originalCharsCount = 0
                    for (i in formatted.indices) {
                        if (originalCharsCount == offset) break
                        if (formatted[i].isDigit()) originalCharsCount++
                        transformedOffset++
                    }
                    return transformedOffset
                }
                override fun transformedToOriginal(offset: Int): Int {
                    var originalOffset = 0
                    for (i in 0 until offset) {
                        if (formatted.getOrNull(i)?.isDigit() == true) originalOffset++
                    }
                    return minOf(originalOffset, original.length)
                }
            }
        )
    }
}

@Composable
internal fun defaultFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    disabledTextColor = Color.White,
    focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    disabledBorderColor = Color.White.copy(alpha = 0.3f),
    cursorColor = Color(0xFF34D399), focusedLabelColor = Color(0xFF34D399), unfocusedLabelColor = Color.White.copy(alpha = 0.55f),
    disabledLabelColor = Color.White.copy(alpha = 0.55f)
)

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
        AppScreen.RoomTypes -> RoomTypeFormDialog(item, houses, onDismiss, onSave)
        AppScreen.Rooms -> RoomFormDialog(item, houses, onDismiss, onSave)
        AppScreen.Services -> ServiceFormDialog(item, houses, rooms, onDismiss, onSave)
        AppScreen.Electric, AppScreen.Water -> UtilityReadingFormDialog(screen, item, houses, rooms, onDismiss, onSaveUtility, repository)
        AppScreen.Invoices -> InvoiceFormDialog(item, houses, rooms, onDismiss, onSaveInvoice)
        AppScreen.Payments -> SubmitPaymentFormDialog(item, invoices, onDismiss, onSavePayment)
        AppScreen.Incidents -> IncidentFormDialog(item, houses, rooms, onDismiss, onSave)
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
    houses: List<RentalItem>,
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var selectedHouseId by remember(item) { mutableStateOf(item.detail("houseId")) }
    var roomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var tenantUsername by remember(item) { mutableStateOf(item.detail("tenantUsername")) }
    var startDate by remember(item) { mutableStateOf(item.detail("startDate")) }
    var endDate by remember(item) { mutableStateOf(item.detail("endDate")) }
    var deposit by remember(item) { mutableStateOf(item.detail("deposit")) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Chờ người thuê xác nhận" }) }
    var localError by remember { mutableStateOf<String?>(null) }
    
    var houseExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val filteredRooms = remember(selectedHouseId, rooms) { rooms.filter { it.detail("houseId") == selectedHouseId } }
    val statuses = listOf("Chờ người thuê xác nhận", "Đang hiệu lực", "Đã thanh lý", "Hủy")

    val context = LocalContext.current
    val calendar = java.util.Calendar.getInstance()
    
    val startDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> startDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year) },
        calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
    val endDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> endDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year) },
        calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        disabledTextColor = Color.White,
        focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
        disabledBorderColor = Color.White.copy(alpha = 0.3f),
        cursorColor = Color(0xFF34D399), focusedLabelColor = Color(0xFF34D399), unfocusedLabelColor = Color.White.copy(alpha = 0.55f),
        disabledLabelColor = Color.White.copy(alpha = 0.55f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item.id.isBlank()) "Tạo hợp đồng" else "Sửa hợp đồng", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Box {
                        OutlinedTextField(
                            value = houses.find { it.id == selectedHouseId }?.let { "${it.id} - ${it.title}" } ?: "Chọn nhà trọ...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Nhà trọ *") }, modifier = Modifier.fillMaxWidth().clickable { houseExpanded = true },
                            singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                        DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                            houses.forEach { house ->
                                DropdownMenuItem(
                                    text = { Text("${house.id} - ${house.title}") },
                                    onClick = { selectedHouseId = house.id; roomId = ""; houseExpanded = false }
                                )
                            }
                        }
                    }
                }
                item { 
                    Box {
                        OutlinedTextField(
                            value = filteredRooms.find { it.id == roomId }?.let { "${it.id} - ${it.title}" } ?: "Chọn phòng trọ...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Phòng trọ *") }, modifier = Modifier.fillMaxWidth().clickable { roomExpanded = true },
                            singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors,
                            enabled = false // Disable to let Box intercept click
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { roomExpanded = true })
                        DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                            filteredRooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text("${room.id} - ${room.title}") },
                                    onClick = { roomId = room.id; roomExpanded = false }
                                )
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = tenantUsername, onValueChange = { tenantUsername = it }, label = { Text("Tên đăng nhập người thuê *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = startDate, onValueChange = {}, readOnly = true, label = { Text("Bắt đầu *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false)
                            Box(modifier = Modifier.matchParentSize().clickable { startDatePickerDialog.show() })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = endDate, onValueChange = {}, readOnly = true, label = { Text("Kết thúc *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false)
                            Box(modifier = Modifier.matchParentSize().clickable { endDatePickerDialog.show() })
                        }
                    }
                }
                item {
                    if (startDate.isNotBlank() && endDate.isNotBlank()) {
                        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        val sDate = runCatching { fmt.parse(startDate) }.getOrNull()
                        val eDate = runCatching { fmt.parse(endDate) }.getOrNull()
                        if (sDate != null && eDate != null) {
                            val diffInMillies = eDate.time - sDate.time
                            if (diffInMillies > 0) {
                                val diff = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS)
                                val months = diff / 30
                                val rPrice = com.example.myapplication.domain.util.moneyValue(filteredRooms.find { it.id == roomId }?.value ?: "0")
                                val estimatedRent = rPrice * months
                                Card(
                                    shape = CutCornerShape(8.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF0F766E).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Thời gian thuê dự kiến: $diff ngày (~$months tháng)", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                        if (months > 0) {
                                            Text("Tổng tiền phòng dự kiến: ${com.example.myapplication.domain.util.formatMoney(estimatedRent)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            } else {
                                Text("Ngày kết thúc phải sau ngày bắt đầu.", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = deposit, onValueChange = { deposit = it.filter { char -> char.isDigit() } }, label = { Text("Tiền đặt cọc") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = CurrencyVisualTransformation()) }
                item { 
                    Box {
                        OutlinedTextField(
                            value = status, onValueChange = {}, readOnly = true,
                            label = { Text("Trạng thái") }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { statusExpanded = true })
                        DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                            statuses.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = { status = s; statusExpanded = false }
                                )
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú / điều khoản") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = CutCornerShape(8.dp), colors = fieldColors) }
                localError?.let { item { Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) } }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable {
                        if (selectedHouseId.isBlank() || roomId.isBlank() || tenantUsername.isBlank() || startDate.isBlank() || endDate.isBlank()) {
                            localError = "Vui lòng nhập đủ nhà trọ, mã phòng, người thuê, ngày bắt đầu và kết thúc."
                        } else onSave(roomId.trim(), tenantUsername.trim(), startDate.trim(), endDate.trim(), deposit.trim(), note.trim(), status.trim())
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Lưu hợp đồng", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun RenewRequestDialog(contract: RentalItem, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var newEndDate by remember(contract) { mutableStateOf(contract.detail("endDate")) }
    var note by remember(contract) { mutableStateOf("Mình muốn gia hạn hợp đồng này.") }
    var localError by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = java.util.Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> newEndDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year) },
        calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    val fieldColors = defaultFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gửi yêu cầu gia hạn", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Hợp đồng", contract.title)
                DetailRow("Ngày kết thúc hiện tại", contract.detail("endDate").ifBlank { contract.value })
                Box {
                    OutlinedTextField(
                        value = newEndDate, onValueChange = {}, readOnly = true,
                        label = { Text("Ngày kết thúc mới") },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }, singleLine = true,
                        shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    shape = CutCornerShape(8.dp), colors = fieldColors
                )
                localError?.let { Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable {
                        if (newEndDate.isBlank()) localError = "Vui lòng nhập ngày kết thúc mới." else onSubmit(newEndDate.trim(), note.trim())
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Gửi gia hạn", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun RegisterServiceDialog(service: RentalItem, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var note by remember(service) { mutableStateOf("Tôi muốn đăng ký dịch vụ này.") }
    val fieldColors = defaultFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng ký dịch vụ", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Dịch vụ", service.title)
                DetailRow("Giá", service.value)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CutCornerShape(8.dp),
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(8.dp))
                    .background(Color(0xFF34D399))
                    .clickable { onSubmit(note.trim()) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Gửi đăng ký", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun RentRequestDialog(room: RentalItem, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var endDate by remember(room) { mutableStateOf("") }
    var note by remember(room) { mutableStateOf("Mình muốn thuê phòng này.") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = java.util.Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> endDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year) },
        calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Gửi yêu cầu thuê phòng", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Phòng cần thuê", room.title)
                DetailRow("Giá thuê", room.value)
                Box {
                    OutlinedTextField(
                        value = endDate, onValueChange = {}, readOnly = true,
                        label = { Text("Ngày dự kiến chuyển đi") },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }, singleLine = true,
                        shape = RoundedCornerShape(8.dp), enabled = false
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                }
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
                onClick = { onSubmit(if (endDate.isBlank()) "Không xác định" else "Đến $endDate", note) },
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

    val fieldColors = defaultFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id.isBlank()) "Thêm nhà trọ" else "Sửa nhà trọ", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên nhà trọ *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Địa chỉ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                OutlinedTextField(value = roomCount, onValueChange = { roomCount = it }, label = { Text("Số phòng (VD: 20 phòng)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                Text("Trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF34D399))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Đang hoạt động", "Tạm dừng")) { s ->
                        val active = status == s
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(8.dp))
                                .background(if (active) Color(0xFF34D399).copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (active) Color(0xFF34D399) else Color.White.copy(alpha = 0.3f), CutCornerShape(8.dp))
                                .clickable { status = s }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(s, color = if (active) Color(0xFF34D399) else Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
                error?.let { Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable {
                        if (name.isBlank()) error = "Tên nhà trọ không được để trống." else onSave(item.copy(title = name.trim(), status = status, value = roomCount.trim(), note = address.trim()))
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Lưu nhà trọ", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun RoomTypeFormDialog(item: RentalItem, houses: List<RentalItem>, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item.title) }
    var priceRange by remember(item) { mutableStateOf(item.value) }
    var selectedHouseId by remember(item) { mutableStateOf(item.detail("houseId")) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Đang dùng" }) }
    var error by remember { mutableStateOf<String?>(null) }
    var houseExpanded by remember { mutableStateOf(false) }

    val fieldColors = defaultFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id.isBlank()) "Thêm loại phòng" else "Sửa loại phòng", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(
                        value = houses.find { it.id == selectedHouseId }?.let { "${it.id} - ${it.title}" } ?: "Chọn nhà trọ...",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Nhà trọ *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                    DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                        houses.forEach { house ->
                            DropdownMenuItem(
                                text = { Text("${house.id} - ${house.title}") },
                                onClick = { selectedHouseId = house.id; houseExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên loại phòng *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                OutlinedTextField(value = priceRange, onValueChange = { priceRange = it }, label = { Text("Khoảng giá (VD: 2.000.000đ - 3.500.000đ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                Text("Trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF34D399))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Đang dùng", "Ngừng dùng")) { s ->
                        val active = status == s
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(8.dp))
                                .background(if (active) Color(0xFF34D399).copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (active) Color(0xFF34D399) else Color.White.copy(alpha = 0.3f), CutCornerShape(8.dp))
                                .clickable { status = s }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(s, color = if (active) Color(0xFF34D399) else Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = CutCornerShape(8.dp), colors = fieldColors)
                error?.let { Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable {
                        if (name.isBlank()) error = "Tên loại phòng không được để trống." else onSave(item.copy(title = name.trim(), status = status, value = priceRange.trim(), note = note.trim()))
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Lưu loại phòng", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
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
    var area by remember(item) { mutableStateOf(item.detail("area")) }
    var capacity by remember(item) { mutableStateOf(item.detail("capacity")) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Còn trống" }) }
    var error by remember { mutableStateOf<String?>(null) }
    var houseExpanded by remember { mutableStateOf(false) }

    val fieldColors = defaultFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id.isBlank()) "Thêm phòng" else "Sửa phòng", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên phòng * (VD: Phòng A01)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }
                item {
                    Box {
                        OutlinedTextField(
                            value = houses.find { it.id == houseId }?.let { "${it.id} - ${it.title}" } ?: "Chọn nhà trọ...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Nhà trọ *") }, modifier = Modifier.fillMaxWidth().clickable { houseExpanded = true },
                            singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                        DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                            houses.forEach { house ->
                                DropdownMenuItem(
                                    text = { Text("${house.id} - ${house.title}") },
                                    onClick = { houseId = house.id; houseExpanded = false }
                                )
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = price, onValueChange = { price = it.filter { char -> char.isDigit() } }, label = { Text("Giá thuê phòng * (VNĐ/tháng)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = CurrencyVisualTransformation(" VNĐ/tháng")) }
                item { OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Diện tích (m2)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }
                item { OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Sức chứa (người)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }
                item {
                    Text("Trạng thái", style = MaterialTheme.typography.labelMedium, color = Color(0xFF34D399))
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Còn trống", "Đã thuê", "Đang giữ chỗ", "Đang sửa chữa")) { s ->
                            val active = status == s
                            Box(
                                modifier = Modifier
                                    .clip(CutCornerShape(8.dp))
                                    .background(if (active) Color(0xFF34D399).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (active) Color(0xFF34D399) else Color.White.copy(alpha = 0.3f), CutCornerShape(8.dp))
                                    .clickable { status = s }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text(s, color = if (active) Color(0xFF34D399) else Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                }
                item { OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú (Tầng, tiện ích, ...)") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = CutCornerShape(8.dp), colors = fieldColors) }
                error?.let { err -> item { Text(err, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) } }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable {
                        when {
                            name.isBlank() -> error = "Tên phòng không được để trống."
                            houseId.isBlank() -> error = "Vui lòng chọn nhà trọ."
                            price.isBlank() -> error = "Giá thuê không được để trống."
                            else -> {
                                val detailsWithHouse = item.details.filterNot { it.first == "houseId" || it.first == "area" || it.first == "capacity" }.toMutableList().also { 
                                    it.add(0, "houseId" to houseId)
                                    it.add("area" to area.trim())
                                    it.add("capacity" to capacity.trim())
                                }
                                onSave(item.copy(title = name.trim(), status = status, value = price.trim(), note = note.trim(), details = detailsWithHouse))
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Lưu phòng", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun ServiceFormDialog(item: RentalItem, houses: List<RentalItem>, rooms: List<RentalItem>, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item.title) }
    var price by remember(item) { mutableStateOf(item.value) }
    var selectedHouseId by remember(item) { mutableStateOf(item.detail("houseId")) }
    var selectedRoomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var note by remember(item) { mutableStateOf(item.note) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { "Tiện ích nhà trọ" }) }
    var error by remember { mutableStateOf<String?>(null) }

    var houseExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val filteredRooms = remember(selectedHouseId, rooms) { rooms.filter { it.detail("houseId") == selectedHouseId } }

    val fieldColors = defaultFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id.isBlank()) "Thêm dịch vụ" else "Sửa dịch vụ", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(
                        value = houses.find { it.id == selectedHouseId }?.let { "${it.id} - ${it.title}" } ?: "Chọn nhà trọ...",
                        onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text("Nhà trọ *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                    DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                        houses.forEach { house ->
                            DropdownMenuItem(
                                text = { Text("${house.id} - ${house.title}") },
                                onClick = { selectedHouseId = house.id; selectedRoomId = ""; houseExpanded = false }
                            )
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = filteredRooms.find { it.id == selectedRoomId }?.let { "${it.id} - ${it.title}" } ?: "Chọn phòng trọ...",
                        onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text("Phòng trọ *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { roomExpanded = true })
                    DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                        filteredRooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text("${room.id} - ${room.title}") },
                                onClick = { selectedRoomId = room.id; roomExpanded = false }
                            )
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text("Loại dịch vụ *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { statusExpanded = true })
                    DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        listOf("Tiện ích nhà trọ", "Dịch vụ tính phí", "Tiện nghi phòng").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = { status = s; statusExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên dịch vụ * (VD: Internet, Rác...)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                OutlinedTextField(value = price, onValueChange = { price = it.filter { char -> char.isDigit() } }, label = { Text("Đơn giá (VNĐ/tháng) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = CurrencyVisualTransformation())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = CutCornerShape(8.dp), colors = fieldColors)
                error?.let { Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable {
                        when {
                            selectedHouseId.isBlank() -> error = "Vui lòng chọn nhà trọ."
                            selectedRoomId.isBlank() -> error = "Vui lòng chọn phòng trọ."
                            name.isBlank() -> error = "Tên dịch vụ không được để trống."
                            price.isBlank() -> error = "Đơn giá không được để trống."
                            else -> {
                                val newDetails = item.details.filterNot { it.first == "houseId" || it.first == "roomId" || it.first == "unit" } + ("houseId" to selectedHouseId) + ("roomId" to selectedRoomId)
                                onSave(item.copy(title = name.trim(), status = status, value = price.trim(), note = note.trim(), details = newDetails))
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Lưu dịch vụ", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun UtilityReadingFormDialog(
    screen: AppScreen,
    item: RentalItem,
    houses: List<RentalItem>,
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSaveUtility: (AppScreen, String, String, Double, Double, Double) -> Unit,
    repository: RentalRepository
) {
    var selectedHouseId by remember(item) { mutableStateOf(item.detail("houseId")) }
    var roomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var period by remember(item) { mutableStateOf(item.detail("period").ifBlank { "2026-06" }) }
    var oldIndex by remember(item) { mutableStateOf(item.detail("oldIndex").toDoubleOrNull() ?: 0.0) }
    var newIndexStr by remember(item) { mutableStateOf(item.detail("newIndex").ifBlank { "" }) }
    val defaultPrice = if (screen == AppScreen.Electric) 3500.0 else 15000.0
    var priceStr by remember(item) { mutableStateOf(item.detail("price").ifBlank { defaultPrice.toLong().toString() }) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadingOldIndex by remember { mutableStateOf(false) }
    
    var houseExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }
    val filteredRooms = remember(selectedHouseId, rooms) { rooms.filter { it.detail("houseId") == selectedHouseId } }

    LaunchedEffect(roomId) {
        if (roomId.isNotBlank() && item.id.isBlank()) {
            loadingOldIndex = true
            repository.getLatestUtilityIndex(screen, roomId)
                .onSuccess { oldIndex = it }
                .onFailure { /* fallback */ }
            loadingOldIndex = false
        }
    }

    val fieldColors = defaultFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id.isBlank()) "Ghi chỉ số ${screen.label.lowercase()}" else "Sửa chỉ số ${screen.label.lowercase()}", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Box {
                        OutlinedTextField(
                            value = houses.find { it.id == selectedHouseId }?.let { "${it.id} - ${it.title}" } ?: "Chọn nhà trọ...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Nhà trọ *") }, modifier = Modifier.fillMaxWidth().clickable { houseExpanded = true },
                            singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                        DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                            houses.forEach { house ->
                                DropdownMenuItem(
                                    text = { Text("${house.id} - ${house.title}") },
                                    onClick = { selectedHouseId = house.id; roomId = ""; houseExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    Box {
                        OutlinedTextField(
                            value = filteredRooms.find { it.id == roomId }?.let { "${it.id} - ${it.title}" } ?: "Chọn phòng trọ...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Phòng trọ *") }, modifier = Modifier.fillMaxWidth().clickable { roomExpanded = true },
                            singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, enabled = false
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { roomExpanded = true })
                        DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                            filteredRooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text("${room.id} - ${room.title}") },
                                    onClick = { roomId = room.id; roomExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    val context = LocalContext.current
                    OutlinedTextField(
                        value = period, onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text("Kỳ ghi chỉ số * (VD: 2026-06)") }, modifier = Modifier.fillMaxWidth().clickable {
                            val cal = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(context, { _, year, month, _ ->
                                period = "$year-${(month + 1).toString().padStart(2, '0')}"
                            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
                        }, singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = if (loadingOldIndex) "Đang tải..." else oldIndex.toString(), onValueChange = {}, label = { Text("Chỉ số cũ") }, enabled = false, modifier = Modifier.weight(1f), shape = CutCornerShape(8.dp), colors = fieldColors)
                        OutlinedTextField(value = newIndexStr, onValueChange = { newIndexStr = it }, label = { Text("Chỉ số mới *") }, modifier = Modifier.weight(1f), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                    }
                }
                item { OutlinedTextField(value = priceStr, onValueChange = { priceStr = it.filter { char -> char.isDigit() } }, label = { Text("Đơn giá (${if (screen == AppScreen.Electric) "đ/kWh" else "đ/m3"})") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = CurrencyVisualTransformation()) }
                error?.let { err -> item { Text(err, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) } }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable {
                        val newIndex = newIndexStr.toDoubleOrNull()
                        val price = priceStr.toDoubleOrNull()
                        when {
                            roomId.isBlank() -> error = "Vui lòng chọn phòng."
                            period.isBlank() -> error = "Vui lòng nhập kỳ ghi."
                            newIndex == null -> error = "Vui lòng nhập chỉ số mới hợp lệ."
                            newIndex < oldIndex -> error = "Chỉ số mới không được nhỏ hơn chỉ số cũ."
                            price == null || price <= 0 -> error = "Vui lòng nhập đơn giá hợp lệ."
                            else -> onSaveUtility(screen, roomId, period.trim(), oldIndex, newIndex, price)
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Lưu chỉ số", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun InvoiceFormDialog(
    item: RentalItem,
    houses: List<RentalItem>,
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSaveInvoice: (String, String, Double, String) -> Unit
) {
    var selectedHouseId by remember(item) { mutableStateOf(item.detail("houseId")) }
    var roomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var period by remember(item) { mutableStateOf(item.detail("period").ifBlank { "2026-06" }) }
    var otherCostStr by remember(item) { mutableStateOf(item.detail("otherCost").ifBlank { "0" }) }
    var otherNote by remember(item) { mutableStateOf(item.detail("otherNote")) }
    var error by remember { mutableStateOf<String?>(null) }
    
    var houseExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

    val filteredRooms = remember(selectedHouseId, rooms) { rooms.filter { it.detail("houseId") == selectedHouseId && (it.status.equals("Đã thuê", true) || it.detail("tenantUsername").isNotBlank()) } }
    
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
                    Box {
                        OutlinedTextField(
                            value = houses.find { it.id == selectedHouseId }?.let { "${it.id} - ${it.title}" } ?: "Chọn nhà trọ...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Nhà trọ *") }, modifier = Modifier.fillMaxWidth().clickable { houseExpanded = true },
                            singleLine = true, shape = RoundedCornerShape(8.dp)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                        DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                            houses.forEach { house ->
                                DropdownMenuItem(
                                    text = { Text("${house.id} - ${house.title}") },
                                    onClick = { selectedHouseId = house.id; roomId = ""; houseExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    Box {
                        OutlinedTextField(
                            value = filteredRooms.find { it.id == roomId }?.let { "${it.id} - ${it.title}" } ?: "Chọn phòng thuê...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Phòng thuê *") }, modifier = Modifier.fillMaxWidth().clickable { roomExpanded = true },
                            singleLine = true, shape = RoundedCornerShape(8.dp)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { roomExpanded = true })
                        DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                            filteredRooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text("${room.id} - ${room.title}") },
                                    onClick = { roomId = room.id; roomExpanded = false }
                                )
                            }
                        }
                    }
                    if (selectedHouseId.isNotBlank() && filteredRooms.isEmpty()) {
                        Text("Nhà trọ này không có phòng nào đang được thuê.", color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    val context = LocalContext.current
                    OutlinedTextField(
                        value = period, onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text("Kỳ hóa đơn * (VD: 2026-06)") },
                        modifier = Modifier.fillMaxWidth().clickable {
                            val cal = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(context, { _, year, month, _ ->
                                period = "$year-${(month + 1).toString().padStart(2, '0')}"
                            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
                        }, singleLine = true, shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = otherCostStr, onValueChange = { otherCostStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Chi phí phát sinh (nếu có)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = CurrencyVisualTransformation()
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
                        selectedHouseId.isBlank() -> error = "Vui lòng chọn nhà trọ."
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
                            onClick = { imagePicker.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                            enabled = !picking
                        ) {
                            Text(if (picking) "Đang đọc ảnh..." else if (receiptImage.isBlank()) "Chọn ảnh biên lai" else "Đổi ảnh khác", fontWeight = FontWeight.Bold)
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
        title = { Text(item.title, fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DetailRow("Module", screen.label) }
                item { DetailRow("Mã", item.id) }
                item { DetailRow("Trạng thái", item.status) }
                item { DetailRow("Giá trị", item.value) }
                item { DetailRow("Ghi chú", item.note) }
                items(item.details) { 
                    val displayKey = when(it.first) {
                        "tenantUsername" -> "Tên đăng nhập người thuê"
                        "roomId" -> "Mã phòng"
                        "roomName" -> "Tên phòng"
                        "period" -> "Kỳ thanh toán"
                        "totalAmount" -> "Tổng tiền"
                        "createdBy" -> "Người tạo"
                        "startDate" -> "Ngày bắt đầu"
                        "endDate" -> "Ngày kết thúc"
                        "deposit" -> "Tiền cọc"
                        "houseId" -> "Mã nhà"
                        "roomType" -> "Loại phòng"
                        "price" -> "Giá thuê"
                        "area" -> "Diện tích"
                        "maxTenants" -> "Số người tối đa"
                        "transactionId" -> "Mã giao dịch"
                        "receiptImage" -> "Ảnh biên lai"
                        "rejectReason" -> "Lý do từ chối"
                        "invoiceId" -> "Mã hóa đơn"
                        "duration" -> "Thời hạn thuê"
                        "targetType" -> "Đối tượng nhận"
                        "content" -> "Nội dung"
                        "response" -> "Phản hồi"
                        "oldIndex" -> "Chỉ số cũ"
                        "newIndex" -> "Chỉ số mới"
                        "unitPrice" -> "Đơn giá"
                        else -> it.first
                    }
                    if (it.first == "receiptImage" && it.second.startsWith("data:image")) {
                        Text("Ảnh biên lai:", color = Color.White.copy(alpha=0.7f), style = MaterialTheme.typography.labelMedium)
                        val base64String = it.second.substringAfter("base64,")
                        try {
                            val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = androidx.compose.ui.graphics.asImageBitmap(bitmap),
                                    contentDescription = "Ảnh biên lai",
                                    modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text("Lỗi hiển thị ảnh", color = Color.Red)
                            }
                        } catch (e: Exception) {
                            Text("Lỗi dữ liệu ảnh", color = Color.Red)
                        }
                    } else {
                        DetailRow(displayKey, it.second) 
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.5f), CutCornerShape(10.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Đóng", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}

@Composable
internal fun IncidentFormDialog(
    item: RentalItem,
    houses: List<RentalItem>,
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSave: (RentalItem) -> Unit
) {
    var selectedHouseId by remember(item) { mutableStateOf(item.detail("houseId")) }
    var selectedRoomId by remember(item) { mutableStateOf(item.detail("roomId")) }
    var title by remember(item) { mutableStateOf(item.title) }
    var description by remember(item) { mutableStateOf(item.note) }
    var urgency by remember(item) { mutableStateOf(item.value.ifBlank { "Bình thường" }) }
    var error by remember { mutableStateOf<String?>(null) }
    
    var houseExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }
    val filteredRooms = remember(selectedHouseId, rooms) { rooms.filter { it.detail("houseId") == selectedHouseId } }

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
                Box {
                    OutlinedTextField(
                        value = houses.find { it.id == selectedHouseId }?.let { "${it.id} - ${it.title}" } ?: "Chọn nhà trọ...",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Nhà trọ *") }, modifier = Modifier.fillMaxWidth().clickable { houseExpanded = true },
                        singleLine = true, shape = RoundedCornerShape(8.dp)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                    DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                        houses.forEach { house ->
                            DropdownMenuItem(
                                text = { Text("${house.id} - ${house.title}") },
                                onClick = { selectedHouseId = house.id; selectedRoomId = ""; houseExpanded = false }
                            )
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = filteredRooms.find { it.id == selectedRoomId }?.let { "${it.id} - ${it.title}" } ?: "Chọn phòng trọ...",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Phòng trọ *") }, modifier = Modifier.fillMaxWidth().clickable { roomExpanded = true },
                        singleLine = true, shape = RoundedCornerShape(8.dp)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { roomExpanded = true })
                    DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                        filteredRooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text("${room.id} - ${room.title}") },
                                onClick = { selectedRoomId = room.id; roomExpanded = false }
                            )
                        }
                    }
                }
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
                        selectedHouseId.isBlank() -> error = "Vui lòng chọn nhà trọ."
                        selectedRoomId.isBlank() -> error = "Vui lòng chọn phòng trọ."
                        title.isBlank() -> error = "Vui lòng nhập tiêu đề sự cố."
                        description.isBlank() -> error = "Vui lòng mô tả chi tiết sự cố."
                        else -> {
                            val newDetails = item.details.filterNot { it.first == "houseId" || it.first == "roomId" } + ("houseId" to selectedHouseId) + ("roomId" to selectedRoomId)
                            onSave(
                                item.copy(
                                    title = title.trim(),
                                    status = "Mới",
                                    value = urgency,
                                    note = description.trim(),
                                    details = newDetails
                                )
                            )
                        }
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
    var newStatus by remember(incident) { mutableStateOf("DangXuLy") }
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
                    items(listOf("DangXuLy", "DaXuLy", "KhongTheXuLy")) { s ->
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
    houses: List<RentalItem>,
    rooms: List<RentalItem>,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("house") }
    var selectedHouseId by remember { mutableStateOf("") }
    var selectedRoomId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    var houseExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

    val filteredRooms = remember(selectedHouseId, rooms) {
        if (selectedHouseId.isBlank()) rooms else rooms.filter { it.detail("houseId") == selectedHouseId }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1E3A5F), unfocusedTextColor = Color(0xFF1E3A5F),
        disabledTextColor = Color(0xFF1E3A5F),
        focusedBorderColor = screenAccent(AppScreen.Notices), unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
        disabledBorderColor = Color.Gray.copy(alpha = 0.5f),
        disabledLabelColor = Color(0xFF64748B)
    )

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
                        shape = RoundedCornerShape(8.dp), colors = fieldColors
                    )
                }
                item {
                    OutlinedTextField(
                        value = content, onValueChange = { content = it },
                        label = { Text("Nội dung thông báo *") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                        shape = RoundedCornerShape(8.dp), colors = fieldColors
                    )
                }
                item {
                    Text("Gửi đến", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("house" to "Theo nhà trọ", "room" to "Theo phòng")) { (k, v) ->
                            FilterChip(
                                selected = targetType == k,
                                onClick = { targetType = k; selectedRoomId = ""; selectedHouseId = "" },
                                label = { Text(v) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
                item {
                    Text("Chọn Nhà Trọ *", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedTextField(
                            value = houses.find { it.id == selectedHouseId }?.title ?: "Chọn nhà trọ...", 
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { houseExpanded = true },
                            singleLine = true, shape = RoundedCornerShape(8.dp), colors = fieldColors, enabled = false
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { houseExpanded = true })
                        DropdownMenu(expanded = houseExpanded, onDismissRequest = { houseExpanded = false }) {
                            houses.forEach { house ->
                                DropdownMenuItem(
                                    text = { Text("${house.id} - ${house.title}") },
                                    onClick = { selectedHouseId = house.id; selectedRoomId = ""; houseExpanded = false }
                                )
                            }
                        }
                    }
                }
                if (targetType == "room") {
                    item {
                        Text("Chọn Phòng *", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        Box {
                            OutlinedTextField(
                                value = filteredRooms.find { it.id == selectedRoomId }?.title ?: "Chọn phòng...", 
                                onValueChange = {}, readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable { roomExpanded = true },
                                singleLine = true, shape = RoundedCornerShape(8.dp), colors = fieldColors, enabled = false
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { roomExpanded = true })
                            DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                                filteredRooms.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text("${room.id} - ${room.title}") },
                                        onClick = { selectedRoomId = room.id; roomExpanded = false }
                                    )
                                }
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
