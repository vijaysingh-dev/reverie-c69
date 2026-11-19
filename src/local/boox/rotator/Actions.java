package local.boox.rotator;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The actions a back-button press can be mapped to. Stored as strings: "none", "g:<id>", "app:<pkg>", "refresh". */
final class Actions {
    static final String NONE = "none";

    /** {id, label} for the built-in actions, in menu order. */
    static final String[][] BUILT_IN = {
        {"none", "Do nothing"},
        {"g:1", "Back"},
        {"g:2", "Home"},
        {"g:3", "Recents"},
        {"last", "Previous app"},
        {"refresh", "Full screen refresh"},
        {"g:4", "Notifications"},
        {"g:5", "Quick settings"},
        {"g:9", "Screenshot"},
        {"g:8", "Lock screen (sleep)"},
        {"g:6", "Power menu"},
    };

    /** Built-in actions followed by "Open <app>" for every launcher app. Each entry is {id, label}. */
    static List<String[]> all(Context c) {
        List<String[]> out = new ArrayList<>();
        Collections.addAll(out, BUILT_IN);
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        List<String[]> appItems = new ArrayList<>();
        for (ResolveInfo r : apps) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(c.getPackageName())) continue;
            appItems.add(new String[]{"app:" + pkg, "Open " + r.loadLabel(pm)});
        }
        Collections.sort(appItems, new java.util.Comparator<String[]>() {
            public int compare(String[] a, String[] b) { return a[1].compareToIgnoreCase(b[1]); } });
        out.addAll(appItems);
        return out;
    }

    static void run(final AccessibilityService s, String id, Handler h) {
        if (id == null || id.equals(NONE)) return;
        if (id.startsWith("g:")) {
            s.performGlobalAction(Integer.parseInt(id.substring(2)));
        } else if (id.equals("last")) {
            // Double-tapping Recents switches to the previous app (stock Android behaviour).
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
            h.postDelayed(new Runnable() { public void run() { s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS); } }, 250);
        } else if (id.equals("refresh")) {
            s.sendBroadcast(new Intent("onyx.android.intent.action.REFRESH_SCREEN"));
        } else if (id.startsWith("app:")) {
            Intent i = s.getPackageManager().getLaunchIntentForPackage(id.substring(4));
            if (i != null) s.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
