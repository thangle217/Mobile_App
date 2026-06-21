$file = "D:\QuanLyNhaTro_MobileApp\app\src\main\java\com\example\myapplication\ui\app\ModuleDialogs.kt"
$content = Get-Content -Path $file -Raw -Encoding UTF8

$content = $content.Replace(
    'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Còn trống" }) }',
    'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Còn trống" }) }
    var area by remember(item) { mutableStateOf(item.details.firstOrNull { it.first == "area" }?.second ?: "") }
    var capacity by remember(item) { mutableStateOf(item.details.firstOrNull { it.first == "capacity" }?.second ?: "") }'
)

$content = $content.Replace(
    'OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Giá thuê * (VD: 3.200.000đ/tháng)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)',
    'OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Giá thuê phòng * (VNĐ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)
                item { OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Diện tích (m2)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }
                item { OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Sức chứa (người)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors) }'
)

$content = $content.Replace(
    'onSave(item.copy(title = name.trim(), status = status, value = price.trim(), note = note.trim(), details = listOf("houseId" to houseId)))',
    'onSave(item.copy(title = name.trim(), status = status, value = price.trim(), note = note.trim(), details = listOf("houseId" to houseId, "area" to area.trim(), "capacity" to capacity.trim())))'
)

$content = $content.Replace(
    'var status by remember(item) { mutableStateOf(item.status.ifBlank { "Tính phí" }) }',
    'var status by remember(item) { mutableStateOf(item.status.ifBlank { "TinhPhi" }) }
    var houseId by remember(item) { mutableStateOf(item.details.firstOrNull { it.first == "houseId" }?.second ?: "") }'
)

$content = $content.Replace(
    'items(listOf("Tính phí", "Miễn phí", "Tạm dừng")) { s ->',
    'items(listOf("TienIch", "TienNghi", "TinhPhi")) { s ->'
)

$content = $content.Replace(
    'onSave(item.copy(title = name.trim(), status = status, value = unitPrice.trim(), note = note.trim(), details = listOf("unit" to unit)))',
    'onSave(item.copy(title = name.trim(), status = status, value = unitPrice.trim(), note = note.trim(), details = listOf("unit" to unit, "houseId" to houseId)))'
)

$content = $content.Replace(
    'OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Giá thuê/tháng * (VD: 3.200.000)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)',
    '// Price is auto-calculated'
)

$content = $content.Replace(
    'onSave(item.copy(title = "HĐ Thuê: $roomName", status = status, value = price.trim(), note = note.trim(), details = listOf(',
    'onSave(item.copy(title = "HĐ Thuê: $roomName", status = status, value = "0", note = note.trim(), details = listOf('
)

$content = $content.Replace(
    'var totalAmount by remember(item) { mutableStateOf(item.value) }',
    'var totalAmount by remember(item) { mutableStateOf("") }
    var otherAmount by remember(item) { mutableStateOf(item.value) }'
)

$content = $content.Replace(
    'OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = { Text("Tổng tiền * (VNĐ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)',
    'OutlinedTextField(value = otherAmount, onValueChange = { otherAmount = it }, label = { Text("Tiền phát sinh khác (VNĐ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CutCornerShape(8.dp), colors = fieldColors)'
)

$content = $content.Replace(
    'value = totalAmount.trim()',
    'value = otherAmount.trim()'
)

$content = $content.Replace(
    'items(listOf("Bình thường", "Cao", "Nghiêm trọng")) { u ->',
    'items(listOf("Bình thường", "Gấp", "Rất gấp")) { u ->'
)

$content = $content.Replace(
    'items(listOf("Đang xử lý", "Đã xử lý", "Từ chối")) { s ->',
    'items(listOf("Moi", "DangXuLy", "DaXuLy")) { s ->'
)

Set-Content -Path $file -Value $content -Encoding UTF8
Write-Host "Dialogs patched successfully!"
