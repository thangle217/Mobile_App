import sys

file_path = r'D:\QuanLyNhaTro_MobileApp\app\src\main\java\com\example\myapplication\ui\app\ModuleDialogs.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Make RoomFormDialog replacements
if 'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Còn trống" }) }' in content:
    content = content.replace(
        'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Còn trống" }) }',
        'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Còn trống" }) }\n    var area by remember(item) { mutableStateOf(item.details.firstOrNull { it.first == "area" }?.second ?: "") }\n    var capacity by remember(item) { mutableStateOf(item.details.firstOrNull { it.first == "capacity" }?.second ?: "") }'
    )
    content = content.replace(
        'OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Giá thuê * (VD: 3.200.000đ/tháng)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)',
        'OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Giá thuê phòng * (VNĐ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)\n                item { OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Diện tích (m2)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }\n                item { OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Sức chứa (người)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }'
    )
    content = content.replace(
        'onSave(item.copy(title = name.trim(), status = status, value = price.trim(), note = note.trim(), details = listOf("houseId" to houseId)))',
        'onSave(item.copy(title = name.trim(), status = status, value = price.trim(), note = note.trim(), details = listOf("houseId" to houseId, "area" to area.trim(), "capacity" to capacity.trim())))'
    )
    print("RoomFormDialog updated.")

# Make ServiceFormDialog replacements
if 'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Tính phí" }) }' in content:
    content = content.replace(
        'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Tính phí" }) }',
        'var status by remember(item) { mutableStateOf(item.status.ifBlank { "TinhPhi" }) }\n    var houseId by remember(item) { mutableStateOf(item.details.firstOrNull { it.first == "houseId" }?.second ?: "") }'
    )
    content = content.replace(
        'items(listOf("Tính phí", "Miễn phí", "Tạm dừng")) { s ->',
        'items(listOf("TienIch", "TienNghi", "TinhPhi")) { s ->'
    )
    content = content.replace(
        'onSave(item.copy(title = name.trim(), status = status, value = unitPrice.trim(), note = note.trim(), details = listOf("unit" to unit)))',
        'onSave(item.copy(title = name.trim(), status = status, value = unitPrice.trim(), note = note.trim(), details = listOf("unit" to unit, "houseId" to houseId)))'
    )
    print("ServiceFormDialog updated.")

# ContractEditorDialog replacements
if 'var price by remember(item) { mutableStateOf(item.value) }' in content:
    content = content.replace(
        'var price by remember(item) { mutableStateOf(item.value) }',
        '// Removed arbitrary price input'
    )
    content = content.replace(
        'OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Giá thuê/tháng * (VD: 3.200.000)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)',
        '// Price is auto-calculated'
    )
    content = content.replace(
        'onSave(item.copy(title = "HĐ Thuê: $roomName", status = status, value = price.trim(), note = note.trim(), details = listOf(',
        'onSave(item.copy(title = "HĐ Thuê: $roomName", status = status, value = "0", note = note.trim(), details = listOf('
    )
    print("ContractEditorDialog updated.")

# InvoiceFormDialog replacements
if 'var totalAmount by remember(item) { mutableStateOf(item.value) }' in content:
    content = content.replace(
        'var totalAmount by remember(item) { mutableStateOf(item.value) }',
        'var totalAmount by remember(item) { mutableStateOf("") }\n    var otherAmount by remember(item) { mutableStateOf(item.value) }'
    )
    content = content.replace(
        'OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = { Text("Tổng tiền * (VNĐ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)',
        'OutlinedTextField(value = otherAmount, onValueChange = { otherAmount = it }, label = { Text("Tiền phát sinh khác (VNĐ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)'
    )
    content = content.replace(
        'value = totalAmount.trim()',
        'value = otherAmount.trim()'
    )
    print("InvoiceFormDialog updated.")

# IncidentFormDialog replacements
if 'var urgency by remember(item) { mutableStateOf(item.value.ifBlank { "Bình thường" }) }' in content:
    content = content.replace(
        'items(listOf("Bình thường", "Cao", "Nghiêm trọng")) { u ->',
        'items(listOf("Bình thường", "Gấp", "Rất gấp")) { u ->'
    )
    print("IncidentFormDialog updated.")

# RespondIncidentDialog replacements
if 'items(listOf("Đang xử lý", "Đã xử lý", "Từ chối")) { s ->' in content:
    content = content.replace(
        'items(listOf("Đang xử lý", "Đã xử lý", "Từ chối")) { s ->',
        'items(listOf("Moi", "DangXuLy", "DaXuLy")) { s ->'
    )
    print("RespondIncidentDialog updated.")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
