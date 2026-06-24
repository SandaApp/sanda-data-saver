# 🔑 SANDA APPS HANDOVER NOTES & BUILD GUIDE (v1.0.1 Release)

This document serves as your official developer handover note and comprehensive compilation guide for the **Sanda Data Saver** project suite. It outlines what was accomplished, how to compile your software, and how to deploy your website pages cleanly.

---

## 🛠️ Work Accomplished in This Session

### 1. Web Platform (v1.0.2 Sync)
* **`index.html` & `help.html`:** Updated to feature the new **Android v1.0.2** capabilities (Phone Cleaner, Schedule Timer, Data Usage Alerts, Widget) and **Windows PC v1.0.1** capabilities.
* **`.htaccess`:** Fixed a fatal markdown link formatting bug that would have crashed your Apache server with a **500 Internal Server Error**. Corrected rules to handle both `apps.davidsanda.com` and `www.apps.davidsanda.com` subdomain redirects.
* **`counter.php`:** Resolved a critical compile parse bug where markdown bolding had mutated the PHP magic constant `__DIR__` into `**DIR**`.
* **`download_counts.json`:** Saved clean initial metrics: **Android: 28**, **Windows: 10**.

### 2. PC Desktop App (v1.0.1 Python Upgrade)
* **`sanda_pc_app.py`:** 
  * Fixed tray icon left-click/hover looping bug by removing `default=True` from pystray item flags (only right-click menu controls the state now).
  * Resolved standard Tkinter checkbox dark theme visibility: forced a crisp **white checkmark** on selected items.
  * Integrated **Windows Startup Auto-Start Registry hook** toggled in-app.
  * Integrated **Selective Clean options** to check/uncheck clean targets.
  * Integrated **Silent background auto-clean loop** running every 24 hours.
  * Integrated **Real-time estimated saved bandwidth calculator** tracking active session hotspot MBs saved, visible in menu statuses and hover titles.
  * Added beautiful Christian Ministry branding: *"✝️ This software is provided 100% free of charge for the Glory of Jesus Christ, my Savior."* inside the **About Window**.

### 3. Inno Setup Compiler (`sanda_pc_setup.iss`)
* **`sanda_pc_setup.iss`:** 
  * Corrected Pascal code syntax: fixed HTML entity `&gt;` back to a clean logical check `>` to prevent compiler errors.
  * Aligned software versions to **v1.0.1** and setup setup names to generate `SandaDataSaver_Setup_v1.0.1.exe`.
  * Customized welcome installer text to display the spiritual dedication to our Savior.

---

## 🖥️ PC App Build & Compilation Steps (Python to EXE)

Follow these instructions on your local Windows PC to build the executable from Python:

### Step 1: Install Dependencies
Open your Command Prompt (`cmd`) and install the required library packages:
```bash
pip install pystray pillow PyInstaller
```

### Step 2: Compile the Standalone EXE
To bundle the script into a single-file, windowless executable, run this `PyInstaller` command:
```bash
pyinstaller --noconsole --onefile --admin --icon=assets/icon.ico --name=SandaDataSaver sanda_pc_app.py
```
*(Make sure you have your icon file at `assets/icon.ico` relative to where you run the command!)*

### Step 3: Inspect the Output
* Once completed, your compiled standalone executable will be saved in the newly created **`dist\`** folder: `dist\SandaDataSaver.exe`.
* Test run the `.exe` as Administrator to verify the tray icon boots up successfully.

---

## 📦 Creating the Installer (Inno Setup)

To compile the final client installation file using Inno Setup:

1. Download and open **Inno Setup Compiler** on your PC.
2. Open your updated script file: **`sanda_pc_setup.iss`**.
3. Confirm that your compiled executable is located at `dist\SandaDataSaver.exe` relative to the script path.
4. Click the **Compile** (green play button) in Inno Setup.
5. Once completed, your standalone setup file will be saved in the folder: `InstallerOutput\SandaDataSaver_Setup_v1.0.1.exe`.
6. Upload this final setup file to your cPanel softs folder!

---

## 🌐 cPanel Website Deployment Checklist

Upload your website files to `/public_html/apps/` on your server in these exact paths:

* `/public_html/apps/index.html`
* `/public_html/apps/help.html`
* `/public_html/apps/privacy.html`
* `/public_html/apps/test_counter.html`
* `/public_html/apps/counter.php`
* `/public_html/apps/download_counts.json` (Ensure write permissions: set to **`0644`** or **`0666`**)
* Subdomain Redirect `.htaccess` goes directly into your subdomain root directory.

---

## 📱 Android App Sign & Release Build Guide (Java)

When compiling your Android updates inside **Android Studio**:

1. Keep `minifyEnabled false` in your `build.gradle` (Module:app) file unless you have tested custom ProGuard rules.
2. To build the signed binary, select **Build → Generate Signed Bundle / APK...**
3. Choose **APK** (or Android App Bundle `AAB` for Play Store).
4. Point to your secure Keystore backup: **`sanda_release.jks`**.
5. Input your private Keystore password, key alias, and key password.
6. Select **release** build variant and set signature versions to V1 and V2.
7. Click **Finish** and upload the signed APK to `davidsanda.com/apps/softs/sanda_data_saver.apk`.

---

*“Commit your work to the Lord, and your plans will be established.” — Proverbs 16:3* 🙏✝️
