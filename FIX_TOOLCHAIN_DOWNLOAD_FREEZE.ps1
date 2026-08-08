# FIX_TOOLCHAIN_DOWNLOAD_FREEZE.ps1
# Fixes: > Starting Daemon > Downloading toolchain from URI https://api.foojay.io/disco/v3.0/ids/39701d92e1756bb2f141eb67cd4c660
# Freezes because gradle/gradle-daemon-jvm.properties forces JDK 21 download from Foojay

Write-Host "=== Fix Gradle Toolchain Download Freeze ===" -ForegroundColor Cyan
Write-Host "Error you saw: Downloading toolchain from https://api.foojay.io/disco/v3.0/ids/39701d92e1756bb2f141eb67cd4c660" -ForegroundColor Yellow

$projects = @(
    "C:\Android\AndriodStudioProjects\SandaDataSaver",
    "C:\Android\AndroidStudioProjects\SandaDataSaver",
    "C:\Projects\SandaPC\android-app",
    "C:\Projects\SandaPC"
)

foreach ($root in $projects) {
    if (-not (Test-Path $root)) { continue }
    Write-Host "`n--- Checking $root ---" -ForegroundColor Green

    # 1. Find gradle-daemon-jvm.properties files
    $daemonFiles = Get-ChildItem -Path $root -Recurse -Filter "gradle-daemon-jvm.properties" -ErrorAction SilentlyContinue
    foreach ($f in $daemonFiles) {
        Write-Host "  Found daemon file: $($f.FullName)" -ForegroundColor Yellow
        Get-Content $f.FullName | Write-Host
        Write-Host "  -> Deleting it (forces Android Studio to use embedded jbr-17)" -ForegroundColor Red
        Remove-Item $f.FullName -Force -ErrorAction SilentlyContinue
    }

    # 2. Update gradle.properties to disable auto-download
    $gradleProps = Join-Path $root "gradle.properties"
    if (-not (Test-Path $gradleProps)) {
        $gradleProps = Join-Path $root "android-app\gradle.properties"
    }
    if (Test-Path $gradleProps) {
        Write-Host "  Updating $gradleProps to disable auto-download" -ForegroundColor White
        $content = Get-Content $gradleProps -Raw -ErrorAction SilentlyContinue
        if ($content -notmatch "org.gradle.java.installations.auto-download") {
            Add-Content $gradleProps "`n# FIX: Disable toolchain auto-download from Foojay (was freezing)"
            Add-Content $gradleProps "org.gradle.java.installations.auto-download=false"
            Add-Content $gradleProps "org.gradle.java.installations.auto-detect=true"
            Write-Host "    Added auto-download=false" -ForegroundColor Green
        } else {
            Write-Host "    Already has auto-download fix" -ForegroundColor Gray
        }
        Get-Content $gradleProps | Select-Object -Last 10 | Write-Host
    } else {
        # Create new gradle.properties in project root
        $newProps = Join-Path $root "gradle.properties"
        if (-not (Test-Path $newProps)) {
            $parentProps = Join-Path (Split-Path $root -Parent) "gradle.properties"
        }
    }
}

Write-Host "`n=== Also fix gradle.properties in C:\Android\... ===" -ForegroundColor Cyan
$extraPaths = @(
    "C:\Android\AndriodStudioProjects\SandaDataSaver\gradle.properties",
    "C:\Android\AndriodStudioProjects\SandaDataSaver\android-app\gradle.properties",
    "C:\Projects\SandaPC\android-app\gradle.properties"
)

foreach ($gp in $extraPaths) {
    if (Test-Path $gp) {
        $c = Get-Content $gp -Raw
        if ($c -notmatch "auto-download") {
            Write-Host "  Adding fix to $gp" -ForegroundColor White
            Add-Content $gp "`norg.gradle.java.installations.auto-download=false"
            Add-Content $gp "org.gradle.java.installations.auto-detect=true"
        }
    }
}

Write-Host "`n=== Kill Gradle Daemons ===" -ForegroundColor Cyan
try {
    Push-Location "C:\Android\AndriodStudioProjects\SandaDataSaver"
    if (Test-Path ".\gradlew.bat") {
        Write-Host "  Running gradlew --stop"
        & .\gradlew.bat --stop 2>&1 | Write-Host
    }
    Pop-Location
} catch {}

try {
    Push-Location "C:\Projects\SandaPC\android-app"
    if (Test-Path ".\gradlew.bat") {
        & .\gradlew.bat --stop 2>&1 | Write-Host
    }
    Pop-Location
} catch {}

Write-Host "`n=== NEXT STEPS IN ANDROID STUDIO ===" -ForegroundColor Green
Write-Host "1. Open Android Studio"
Write-Host "2. File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle"
Write-Host "3. Under 'Gradle JDK' -> Select 'jbr-17' or 'Embedded JDK 17' or 'C:\Program Files\Android\Android Studio\jbr' -> NOT 'Download JDK'"
Write-Host "4. Click Apply -> OK"
Write-Host "5. File -> Invalidate Caches -> Invalidate and Restart"
Write-Host "6. Re-open project C:\Android\AndriodStudioProjects\SandaDataSaver"
Write-Host "7. Bottom bar should say 'Gradle Sync' WITHOUT 'Downloading toolchain'"
Write-Host "8. Then Build -> Generate Signed Bundle / APK..."

Write-Host "`nDone! No more Foojay download freeze." -ForegroundColor Green
Pause
