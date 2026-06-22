import re

with open("D:\\QuanLyNhaTro_MobileApp\\app\\src\\main\\java\\com\\example\\myapplication\\ui\\app\\ModuleDialogs.bak", "r", encoding="utf-8") as f:
    content = f.read()

# Fix the ItemFormSelector switch
switch_broken = "        AppScreen.Services -> ServiceFormDialog(item, houses, rooms, onDismiss, onSave)\n        AppScreen.Electric, AppScreen.Water -> UtilityReadingFormDialog(screen, item, houses, rooms, onDismiss, onSaveUtility, repository)\n        AppScreen.Invoices -> InvoiceFormDialog(item, houses, rooms, onDismiss, onSaveInvoice)\n    item: RentalItem,"

switch_fixed = """        AppScreen.Services -> ServiceFormDialog(item, houses, rooms, onDismiss, onSave)
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
    emailHint: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var email by remember { androidx.compose.runtime.mutableStateOf(emailHint) }

    val fieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedTextColor = androidx.compose.ui.graphics.Color.White,
        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
        focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF34D399),
        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
        cursorColor = androidx.compose.ui.graphics.Color(0xFF34D399),
        focusedLabelColor = androidx.compose.ui.graphics.Color(0xFF34D399),
        unfocusedLabelColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f)
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("Khôi ph?c m?t kh?u", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text("H? th?ng s? g?i m?t ðý?ng d?n khôi ph?c m?t kh?u qua email c?a b?n.", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { androidx.compose.material3.Text("Email liên k?t v?i tài kho?n") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.CutCornerShape(8.dp), colors = fieldColors
                )
            }
        },
        confirmButton = {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .androidx.compose.ui.draw.clip(androidx.compose.foundation.shape.CutCornerShape(10.dp))
                    .androidx.compose.foundation.background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF0F766E), androidx.compose.ui.graphics.Color(0xFF0369A1))))
                    .androidx.compose.foundation.clickable(enabled = !saving && email.isNotBlank()) { onSubmit(email.trim()) }
                    .androidx.compose.foundation.layout.padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                androidx.compose.material3.Text(if (saving) "Ðang g?i..." else "G?i link khôi ph?c", color = androidx.compose.ui.graphics.Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text("H?y", color = androidx.compose.ui.graphics.Color(0xFF34D399)) }
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFF064E3B),
        titleContentColor = androidx.compose.ui.graphics.Color.White,
        shape = androidx.compose.foundation.shape.CutCornerShape(20.dp)
    )
}

@Composable
internal fun ContractEditorDialog(
    item: RentalItem,"""

content = content.replace(switch_broken, switch_fixed)

with open("D:\\QuanLyNhaTro_MobileApp\\app\\src\\main\\java\\com\\example\\myapplication\\ui\\app\\ModuleDialogs.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("Fixed!")
