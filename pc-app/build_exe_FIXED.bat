@echo off
cd /d "%~dp0"
title Sanda Data Saver EXE Compiler v1.0.7 FINAL MERGE

echo.
echo  =========================================================
echo   Sanda Data Saver -- PC App Compiler v1.0.7 FINAL MERGE
echo   Logo-working + Cleaner-working merged
echo  =========================================================
echo.

set APP_VERSION=1.0.7
if exist "VERSION.txt" (
    set /p APP_VERSION=<"VERSION.txt"
    echo  [OK] Version: %APP_VERSION%
) else (
    echo 1.0.7>"VERSION.txt"
    echo  [OK] Version: %APP_VERSION%
)

echo.
taskkill /F /IM SandaDataSaver.exe >nul 2>&1
if exist "dist\SandaDataSaver.exe" del /f /q "dist\SandaDataSaver.exe" >nul 2>&1
if exist "build" rmdir /s /q "build" >nul 2>&1
if exist "*.spec" del /q "*.spec" >nul 2>&1

echo  [INFO] Installing libs...
pip install pystray pillow pyinstaller --quiet

echo  [INFO] Compiling...
pyinstaller --noconsole --onefile --uac-admin --icon="assets\sanda_icon.ico" --add-data "assets\sanda_logo.png;assets" --add-data "assets\sanda_icon.ico;assets" --add-data "VERSION.txt;." --name=SandaDataSaver sanda_pc_app.py

if %errorlevel% neq 0 (
    echo  ERROR: Build failed!
    pause
    exit /b
)

echo.
echo  [INFO] Copying logo files to dist folder for _install_logo...
if not exist "dist" mkdir dist
if exist "assets\sanda_logo.png" copy /Y "assets\sanda_logo.png" "dist\sanda_logo.png" >nul
if exist "assets\sanda_icon.ico" copy /Y "assets\sanda_icon.ico" "dist\sanda_icon.ico" >nul
if exist "sanda_logo.png" copy /Y "sanda_logo.png" "dist\sanda_logo.png" >nul
if exist "sanda_icon.ico" copy /Y "sanda_icon.ico" "dist\sanda_icon.ico" >nul

echo  [OK] Logo files copied to dist\
echo.
echo  =========================================================
echo   SUCCESS! v%APP_VERSION% - dist\SandaDataSaver.exe
echo   + dist\sanda_logo.png (for logo install)
echo  =========================================================
start "" explorer "%~dp0dist"
pause
