# Sanda Apps Website Directory (davidsanda.com/apps)

This directory hosts the web presence, help center, download tracker, and download assets for the **Sanda Data Saver** project suite, designed and maintained by Bishop David Sanda.

---

## 📁 Files to Upload & Target Locations
Upload the updated files in your workspace to your web server using cPanel File Manager or FTP in the following mapping:

| File / Folder | Upload Destination | Description |
| :--- | :--- | :--- |
| **`index.html`** | `/public_html/apps/index.html` | The primary landing page. |
| **`privacy.html`** | `/public_html/apps/privacy.html` | The privacy policy page. |
| **`help.html`** | `/public_html/apps/help.html` | The help guide and FAQ page. |
| **`test_counter.html`** | `/public_html/apps/test_counter.html` | API counter diagnostic page. |
| **`counter.php`** | `/public_html/apps/counter.php` | The PHP download tracker API. |
| **`download_counts.json`**| `/public_html/apps/download_counts.json`| Local flat-file counts storage. |
| **`images/`** (Folder) | `/public_html/apps/images/` | Site favicons, icons, and screenshots. |
| **`.htaccess`** | Root folder of subdomain `/public_html/apps/` | Configures redirect for `apps.davidsanda.com`. |

---

## 🔀 Subdomain Redirect (`apps.davidsanda.com`)
If you want `apps.davidsanda.com` to redirect seamlessly to `www.davidsanda.com/apps/`:

1. Create a DNS **A record** (or CNAME) in your domain zone editor for the host `apps` pointing to your server's shared IP address.
2. Place the optimized `.htaccess` file inside the root folder designated for the `apps.davidsanda.com` subdomain on your server.
3. *Note:* If your server uses **Nginx** instead of Apache, add this block to your Nginx server configuration instead:
   ```nginx
   server {
       server_name apps.davidsanda.com;
       return 301 https://www.davidsanda.com/apps$request_uri;
   }
   ```
   *(If you don't require the subdomain, `www.davidsanda.com/apps` operates fully without any additional server configurations!)*

---

## 📊 Download Counter Configuration

### Option A: PHP Counter (Easiest & Configured by Default)
* Upload `counter.php` and `download_counts.json` to the same folder as `index.html` (i.e. `/public_html/apps/`).
* The JavaScript in `index.html` uses a local relative path (`counter.php`), so tracking will work out-of-the-box.
* **File Permissions:** Ensure `/public_html/apps/` has write permissions (typically `0755` for directories and `0644` for files). If permission settings restrict folder writing, `counter.php` automatically uses the server's temp directory `/tmp/` as a fallback.
* **Verification:** Visit `https://www.davidsanda.com/apps/test_counter.html` or navigate directly to `https://www.davidsanda.com/apps/counter.php` to verify JSON counts are active.

### Option B: Supabase (Alternative for Static/No-PHP Hosting Only)
If you ever migrate to a static CDN host that does not support PHP execution:
1. Go to [supabase.com](https://supabase.com) and create a free project.
2. In the SQL Editor, create a table called `downloads` with two columns: `platform` (text, primary key) and `count` (int, default 0).
3. Insert starting rows: `platform='android', count=28` and `platform='windows', count=10`.
4. Enable **Row Level Security (RLS)** and construct a policy allowing anonymous reads and updates.
5. Fetch your Project API URL and anon keys from Settings → API.
6. Swap the javascript `loadDownloadCount()` and `trackDownload()` methods in `index.html` with your Supabase JS Client endpoints.

---

## 💰 Donation Channels
The dashboard contains predefined hooks to support the continuing ministries:
* **Paystack Shop:** `https://paystack.shop/pay/f25qa34d6z` (Enables card payments, USSD, and bank transfers).
* **Direct Bank Transfer:** 
  * **Account Name:** DAVID SANDA
  * **Account Number:** `6110409146`
  * **Bank:** OPAY

---

## 💬 Feedback Form Setup
The landing page incorporates **Formsubmit.co** for completely free, database-free support emails!
* The form action is configured to send entries directly to `sandadatasaver@gmail.com`.
* **First-Time Activation:** When the first message is submitted, Formsubmit.co will send a verification email to your inbox containing a confirmation link. Click that link once to activate the form.
* **Customizing Target Email:** If you need to change the destination, search for the `form` element in `index.html` and replace the email parameter:
  ```html
  <form action="https://formsubmit.co/YOUR_EMAIL@example.com" method="POST">
  ```

---

## ⚠️ Important Binary Asset Mapping
Please confirm that your compiled Android App and Windows Setup installers are uploaded to the correct folder. 

Depending on your cPanel structure, place them in either:
* **Option A (Subfolder):** `/public_html/apps/softs/`
  * Android APK: `https://www.davidsanda.com/apps/softs/sanda_data_saver.apk`
  * Windows EXE: `https://www.davidsanda.com/apps/softs/SandaDataSaver_Setup_v1.0.exe`
* **Option B (Root domain):** `/public_html/softs/`
  * Android APK: `https://www.davidsanda.com/softs/sanda_data_saver.apk`
  * Windows EXE: `https://www.davidsanda.com/softs/SandaDataSaver_Setup_v1.0.exe`

*(Note: The link attributes in your workspace's `index.html` are configured to point to `/apps/softs/` by default. If you prefer to host them at the root `/softs/`, simple let me know, or search for `apps/softs` in `index.html` and edit them to `softs` using our files editor!)*
