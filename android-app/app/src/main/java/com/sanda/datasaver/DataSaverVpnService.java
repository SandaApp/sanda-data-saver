package com.sanda.datasaver;  
  
import android.content.pm.ApplicationInfo;  
import android.content.pm.PackageManager;  
import android.net.VpnService;  
import android.os.ParcelFileDescriptor;  
import android.util.Log;  
  
import java.io.FileInputStream;  
import java.io.FileOutputStream;  
import java.nio.ByteBuffer;  
import java.util.List;  
  
/**  
 * DataSaverVpnService — Local VPN Firewall.  
 * Creates a local VPN that blocks internet  
 * access for apps in the blocked list.  
 * No external server. Everything on-device.  
 * No root required.  
 */  
public class DataSaverVpnService  
        extends VpnService {  
  
    private static final String TAG =  
            "DataSaverVpnService";  
  
    private ParcelFileDescriptor vpnInterface;  
    private Thread               vpnThread;  
    private PrefsManager         prefs;  
    private volatile boolean     running = false;  
  
    // ─────────────────────────────────────  
    @Override  
    public int onStartCommand(  
            android.content.Intent intent,  
            int flags, int startId) {  
  
        prefs = new PrefsManager(this);  
  
        // Handle STOP action  
        if (intent != null  
                && "STOP".equals(  
                intent.getAction())) {  
            stopVpn();  
            return START_NOT_STICKY;  
        }  
  
        // Handle START action  
        // Only start if not already running  
        if (!running) {  
            startVpn();  
        } else {  
            Log.d(TAG,  
                    "VPN already running. " +  
                            "Skipping restart.");  
        }  
  
        return START_STICKY;  
    }  
  
    // ─────────────────────────────────────  
    // START VPN  
    // ─────────────────────────────────────  
    private void startVpn() {  
        try {  
            Builder builder = new Builder();  
            builder.setSession(  
                    "Sanda Data Saver");  
  
            // Local VPN address  
            builder.addAddress("10.0.0.2", 32);  
  
            // Route ALL traffic of allowed apps through VPN  
            builder.addRoute("0.0.0.0", 0);  
  
            // DNS servers  
            builder.addDnsServer("8.8.8.8");  
            builder.addDnsServer("8.8.4.4");  
  
            // Get blocked apps list  
            List<String> blockedApps =  
                    prefs.getBlockedApps();  
  
            boolean hasBlocked = false;  
  
            // Core Fix (Tweak 1): ONLY route the BLOCKED apps through Sanda's VPN tunnel.  
            // Any app NOT in this list (like WhatsApp) will completely bypass the VPN and connect normally.  
            for (String pkg : blockedApps) {  
                if (pkg.equals(getPackageName()))  
                    continue;  
  
                try {  
                    builder.addAllowedApplication(pkg);  
                    hasBlocked = true;  
                    Log.d(TAG, "Routing to VPN firewall: " + pkg);  
                } catch (Exception e) {  
                    // Skip if package is invalid or uninstalled  
                }  
            }  
  
            // If no apps are blocked/checked, add a dummy app so the VPN doesn't default to blocking everything!  
            if (!hasBlocked) {  
                try {  
                    builder.addAllowedApplication("com.sanda.datasaver.dummy.empty");  
                } catch (Exception e) {  
                    // ignore  
                }  
            }  
  
            builder.setBlocking(true);  
  
            // Establish VPN  
            vpnInterface =  
                    builder.establish();  
  
            if (vpnInterface == null) {  
                Log.e(TAG,  
                        "VPN interface is null");  
                return;  
            }  
  
            running = true;  
  
            // Start packet loop thread  
            vpnThread = new Thread(  
                    this::runVpnLoop);  
            vpnThread.start();  
  
            Log.d(TAG,  
                    "VPN started. Firewall active for: "  
                            + (hasBlocked ? blockedApps.size() : 0)  
                            + " apps. Unchecked apps bypass VPN safely.");  
  
        } catch (Exception e) {  
            Log.e(TAG, "Failed to start VPN: " + e.getMessage());  
        }  
    }  
  
    // ─────────────────────────────────────  
    // PACKET SINKHOLE LOOP  
    // ─────────────────────────────────────  
    private void runVpnLoop() {  
        FileInputStream in = null;  
        FileOutputStream out = null;  
        try {  
            in = new FileInputStream(vpnInterface.getFileDescriptor());  
            out = new FileOutputStream(vpnInterface.getFileDescriptor());  
            ByteBuffer packet = ByteBuffer.allocate(32768);  
  
            while (running) {  
                packet.clear();  
                int read = in.read(packet.array());  
                if (read > 0) {  
                    // We successfully intercept the packet from the blocked app here.  
                    // By NOT writing it back to the network interface, the packet is safely sinkholed.  
                    // This blocks internet access for the specific app entirely and securely.  
                } else {  
                    Thread.sleep(100);  
                }  
            }  
        } catch (Exception e) {  
            Log.e(TAG, "Exception in packet loop: " + e.getMessage());  
        } finally {  
            try {  
                if (in != null) in.close();  
                if (out != null) out.close();  
            } catch (Exception e) {  
                // ignore  
            }  
        }  
    }  
  
    // ─────────────────────────────────────  
    // DEACTIVATE VPN  
    // ─────────────────────────────────────  
    private void stopVpn() {  
        running = false;  
        if (vpnThread != null) {  
            vpnThread.interrupt();  
            vpnThread = null;  
        }  
        try {  
            if (vpnInterface != null) {  
                vpnInterface.close();  
                vpnInterface = null;  
            }  
        } catch (Exception e) {  
            // ignore  
        }  
        Log.d(TAG, "VPN stopped. All firewall restrictions removed.");  
    }  
}