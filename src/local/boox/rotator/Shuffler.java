package local.boox.rotator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

/**
 * Images live in Pictures because Onyx copies every image it is given into Pictures (and never refreshes an
 * existing copy); handing it files already there avoids a duplicate of each image.
 * Hands Onyx a random image for the next sleep (16) and power-off (17), never repeating the last one. */
final class Shuffler {
    static final String DIR = "/storage/emulated/0/Pictures";
    private static final Random RND = new Random();

    static void shuffle(Context c) {
        File[] files = new File(DIR).listFiles();
        if (files == null) return;
        ArrayList<String> pool = new ArrayList<>();
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && f.length() < 10_000_000L &&
                (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".bmp")))
                pool.add(f.getAbsolutePath());
        }
        if (pool.isEmpty()) return;
        SharedPreferences p = c.getSharedPreferences("last", Context.MODE_PRIVATE);
        // Shuffle-bag: every image is shown once before any repeats (like a Kindle's rotation).
        ArrayList<String> bag = new ArrayList<>();
        // Folder changed (images added/removed) -> start a fresh shuffle so new images join right away.
        if (p.getInt("poolSize", -1) == pool.size())
            for (String x : p.getString("bag", "").split("\n")) if (pool.contains(x)) bag.add(x);
        String sleep = next(bag, pool, p.getString("sleep", null));
        String off = next(bag, pool, sleep);
        send(c, 16, sleep);
        send(c, 17, off);
        p.edit().putString("sleep", sleep).putString("off", off)
                .putString("bag", android.text.TextUtils.join("\n", bag)).putInt("poolSize", pool.size()).apply();
    }

    /** Takes the next image from the bag, refilling (reshuffled, not starting with [avoid]) when empty. */
    private static String next(ArrayList<String> bag, ArrayList<String> pool, String avoid) {
        if (bag.isEmpty()) {
            bag.addAll(pool);
            Collections.shuffle(bag, RND);
            if (bag.size() > 1 && bag.get(0).equals(avoid)) Collections.swap(bag, 0, bag.size() - 1);
        }
        return bag.remove(0);
    }

    private static void send(Context c, int type, String path) {
        Intent i = new Intent("onyx.action.SCREENSAVER");
        i.putExtra("type", type);
        i.putExtra("file", path);
        i.putExtra("show_result_hint", false);
        c.sendBroadcast(i);
    }
}
