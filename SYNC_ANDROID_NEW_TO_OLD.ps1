# SYNC_ANDROID_NEW_TO_OLD.ps1
# Sync new fixed code from C:\Projects\SandaPC (v1.0.14 new logo) to old AndroidStudioProjects folder
# Fixes: old logo, version 1.0.2, proguard missing, toolchain freeze

Write-Host "=== Sync Android New (C:\Projects\SandaPC) -> Old (C:\Android\AndriodStudioProjects\SandaDataSaver) ===" -ForegroundColor Cyan

$source = "C:\Projects\SandaPC\android-app"
$dest = "C:\Android\AndriodStudioProjects\SandaDataSaver"

if (-not (Test-Path $source)) {
    Write-Host "Source not found: $source. Trying sanda-data-saver..." -ForegroundColor Yellow
    $source = "C:\Projects\SandaPC"
    if (-not (Test-Path "$source\android-app")) {
        Write-Host "Cannot find android-app folder!" -ForegroundColor Red
        Pause
        exit 1
    }
}

if (-not (Test-Path $dest)) {
    Write-Host "Destination not found: $dest" -ForegroundColor Red
    Write-Host "Trying C:\Android\AndroidStudioProjects\SandaDataSaver" -ForegroundColor Yellow
    $dest = "C:\Android\AndroidStudioProjects\SandaDataSaver"
    if (-not (Test-Path $dest)) {
        Write-Host "No old project found! Will build from C:\Projects\SandaPC directly" -ForegroundColor Yellow
        Pause
        exit 1
    }
}

Write-Host "Source: $source\app" -ForegroundColor White
Write-Host "Dest: $dest\app" -ForegroundColor White

# 1. Backup old project
$backup = "$dest-backup-$(Get-Date -Format yyyyMMdd-HHmm)"
Write-Host "`n1. Backing up old project to $backup" -ForegroundColor Green
Copy-Item -Path $dest -Destination $backup -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "   Backup done"

# 2. Copy build.gradle + proguard + keystore.properties.example + gradle.properties
Write-Host "`n2. Copying build system files..." -ForegroundColor Green
Copy-Item "$source\app\build.gradle" "$dest\app\build.gradle" -Force
Write-Host "   build.gradle -> versionCode 14 versionName 1.0.14"
Copy-Item "$source\app\proguard-rules.pro" "$dest\app\proguard-rules.pro" -Force -ErrorAction SilentlyContinue
Write-Host "   proguard-rules.pro fixed"
Copy-Item "$source\keystore.properties.example" "$dest\keystore.properties.example" -Force -ErrorAction SilentlyContinue
Copy-Item "$source\gradle.properties" "$dest\gradle.properties" -Force
Write-Host "   gradle.properties -> auto-download=false"

# Remove daemon file that causes Foojay download freeze
Remove-Item "$dest\gradle\gradle-daemon-jvm.properties" -Force -ErrorAction SilentlyContinue
Remove-Item "$source\gradle\gradle-daemon-jvm.properties" -Force -ErrorAction SilentlyContinue
Write-Host "   Removed gradle-daemon-jvm.properties (fixes Foojay JDK 21 download freeze)"

# 3. Copy mipmap new logo (fix old logo)
Write-Host "`n3. Copying new Sanda logo mipmap (fix old logo)..." -ForegroundColor Green
Get-ChildItem "$source\app\src\main\res\mipmap-*" -Directory | ForEach-Object {
    $srcMipmap = $_.FullName
    $dstMipmap = Join-Path "$dest\app\src\main\res" $_.Name
    if (-not (Test-Path $dstMipmap)) { New-Item -ItemType Directory -Path $dstMipmap -Force | Out-Null }
    # Delete duplicate png where webp exists first
    Get-ChildItem $dstMipmap -File -Filter "*.png" | Where-Object { Test-Path (Join-Path $dstMipmap ($_.BaseName + ".webp")) } | ForEach-Object { Remove-Item $_.FullName -Force; Write-Host "   Deleted duplicate PNG: $($_.Name)" }
    Copy-Item "$srcMipmap\*" $dstMipmap -Force -Recurse
    Write-Host "   Copied $($_.Name): sanda_launcher.webp 13KB new"
}
# Copy anydpi adaptive xml
Copy-Item "$source\app\src\main\res\mipmap-anydpi-v26\*" "$dest\app\src\main\res\mipmap-anydpi-v26\" -Force
Write-Host "   Copied mipmap-anydpi-v26 adaptive xml"

# 4. Copy strings.xml version bump (fix version 1.0.2 -> 1.0.14)
Write-Host "`n4. Copying strings.xml + AndroidManifest..." -ForegroundColor Green
Copy-Item "$source\app\src\main\res\values\strings.xml" "$dest\app\src\main\res\values\strings.xml" -Force
Write-Host "   strings.xml -> app_version 1.0.14, author Bishop Dr."
Copy-Item "$source\app\src\main\AndroidManifest.xml" "$dest\app\src\main\AndroidManifest.xml" -Force
Write-Host "   AndroidManifest.xml -> launcher_round fixed"

# 5. Copy images folder for GitHub (optional)
if (Test-Path "$source\..\images") {
    Write-Host "`n5. Copying GitHub images folder..." -ForegroundColor Green
    if (-not (Test-Path "$dest\..\images")) { New-Item -ItemType Directory -Path "$dest\..\images" -Force | Out-Null }
    Copy-Item "$source\..\images\*" "$dest\..\images\" -Force -ErrorAction SilentlyContinue
}

Write-Host "`n=== VERIFICATION ===" -ForegroundColor Cyan
Write-Host "Checking version in dest..."
Select-String "$dest\app\build.gradle" -Pattern "versionCode|versionName"
Select-String "$dest\app\src\main\res\values\strings.xml" -Pattern "app_version"
Get-ChildItem "$dest\app\src\main\res\mipmap-xxxhdpi\sanda_launcher.webp" | Select Name, Length
# Should be ~13762 bytes new logo, not 3218 old

Write-Host "`n=== DONE ===" -ForegroundColor Green
Write-Host "Old project now has:"
Write-Host "  - New logo mipmap 14KB (was 1KB old)"
Write-Host "  - Version 1.0.14 (was 1.0.2)"
Write-Host "  - ProGuard file fixed"
Write-Host "  - Toolchain freeze fixed"
Write-Host "  - Health: Note Android health module is PC-only for now (v1.0.14). PC has Health Reminder with Bible verses. Android will get health in v1.0.15."
Write-Host ""
Write-Host "Next: Open Android Studio -> File -> Open -> $dest -> Wait Sync -> Build -> Generate Signed Bundle"
Write-Host "If duplicate error still, run CLEAN_DUPLICATES.ps1 first"
Pause
