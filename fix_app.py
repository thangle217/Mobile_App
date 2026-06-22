import sys

def main():
    with open('app/src/main/java/com/example/myapplication/ui/app/RentalManagerApp.kt', 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    out = []
    skip = False
    for i, line in enumerate(lines):
        # 1. Add onShowError to MainShell caller
        if 'else -> ModuleScreen(repository, session, screen, onLogout)' in line:
            line = line.replace('else -> ModuleScreen(repository, session, screen, onLogout)', 'else -> ModuleScreen(repository, session, screen, onLogout, { msg -> globalError = msg; errorKey++ })')
        
        # 2. Add onShowError to ModuleScreen signature
        if 'onSessionExpired: () -> Unit' in line and i < 250:
            line = line.replace('onSessionExpired: () -> Unit', 'onSessionExpired: () -> Unit,\n    onShowError: (String) -> Unit')
        
        # 3. Remove actionError declaration
        if 'var actionError by remember { mutableStateOf<String?>(null) }' in line:
            continue
            
        # 4. Remove actionError?.let { ... } UI
        if 'actionError?.let {' in line:
            skip = True
            continue
        if skip:
            if '}' in line and 'item {' not in line:
                skip = False
            continue
            
        # 5. Remove actionError = null
        if 'actionError = null' in line:
            continue
            
        # 6. Replace actionError assignment in onFailure
        if '.onFailure { actionError =' in line:
            # .onFailure { actionError = it.message ?: "error msg" } -> .onFailure { onShowError(it.message ?: "error msg") }
            parts = line.split('actionError =')
            prefix = parts[0]
            rest = parts[1].strip() # 'it.message ?: "error msg" }'
            rest = rest[:-1].strip() # 'it.message ?: "error msg"'
            line = prefix + 'onShowError(' + rest + ') }\n'
            
        # 7. RentRequestDialog fix caller
        if 'onSubmit = { duration, note ->' in line:
            line = line.replace('onSubmit = { duration, note ->', 'onSubmit = { moveInDate, expectedMoveOutDate, note ->')
        if 'repository.requestRoom(room.id, session, duration, note)' in line:
            line = line.replace('repository.requestRoom(room.id, session, duration, note)', 'repository.requestRoom(room.id, session, moveInDate, expectedMoveOutDate, note)')
            
        # 8. RegisterServiceDialog fix caller
        if 'service = service,' in line and lines[i-1].strip() == 'RegisterServiceDialog(':
            line = line + '            rooms = rooms,\n'
        if 'onSubmit = { note ->' in line and 'val regItem = RentalItem(' in lines[i+1]:
            line = line.replace('onSubmit = { note ->', 'onSubmit = { roomId, period, note ->')
        if '"roomId" to service.detail("roomId")' in line and 'val regItem = RentalItem(' in lines[i-8]:
            line = line.replace('"roomId" to service.detail("roomId")', '"roomId" to roomId,\n                        "period" to period')
            
        # 9. Clean up RoomTypes
        if 'AppScreen.RoomTypes ->' in line:
            continue
        if 'AppScreen.RoomTypes,' in line:
            line = line.replace('AppScreen.RoomTypes,', '')
        if ', AppScreen.RoomTypes' in line:
            line = line.replace(', AppScreen.RoomTypes', '')
            
        out.append(line)
        
    with open('app/src/main/java/com/example/myapplication/ui/app/RentalManagerApp.kt', 'w', encoding='utf-8') as f:
        f.writelines(out)

main()
