$appFile = "D:\QuanLyNhaTro_MobileApp\app\src\main\java\com\example\myapplication\ui\app\RentalManagerApp.kt"
$lines = Get-Content $appFile -Encoding UTF8
$out = @()
$skipActionError = $false
foreach ($line in $lines) {
    if ($line -match "AppScreen.RoomTypes -> Color") { continue }
    
    if ($line -match 'else -> ModuleScreen\(repository, session, screen, onLogout\)') {
        $out += $line.Replace("else -> ModuleScreen(repository, session, screen, onLogout)", "else -> ModuleScreen(repository, session, screen, onLogout, { msg -> globalError = msg; errorKey++ })")
        continue
    }
    
    if ($line -match 'Box\(modifier = Modifier\.padding\(padding\)\) \{') {
        $line = $line.Replace("Box(modifier = Modifier.padding(padding)) {", "Box(modifier = Modifier.padding(padding).fillMaxSize()) {")
    }

    if ($line -match 'onSessionExpired: \(\) -> Unit') {
        if ($line -notmatch 'onShowError') {
            $out += "    onSessionExpired: () -> Unit,"
            $out += "    onShowError: (String) -> Unit"
            continue
        }
    }
    if ($line -match 'var actionError by remember \{ mutableStateOf<String\?>\(null\) \}') { continue }
    if ($line -match 'actionError\?\.let \{') {
        $skipActionError = $true
        continue
    }
    if ($skipActionError) {
        if ($line -match '\}' -and $line -notmatch 'item \{') { $skipActionError = $false }
        continue
    }
    if ($line -match 'actionError = null') { continue }
    if ($line -match '\.onFailure \{ actionError = (.*?\.message.*?) \}') {
        $line = $line -replace '\.onFailure \{ actionError = (.*?) \}', '.onFailure { onShowError($1) }'
    }
    
    if ($line -match 'onSubmit = \{ duration, note ->') {
        $line = $line.Replace("duration, note", "moveInDate, expectedMoveOutDate, note")
    }
    if ($line -match 'repository.requestRoom\(room.id, session, duration, note\)') {
        $line = $line.Replace("duration", "moveInDate, expectedMoveOutDate")
    }
    
    if ($line -match 'service = service,') {
        if ($out[-1] -match 'RegisterServiceDialog\(') {
            $out += $line
            $out += "            rooms = rooms,"
            continue
        }
    }
    if ($line -match 'onSubmit = \{ note ->') {
        if ($out[-1] -match 'onDismiss = \{ registeringService = null \},') {
            $line = $line.Replace("note ->", "roomId, period, note ->")
        }
    }
    if ($line -match '"roomId" to service.detail\("roomId"\)') {
        $line = $line.Replace('"roomId" to service.detail("roomId")', '"roomId" to roomId, period')
    }
    
    if ($line -match 'AppScreen.RoomTypes ->') { continue }
    if ($line -match 'AppScreen.RoomTypes,') { $line = $line.Replace("AppScreen.RoomTypes,", "") }
    if ($line -match ', AppScreen.RoomTypes') { $line = $line.Replace(", AppScreen.RoomTypes", "") }
    
    $out += $line
}

$finalOut = @()
$skipNextBrace = $false
foreach ($line in $out) {
    $finalOut += $line
    if ($line -match 'val scope = rememberCoroutineScope\(\)') {
        if ($finalOut.Length -lt 150) {
            $finalOut += "    var globalError by remember { mutableStateOf<String?>(null) }"
            $finalOut += "    var errorKey by remember { mutableStateOf(0) }"
            $finalOut += "    androidx.compose.runtime.LaunchedEffect(globalError, errorKey) {"
            $finalOut += "        if (globalError != null) {"
            $finalOut += "            kotlinx.coroutines.delay(4000)"
            $finalOut += "            globalError = null"
            $finalOut += "        }"
            $finalOut += "    }"
        }
    }
    if ($line -match 'else -> ModuleScreen\(repository, session, screen, onLogout') {
        $finalOut += "                }"
        $finalOut += "                androidx.compose.animation.AnimatedVisibility("
        $finalOut += "                    visible = globalError != null,"
        $finalOut += "                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),"
        $finalOut += "                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),"
        $finalOut += "                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)"
        $finalOut += "                ) {"
        $finalOut += "                    Box("
        $finalOut += "                        modifier = Modifier.clip(CutCornerShape(8.dp)).background(Color(0xFFB91C1C)).border(1.dp, Color(0xFFFCA5A5), CutCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 10.dp)"
        $finalOut += "                    ) {"
        $finalOut += "                        Text(text = globalError ?: `"`", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)"
        $finalOut += "                    }"
        $finalOut += "                }"
        $skipNextBrace = $true
    }
    if ($skipNextBrace -and $line -match '^\s*\}\s*$') {
        $skipNextBrace = $false
        $finalOut = $finalOut[0..($finalOut.Length-2)]
    }
}

Set-Content -Path $appFile -Value $finalOut -Encoding UTF8
