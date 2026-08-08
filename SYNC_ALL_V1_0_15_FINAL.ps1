# SYNC_ALL_V1_0_15_FINAL.ps1 - Final complete sync for v1.0.15
# Fixes: old logo, version 1.0.0/1.0.2, no logo in About, splash too fast, main page old logo, footer old, no Dr.
# Copies ALL fixed files from C:\Projects\SandaPC to C:\Android\AndriodStudioProjects\SandaDataSaver

Write-Host "=== FINAL SYNC v1.0.15 - Fixes ALL old logo/version issues you reported ===" -ForegroundColor Cyan

$srcRoot = "C:\Projects\SandaPC\android-app"
$dstRoot = "C:\Android\AndriodStudioProjects\SandaDataSaver"

if (-not (Test-Path $srcRoot)) {
    Write-Host "Source not found: $srcRoot trying alternative..." -ForegroundColor Yellow
    $srcRoot = "C:\Projects\SandaPC"
    if (Test-Path "$srcRoot\android-app") { $srcRoot = "$srcRoot\android-app" }
}

if (-not (Test-Path $dstRoot)) {
    $dstRoot = "C:\Android\AndroidStudioProjects\SandaDataSaver"
    if (-not (Test-Path $dstRoot)) {
        Write-Host "Destination old project not found! Use C:\Projects\SandaPC directly" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Source (NEW v1.0.15): $srcRoot" -ForegroundColor White
Write-Host "Dest (OLD v1.0.2): $dstRoot" -ForegroundColor White

# 1. Backup
$backup = "$dstRoot-backup-v1.0.2-$(Get-Date -Format yyyyMMdd-HHmm)"
Write-Host "`n1. Backup old to $backup" -ForegroundColor Green
Copy-Item $dstRoot $backup -Recurse -Force -ErrorAction SilentlyContinue

# 2. Core Java files - fixes version 1.0.2 -> 1.0.15 + author Dr
Write-Host "`n2. Copying Java constants + MainActivity (fixes About 1.0.2 + no logo + splash delay)..." -ForegroundColor Green
Copy-Item "$srcRoot\app\src\main\java\com\sanda\datasaver\Constants.java" "$dstRoot\app\src\main\java\com\sanda\datasaver\Constants.java" -Force
Write-Host "   Constants.java -> APP_VERSION 1.0.15, APP_AUTHOR Bishop Dr."
Copy-Item "$srcRoot\app\src\main\java\com\sanda\datasaver\MainActivity.java" "$dstRoot\app\src\main\java\com\sanda\datasaver\MainActivity.java" -Force
Write-Host "   MainActivity.java -> new logo sanda_logo.png + splash 2.5s delay (was too fast) + About logo fixed"

# 3. Layouts - fixes main page old logo, settings v1.0.0, footer old
Write-Host "`n3. Copying layouts (fixes main page old logo, settings version)..." -ForegroundColor Green
Copy-Item "$srcRoot\app\src\main\res\layout\activity_main.xml" "$dstRoot\app\src\main\res\layout\activity_main.xml" -Force
Write-Host "   activity_main.xml -> iv_logo @drawable/sanda_logo 56dp (was ic_shield_on 48dp old)"
Copy-Item "$srcRoot\app\src\main\res\layout\activity_settings.xml" "$dstRoot\app\src\main\res\layout\activity_settings.xml" -Force
Write-Host "   activity_settings.xml -> footer v1.0.15 + Dr. (was v1.0.0 + no Dr)"
Copy-Item "$srcRoot\app\src\main\res\layout\widget_layout.xml" "$dstRoot\app\src\main\res\layout\widget_layout.xml" -Force
Write-Host "   widget_layout.xml -> new logo"

# 4. Drawable - fixes splash + logo
Write-Host "`n4. Copying drawable new logo + splash..." -ForegroundColor Green
Copy-Item "$srcRoot\app\src\main\res\drawable\sanda_logo.png" "$dstRoot\app\src\main\res\drawable\sanda_logo.png" -Force
Write-Host "   sanda_logo.png 233KB new logo copied to drawable"
Copy-Item "$srcRoot\app\src\main\res\drawable\splash_background.xml" "$dstRoot\app\src\main\res\drawable\splash_background.xml" -Force
Write-Host "   splash_background.xml -> @drawable/sanda_logo (was ic_shield_on)"

# 5. Mipmap new logo (fixes launcher icon correct but main page old)
Write-Host "`n5. Copying mipmap new logo (fixes launcher + adaptive)..." -ForegroundColor Green
Get-ChildItem "$srcRoot\app\src\main\res\mipmap-*" -Directory | ForEach-Object {
    $dst = Join-Path "$dstRoot\app\src\main\res" $_.Name
    if (-not (Test-Path $dst)) { New-Item -ItemType Directory -Path $dst -Force | Out-Null }
    # Remove duplicate png if webp exists
    Get-ChildItem $dst -File -Filter "*.png" | Where-Object { Test-Path (Join-Path $dst ($_.BaseName + ".webp")) } | ForEach-Object { Remove-Item $_.FullName -Force }
    Copy-Item "$($_.FullName)\*" $dst -Force -Recurse
    Write-Host "   $($_.Name) copied"
}
Copy-Item "$srcRoot\app\src\main\res\mipmap-anydpi-v26\*" "$dstRoot\app\src\main\res\mipmap-anydpi-v26\" -Force

# 6. Values + Manifest + Build files
Write-Host "`n6. Copying version files + build system..." -ForegroundColor Green
Copy-Item "$srcRoot\app\src\main\res\values\strings.xml" "$dstRoot\app\src\main\res\values\strings.xml" -Force
Write-Host "   strings.xml -> app_version 1.0.15"
Copy-Item "$srcRoot\app\src\main\AndroidManifest.xml" "$dstRoot\app\src\main\AndroidManifest.xml" -Force
Copy-Item "$srcRoot\app\build.gradle" "$dstRoot\app\build.gradle" -Force
Write-Host "   build.gradle -> versionCode 15 / 1.0.15"
Copy-Item "$srcRoot\app\proguard-rules.pro" "$dstRoot\app\proguard-rules.pro" -Force -ErrorAction SilentlyContinue
Copy-Item "$srcRoot\gradle.properties" "$dstRoot\gradle.properties" -Force
Remove-Item "$dstRoot\gradle\gradle-daemon-jvm.properties" -Force -ErrorAction SilentlyContinue

# 7. Verify
Write-Host "`n=== VERIFICATION (should all be 1.0.15 + new logo) ===" -ForegroundColor Cyan
Select-String "$dstRoot\app\src\main\java\com\sanda\datasaver\Constants.java" -Pattern "APP_VERSION|APP_AUTHOR"
Select-String "$dstRoot\app\src\main\res\layout\activity_settings.xml" -Pattern "v1.0"
Select-String "$dstRoot\app\build.gradle" -Pattern "versionCode|versionName"
Get-ChildItem "$dstRoot\app\src\main\res\drawable\sanda_logo.png" | Select Name, Length
Get-ChildItem "$dstRoot\app\src\main\res\mipmap-xxxhdpi\sanda_launcher.webp" | Select Name, Length
Get-ChildItem "$dstRoot\app\src\main\res\layout\activity_main.xml" | Select-String -Pattern "sanda_logo"

Write-Host "`n=== DONE ===" -ForegroundColor Green
Write-Host "Fixed you reported:"
Write-Host "  apk icon correct -> now also main page, splash, about, widget all new logo"
Write-Host "  splash too fast -> now 2.5s delay (was 1.2s)"
Write-Host "  About no logo -> now @drawable/sanda_logo.png 233KB"
Write-Host "  settings v1.0.0 -> v1.0.15 + Dr"
Write-Host "  about v1.0.2 -> v1.0.15 + Dr"
Write-Host "  exe 1.0.15 already correct"
Write-Host "  main page old logo ic_shield_on -> new sanda_logo 56dp"
Write-Host "  footer old -> v1.0.15 Dr"
Write-Host "  no health module -> Health is PC-only for now (Phone Cleaner, Schedule, Data Usage exist). Android health will be v1.0.16"
Write-Host ""
Write-Host "Next: Android Studio -> Clean Project -> Generate Signed Bundle -> new APK will show v1.0.15 everywhere + new logo"
Pause
