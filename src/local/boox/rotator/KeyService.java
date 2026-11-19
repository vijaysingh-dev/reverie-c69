package local.boox.rotator;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * "Reverie Home": remaps the physical Back button (bottom middle): single, double and long press each run a configurable action.
 * Only hardware presses are handled (scanCode != 0); the Back events this service itself injects pass through.
 *
 * Also makes the side page buttons act as volume keys in chosen apps (default: Kindle, which only turns pages on
 * volume keys). Onyx's framework does the conversion whenever Settings.System "switch_key" = 1; this service flips
 * it on window changes. Writing that non-public key needs targetSdk 22 + WRITE_SETTINGS (see manifest).
 */
public class KeyService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String PREFS = "keys";
    static final String SINGLE = "single", DOUBLE = "double", LONG = "long";
    static final String DEF_SINGLE = "g:1", DEF_DOUBLE = "g:2", DEF_LONG = "g:3";
    static final int LONG_MS = 500, DOUBLE_MS = 300;
    static final String VOL_APPS = "volApps";
    static final Set<String> DEF_VOL_APPS = new HashSet<>(java.util.Collections.singleton("com.amazon.kindle"));
    private Set<String> volApps;
    private int switchKey = -1;
    private final HashMap<String, Boolean> isActivity = new HashMap<>();

    private final Handler h = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private String single, dbl, lng;
    private boolean down, longFired, waitingSecond, secondPress;

    private final Runnable onLong = new Runnable() { public void run() { longFired = true; Actions.run(KeyService.this, lng, h); } };
    private final Runnable onSingle = new Runnable() { public void run() { waitingSecond = false; Actions.run(KeyService.this, single, h); } };

    /** True while Android has this service bound (same process as A11yKeeper, which checks it). */
    static volatile boolean connected;

    @Override public boolean onUnbind(android.content.Intent i) { connected = false; return super.onUnbind(i); }

    @Override protected void onServiceConnected() {
        connected = true;
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        load();
    }

    private void load() {
        single = prefs.getString(SINGLE, DEF_SINGLE);
        dbl = prefs.getString(DOUBLE, DEF_DOUBLE);
        lng = prefs.getString(LONG, DEF_LONG);
        volApps = new HashSet<>(prefs.getStringSet(VOL_APPS, DEF_VOL_APPS));
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences p, String k) { load(); }

    @Override protected boolean onKeyEvent(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.KEYCODE_BACK || e.getScanCode() == 0) return false;
        boolean hasLong = !Actions.NONE.equals(lng), hasDouble = !Actions.NONE.equals(dbl);
        // Plain Back with nothing else mapped: leave the key completely alone.
        if (!hasLong && !hasDouble && "g:1".equals(single)) return false;

        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() > 0 || down) return true;
            down = true; longFired = false;
            if (waitingSecond) {                       // second press began within the window
                h.removeCallbacks(onSingle); waitingSecond = false; secondPress = true;
            } else if (hasLong) {
                h.postDelayed(onLong, LONG_MS);
            }
        } else if (e.getAction() == KeyEvent.ACTION_UP) {
            down = false;
            h.removeCallbacks(onLong);
            if (longFired) { longFired = false; return true; }
            if (secondPress) {
                secondPress = false;
                Actions.run(this, dbl, h);
            } else if (hasDouble) {                    // wait to see if a second press follows
                waitingSecond = true; h.postDelayed(onSingle, DOUBLE_MS);
            } else {
                Actions.run(this, single, h);
            }
        }
        return true;
    }

    /** Window changes: when a real activity comes to the front, set page-buttons-as-volume for that app. */
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null
                || e.getClassName() == null) return;
        String pkg = e.getPackageName().toString(), cls = e.getClassName().toString();
        String k = pkg + "/" + cls;
        Boolean act = isActivity.get(k);
        if (act == null) {                         // ignore dialogs, popups, keyboards, the navigation ball...
            try { getPackageManager().getActivityInfo(new ComponentName(pkg, cls), 0); act = true; }
            catch (Exception ex) { act = false; }
            isActivity.put(k, act);
        }
        if (act) setSwitchKey(volApps.contains(pkg) ? 1 : 0);
    }

    private void setSwitchKey(int v) {
        if (v == switchKey) return;
        try { Settings.System.putInt(getContentResolver(), "switch_key", v); switchKey = v; }
        catch (Exception ex) { android.util.Log.w("Reverie", "switch_key write failed", ex); }
    }
    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        connected = false;
        h.removeCallbacksAndMessages(null);
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
}
