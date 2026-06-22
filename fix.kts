import java.io.File

fun main() {
    val file = File("app/src/main/java/com/example/myapplication/ui/app/ModuleDialogs.kt")
    var content = file.readText(Charsets.UTF_8)
    
    // Remove RoomTypeFormDialog
    content = content.replace(Regex("@Composable\\s*internal fun RoomTypeFormDialog.*?\\n\\}\\n", setOf(RegexOption.DOT_MATCHES_ALL)), "")

    // RentRequestDialog
    val oldRentReq = """internal fun RentRequestDialog(room: RentalItem, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var endDate by remember(room) { mutableStateOf("") }
    var note by remember(room) { mutableStateOf("Mình muốn thuê phòng này.") }"""
    
    val newRentReq = """internal fun RentRequestDialog(room: RentalItem, onDismiss: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var startDate by remember(room) { mutableStateOf("") }
    var endDate by remember(room) { mutableStateOf("") }
    var note by remember(room) { mutableStateOf("Mình muốn thuê phòng này.") }"""
    
    content = content.replace(oldRentReq, newRentReq)

    val oldRentDate = """                    OutlinedTextField(
                        value = endDate, onValueChange = {}, readOnly = true,
                        label = { Text("Dự kiến kết thúc") }, modifier = Modifier.weight(1f),"""
    val newRentDate = """                    OutlinedTextField(
                        value = startDate, onValueChange = {}, readOnly = true,
                        label = { Text("Ngày bắt đầu") }, modifier = Modifier.weight(1f).clickable {
                            val c = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(context, { _, y, m, d -> startDate = String.format("%02d/%02d/%04d", d, m + 1, y) }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
                        },
                        singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                    OutlinedTextField(
                        value = endDate, onValueChange = {}, readOnly = true,
                        label = { Text("Dự kiến kết thúc") }, modifier = Modifier.weight(1f),"""
    content = content.replace(oldRentDate, newRentDate)
    
    val oldRentSubmit = """                    .clickable(enabled = endDate.isNotBlank()) { onSubmit(endDate, note.trim()) }"""
    val newRentSubmit = """                    .clickable(enabled = endDate.isNotBlank() && startDate.isNotBlank()) { onSubmit(startDate, endDate, note.trim()) }"""
    content = content.replace(oldRentSubmit, newRentSubmit)

    // RegisterServiceDialog
    val oldReg = """internal fun RegisterServiceDialog(service: RentalItem, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
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
                    .clip(CutCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                    .clickable { onSubmit(note.trim()) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Đăng ký", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}"""
    val newReg = """internal fun RegisterServiceDialog(service: RentalItem, rooms: List<RentalItem>, onDismiss: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var note by remember(service) { mutableStateOf("Tôi muốn đăng ký dịch vụ này.") }
    var selectedRoomId by remember(service) { mutableStateOf(rooms.firstOrNull()?.id ?: "") }
    
    val cal = java.util.Calendar.getInstance()
    var period by remember(service) { mutableStateOf("{cal.get(java.util.Calendar.YEAR)}-{(cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}") }
    var roomExpanded by remember { mutableStateOf(false) }
    val fieldColors = defaultFieldColors()
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng ký dịch vụ", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Dịch vụ", service.title)
                val priceDisplay = service.value.toLongOrNull()?.let { "%,d VNĐ/tháng".format(it).replace(',', '.') } ?: service.value
                DetailRow("Giá", priceDisplay)
                
                Box {
                    OutlinedTextField(
                        value = rooms.find { it.id == selectedRoomId }?.title ?: "Chọn phòng",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Phòng") }, modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { roomExpanded = true })
                    DropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                        rooms.forEach { r ->
                            DropdownMenuItem(text = { Text(r.title) }, onClick = { selectedRoomId = r.id; roomExpanded = false })
                        }
                    }
                }
                
                Box {
                    OutlinedTextField(
                        value = period, onValueChange = {}, readOnly = true,
                        label = { Text("Tháng đăng ký") }, modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp), colors = fieldColors
                    )
                    Box(modifier = Modifier.matchParentSize().clickable {
                        val parts = period.split("-")
                        val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(java.util.Calendar.YEAR)
                        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(java.util.Calendar.MONTH) + 1)) - 1
                        android.app.DatePickerDialog(context, { _, year, month, _ ->
                            period = "year-{(month + 1).toString().padStart(2, '0')}"
                        }, y, m, 1).show()
                    })
                }

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
                    .clip(CutCornerShape(10.dp))
                    .background(if (selectedRoomId.isNotBlank()) Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))) else Brush.horizontalGradient(listOf(Color.Gray, Color.Gray)))
                    .clickable(enabled = selectedRoomId.isNotBlank()) { onSubmit(selectedRoomId, period, note.trim()) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("Đăng ký", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF34D399)) } },
        containerColor = Color(0xFF064E3B),
        titleContentColor = Color.White,
        shape = CutCornerShape(20.dp)
    )
}"""
    content = content.replace(oldReg, newReg)
    
    // Formatting
    content = content.replace("DetailRow(\"Giá thuê\", room.value)", "val priceDisplay = room.value.toLongOrNull()?.let { \"%,d VNĐ/tháng\".format(it).replace(',', '.') } ?: room.value\n                DetailRow(\"Giá thuê\", priceDisplay)")
    val oldDetailPrice = "item { DetailRow(\"Giá trị\", item.value) }"
    val newDetailPrice = """item { 
                    val displayVal = if (screen in setOf(AppScreen.Rooms, AppScreen.Services)) {
                        item.value.toLongOrNull()?.let { "%,d VNĐ/tháng".format(it).replace(',', '.') } ?: item.value
                    } else if (screen in setOf(AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments, AppScreen.Electric, AppScreen.Water)) {
                        item.value.toLongOrNull()?.let { "%,d VNĐ".format(it).replace(',', '.') } ?: item.value
                    } else {
                        item.value
                    }
                    DetailRow("Giá trị", displayVal) 
                }"""
    content = content.replace(oldDetailPrice, newDetailPrice)
    
    // NoticeFormDialog
    content = content.replace("targetType == \"room\" && selectedRoomId.isBlank() -> { error = \"Vui l?ng ch?n ph?ng c?n g?i.\"; return@Button }\n                        else -> \"all\"", "targetType == \"room\" && selectedRoomId.isBlank() -> { error = \"Vui lòng chọn phòng cần gửi.\"; return@Button }\n                        targetType == \"house\" && selectedHouseId.isNotBlank() -> \"house:\selectedHouseId\"\n                        targetType == \"house\" && selectedHouseId.isBlank() -> { error = \"Vui lòng chọn nhà trọ cần gửi.\"; return@Button }\n                        else -> \"all\"")

    // SubmitPaymentFormDialog
    val oldImgRead = """val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                        receiptImage = "data:image/jpeg;base64,base64"
                    }"""
    val newImgRead = """val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val ratio = 800f / maxOf(bitmap.width, bitmap.height)
                        val scaled = if (ratio < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
                        val outputStream = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val bytes = outputStream.toByteArray()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        receiptImage = "data:image/jpeg;base64,base64"
                    }"""
    content = content.replace(oldImgRead, newImgRead)
    
    val oldImgPreview = """if (receiptImage.isNotBlank()) {
                            Text("Đã chọn file ảnh", color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }"""
    val newImgPreview = """if (receiptImage.isNotBlank()) {
                            val b64 = receiptImage.substringAfter("base64,")
                            val bmp = try { val b = android.util.Base64.decode(b64, android.util.Base64.DEFAULT); android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size) } catch (e: Exception) { null }
                            if (bmp != null) {
                                androidx.compose.foundation.Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            }
                            Text("Đã chọn file ảnh", color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }"""
    content = content.replace(oldImgPreview, newImgPreview)
    
    file.writeText(content, Charsets.UTF_8)
}
main()
