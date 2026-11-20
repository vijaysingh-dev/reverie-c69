package local.boox.rotator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.List;

/** Plain, e-ink friendly Reverie Home screen: bottom-button single / double / long press actions, and page-buttons-as-volume apps. */
public class SettingsActivity extends Activity {
    private List<String[]> actions;
    private SharedPreferences prefs;
    private Button volBtn;
    private LinearLayout setup;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(KeyService.PREFS, MODE_PRIVATE);
        actions = Actions.all(this);
        String[] labels = new String[actions.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = actions.get(i)[1];

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        col.setPadding(pad, pad, pad, pad);
        col.setBackgroundColor(Color.WHITE);

        col.addView(text("Reverie", 26, true));
        setup = new LinearLayout(this);
        setup.setOrientation(LinearLayout.VERTICAL);
        col.addView(setup);

        TextView home = text("Reverie Home", 22, true);
        home.setPadding(0, dp(32), 0, 0);
        col.addView(home);

        row(col, labels, "Single press", KeyService.SINGLE, KeyService.DEF_SINGLE);
        row(col, labels, "Double press", KeyService.DOUBLE, KeyService.DEF_DOUBLE);
        row(col, labels, "Long press", KeyService.LONG, KeyService.DEF_LONG);

        TextView pt = text("Page buttons as volume keys", 22, true);
        pt.setPadding(0, dp(32), 0, dp(8));
        col.addView(pt);
        volBtn = new Button(this);
        volBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickVolApps(); } });
        col.addView(volBtn, new LinearLayout.LayoutParams(-1, -2));
        updateVolBtn();

        Button acc = new Button(this);
        acc.setText("Open Accessibility settings");
        acc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(32);
        col.addView(acc, lp);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        sv.addView(col);
        setContentView(sv);
    }

    @Override protected void onResume() {
        super.onResume();
        buildSetup();
    }

    /** Setup checklist (details in README): each missing piece gets a button. */
    private void buildSetup() {
        setup.removeAllViews();
        String pkg = getPackageName();
        String notif = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        String a11y = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean notifOn = notif != null && notif.contains(pkg + "/");
        boolean a11yOn = a11y != null && a11y.contains(pkg + "/");
        boolean canWrite = android.os.Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this);
        boolean ksync = installed("com.onyx.android.ksync");

        TextView h = text("Setup", 18, true);
        h.setPadding(0, dp(20), 0, dp(4));
        setup.addView(h);
        step(notifOn, "Notification access",
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        step(a11yOn, "Accessibility: Reverie Home",
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        step(canWrite, "Modify system settings",
                new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:" + pkg)));
        step(ksync, "Onyx ksync installed", null);
    }

    private void step(boolean ok, String label, final Intent fix) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView t = text((ok ? "\u2713  " : "\u2717  ") + label, 15, !ok);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        if (!ok && fix != null) {
            Button b = new Button(this);
            b.setText("Turn on");
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { try { startActivity(fix); } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS)); } }
            });
            row.addView(b, new LinearLayout.LayoutParams(-2, -2));
        }
        setup.addView(row);
    }

    private boolean installed(String p) {
        try { getPackageManager().getPackageInfo(p, 0); return true; } catch (Exception e) { return false; }
    }

    private void row(LinearLayout col, String[] labels, String title, final String key, String def) {
        TextView t = text(title, 18, true);
        t.setPadding(0, dp(24), 0, dp(4));
        col.addView(t);
        Spinner sp = new Spinner(this, Spinner.MODE_DIALOG);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setPrompt(title);
        String cur = prefs.getString(key, def);
        for (int i = 0; i < actions.size(); i++) if (actions.get(i)[0].equals(cur)) sp.setSelection(i);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.edit().putString(key, actions.get(pos)[0]).apply();
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        col.addView(sp, new LinearLayout.LayoutParams(-1, -2));
    }

    private java.util.Set<String> volApps() {
        return new java.util.HashSet<>(prefs.getStringSet(KeyService.VOL_APPS, KeyService.DEF_VOL_APPS));
    }

    private void updateVolBtn() {
        java.util.Set<String> sel = volApps();
        StringBuilder sb = new StringBuilder();
        for (String[] a : actions)
            if (a[0].startsWith("app:") && sel.contains(a[0].substring(4)))
                sb.append(sb.length() == 0 ? "" : ", ").append(a[1].substring(5));
        volBtn.setText(sb.length() == 0 ? "None (tap to choose)" : sb.toString());
    }

    private void pickVolApps() {
        final java.util.ArrayList<String> pkgs = new java.util.ArrayList<>();
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (String[] a : actions) if (a[0].startsWith("app:")) { pkgs.add(a[0].substring(4)); names.add(a[1].substring(5)); }
        final java.util.Set<String> sel = volApps();
        boolean[] checked = new boolean[pkgs.size()];
        for (int i = 0; i < checked.length; i++) checked[i] = sel.contains(pkgs.get(i));
        new android.app.AlertDialog.Builder(this)
            .setTitle("Page buttons act as volume keys in")
            .setMultiChoiceItems(names.toArray(new String[0]), checked,
                new android.content.DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(android.content.DialogInterface d, int i, boolean on) {
                        if (on) sel.add(pkgs.get(i)); else sel.remove(pkgs.get(i));
                    }
                })
            .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    prefs.edit().putStringSet(KeyService.VOL_APPS, sel).apply();
                    updateVolBtn();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.BLACK);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.START);
        return t;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
