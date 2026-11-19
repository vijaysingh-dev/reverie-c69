package local.boox.rotator;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import java.util.LinkedHashSet;

/**
 * Keeps chosen accessibility services enabled. Onyx force-stops third-party apps shortly after boot, and Android
 * removes a force-stopped app's accessibility service from enabled_accessibility_services, so Reverie's KeyService
 * gets switched off. (KEEP can list other apps' services too; Bitwarden was tried and dropped by the user.) This watches the setting and puts missing entries
 * back. Bounded: the observer (passive, no polling) is only registered for WATCH_MS after start, and at most
 * MAX_REPAIRS writes are made per process lifetime (i.e. per boot); after that it does nothing until next boot.
 * Needs: adb shell pm grant local.boox.rotator android.permission.WRITE_SECURE_SETTINGS
 */
final class A11yKeeper {
    static final String[] KEEP = {
        "local.boox.rotator/local.boox.rotator.KeyService",
    };
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static ContentObserver observer;
    private static Context app;
    static final long WATCH_MS = 180_000;   // watch for 3 min after boot (Onyx's force-stops happen ~10 s in)
    static final int MAX_REPAIRS = 4;       // total settings writes per boot
    private static int repairs;
    private static final Runnable STOP_WATCHING = new Runnable() { public void run() { unregister(); } };
    private static final Runnable CHECK = new Runnable() { public void run() { if (app != null) check(app); } };

    static void start(Context c) {
        app = c.getApplicationContext();
        if (observer == null) {
            observer = new ContentObserver(H) {
                @Override public void onChange(boolean self, Uri uri) { schedule(1500); }
            };
            app.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, observer);
        }
        // Onyx's post-boot force-stops land a few seconds after we start: re-check a few times, then stop watching.
        schedule(0); schedule(15_000); schedule(60_000);
        H.removeCallbacks(STOP_WATCHING);
        H.postDelayed(STOP_WATCHING, WATCH_MS);
    }

    static void stop(Context c) { unregister(); H.removeCallbacks(CHECK); }

    private static void unregister() {
        if (observer != null && app != null) app.getContentResolver().unregisterContentObserver(observer);
        observer = null;
    }

    private static void schedule(long ms) { H.postDelayed(CHECK, ms); }

    static void check(Context c) {
        if (repairs >= MAX_REPAIRS) return;   // budget spent: no work at all (also covers screen-on calls)
        // Without the adb-granted permission (e.g. APK installed on another device) there is nothing we can do.
        if (c.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        try {
            ContentResolver cr = c.getContentResolver();
            String cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            LinkedHashSet<String> have = new LinkedHashSet<>();
            LinkedHashSet<ComponentName> haveCn = new LinkedHashSet<>();
            if (cur != null) for (String s : cur.split(":")) {
                if (s.isEmpty()) continue;
                have.add(s);
                ComponentName cn = ComponentName.unflattenFromString(s);
                if (cn != null) haveCn.add(cn);
            }
            boolean changed = false;
            for (String k : KEEP) {
                ComponentName cn = ComponentName.unflattenFromString(k);
                if (haveCn.contains(cn) || !installed(c, cn.getPackageName())) continue;
                have.add(k); changed = true;
            }
            if (!changed) {
                // Enabled but not running: Android marks a service "crashed" when its process is killed (Onyx's
                // post-boot force-stop) and won't rebind it. Removing and re-adding the entry clears that state.
                String me = KEEP[0];
                if (!KeyService.connected && haveCn.contains(ComponentName.unflattenFromString(me)) && allowWrite()) {
                    LinkedHashSet<String> without = new LinkedHashSet<>();
                    for (String x : have) {
                        ComponentName cn = ComponentName.unflattenFromString(x);
                        if (cn == null || !cn.equals(ComponentName.unflattenFromString(me))) without.add(x);
                    }
                    Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, android.text.TextUtils.join(":", without));
                    Log.i("Reverie", "a11y keeper: KeyService enabled but not bound, toggling");
                    // The observer fires on that write and re-adds it via the branch below.
                }
                return;
            }
            if (!allowWrite()) return;
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, android.text.TextUtils.join(":", have));
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            Log.i("Reverie", "a11y keeper: re-enabled services");
        } catch (Exception e) {
            Log.w("Reverie", "a11y keeper failed", e);
        }
    }

    private static boolean allowWrite() {
        if (repairs >= MAX_REPAIRS) { unregister(); Log.w("Reverie", "a11y keeper: max repairs reached, giving up until reboot"); return false; }
        repairs++;
        return true;
    }

    private static boolean installed(Context c, String pkg) {
        try { c.getPackageManager().getPackageInfo(pkg, 0); return true; } catch (Exception e) { return false; }
    }
}
