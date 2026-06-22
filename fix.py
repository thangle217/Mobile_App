import re

with open('app/src/main/java/com/example/myapplication/ui/app/ModuleDialogs.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. RegisterServiceDialog
new_reg_service = '''@Composable
internal fun RegisterServiceDialog(service: RentalItem, rooms: List<RentalItem>, onDismiss: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var note by remember(service) { mutableStateOf("Tôi muốn đăng ký dịch vụ này.") }
    var selectedRoomId by remember(service) { mutableStateOf(rooms.firstOrNull()?.id ?: "") }
    
    val cal = java.util.Calendar.getInstance()
    var period by remember(service) { mutableStateOf("${cal.get(java.util.Calendar.YEAR)}-${(cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}") }
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
                            period = "${year}-${(month + 1).toString().padStart(2, '0')}"
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
}'''
content = re.sub(r'@Composable\s+internal fun RegisterServiceDialog.*?shape = CutCornerShape\(20\.dp\)\s+\)\s+\}', new_reg_service, content, flags=re.DOTALL)

# 2. Format Prices
content = content.replace('DetailRow("Giá thuê", room.value)', 'val priceDisplay = room.value.toLongOrNull()?.let { "%,d VNĐ/tháng".format(it).replace(\',\', \'.\') } ?: room.value\n                DetailRow("Giá thuê", priceDisplay)')
content = content.replace('item { DetailRow("Giá trị", item.value) }', 'item { \n                    val displayVal = if (screen in setOf(AppScreen.Rooms, AppScreen.Services)) {\n                        item.value.toLongOrNull()?.let { "%,d VNĐ/tháng".format(it).replace(\',\', \'.\') } ?: item.value\n                    } else if (screen in setOf(AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments, AppScreen.Electric, AppScreen.Water)) {\n                        item.value.toLongOrNull()?.let { "%,d VNĐ".format(it).replace(\',\', \'.\') } ?: item.value\n                    } else {\n                        item.value\n                    }\n                    DetailRow("Giá trị", displayVal) \n                }')

# 3. NoticeFormDialog targetType
content = content.replace('targetType == "room" && selectedRoomId.isBlank() -> { error = "Vui l?ng ch?n ph?ng c?n g?i."; return@Button }\n                        else -> "all"', 'targetType == "room" && selectedRoomId.isBlank() -> { error = "Vui lòng chọn phòng cần gửi."; return@Button }\n                        targetType == "house" && selectedHouseId.isNotBlank() -> "house:"\n                        targetType == "house" && selectedHouseId.isBlank() -> { error = "Vui lòng chọn nhà trọ cần gửi."; return@Button }\n                        else -> "all"')

# 4. SubmitPaymentFormDialog base64 encode
old_img_read = r'val bytes = context\.contentResolver\.openInputStream\(uri\)\?\.use \{ it\.readBytes\(\) \}\s+if \(bytes != null\) \{\s+val base64 = android\.util\.Base64\.encodeToString\(bytes, android\.util\.Base64\.DEFAULT\)\s+receiptImage = "data:image/jpeg;base64,\"\s+\}'
new_img_read = '''val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val ratio = 800f / maxOf(bitmap.width, bitmap.height)
                        val scaled = if (ratio < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
                        val outputStream = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val bytes = outputStream.toByteArray()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        receiptImage = "data:image/jpeg;base64,"
                    }'''
content = re.sub(old_img_read, new_img_read, content)

# 4b. SubmitPaymentFormDialog image preview
old_preview = r'if \(receiptImage\.isNotBlank\(\)\) \{\s+Text\("[^"]+", color = Color\(0xFF10B981\), style = MaterialTheme\.typography\.bodySmall, fontWeight = FontWeight\.Medium\)\s+\}'
new_preview = '''if (receiptImage.isNotBlank()) {
                            val b64 = receiptImage.substringAfter("base64,")
                            val bmp = try { val b = android.util.Base64.decode(b64, android.util.Base64.DEFAULT); android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size) } catch (e: Exception) { null }
                            if (bmp != null) {
                                androidx.compose.foundation.Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            }
                            Text("Đã chọn file ảnh", color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }'''
content = re.sub(old_preview, new_preview, content)

with open('app/src/main/java/com/example/myapplication/ui/app/ModuleDialogs.kt', 'w', encoding='utf-8') as f:
    f.write(content)
