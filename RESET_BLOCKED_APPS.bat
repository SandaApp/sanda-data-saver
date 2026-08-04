@echo off
echo Resetting Sanda Data Saver configs to fix Blocked Apps...
taskkill /F /IM SandaDataSaver.exe >nul 2>&1
timeout /t 2 >nul
del /f /q "%APPDATA%\SandaDataSaver\blocked_apps.json" >nul 2>&1
del /f /q "%APPDATA%\SandaDataSaver\config.json" >nul 2>&1
echo [OK] Deleted old configs. Next launch will recreate defaults (9 apps).
echo Now rebuild and run:
echo   build_exe_FIXED.bat
pause
