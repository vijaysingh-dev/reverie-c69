package local.boox.rotator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;

/**
 * Bound by the system at every boot (even after Onyx force-stops the app), so it is a reliable,
 * notification-free way to stay alive. It ignores notifications; it only listens for screen-on.
 */
public class ShuffleListener extends NotificationListenerService {
    private boolean registered;
    private final BroadcastReceiver onWake = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { A11yKeeper.check(c); Shuffler.shuffle(c); }
    };

    @Override public void onListenerConnected() {
        if (!registered) {
            registerReceiver(onWake, new IntentFilter(Intent.ACTION_SCREEN_ON));
            registered = true;
        }
        A11yKeeper.start(this);
        Shuffler.shuffle(this);
    }


    // API 24+ callback (not in the API-23 stub jar), still invoked on Android 11.
    public void onListenerDisconnected() {
        if (registered) { unregisterReceiver(onWake); registered = false; }
        A11yKeeper.stop(this);
    }
}
