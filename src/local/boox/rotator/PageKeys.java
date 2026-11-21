package local.boox.rotator;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * Swaps the two side page buttons. Onyx's key handler (android.onyx.util.KeyboardEntryMap, in system_server) reads a
 * per-key override from Settings.Global "<KEYCODE name>" as JSON and reloads on "onyx.action.refresh_keymapping".
 * Only shortPressAction is set, so Onyx's long-press actions stay as they are. The stock short action
 * "switchByConf" (page key, or volume key when switch_key = 1) is replaced by an explicit key, so the volume choice
 * per app is made here. Writing Settings.Global needs WRITE_SECURE_SETTINGS (granted with adb).
 */
final class PageKeys {
    static final String SWAP = "swapPages";
    private static final String UP = "KEYCODE_PAGE_UP", DOWN = "KEYCODE_PAGE_DOWN";

    static boolean allowed(Context c) {
        return c.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    /** swap off: remove the overrides (Onyx defaults). swap on: page up acts as page down (or volume down), and back. */
    static void apply(Context c, boolean swap, boolean volume) {
        if (!allowed(c)) return;
        boolean changed = set(c, UP, swap ? json(UP, volume ? "KEYCODE_VOLUME_DOWN" : DOWN) : null);
        changed |= set(c, DOWN, swap ? json(DOWN, volume ? "KEYCODE_VOLUME_UP" : UP) : null);
        if (changed) c.sendBroadcast(new Intent("onyx.action.refresh_keymapping"));
    }

    private static String json(String key, String action) {
        return "{\"keySet\":[\"" + key + "\"],\"shortPressAction\":\"" + action + "\",\"repeat\":false}";
    }

    private static boolean set(Context c, String key, String v) {
        String cur = Settings.Global.getString(c.getContentResolver(), key);
        if (v == null ? cur == null || cur.isEmpty() : v.equals(cur)) return false;
        try { return Settings.Global.putString(c.getContentResolver(), key, v); }
        catch (Exception e) { android.util.Log.w("Reverie", "page key write failed", e); return false; }
    }
}
