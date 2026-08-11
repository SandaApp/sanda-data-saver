@echo off
:: =========================================================================
:: Sanda Data Saver — Standalone EXE One-Click Compiler
:: By Bishop David Sanda
:: =========================================================================
title Sanda Data Saver EXE Compiler

echo ✝️ Sanda Data Saver — PC App v1.0.1 Compiler ✝️
echo "Freely Given, Freely Received"
echo ===================================================
echo.

:: Step 1: Check Python installation
where python >nul 2>and1
if %errorlevel% neq 0 (
    echo ❌ ERROR: Python is not installed or not added to your system PATH!
    echo Please install Python 3.10+ and make sure to check "Add Python to PATH" during setup.
    pause
    exit
)
echo [OK] Python detected successfully.

:: Step 2: Install required libraries
echo [INFO] Verifying and installing compilation libraries...
python -m pip install --upgrade pip
pip install pystray pillow pyinstaller

:: Step 3: Check for Icon Asset
set ICON_FLAG=
if exist "assets\icon.ico" (
    echo [OK] Custom icon found at assets\icon.ico
    set ICON_FLAG=--icon=assets\icon.ico
) else if exist "sanda_icon.ico" (
    echo [OK] Custom icon found at sanda_icon.ico
    set ICON_FLAG=--icon=sanda_icon.ico
) else (
    echo ⚠️ WARNING: No custom icon.ico file found!
    echo Sanda will be compiled using the default Windows application icon.
    echo To use Sanda's custom icon, convert your PNG to sanda_icon.ico and place it in this folder.
    echo.
    set /p choice="Build with default icon? (Y/N): "
    if /I "%choice%" neq "Y" (
        echo Compilation cancelled by user.
        pause
        exit
    )
)

:: Step 4: Run PyInstaller
echo.
echo [INFO] Compiling sanda_pc_app.py into a windowless, single-file EXE...
echo [INFO] This may take a minute. Please wait...
echo.

pyinstaller --noconsole --onefile --uac-admin %ICON_FLAG% --name=SandaDataSaver sanda_pc_app.py

if %errorlevel% equ 0 (
    echo.
    echo ===================================================
    echo 🎉 SUCCESS! Sanda Data Saver has compiled cleanly!
    echo ===================================================
    echo 📁 Your ready-to-run file is saved inside the 'dist' folder:
    echo    =^> %~dp0dist\SandaDataSaver.exe
    echo.
    echo 📦 You can now run Inno Setup on 'sanda_pc_setup.iss' to build the installer.
) else (
    echo.
    echo ❌ ERROR: Compilation failed!
    echo Please check the error log above. Common fixes:
    echo   1. Close any running SandaDataSaver.exe processes.
    echo   2. Ensure all file paths and script names are correct.
)

pause
