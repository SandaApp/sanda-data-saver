# Cleanup script for C:\Projects\SandaPC - removes junk from root that shouldn't be pushed

cd C:\Projects\SandaPC

Write-Host "=== Cleaning junk files from root (should only be in pc-app/) ==="

# Remove junk files from root if they exist
$junkFiles = @(
    "build.yml",
    "build_exe_FIXED.bat",
    "RESET_BLOCKED_APPS.bat",
    "reset_blocked_apps.bat",
    "sanda_logo.png",
    "SandaPC_*.zip",
    "pc-app_GOLD_MASTER*.zip",
    "SandaPC_GitHub_Repo*.zip",
    "*.spec",
    "sandadatasaver.spec",
    "SandaDataSaver.spec"
)

foreach ($pattern in $junkFiles) {
    Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "Removing file: $($_.FullName)"
        Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
        git rm --cached $_.Name -ErrorAction SilentlyContinue
    }
}

# Remove build and dist folders from root (should only be in pc-app/ during build, not committed)
$junkDirs = @("build", "dist", "home")
foreach ($dir in $junkDirs) {
    if (Test-Path $dir) {
        Write-Host "Removing dir: $dir"
        Remove-Item -Recurse -Force $dir -ErrorAction SilentlyContinue
        git rm -r --cached $dir -ErrorAction SilentlyContinue
    }
}

# Ensure .gitignore has correct ignores
Write-Host "`n=== Checking .gitignore ==="
if (-not (Select-String -Path .gitignore -Pattern "build/" -ErrorAction SilentlyContinue)) {
    Add-Content .gitignore "`nbuild/`ndist/`n*.spec`n*.zip`n"
}

# Ensure .github/workflows/build.yml exists
if (-not (Test-Path ".github\workflows\build.yml")) {
    Write-Host "ERROR: .github/workflows/build.yml missing! Extracting from backup..."
    # Try to find it in pc-app_GOLD_MASTER zip or full repo zip
    if (Test-Path "SandaPC_GitHub_Repo_v1_0_12i.zip") {
        Expand-Archive -Force SandaPC_GitHub_Repo_v1_0_12i.zip -DestinationPath temp_restore
        Copy-Item -Recurse -Force temp_restore\sanda-data-saver\.github .\.github
        Remove-Item -Recurse -Force temp_restore
    }
}

Write-Host "`n=== Current root files (should be clean) ==="
Get-ChildItem -File | Where-Object { $_.Name -notlike ".*" } | Select-Object Name

Write-Host "`n=== .github/workflows/ ==="
Get-ChildItem .github\workflows\ -ErrorAction SilentlyContinue | Select-Object Name

Write-Host "`n=== pc-app/ ==="
Get-ChildItem pc-app\ | Select-Object Name

Write-Host "`n=== Ready to commit clean ==="
git status
