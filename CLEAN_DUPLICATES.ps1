# CLEAN_DUPLICATES.ps1 - Fix Android duplicate resources error
# Fix: [mipmap-hdpi-v4/ic_launcher_adaptive_back] .webp + .png = Duplicate resources
# by Bishop Dr. David Sanda - v1.0.14d - August 2026 - Free for Jesus

Write-Host "=== Sanda Data Saver - Clean Duplicate Mipmap Resources ===" -ForegroundColor Cyan
Write-Host "Fixes: Error: Duplicate resources .webp + .png with same base name" -ForegroundColor Yellow

# Folders to clean - add more if needed
$foldersToClean = @(
    "C:\Projects\SandaPC",
    "C:\Android\AndriodStudioProjects\SandaDataSaver",
    "C:\Android\AndroidStudioProjects\SandaDataSaver"
)

foreach ($projectRoot in $foldersToClean) {
    if (-not (Test-Path $projectRoot)) {
        Write-Host "`nSkipping (not found): $projectRoot" -ForegroundColor Gray
        continue
    }

    Write-Host "`n--- Cleaning: $projectRoot ---" -ForegroundColor Green
    $resPath = Join-Path $projectRoot "app\src\main\res"
    if (-not (Test-Path $resPath)) {
        # Try alternative path for C:\Projects\SandaPC which is android-app/app/src/main/res
        $resPath = Join-Path $projectRoot "android-app\app\src\main\res"
        if (-not (Test-Path $resPath)) {
            $resPath = Join-Path $projectRoot "app\src\main\res"
            # For C:\Projects\SandaPC the actual mipmap is in android-app folder, but also check both
        }
    }

    # Find all possible res paths in this project
    $resPaths = @()
    if (Test-Path (Join-Path $projectRoot "app\src\main\res")) { $resPaths += Join-Path $projectRoot "app\src\main\res" }
    if (Test-Path (Join-Path $projectRoot "android-app\app\src\main\res")) { $resPaths += Join-Path $projectRoot "android-app\app\src\main\res" }

    if ($resPaths.Count -eq 0) {
        Write-Host "  No res folder found in $projectRoot" -ForegroundColor Red
        continue
    }

    foreach ($res in $resPaths) {
        Write-Host "  Scanning: $res" -ForegroundColor White

        # Find duplicates: same directory + same base name but different extension (webp vs png)
        $allFiles = Get-ChildItem -Path $res -Recurse -File -Include "*.png","*.webp","*.jpg" | Where-Object { $_.Directory.Name -like "mipmap-*" }

        $groups = $allFiles | Group-Object -Property { $_.DirectoryName + "\" + $_.BaseName } | Where-Object { $_.Count -gt 1 }

        if ($groups.Count -eq 0) {
            Write-Host "    No duplicates found - CLEAN" -ForegroundColor Green
        } else {
            Write-Host "    Found $($groups.Count) duplicate groups" -ForegroundColor Yellow
            foreach ($g in $groups) {
                $groupFiles = $g.Group | Sort-Object Extension
                Write-Host "    DUPLICATE: $($g.Name)" -ForegroundColor Yellow
                foreach ($f in $groupFiles) { Write-Host "      - $($f.Name) ($($f.Length) bytes)" }

                # Keep WEBP (smallest, modern), delete PNG/JPG
                $webp = $groupFiles | Where-Object Extension -eq ".webp" | Select-Object -First 1
                $toDelete = $groupFiles | Where-Object Extension -ne ".webp"

                if ($webp -and $toDelete) {
                    foreach ($del in $toDelete) {
                        Write-Host "      -> Deleting duplicate PNG: $($del.FullName)" -ForegroundColor Red
                        Remove-Item $del.FullName -Force -ErrorAction SilentlyContinue
                    }
                } else {
                    # If no webp, keep first png, delete rest
                    $keep = $groupFiles | Select-Object -First 1
                    $delRest = $groupFiles | Select-Object -Skip 1
                    foreach ($del in $delRest) {
                        Write-Host "      -> Deleting extra: $($del.FullName)" -ForegroundColor Red
                        Remove-Item $del.FullName -Force -ErrorAction SilentlyContinue
                    }
                }
            }
        }

        # Final list
        $remaining = Get-ChildItem -Path $res -Recurse -File | Where-Object { $_.Directory.Name -like "mipmap-*" } | Sort-Object FullName
        Write-Host "    Remaining mipmap files in $res : $($remaining.Count)" -ForegroundColor Cyan
        $remaining | ForEach-Object { Write-Host "      $($_.Directory.Name)\$($_.Name)" }
    }
}

Write-Host "`n=== Cleaning build caches (optional) ===" -ForegroundColor Cyan
foreach ($projectRoot in $foldersToClean) {
    if (Test-Path $projectRoot) {
        $gradlewBat = Join-Path $projectRoot "gradlew.bat"
        $gradlew = Join-Path $projectRoot "gradlew"
        $appGradlewBat = Join-Path $projectRoot "app\gradlew.bat"
        # Try to clean if gradlew exists
        if (Test-Path $gradlewBat) {
            Write-Host "  Cleaning: $projectRoot (gradlew clean)" -ForegroundColor Gray
            Push-Location $projectRoot
            try { & .\gradlew.bat clean --no-daemon -q 2>$null; Write-Host "    Cleaned" -ForegroundColor Green } catch { Write-Host "    Skip gradle clean" -ForegroundColor Gray }
            Pop-Location
        } elseif (Test-Path (Join-Path $projectRoot "android-app\gradlew.bat")) {
            Write-Host "  Cleaning: $projectRoot\android-app" -ForegroundColor Gray
            Push-Location (Join-Path $projectRoot "android-app")
            try { & .\gradlew.bat clean --no-daemon -q 2>$null; Write-Host "    Cleaned" -ForegroundColor Green } catch {}
            Pop-Location
        }
    }
}

Write-Host "`n=== DONE ===" -ForegroundColor Green
Write-Host "Now in Android Studio: Build -> Clean Project -> Generate Signed Bundle / APK..."
Write-Host "All mipmap duplicates removed - will show new Sanda logo 14KB not old 1KB"
Write-Host "Freely received, freely give. Matthew 10:8" -ForegroundColor Cyan
Pause
