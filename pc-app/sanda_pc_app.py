import os
import shutil
import subprocess
import ctypes
import sys
import glob
import threading
import time
import json
import winreg
from pathlib import Path
from datetime import datetime
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext, filedialog
import pystray
from PIL import Image, ImageDraw, ImageFont, ImageTk

# ============================================
# BRANDING
# ============================================
APP_BRAND_NAME    = "Sanda Data Saver"
APP_VERSION       = "1.0.1"
APP_TAGLINE       = "Smart Data. Your Control."
APP_AUTHOR        = "Bishop David Sanda"
APP_COLOR_PRIMARY = "#00C9FF"
APP_COLOR_ACCENT  = "#FF6B6B"
APP_COLOR_SUCCESS = "#00FF88"
APP_COLOR_BG      = "#0D1117"
APP_COLOR_BG2     = "#161B22"
APP_COLOR_BG3     = "#21262D"
APP_COLOR_TEXT    = "#E6EDF3"
APP_COLOR_MUTED   = "#8B949E"
APP_COLOR_WARNING = "#FFB347"

# ============================================
# PATHS & CONFIG
# ============================================
APP_NAME    = "SandaDataSaver"
APP_DIR     = os.path.join(
    os.environ.get("APPDATA", ""), APP_NAME)
CONFIG_FILE = os.path.join(APP_DIR, "config.json")
LOG_FILE    = os.path.join(APP_DIR, "activity.log")
APPS_FILE   = os.path.join(APP_DIR, "blocked_apps.json")
os.makedirs(APP_DIR, exist_ok=True)

# ============================================
# LOGGING
# ============================================
def write_log(message):
    timestamp = datetime.now().strftime(
        "%Y-%m-%d %H:%M:%S")
    line = f"[{timestamp}] {message}"
    try:
        with open(LOG_FILE, "a") as f:
            f.write(line + "\n")
    except Exception:
        pass
    return line

def clear_log():
    try:
        with open(LOG_FILE, "w") as f:
            f.write("")
    except Exception:
        pass

# ============================================
# ADMIN CHECK
# ============================================
def is_admin():
    try:
        return ctypes.windll.shell32.IsUserAnAdmin()
    except Exception:
        return False

def request_admin():
    if not is_admin():
        ctypes.windll.shell32.ShellExecuteW(
            None, "runas", sys.executable,
            f'"{os.path.abspath(sys.argv[0])}"',
            None, 1
        )
        sys.exit(0)

# ============================================
# CONFIG & STATE
# ============================================
DEFAULT_CONFIG = {
    "data_saver_on":         False,
    "notifications_enabled": True,
    "auto_clean_enabled":    False,
}

DEFAULT_BLOCKED_APPS = [
    {
        "name": "OneDrive",
        "path": os.path.expandvars(
            r"%LOCALAPPDATA%\Microsoft"
            r"\OneDrive\OneDrive.exe"),
        "enabled": True
    },
    {
        "name": "Microsoft Teams",
        "path": os.path.expandvars(
            r"%LOCALAPPDATA%\Microsoft"
            r"\Teams\current\Teams.exe"),
        "enabled": True
    },
    {
        "name": "Spotify",
        "path": os.path.expandvars(
            r"%APPDATA%\Spotify\Spotify.exe"),
        "enabled": True
    },
    {
        "name": "Discord",
        "path": os.path.expandvars(
            r"%LOCALAPPDATA%\Discord\Update.exe"),
        "enabled": True
    },
    {
        "name": "Dropbox",
        "path": os.path.expandvars(
            r"%LOCALAPPDATA%\Dropbox"
            r"\Update\DropboxUpdate.exe"),
        "enabled": True
    },
    {
        "name": "Slack",
        "path": os.path.expandvars(
            r"%LOCALAPPDATA%\slack\slack.exe"),
        "enabled": True
    },
    {
        "name": "Zoom",
        "path": os.path.expandvars(
            r"%APPDATA%\Zoom\bin\Zoom.exe"),
        "enabled": True
    },
    {
        "name": "Edge Updater",
        "path": (
            r"C:\Program Files (x86)\Microsoft"
            r"\EdgeUpdate\MicrosoftEdgeUpdate.exe"),
        "enabled": True
    },
    {
        "name": "Skype",
        "path": os.path.expandvars(
            r"%LOCALAPPDATA%\Microsoft"
            r"\SkypeApp\Skype.exe"),
        "enabled": True
    },
]

def load_config():
    try:
        with open(CONFIG_FILE, 'r') as f:
            data = json.load(f)
            for key, val in DEFAULT_CONFIG.items():
                if key not in data:
                    data[key] = val
            return data
    except (FileNotFoundError, json.JSONDecodeError):
        return DEFAULT_CONFIG.copy()

def save_config(config):
    with open(CONFIG_FILE, 'w') as f:
        json.dump(config, f, indent=2)

def load_blocked_apps():
    try:
        with open(APPS_FILE, 'r') as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        save_blocked_apps(DEFAULT_BLOCKED_APPS)
        return DEFAULT_BLOCKED_APPS.copy()

def save_blocked_apps(apps):
    with open(APPS_FILE, 'w') as f:
        json.dump(apps, f, indent=2)

# ============================================
# SERVICES & TASKS
# ============================================
SERVICES_TO_DISABLE = [
    ("wuauserv",         "Windows Update"),
    ("UsoSvc",           "Update Orchestrator"),
    ("WaaSMedicSvc",     "Windows Update Medic"),
    ("BITS",             "Background Transfer"),
    ("DoSvc",            "Delivery Optimization"),
    ("DiagTrack",        "Telemetry & Diagnostics"),
    ("dmwappushservice", "WAP Push Routing"),
    ("WSearch",          "Windows Search"),
    ("OneSyncSvc",       "Sync Host"),
    ("MapsBroker",       "Downloaded Maps Manager"),
    ("WerSvc",           "Windows Error Reporting"),
    ("lfsvc",            "Geolocation Service"),
    ("InstallService",   "MS Store Install Service"),
]

PROCESSES_TO_KILL = [
    "OneDrive.exe", "Teams.exe", "Spotify.exe",
    "Discord.exe",  "DropboxUpdate.exe",
    "Dropbox.exe",  "Slack.exe", "Zoom.exe",
    "Skype.exe",    "MicrosoftEdgeUpdate.exe",
    "GoogleDriveSync.exe",
]

SCHEDULED_TASKS = [
    r"\Microsoft\Windows\WindowsUpdate\Scheduled Start",
    r"\Microsoft\Windows\UpdateOrchestrator\Schedule Scan",
    r"\Microsoft\Windows\UpdateOrchestrator\USO_UxBroker",
    r"\Microsoft\Windows\Application Experience"
    r"\Microsoft Compatibility Appraiser",
    r"\Microsoft\Windows\Customer Experience "
    r"Improvement Program\Consolidator",
    r"\Microsoft\Windows\Customer Experience "
    r"Improvement Program\UsbCeip",
    r"\Microsoft\Windows\Maps\MapsUpdateTask",
    r"\Microsoft\Windows\Maps\MapsToastTask",
]

# ============================================
# CORE COMMANDS
# ============================================
def run_cmd(cmd, timeout=15):
    try:
        subprocess.run(
            cmd, shell=True,
            capture_output=True,
            text=True, timeout=timeout
        )
    except Exception:
        pass

def notify(title, message, icon_ref=None):
    config = load_config()
    if not config.get("notifications_enabled", True):
        return
    try:
        if icon_ref:
            icon_ref.notify(message, title)
    except Exception:
        pass

# ============================================
# PC CLEANER FUNCTIONS
# ============================================
def get_size(path):
    total = 0
    try:
        for dirpath, dirnames, filenames \
                in os.walk(path):
            for f in filenames:
                fp = os.path.join(dirpath, f)
                try:
                    total += os.path.getsize(fp)
                except (OSError, PermissionError):
                    pass
    except (OSError, PermissionError):
        pass
    return total

def bytes_to_readable(num_bytes):
    for unit in ['B', 'KB', 'MB', 'GB']:
        if num_bytes < 1024.0:
            return f"{num_bytes:.2f} {unit}"
        num_bytes /= 1024.0
    return f"{num_bytes:.2f} TB"

def safe_clean_folder(folder_path,
                       description,
                       log_callback=None):
    cleaned    = 0
    errors     = 0
    space_freed = 0
    if not os.path.exists(folder_path):
        msg = f"  [SKIP] {description} - Not found"
        if log_callback:
            log_callback(msg)
        return 0

    space_before = get_size(folder_path)
    for item in os.listdir(folder_path):
        item_path = os.path.join(
            folder_path, item)
        try:
            if (os.path.isfile(item_path)
                    or os.path.islink(item_path)):
                os.unlink(item_path)
                cleaned += 1
            elif os.path.isdir(item_path):
                shutil.rmtree(
                    item_path,
                    ignore_errors=True)
                cleaned += 1
        except (PermissionError, OSError):
            errors += 1

    space_after  = get_size(folder_path)
    space_freed  = space_before - space_after
    msg = (
        f"  ✅ {description}: "
        f"{cleaned} items removed "
        f"({bytes_to_readable(space_freed)} freed)"
        f", {errors} skipped"
    )
    if log_callback:
        log_callback(msg)
    write_log(msg)
    return space_freed

def clean_browser_caches(log_callback=None):
    local = os.environ.get("LOCALAPPDATA", "")
    total = 0
    browser_paths = {
        "Chrome Cache": [
            os.path.join(local,
                r"Google\Chrome\User Data"
                r"\Default\Cache"),
            os.path.join(local,
                r"Google\Chrome\User Data"
                r"\Default\Code Cache"),
            os.path.join(local,
                r"Google\Chrome\User Data"
                r"\Default\GPUCache"),
        ],
        "Edge Cache": [
            os.path.join(local,
                r"Microsoft\Edge\User Data"
                r"\Default\Cache"),
            os.path.join(local,
                r"Microsoft\Edge\User Data"
                r"\Default\Code Cache"),
        ],
        "Firefox Cache": glob.glob(
            os.path.join(local,
                r"Mozilla\Firefox\Profiles"
                r"\*\cache2")
        ),
    }
    for browser, paths in browser_paths.items():
        for path in paths:
            if os.path.exists(path):
                total += safe_clean_folder(
                    path, browser, log_callback)
    return total

def clean_thumbnail_cache(log_callback=None):
    explorer_path = os.path.join(
        os.environ.get("LOCALAPPDATA", ""),
        r"Microsoft\Windows\Explorer"
    )
    total = 0
    if os.path.exists(explorer_path):
        for f in os.listdir(explorer_path):
            if (f.startswith("thumbcache_")
                    and f.endswith(".db")):
                fp = os.path.join(
                    explorer_path, f)
                try:
                    size = os.path.getsize(fp)
                    os.unlink(fp)
                    total += size
                except (PermissionError, OSError):
                    pass
    msg = (f"  ✅ Thumbnail cache: "
           f"{bytes_to_readable(total)} freed")
    if log_callback:
        log_callback(msg)
    write_log(msg)
    return total

def flush_dns_cache(log_callback=None):
    try:
        subprocess.run(
            "ipconfig /flushdns",
            shell=True,
            capture_output=True)
        msg = "  ✅ DNS cache flushed"
        if log_callback:
            log_callback(msg)
        write_log(msg)
    except Exception:
        msg = "  ❌ Could not flush DNS"
        if log_callback:
            log_callback(msg)

def clean_windows_logs(log_callback=None):
    total = 0
    log_paths = [
        r"C:\Windows\Logs\CBS",
        r"C:\Windows\Logs\DISM",
    ]
    for path in log_paths:
        if os.path.exists(path):
            total += safe_clean_folder(
                path,
                f"Logs: {os.path.basename(path)}",
                log_callback)
    try:
        subprocess.run(
            'for /F "tokens=*" %G in '
            "('wevtutil el') do "
            'wevtutil cl "%G"',
            shell=True,
            capture_output=True,
            timeout=60)
        msg = "  ✅ Windows Event Logs cleared"
        if log_callback:
            log_callback(msg)
        write_log(msg)
    except Exception:
        msg = "  ⚠️ Could not clear event logs"
        if log_callback:
            log_callback(msg)
    return total

def run_full_clean(options=None,
                   log_callback=None,
                   done_callback=None):
    """
    Runs selected cleaning steps.
    """
    if options is None:
        options = {
            "temp": True, "prefetch": True, "update_cache": True,
            "recent": True, "browser": True, "logs": True, "thumb": True
        }

    total = 0
    steps = []

    if options.get("temp"):
        steps.append((
            "🗑️ [1/8] Cleaning User Temp...",
            lambda: safe_clean_folder(
                os.environ.get("TEMP", ""),
                "User Temp",
                log_callback)
        ))
        steps.append((
            "🗑️ [2/8] Cleaning Windows Temp...",
            lambda: safe_clean_folder(
                r"C:\Windows\Temp",
                "Windows Temp",
                log_callback)
        ))

    if options.get("prefetch"):
        steps.append((
            "🗑️ [3/8] Cleaning Prefetch...",
            lambda: safe_clean_folder(
                r"C:\Windows\Prefetch",
                "Prefetch",
                log_callback)
        ))

    if options.get("update_cache"):
        steps.append((
            "🗑️ [4/8] Cleaning Update Cache...",
            lambda: clean_update_cache(
                log_callback)
        ))

    if options.get("recent"):
        steps.append((
            "🗑️ [5/8] Cleaning Recent Files...",
            lambda: safe_clean_folder(
                os.path.join(
                    os.environ.get(
                        "APPDATA", ""),
                    r"Microsoft\Windows\Recent"),
                "Recent Files",
                log_callback)
        ))

    if options.get("browser"):
        steps.append((
            "🌐 [6/8] Cleaning Browser Caches...",
            lambda: clean_browser_caches(
                log_callback)
        ))

    if options.get("logs"):
        steps.append((
            "📋 [7/8] Cleaning Windows Logs...",
            lambda: clean_windows_logs(
                log_callback)
        ))

    if options.get("thumb"):
        steps.append((
            "🖼️ [8/8] Cleaning Thumbnail Cache...",
            lambda: clean_thumbnail_cache(
                log_callback)
        ))

    for step_msg, step_fn in steps:
        if log_callback:
            log_callback(step_msg)
        try:
            result = step_fn()
            if isinstance(result, (int, float)):
                total += result
        except Exception as e:
            if log_callback:
                log_callback(
                    f"  ⚠️ Error: {e}")
    if log_callback:
        log_callback(
            f"\n{'='*40}\n"
            f"✅ CLEANUP COMPLETE!\n"
            f"💾 Total freed: "
            f"{bytes_to_readable(total)}\n"
            f"{'='*40}"
        )
    write_log(
        f"PC Cleaner: {bytes_to_readable(total)} freed.")
    if done_callback:
        done_callback(total)

def clean_update_cache(log_callback=None):
    run_cmd("net stop wuauserv")
    freed = safe_clean_folder(
        r"C:\Windows\SoftwareDistribution\Download",
        "Windows Update Cache",
        log_callback)
    run_cmd("net start wuauserv")
    return freed

# ============================================
# ACTIVATE DATA SAVER
# ============================================
def activate_data_saver(icon_ref=None):
    write_log("=== SANDA DATA SAVER ACTIVATED ===")
    write_log("Killing data-hungry processes...")
    for proc in PROCESSES_TO_KILL:
        run_cmd(f'taskkill /F /IM "{proc}"')

    apps = load_blocked_apps()
    for app in apps:
        if app.get("enabled", True):
            proc_name = os.path.basename(
                app["path"])
            run_cmd(f'taskkill /F /IM "{proc_name}"')
    write_log("  Processes terminated.")

    write_log("Stopping services...")
    for svc_name, svc_desc in SERVICES_TO_DISABLE:
        run_cmd(f'sc stop "{svc_name}"')
        run_cmd(
            f'sc config "{svc_name}" start= disabled')
        write_log(f"  Stopped: {svc_desc}")

    write_log("Adding firewall blocks...")
    for app in apps:
        if (app.get("enabled", True)
                and os.path.exists(app["path"])):
            rule_name = (
                f"Sanda_Block_"
                f"{app['name'].replace(' ', '_')}")
            run_cmd(
                f'netsh advfirewall firewall '
                f'add rule name="{rule_name}" '
                f'dir=out action=block '
                f'program="{app["path"]}" '
                f'enable=yes'
            )
            write_log(f"  Blocked: {app['name']}")

    write_log("Disabling scheduled tasks...")
    for task in SCHEDULED_TASKS:
        run_cmd(
            f'schtasks /Change /TN "{task}" '
            f'/Disable')
    write_log(
        f"  Tasks disabled: {len(SCHEDULED_TASKS)}")

    write_log("Setting connections as metered...")
    try:
        reg_path = (
            r"SOFTWARE\Microsoft\Windows NT"
            r"\CurrentVersion\NetworkList"
            r"\DefaultMediaCost"
        )
        key = winreg.OpenKey(
            winreg.HKEY_LOCAL_MACHINE,
            reg_path,
            0,
            winreg.KEY_SET_VALUE
            | winreg.KEY_WOW64_64KEY
        )
        for name in [
                "3G", "4G", "Default",
                "Ethernet", "WiFi"]:
            try:
                winreg.SetValueEx(
                    key, name, 0,
                    winreg.REG_DWORD, 2)
            except Exception:
                pass
        winreg.CloseKey(key)
        write_log("  Connections set to metered.")
    except Exception as e:
        write_log(f"  Could not set metered: {e}")

    write_log("Disabling background apps...")
    try:
        reg_path = (
            r"SOFTWARE\Microsoft\Windows"
            r"\CurrentVersion"
            r"\BackgroundAccessApplications"
        )
        key = winreg.OpenKey(
            winreg.HKEY_CURRENT_USER,
            reg_path, 0,
            winreg.KEY_SET_VALUE)
        winreg.SetValueEx(
            key, "GlobalUserDisabled",
            0, winreg.REG_DWORD, 1)
        winreg.CloseKey(key)
        write_log("  Background apps disabled.")
    except Exception as e:
        write_log(
            f"  Could not disable background: {e}")

    run_cmd("ipconfig /flushdns")
    write_log("DNS cache flushed.")
    write_log(
        "=== SANDA DATA SAVER FULLY ACTIVE ===\n")
    config = load_config()
    config["data_saver_on"] = True
    save_config(config)
    notify(
        APP_BRAND_NAME,
        "🛡️ Sanda Data Saver is ON\n"
        "Your hotspot data is now protected!",
        icon_ref
    )

# ============================================
# DEACTIVATE DATA SAVER
# ============================================
def deactivate_data_saver(icon_ref=None):
    write_log(
        "=== SANDA DATA SAVER DEACTIVATING ===")
    write_log("Re-enabling services...")
    for svc_name, svc_desc in SERVICES_TO_DISABLE:
        run_cmd(
            f'sc config "{svc_name}" start= auto')
        run_cmd(f'sc start "{svc_name}"')
        write_log(f"  Started: {svc_desc}")

    write_log("Removing firewall blocks...")
    apps = load_blocked_apps()
    for app in apps:
        rule_name = (
            f"Sanda_Block_"
            f"{app['name'].replace(' ', '_')}")
        run_cmd(
            f'netsh advfirewall firewall '
            f'delete rule name="{rule_name}"')
        write_log(f"  Unblocked: {app['name']}")

    write_log("Re-enabling scheduled tasks...")
    for task in SCHEDULED_TASKS:
        run_cmd(
            f'schtasks /Change /TN "{task}" '
            f'/Enable')
    write_log(
        f"  Tasks re-enabled: {len(SCHEDULED_TASKS)}")

    write_log("Setting connections as unmetered...")
    try:
        reg_path = (
            r"SOFTWARE\Microsoft\Windows NT"
            r"\CurrentVersion\NetworkList"
            r"\DefaultMediaCost"
        )
        key = winreg.OpenKey(
            winreg.HKEY_LOCAL_MACHINE,
            reg_path,
            0,
            winreg.KEY_SET_VALUE
            | winreg.KEY_WOW64_64KEY
        )
        for name in [
                "3G", "4G", "Default",
                "Ethernet", "WiFi"]:
            try:
                winreg.SetValueEx(
                    key, name, 0,
                    winreg.REG_DWORD, 1)
            except Exception:
                pass
        winreg.CloseKey(key)
        write_log(
            "  Connections set to unmetered.")
    except Exception as e:
        write_log(
            f"  Could not set unmetered: {e}")

    write_log("Re-enabling background apps...")
    try:
        reg_path = (
            r"SOFTWARE\Microsoft\Windows"
            r"\CurrentVersion"
            r"\BackgroundAccessApplications"
        )
        key = winreg.OpenKey(
            winreg.HKEY_CURRENT_USER,
            reg_path, 0,
            winreg.KEY_SET_VALUE)
        winreg.SetValueEx(
            key, "GlobalUserDisabled",
            0, winreg.REG_DWORD, 0)
        winreg.CloseKey(key)
        write_log("  Background apps re-enabled.")
    except Exception as e:
        write_log(
            f"  Could not re-enable: {e}")

    write_log(
        "=== SANDA DATA SAVER OFF ===\n")
    config = load_config()
    config["data_saver_on"] = False
    save_config(config)
    notify(
        APP_BRAND_NAME,
        "✅ Sanda Data Saver is OFF\n"
        "All services restored to normal.",
        icon_ref
    )

# ============================================
# RESTORE ALL SETTINGS
# ============================================
def restore_all_settings(icon_ref=None):
    write_log("=== FULL RESTORE TRIGGERED ===")
    deactivate_data_saver(icon_ref)
    run_cmd(
        'netsh advfirewall firewall '
        'delete rule name="Sanda_Block_*"')
    for svc in [
            "wuauserv", "BITS",
            "DoSvc", "WSearch"]:
        run_cmd(
            f'sc config "{svc}" start= auto')
        run_cmd(f'sc start "{svc}"')
    write_log("Full restore complete.")
    notify(
        APP_BRAND_NAME,
        "🔄 All settings restored to defaults.",
        icon_ref
    )

# ============================================
# TRAY STARTUP REGISTRY MANAGEMENT
# ============================================
def is_startup_enabled():
    reg_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, reg_path, 0, winreg.KEY_READ)
        winreg.QueryValueEx(key, "SandaDataSaver")
        winreg.CloseKey(key)
        return True
    except Exception:
        return False

def toggle_startup(icon, item):
    reg_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, reg_path, 0, winreg.KEY_SET_VALUE)
        if is_startup_enabled():
            winreg.DeleteValue(key, "SandaDataSaver")
            write_log("Windows Startup auto-launch disabled by user.")
            notify("Sanda Startup", "Startup auto-launch disabled.", icon)
        else:
            exe_path = os.path.abspath(sys.argv[0])
            winreg.SetValueEx(key, "SandaDataSaver", 0, winreg.REG_SZ, f'"{exe_path}"')
            write_log("Windows Startup auto-launch enabled by user.")
            notify("Sanda Startup", "Sanda will now start automatically with Windows!", icon)
        winreg.CloseKey(key)
    except Exception as e:
        write_log(f"Failed to toggle startup registry: {e}")

# ============================================
# ICON DRAWING
# ============================================
def draw_sanda_logo(size=64, is_on=True):
    img  = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size // 2, size // 2
    draw.ellipse(
        [2, 2, size - 2, size - 2],
        fill=(13, 17, 23, 255),
        outline=(255, 255, 255, 40),
        width=1
    )
    wave_color = (
        (0, 201, 255, 255)
        if is_on
        else (255, 107, 107, 255))
    dot_color = (
        (0, 255, 136, 255)
        if is_on
        else (255, 107, 107, 255))
    for arc_r, arc_w in [
        (int(size * 0.38), 3),
        (int(size * 0.27), 2),
        (int(size * 0.16), 2),
    ]:
        x0 = cx - arc_r
        y0 = cy - arc_r + int(size * 0.05)
        x1 = cx + arc_r
        y1 = cy + arc_r + int(size * 0.05)
        draw.arc(
            [x0, y0, x1, y1],
            start=210, end=330,
            fill=wave_color, width=arc_w)
    dot_r = int(size * 0.07)
    draw.ellipse(
        [cx - dot_r,
         cy + int(size * 0.1) - dot_r,
         cx + dot_r,
         cy + int(size * 0.1) + dot_r],
        fill=dot_color
    )
    try:
        font_small = ImageFont.truetype(
            "arialbd.ttf", int(size * 0.22))
    except Exception:
        font_small = ImageFont.load_default()

    bbox = draw.textbbox((0, 0), "S",
                          font=font_small)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    draw.text(
        (cx - tw // 2,
         size - th - int(size * 0.08)),
        "S", fill=wave_color,
        font=font_small
    )
    return img

def draw_sanda_logo_large(
        width=400, height=120):
    img  = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    icon_size = 80
    icon_img  = draw_sanda_logo(
        icon_size, is_on=True)
    img.paste(
        icon_img,
        (10, (height - icon_size) // 2),
        icon_img)
    try:
        font_title = ImageFont.truetype(
            "arialbd.ttf", 28)
        font_tag   = ImageFont.truetype(
            "arial.ttf", 12)
    except Exception:
        font_title = ImageFont.load_default()
        font_tag   = font_title

    text_x = icon_size + 20
    draw.text(
        (text_x, 18),
        APP_BRAND_NAME,
        fill=(0, 201, 255, 255),
        font=font_title)
    draw.text(
        (text_x + 2, 58),
        APP_TAGLINE,
        fill=(139, 148, 158, 255),
        font=font_tag)
    draw.text(
        (text_x + 2, 76),
        f"Version {APP_VERSION}  •  "
        f"By {APP_AUTHOR}",
        fill=(139, 148, 158, 200),
        font=font_tag)
    return img

# ============================================
# SPLASH SCREEN
# ============================================
class SplashScreen:
    def __init__(self, master=None):
        if master:
            self.root = tk.Toplevel(master)
        else:
            self.root = tk.Tk()
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)
        self.root.configure(bg=APP_COLOR_BG)
        w, h = 420, 260
        sw   = self.root.winfo_screenwidth()
        sh   = self.root.winfo_screenheight()
        x    = (sw - w) // 2
        y    = (sh - h) // 2
        self.root.geometry(f"{w}x{h}+{x}+{y}")
        self._build()

    def _build(self):
        border = tk.Frame(
            self.root,
            bg=APP_COLOR_PRIMARY,
            padx=2, pady=2)
        border.pack(fill=tk.BOTH, expand=True)
        inner = tk.Frame(border, bg=APP_COLOR_BG)
        inner.pack(fill=tk.BOTH, expand=True)

        banner_img = draw_sanda_logo_large(
            400, 110)
        self._banner_photo = ImageTk.PhotoImage(
            banner_img, master=self.root)
        tk.Label(
            inner,
            image=self._banner_photo,
            bg=APP_COLOR_BG
        ).pack(pady=(15, 0))

        tk.Frame(
            inner,
            bg=APP_COLOR_PRIMARY,
            height=1
        ).pack(fill=tk.X, padx=20, pady=8)

        self.status_var = tk.StringVar(
            value="Initializing...")
        tk.Label(
            inner,
            textvariable=self.status_var,
            font=("Segoe UI", 9),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG
        ).pack()

        style = ttk.Style()
        style.theme_use("clam")
        style.configure(
            "Sanda.Horizontal.TProgressbar",
            troughcolor=APP_COLOR_BG2,
            background=APP_COLOR_PRIMARY,
            bordercolor=APP_COLOR_BG,
            lightcolor=APP_COLOR_PRIMARY,
            darkcolor=APP_COLOR_PRIMARY,
        )
        self.progress = ttk.Progressbar(
            inner,
            style="Sanda.Horizontal.TProgressbar",
            orient="horizontal",
            length=360,
            mode="determinate"
        )
        self.progress.pack(pady=10)

        tk.Label(
            inner,
            text=(f"{APP_BRAND_NAME}  •  "
                  f"{APP_TAGLINE}"),
            font=("Segoe UI", 7),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG
        ).pack(pady=(0, 8))

    def update_status(self, text, progress_val):
        self.status_var.set(text)
        self.progress["value"] = progress_val
        self.root.update()

    def close(self):
        self.root.destroy()

# ============================================
# PC CLEANER WINDOW
# ============================================
class PCCleanerWindow:
    def __init__(self):
        self.window      = None
        self._mini_photo = None
        self._running    = False

    def open(self):
        if (self.window
                and tk.Toplevel.winfo_exists(
                    self.window)):
            self.window.focus_force()
            self.window.lift()
            return
        self.window = tk.Toplevel()
        self.window.title(
            f"{APP_BRAND_NAME} — PC Cleaner")
        self.window.geometry("680x700")
        self.window.configure(bg=APP_COLOR_BG)
        self.window.resizable(True, True)
        self._build_ui()

    def _build_ui(self):
        # ── Header ───────────────────────────
        header = tk.Frame(
            self.window,
            bg=APP_COLOR_BG2,
            pady=12, padx=15)
        header.pack(fill=tk.X)
        mini_icon        = draw_sanda_logo(
            32, is_on=True)
        self._mini_photo = ImageTk.PhotoImage(
            mini_icon, master=self.window)
        tk.Label(
            header,
            image=self._mini_photo,
            bg=APP_COLOR_BG2
        ).pack(side=tk.LEFT, padx=(0, 8))
        tk.Label(
            header,
            text="🧹 PC Cleaner",
            font=("Segoe UI", 13, "bold"),
            fg=APP_COLOR_SUCCESS,
            bg=APP_COLOR_BG2
        ).pack(side=tk.LEFT)
        tk.Label(
            header,
            text="Safely removes junk files and frees disk space",
            font=("Segoe UI", 9),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG2
        ).pack(side=tk.LEFT, padx=15)

        # ── Upgrade Option: Selective Cleaning Checkboxes ────
        options_frame = tk.LabelFrame(
            self.window,
            text="🧹  Select What to Clean",
            font=("Segoe UI", 9, "bold"),
            bg=APP_COLOR_BG,
            fg=APP_COLOR_PRIMARY,
            bd=1,
            relief=tk.SOLID,
            pady=8, padx=15
        )
        options_frame.pack(fill=tk.X, padx=15, pady=8)

        self.var_temp = tk.BooleanVar(value=True)
        self.var_prefetch = tk.BooleanVar(value=True)
        self.var_update_cache = tk.BooleanVar(value=True)
        self.var_recent = tk.BooleanVar(value=True)
        self.var_browser = tk.BooleanVar(value=True)
        self.var_logs = tk.BooleanVar(value=True)
        self.var_thumb = tk.BooleanVar(value=True)

        chk_style = {
            "bg": APP_COLOR_BG,
            "fg": APP_COLOR_TEXT,
            "selectcolor": APP_COLOR_BG,
            "activebackground": APP_COLOR_BG,
            "activeforeground": "white",
            "anchor": "w"
        }
        tk.Checkbutton(options_frame, text="User & Windows Temp Files", variable=self.var_temp, **chk_style).grid(row=0, column=0, sticky="w", padx=10, pady=2)
        tk.Checkbutton(options_frame, text="Windows Prefetch Files", variable=self.var_prefetch, **chk_style).grid(row=0, column=1, sticky="w", padx=10, pady=2)
        tk.Checkbutton(options_frame, text="Windows Update Caches", variable=self.var_update_cache, **chk_style).grid(row=1, column=0, sticky="w", padx=10, pady=2)
        tk.Checkbutton(options_frame, text="Recent File Logs", variable=self.var_recent, **chk_style).grid(row=1, column=1, sticky="w", padx=10, pady=2)
        tk.Checkbutton(options_frame, text="Web Browser Caches", variable=self.var_browser, **chk_style).grid(row=2, column=0, sticky="w", padx=10, pady=2)
        tk.Checkbutton(options_frame, text="Windows Diagnostic Logs", variable=self.var_logs, **chk_style).grid(row=2, column=1, sticky="w", padx=10, pady=2)
        tk.Checkbutton(options_frame, text="Explorer Thumbnail Cache", variable=self.var_thumb, **chk_style).grid(row=3, column=0, sticky="w", padx=10, pady=2)

        # ── Buttons Row ───────────────────────
        btn_row = tk.Frame(
            self.window, bg=APP_COLOR_BG,
            pady=5)
        btn_row.pack(fill=tk.X, padx=15)
        self.btn_clean = tk.Button(
            btn_row,
            text="🚀  Start Selected Clean",
            font=("Segoe UI", 11, "bold"),
            bg=APP_COLOR_SUCCESS,
            fg=APP_COLOR_BG,
            relief=tk.FLAT,
            padx=25, pady=8,
            cursor="hand2",
            command=self._start_clean
        )
        self.btn_clean.pack(
            side=tk.LEFT, padx=(0, 10))

        tk.Button(
            btn_row,
            text="🌐  Flush DNS Only",
            font=("Segoe UI", 10),
            bg=APP_COLOR_BG3,
            fg=APP_COLOR_TEXT,
            relief=tk.FLAT,
            padx=15, pady=8,
            cursor="hand2",
            command=self._flush_dns_only
        ).pack(side=tk.LEFT, padx=5)

        tk.Button(
            btn_row,
            text="🗑️  Clear Log",
            font=("Segoe UI", 10),
            bg=APP_COLOR_BG3,
            fg=APP_COLOR_TEXT,
            relief=tk.FLAT,
            padx=15, pady=8,
            cursor="hand2",
            command=self._clear_output
        ).pack(side=tk.LEFT, padx=5)

        # ── Progress Bar ──────────────────────
        self.progress_var = tk.DoubleVar(
            value=0)
        style = ttk.Style()
        style.configure(
            "Clean.Horizontal.TProgressbar",
            troughcolor=APP_COLOR_BG2,
            background=APP_COLOR_SUCCESS,
            bordercolor=APP_COLOR_BG,
            lightcolor=APP_COLOR_SUCCESS,
            darkcolor=APP_COLOR_SUCCESS,
        )
        self.progress = ttk.Progressbar(
            self.window,
            style="Clean.Horizontal.TProgressbar",
            orient="horizontal",
            mode="indeterminate",
            length=640
        )
        self.progress.pack(
            padx=15, pady=(5, 5))

        # ── Status Label ──────────────────────
        self.status_var = tk.StringVar(
            value="Ready to clean your PC...")
        tk.Label(
            self.window,
            textvariable=self.status_var,
            font=("Segoe UI", 9),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG,
            anchor="w"
        ).pack(fill=tk.X, padx=15)

        # ── Output Log ────────────────────────
        log_frame = tk.Frame(
            self.window, bg=APP_COLOR_BG,
            padx=15, pady=5)
        log_frame.pack(
            fill=tk.BOTH, expand=True)

        tk.Label(
            log_frame,
            text="Cleaning Log:",
            font=("Segoe UI", 9, "bold"),
            fg=APP_COLOR_PRIMARY,
            bg=APP_COLOR_BG,
            anchor="w"
        ).pack(fill=tk.X)

        self.output = scrolledtext.ScrolledText(
            log_frame,
            font=("Consolas", 9),
            bg=APP_COLOR_BG2,
            fg=APP_COLOR_SUCCESS,
            wrap=tk.WORD,
            relief=tk.FLAT,
            padx=12, pady=8,
            state=tk.DISABLED,
            insertbackground=APP_COLOR_PRIMARY
        )
        self.output.pack(
            fill=tk.BOTH, expand=True)

        # ── Summary Bar ───────────────────────
        self.summary_var = tk.StringVar(
            value="")
        self.summary_label = tk.Label(
            self.window,
            textvariable=self.summary_var,
            font=("Segoe UI", 10, "bold"),
            fg=APP_COLOR_SUCCESS,
            bg=APP_COLOR_BG2,
            pady=8
        )
        self.summary_label.pack(
            fill=tk.X, padx=0, pady=(5, 0))

        # ── Initial message ───────────────────
        self._log(
            f"{'='*45}\n"
            f"  {APP_BRAND_NAME} — PC Cleaner\n"
            f"  {APP_TAGLINE}\n"
            f"{'='*45}\n"
            f"Choose the elements above and click 'Start Selected Clean' to begin.\n"
        )

    def _log(self, text):
        """Add text to the output log."""
        def _do():
            self.output.configure(
                state=tk.NORMAL)
            self.output.insert(
                tk.END, text + "\n")
            self.output.see(tk.END)
            self.output.configure(
                state=tk.DISABLED)
        if self.window:
            self.window.after(0, _do)

    def _clear_output(self):
        self.output.configure(
            state=tk.NORMAL)
        self.output.delete(1.0, tk.END)
        self.output.configure(
            state=tk.DISABLED)
        self.summary_var.set("")

    def _flush_dns_only(self):
        self._log("\n🌐 Flushing DNS cache...")
        flush_dns_cache(self._log)

    def _start_clean(self):
        if self._running:
            return
        self._running = True
        self.btn_clean.configure(
            state=tk.DISABLED,
            text="⏳ Cleaning...")
        self.progress.start(10)
        self.summary_var.set("")
        self._clear_output()
        self._log(
            f"\n🚀 Starting Configured Clean...\n"
            f"{'='*40}"
        )
        self.status_var.set(
            "Cleaning in progress...")

        options = {
            "temp": self.var_temp.get(),
            "prefetch": self.var_prefetch.get(),
            "update_cache": self.var_update_cache.get(),
            "recent": self.var_recent.get(),
            "browser": self.var_browser.get(),
            "logs": self.var_logs.get(),
            "thumb": self.var_thumb.get(),
        }

        def do_clean():
            run_full_clean(
                options=options,
                log_callback=self._log,
                done_callback=self._on_done
            )
        threading.Thread(
            target=do_clean,
            daemon=True
        ).start()

    def _on_done(self, total_freed):
        def _update():
            self._running = False
            self.progress.stop()
            self.progress["value"] = 100
            self.btn_clean.configure(
                state=tk.NORMAL,
                text="🚀  Start Selected Clean")
            self.status_var.set(
                "✅ Cleaning complete!")
            self.summary_var.set(
                f"  💾 Total space freed: "
                f"{bytes_to_readable(total_freed)}"
                f"   |   "
                f"Restart your PC for best results!"
            )
        if self.window:
            self.window.after(0, _update)

# ============================================
# ABOUT WINDOW
# ============================================
class AboutWindow:
    def __init__(self):
        self.window = None
        self._photo = None

    def open(self):
        if (self.window
                and tk.Toplevel.winfo_exists(
                    self.window)):
            self.window.focus_force()
            self.window.lift()
            return
        self.window = tk.Toplevel()
        self.window.title(
            f"About {APP_BRAND_NAME}")
        self.window.geometry("420x440")
        self.window.configure(bg=APP_COLOR_BG)
        self.window.resizable(False, False)

        border = tk.Frame(
            self.window,
            bg=APP_COLOR_PRIMARY,
            padx=2, pady=2)
        border.pack(fill=tk.BOTH, expand=True)

        inner = tk.Frame(border, bg=APP_COLOR_BG)
        inner.pack(
            fill=tk.BOTH, expand=True,
            padx=1, pady=1)

        banner_img  = draw_sanda_logo_large(
            400, 110)
        self._photo = ImageTk.PhotoImage(
            banner_img, master=self.window)
        tk.Label(
            inner,
            image=self._photo,
            bg=APP_COLOR_BG
        ).pack(pady=(15, 5))

        tk.Frame(
            inner,
            bg=APP_COLOR_PRIMARY,
            height=1
        ).pack(fill=tk.X, padx=20, pady=5)

        # Spiritual Ministry Branding Label (Requested Change!)
        tk.Label(
            inner,
            text="✝️ This software is provided 100% free of charge\nfor the Glory of Jesus Christ, my Savior.",
            font=("Segoe UI", 9, "bold italic"),
            fg=APP_COLOR_SUCCESS,
            bg=APP_COLOR_BG,
            wraplength=360,
            justify="center"
        ).pack(pady=(5, 10))

        info_items = [
            ("App",     APP_BRAND_NAME),
            ("Version", APP_VERSION),
            ("Author",  APP_AUTHOR),
            ("Tagline", APP_TAGLINE),
            ("Config",  APP_DIR),
        ]
        for label, value in info_items:
            row = tk.Frame(
                inner, bg=APP_COLOR_BG)
            row.pack(
                fill=tk.X, padx=30, pady=2)
            tk.Label(
                row,
                text=f"{label}:",
                font=("Segoe UI", 9, "bold"),
                fg=APP_COLOR_PRIMARY,
                bg=APP_COLOR_BG,
                width=10, anchor="w"
            ).pack(side=tk.LEFT)
            tk.Label(
                row,
                text=value,
                font=("Segoe UI", 9),
                fg=APP_COLOR_TEXT,
                bg=APP_COLOR_BG,
                anchor="w"
            ).pack(side=tk.LEFT)

        tk.Frame(
            inner,
            bg=APP_COLOR_BG3,
            height=1
        ).pack(fill=tk.X, padx=20, pady=10)

        tk.Label(
            inner,
            text=(f"© 2026 {APP_AUTHOR}. "
                  f"All rights reserved."),
            font=("Segoe UI", 8),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG
        ).pack()

        tk.Button(
            inner, text="Close",
            font=("Segoe UI", 9, "bold"),
            bg=APP_COLOR_PRIMARY,
            fg=APP_COLOR_BG,
            relief=tk.FLAT,
            padx=30, pady=5,
            cursor="hand2",
            command=self.window.destroy
        ).pack(pady=15)

# ============================================
# LOG WINDOW
# ============================================
class LogWindow:
    def __init__(self):
        self.window      = None
        self._mini_photo = None

    def open(self):
        if (self.window
                and tk.Toplevel.winfo_exists(
                    self.window)):
            self.window.focus_force()
            self.window.lift()
            return
        self.window = tk.Toplevel()
        self.window.title(
            f"{APP_BRAND_NAME} — Activity Log")
        self.window.geometry("700x520")
        self.window.configure(bg=APP_COLOR_BG)
        self.window.resizable(True, True)

        header = tk.Frame(
            self.window,
            bg=APP_COLOR_BG2,
            pady=12, padx=15)
        header.pack(fill=tk.X)

        mini_icon        = draw_sanda_logo(
            32, is_on=True)
        self._mini_photo = ImageTk.PhotoImage(
            mini_icon, master=self.window)
        tk.Label(
            header,
            image=self._mini_photo,
            bg=APP_COLOR_BG2
        ).pack(side=tk.LEFT, padx=(0, 8))
        tk.Label(
            header,
            text="Activity Log",
            font=("Segoe UI", 13, "bold"),
            fg=APP_COLOR_PRIMARY,
            bg=APP_COLOR_BG2
        ).pack(side=tk.LEFT)

        btn_frame = tk.Frame(
            header, bg=APP_COLOR_BG2)
        btn_frame.pack(side=tk.RIGHT)

        for text, cmd, color in [
            ("🔄 Refresh",
             self._refresh,
             APP_COLOR_BG3),
            ("🗑️ Clear",
             self._clear,
             APP_COLOR_ACCENT),
            ("📂 Open Folder",
             lambda: os.startfile(APP_DIR),
             APP_COLOR_BG3),
        ]:
            tk.Button(
                btn_frame,
                text=text,
                font=("Segoe UI", 8),
                bg=color,
                fg=APP_COLOR_TEXT,
                relief=tk.FLAT,
                padx=10, pady=4,
                cursor="hand2",
                command=cmd
            ).pack(side=tk.LEFT, padx=3)

        text_frame = tk.Frame(
            self.window, bg=APP_COLOR_BG)
        text_frame.pack(
            fill=tk.BOTH, expand=True,
            padx=12, pady=12)

        self.text = scrolledtext.ScrolledText(
            text_frame,
            font=("Consolas", 9),
            bg=APP_COLOR_BG2,
            fg=APP_COLOR_SUCCESS,
            wrap=tk.WORD,
            relief=tk.FLAT,
            padx=12, pady=8,
            insertbackground=APP_COLOR_PRIMARY
        )
        self.text.pack(fill=tk.BOTH, expand=True)
        self._refresh()

    def _refresh(self):
        try:
            with open(LOG_FILE, "r") as f:
                content = f.read()
        except FileNotFoundError:
            content = (
                f"Welcome to {APP_BRAND_NAME}!\n"
                f"No activity logged yet.")
        self.text.configure(state=tk.NORMAL)
        self.text.delete(1.0, tk.END)
        self.text.insert(tk.END, content)
        self.text.see(tk.END)
        self.text.configure(state=tk.DISABLED)

    def _clear(self):
        if messagebox.askyesno(
            "Clear Log",
            "Clear the entire activity log?",
            parent=self.window
        ):
            clear_log()
            self._refresh()

# ============================================
# APPS MANAGER WINDOW
# ============================================
class AppsManagerWindow:
    def __init__(self):
        self.window      = None
        self.apps        = []
        self.vars        = []
        self._mini_photo = None

    def open(self):
        if (self.window
                and tk.Toplevel.winfo_exists(
                    self.window)):
            self.window.focus_force()
            self.window.lift()
            return
        self.window = tk.Toplevel()
        self.window.title(
            f"{APP_BRAND_NAME} — Manage Blocked Apps")
        self.window.geometry("720x600")
        self.window.configure(bg=APP_COLOR_BG)
        self.window.resizable(True, True)
        self.apps = load_blocked_apps()
        self._build_ui()

    def _build_ui(self):
        header = tk.Frame(
            self.window,
            bg=APP_COLOR_BG2,
            pady=12, padx=15)
        header.pack(fill=tk.X)

        mini_icon        = draw_sanda_logo(
            32, is_on=False)
        self._mini_photo = ImageTk.PhotoImage(
            mini_icon, master=self.window)
        tk.Label(
            header,
            image=self._mini_photo,
            bg=APP_COLOR_BG2
        ).pack(side=tk.LEFT, padx=(0, 8))
        tk.Label(
            header,
            text="Manage Blocked Apps",
            font=("Segoe UI", 13, "bold"),
            fg=APP_COLOR_ACCENT,
            bg=APP_COLOR_BG2
        ).pack(side=tk.LEFT)

        tk.Label(
            self.window,
            text=(
                "Checked apps will be blocked "
                "from the internet when "
                "Sanda Data Saver is ON."
            ),
            font=("Segoe UI", 9),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG
        ).pack(pady=(8, 2))

        outer = tk.Frame(
            self.window,
            bg=APP_COLOR_BG,
            padx=12, pady=5)
        outer.pack(fill=tk.BOTH, expand=True)

        canvas    = tk.Canvas(
            outer,
            bg=APP_COLOR_BG2,
            highlightthickness=0)
        scrollbar = ttk.Scrollbar(
            outer,
            orient="vertical",
            command=canvas.yview)

        self.scroll_frame = tk.Frame(
            canvas, bg=APP_COLOR_BG2)
        self.scroll_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(
                scrollregion=canvas.bbox("all"))
        )
        canvas.create_window(
            (0, 0),
            window=self.scroll_frame,
            anchor="nw")

        canvas.configure(
            yscrollcommand=scrollbar.set)
        scrollbar.pack(
            side=tk.RIGHT, fill=tk.Y)
        canvas.pack(
            side=tk.LEFT,
            fill=tk.BOTH,
            expand=True)

        self._populate_list()

        add_frame = tk.Frame(
            self.window,
            bg=APP_COLOR_BG3,
            pady=12, padx=15)
        add_frame.pack(
            fill=tk.X, padx=12, pady=(5, 0))

        tk.Label(
            add_frame,
            text="➕  Add New App to Block",
            font=("Segoe UI", 9, "bold"),
            fg=APP_COLOR_PRIMARY,
            bg=APP_COLOR_BG3
        ).grid(
            row=0, column=0,
            columnspan=6,
            sticky="w", pady=(0, 8))

        tk.Label(
            add_frame,
            text="Name:",
            font=("Segoe UI", 9),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG3
        ).grid(row=1, column=0, sticky="w")

        self.name_entry = tk.Entry(
            add_frame,
            font=("Segoe UI", 9),
            width=18,
            bg=APP_COLOR_BG2,
            fg=APP_COLOR_TEXT,
            insertbackground=APP_COLOR_PRIMARY,
            relief=tk.FLAT
        )
        self.name_entry.grid(
            row=1, column=1,
            padx=(5, 15), sticky="w")

        tk.Label(
            add_frame,
            text="EXE Path:",
            font=("Segoe UI", 9),
            fg=APP_COLOR_MUTED,
            bg=APP_COLOR_BG3
        ).grid(row=1, column=2, sticky="w")

        self.path_entry = tk.Entry(
            add_frame,
            font=("Segoe UI", 9),
            width=38,
            bg=APP_COLOR_BG2,
            fg=APP_COLOR_TEXT,
            insertbackground=APP_COLOR_PRIMARY,
            relief=tk.FLAT
        )
        self.path_entry.grid(
            row=1, column=3,
            padx=(5, 8), sticky="ew")

        tk.Button(
            add_frame, text="Browse",
            font=("Segoe UI", 8),
            bg=APP_COLOR_ACCENT,
            fg="white",
            relief=tk.FLAT, padx=8,
            cursor="hand2",
            command=self._browse
        ).grid(row=1, column=4, padx=(0, 5))

        tk.Button(
            add_frame, text="Add",
            font=("Segoe UI", 8, "bold"),
            bg=APP_COLOR_SUCCESS,
            fg=APP_COLOR_BG,
            relief=tk.FLAT, padx=12,
            cursor="hand2",
            command=self._add_app
        ).grid(row=1, column=5)

        add_frame.columnconfigure(3, weight=1)

        btn_frame = tk.Frame(
            self.window,
            bg=APP_COLOR_BG,
            pady=10)
        btn_frame.pack(fill=tk.X, padx=12)

        tk.Button(
            btn_frame,
            text="💾  Save Changes",
            font=("Segoe UI", 10, "bold"),
            bg=APP_COLOR_SUCCESS,
            fg=APP_COLOR_BG,
            relief=tk.FLAT,
            padx=22, pady=6,
            cursor="hand2",
            command=self._save
        ).pack(side=tk.RIGHT, padx=5)

        tk.Button(
            btn_frame,
            text="Cancel",
            font=("Segoe UI", 10),
            bg=APP_COLOR_BG3,
            fg=APP_COLOR_TEXT,
            relief=tk.FLAT,
            padx=22, pady=6,
            cursor="hand2",
            command=self.window.destroy
        ).pack(side=tk.RIGHT, padx=5)

    def _populate_list(self):
        for widget in \
                self.scroll_frame.winfo_children():
            widget.destroy()

        self.vars = []
        headers = [
            "  ", "App Name",
            "Executable Path", "Remove"]
        widths  = [4, 20, 50, 8]

        for col, (h, w) in enumerate(
                zip(headers, widths)):
            tk.Label(
                self.scroll_frame,
                text=h,
                font=("Segoe UI", 9, "bold"),
                fg=APP_COLOR_PRIMARY,
                bg=APP_COLOR_BG2,
                width=w, anchor="w"
            ).grid(
                row=0, column=col,
                padx=5, pady=6, sticky="w")

        tk.Frame(
            self.scroll_frame,
            bg=APP_COLOR_PRIMARY,
            height=1
        ).grid(
            row=1, column=0, columnspan=4,
            sticky="ew", padx=5, pady=(0, 4))

        for i, app in enumerate(self.apps):
            row = i + 2
            bg  = (APP_COLOR_BG2
                   if i % 2 == 0
                   else APP_COLOR_BG3)
            var = tk.BooleanVar(
                value=app.get("enabled", True))
            self.vars.append(var)

            exists     = os.path.exists(
                app.get("path", ""))
            name_color = (APP_COLOR_TEXT
                          if exists
                          else APP_COLOR_MUTED)

            tk.Checkbutton(
                self.scroll_frame,
                variable=var,
                bg=bg,
                activebackground=bg,
                activeforeground="white",
                selectcolor=APP_COLOR_BG,
                fg="white"
            ).grid(
                row=row, column=0,
                padx=8, pady=5)

            tk.Label(
                self.scroll_frame,
                text=app.get("name", "Unknown"),
                font=("Segoe UI", 9, "bold"),
                fg=name_color,
                bg=bg,
                anchor="w", width=20
            ).grid(
                row=row, column=1,
                padx=5, pady=5, sticky="w")

            path_text = app.get("path", "")
            if len(path_text) > 50:
                path_text = (
                    "..." + path_text[-47:])
            status = (
                " ✓" if exists
                else " (not found)")

            tk.Label(
                self.scroll_frame,
                text=path_text + status,
                font=("Consolas", 7),
                fg=name_color,
                bg=bg,
                anchor="w"
            ).grid(
                row=row, column=2,
                padx=5, pady=5, sticky="w")

            tk.Button(
                self.scroll_frame,
                text="🗑️",
                font=("Segoe UI", 9),
                bg=bg,
                fg=APP_COLOR_ACCENT,
                relief=tk.FLAT,
                cursor="hand2",
                command=lambda idx=i:
                    self._delete_app(idx)
            ).grid(
                row=row, column=3,
                padx=5, pady=5)

    def _browse(self):
        path = filedialog.askopenfilename(
            title="Select Application Executable",
            filetypes=[
                ("Executables", "*.exe"),
                ("All Files", "*.*")
            ]
        )
        if path:
            self.path_entry.delete(0, tk.END)
            self.path_entry.insert(0, path)
            if not self.name_entry.get():
                self.name_entry.insert(
                    0,
                    os.path.splitext(
                        os.path.basename(
                            path))[0]
                )

    def _add_app(self):
        name = self.name_entry.get().strip()
        path = self.path_entry.get().strip()
        if not name or not path:
            messagebox.showwarning(
                "Missing Info",
                "Please enter both a name and a path.",
                parent=self.window
            )
            return

        self.apps.append({
            "name":    name,
            "path":    path,
            "enabled": True
        })
        self.name_entry.delete(0, tk.END)
        self.path_entry.delete(0, tk.END)
        self._populate_list()

    def _delete_app(self, idx):
        name = self.apps[idx].get(
            "name", "this app")
        if messagebox.askyesno(
            "Remove App",
            f"Remove '{name}' from the block list?",
            parent=self.window
        ):
            self.apps.pop(idx)
            self._populate_list()

    def _save(self):
        for i, var in enumerate(self.vars):
            if i < len(self.apps):
                self.apps[i]["enabled"] = \
                    var.get()
        save_blocked_apps(self.apps)
        write_log(
            f"App block list updated — {len(self.apps)} apps configured.")
        messagebox.showinfo(
            "Saved",
            "Changes saved!\n"
            "They will apply next time you toggle Sanda Data Saver.",
            parent=self.window
        )
        self.window.destroy()

# ============================================
# TRAY APP
# ============================================
class SandaDataSaverTray:
    def __init__(self):
        config          = load_config()
        self.is_on      = config.get(
            "data_saver_on", False)
        
        # Real-time Upgrade: Session activation tracker
        self.activation_time = time.time() if self.is_on else None
        
        self.log_win    = LogWindow()
        self.apps_win   = AppsManagerWindow()
        self.about_win  = AboutWindow()
        self.clean_win  = PCCleanerWindow()
        self.icon       = None

        self.root = tk.Tk()
        self.root.withdraw()
        self.root.title(APP_BRAND_NAME)

    def get_estimated_saved_bytes(self):
        """
        Estimate saved bandwidth: 85 KB per second (~300 MB/hour)
        """
        if not self.is_on or not self.activation_time:
            return 0
        elapsed_seconds = time.time() - self.activation_time
        return int(elapsed_seconds * 85 * 1024)

    def _toggle(self, icon, item):
        def do_toggle():
            if self.is_on:
                # Log final stats before deactivating
                elapsed = time.time() - self.activation_time if self.activation_time else 0
                saved_bytes = int(elapsed * 85 * 1024)
                write_log(
                    f"Session closed. Duration: {elapsed/60:.1f} minutes. "
                    f"Est. Hotspot Data Saved: {bytes_to_readable(saved_bytes)}"
                )
                deactivate_data_saver(self.icon)
                self.is_on = False
                self.activation_time = None
            else:
                self.activation_time = time.time()
                activate_data_saver(self.icon)
                self.is_on = True
            self._refresh_icon()

        threading.Thread(
            target=do_toggle,
            daemon=True).start()

    def _open_log(self, icon, item):
        self.root.after(0, self.log_win.open)

    def _open_apps(self, icon, item):
        self.root.after(0, self.apps_win.open)

    def _open_about(self, icon, item):
        self.root.after(0, self.about_win.open)

    def _open_cleaner(self, icon, item):
        self.root.after(0, self.clean_win.open)

    def _restore_all(self, icon, item):
        def do_restore():
            restore_all_settings(self.icon)
            self.is_on = False
            self.activation_time = None
            self._refresh_icon()

        if messagebox.askyesno(
            "Restore All Settings",
            "This will turn OFF Sanda Data Saver and restore\nALL Windows settings to normal.\n\nContinue?"
        ):
            threading.Thread(
                target=do_restore,
                daemon=True).start()

    def _toggle_notifications(self, icon, item):
        config = load_config()
        config["notifications_enabled"] = \
            not config.get(
                "notifications_enabled", True)
        save_config(config)
        self._refresh_icon()

    def _toggle_auto_clean(self, icon, item):
        config = load_config()
        config["auto_clean_enabled"] = \
            not config.get(
                "auto_clean_enabled", False)
        save_config(config)
        write_log(f"Silent Auto-Cleaner set to {config['auto_clean_enabled']}")
        self._refresh_icon()

    def _refresh_icon(self):
        if self.icon:
            self.icon.icon  = draw_sanda_logo(
                64, self.is_on)
            
            # Real-time Upgrade: Estimate Saved Bandwidth displayed in Taskbar Title
            if self.is_on:
                saved_bytes = self.get_estimated_saved_bytes()
                saved_readable = bytes_to_readable(saved_bytes)
                self.icon.title = (
                    f"{APP_BRAND_NAME} — ON\n"
                    f"Est. Data Saved: {saved_readable}"
                )
            else:
                self.icon.title = f"{APP_BRAND_NAME} — OFF"
                
            self.icon.menu = self._build_menu()

    def _build_menu(self):
        config   = load_config()
        notif_on = config.get(
            "notifications_enabled", True)
        autoclean_on = config.get(
            "auto_clean_enabled", False)
        
        # Display dynamic calculated counter inside menu status
        if self.is_on:
            saved_bytes = self.get_estimated_saved_bytes()
            status_text = f"🟢 ACTIVE | Est. Saved: {bytes_to_readable(saved_bytes)}"
        else:
            status_text = "🔴 OFF — Normal Mode"

        toggle_text = (
            "🔓  Turn OFF Sanda Data Saver"
            if self.is_on else
            "🛡️  Turn ON Sanda Data Saver"
        )
        return pystray.Menu(
            pystray.MenuItem(
                APP_BRAND_NAME,
                lambda i, it: None,
                enabled=False),
            pystray.MenuItem(
                status_text,
                lambda i, it: None,
                enabled=False),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem(
                toggle_text,
                self._toggle),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem(
                "🧹  PC Cleaner",
                self._open_cleaner),
            pystray.MenuItem(
                "📋  Open Activity Log",
                self._open_log),
            pystray.MenuItem(
                "🚫  Manage Blocked Apps",
                self._open_apps),
            pystray.MenuItem(
                "ℹ️   About Sanda",
                self._open_about),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem(
                "🔄  Restore All Settings",
                self._restore_all),
            pystray.MenuItem(
                "⚙️  Run on Windows Startup",
                toggle_startup,
                checked=lambda item: is_startup_enabled()),
            pystray.MenuItem(
                f"{'🕒' if autoclean_on else '🕒'}"
                f"  Silent Auto-Clean (24h): "
                f"{'On' if autoclean_on else 'Off'}",
                self._toggle_auto_clean),
            pystray.MenuItem(
                f"{'🔔' if notif_on else '🔕'}"
                f"  Notifications: "
                f"{'On' if notif_on else 'Off'}",
                self._toggle_notifications),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem(
                "❌  Exit",
                self._exit),
        )

    def _auto_clean_loop(self):
        """
        Runs silent lightweight purge (Temp and Browsers) in the background every 24 hours.
        """
        # Wait 15 seconds after startup to settle down
        time.sleep(15)
        while True:
            config = load_config()
            if config.get("auto_clean_enabled", False):
                write_log("[Silent Auto-Cleaner] Starting background purge of temporary files and browser cache...")
                try:
                    freed_temp = safe_clean_folder(os.environ.get("TEMP", ""), "User Temp (Auto-Clean)")
                    freed_browser = clean_browser_caches()
                    total_freed = freed_temp + freed_browser
                    write_log(f"[Silent Auto-Cleaner] Purge completed. Silently freed {bytes_to_readable(total_freed)}.")
                except Exception as e:
                    write_log(f"[Silent Auto-Cleaner] Error: {e}")
            # Wait 24 hours
            time.sleep(86400)

    def _ui_refresh_loop(self):
        """
        Refreshes tray icon stats dynamically every 15 seconds when active
        """
        while True:
            time.sleep(15)
            if self.is_on:
                self._refresh_icon()

    def _exit(self, icon, item):
        if self.is_on:
            if not messagebox.askyesno(
                "Exit Warning",
                f"{APP_BRAND_NAME} is still ON.\n"
                "Exiting will NOT restore your services.\n\nExit anyway?"
            ):
                return
        write_log(
            f"{APP_BRAND_NAME} exited by user.")
        self.icon.stop()
        self.root.quit()

    def run(self):
        self.root.update()
        splash = SplashScreen(master=self.root)
        steps  = [
            ("Loading configuration...",      20),
            ("Reading blocked apps...",        40),
            ("Loading PC Cleaner...",          60),
            ("Preparing system tray...",       80),
            (f"Starting {APP_BRAND_NAME}...", 100),
        ]
        for text, val in steps:
            splash.update_status(text, val)
            time.sleep(0.4)
        splash.close()

        write_log(
            f"{APP_BRAND_NAME} v{APP_VERSION} started.")
        
        self.icon = pystray.Icon(
            APP_NAME,
            draw_sanda_logo(64, self.is_on),
            (f"{APP_BRAND_NAME} — ON"
             if self.is_on
             else f"{APP_BRAND_NAME} — OFF"),
            menu=self._build_menu()
        )
        
        # Start background threads for new upgrades
        threading.Thread(target=self._auto_clean_loop, daemon=True).start()
        threading.Thread(target=self._ui_refresh_loop, daemon=True).start()
        
        threading.Thread(
            target=self.icon.run,
            daemon=True).start()
        self.root.mainloop()

# ============================================
# ENTRY POINT
# ============================================
if __name__ == "__main__":
    if not is_admin():
        request_admin()
    app = SandaDataSaverTray()
    app.run()
