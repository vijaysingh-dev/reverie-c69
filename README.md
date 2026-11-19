# Reverie for Onyx BOOX C69

Reverie is a small, no-root companion app for the Onyx BOOX C69 e-reader (BOOX Star Education). It:

- **Rotates the sleep and power-off images.** Each time you wake the reader, it picks the next image from your `Pictures` folder, the way a Kindle rotates its screensavers. Every image is shown once before any repeats.
- **Adds Reverie Home.** You choose what the physical button at the bottom middle does on a single press, a double press and a long press (for example Back, Home and Recents).
- **Turns pages in Kindle with the side buttons.** In the apps you pick, the side page buttons act as volume keys, which is the only thing the Kindle app turns pages with.

It uses about 8–10 MB of RAM and does nothing between screen wakes and button presses.

> Tested on the BOOX Star Education (model `C69_M_STAR`), Android 11, firmware 3.2.2 (build 5465). Other C69 variants on similar firmware should behave the same, but they are untested.

---

## Installation

### What you need

- A BOOX C69 with Wi-Fi or a USB cable, to get the APK onto it.
- Your own images. None are included. See [Adding images](#adding-images).
- Onyx's sync service, `com.onyx.android.ksync`. It is installed on stock devices. Reverie's setup screen tells you if it's missing.

### 1. Remove an older Reverie (only if you have one)

If you installed Reverie from somewhere other than this repository's **Releases** page, uninstall it first: long-press the Reverie icon → **Uninstall**. Otherwise the install fails with **"App not installed"**, because older copies are signed or built differently.

### 2. Download the APK

On the C69 or on a computer, open the [latest release](https://github.com/vijaysingh-dev/reverie-c69/releases/latest) and download **`Reverie-1.0.0.apk`**.

If you downloaded it on a computer, copy it to the C69 with BooxDrop, a USB cable or any file-transfer app. The `Download` folder is a good place.

### 3. Install it

1. On the C69, open **Storage** → **Download** and tap **`Reverie-1.0.0.apk`**.
   - If the file isn't listed yet, go back and open the folder again; Onyx's file index can take a moment to update.
   - If Android asks to allow installing apps from this source, allow it and go back.
2. Tap **Install**, then **Open**.
3. Android shows **"Choose what to allow Reverie to access"** (Files and media). Tap **Continue**.
4. Android warns that **"This app was built for an older version of Android"**. Tap **OK**. This is expected; see [Why the "older Android" warning](#why-the-older-android-warning).

Reverie's screen opens with a **Setup** checklist at the top.

### 4. Complete the Setup checklist

Each item shows ✓ when it's done, or ✗ with a **Turn on** button.

| Item | What to do |
|---|---|
| **Notification access** | Tap **Turn on** → tap **Reverie** → switch on **Allow notification access** → **Allow**. Press Back twice to return. Reverie reads no notifications; this is only how Android keeps it running after every restart. |
| **Accessibility: Reverie Home** | Tap **Turn on** → tap **Reverie Home** → switch on **Use Reverie Home** → **Allow**. Press Back twice. This is what reads the bottom button and switches the page buttons. It reads no screen content. |
| **Modify system settings** | Usually already ✓. If not, tap **Turn on** and allow it. |
| **Onyx ksync installed** | Usually already ✓. If ✗, see [Troubleshooting](#troubleshooting). |

### 5. Keep Reverie Home on after restarts (important)

At every boot, Onyx force-stops each app it "optimizes", and Android switches off the accessibility service of any app that is force-stopped. So unless you turn Onyx's optimization off for Reverie, **Reverie Home stops working after every restart**.

1. Go to **Apps**, long-press the **Reverie** icon and tap **Optimize**.
2. On the **Display** tab, tap **Optimization Switch** (the "HD" icon at the bottom right).
3. The icon gets a line through it and the panel turns grey. Close the panel.

Reverie has no reading screen to optimize, so this changes nothing visible.

> Onyx's **Others → "Allow the app to run in the background"** toggle does **not** prevent the force-stop. Use the Optimization Switch.

### 6. Add your images

Put your images directly in the **`Pictures`** folder. See [Adding images](#adding-images) for sizes and tips.

Then press the power button to sleep the reader, and wake it again. The image for the next sleep is chosen each time the screen wakes, so the first new image appears from the second sleep onward.

### 7. Choose your buttons

Open **Reverie** from the Apps screen:

- **Reverie Home**: choose an action for **Single press**, **Double press** and **Long press** of the bottom-middle button. Defaults: Back, Home, Recents.
- **Page buttons as volume keys**: tap the button and tick the apps where the side buttons should act as volume keys. Kindle is ticked by default.

For Kindle, also make sure the Kindle app's own **Turn pages with volume controls** setting is on. It's in the reading settings of an open book (the **Aa** menu; its exact place varies between Kindle versions).

Changes take effect immediately.

---

## Adding images

- **Folder:** put the files **directly** in `Pictures` (internal storage), not in a subfolder. Subfolders such as `Pictures/Screenshots` are ignored.
- **Formats:** PNG, JPG/JPEG or BMP, under 10 MB each.
- **Size:** the C69 screen is **758 × 1024** pixels in portrait. Images of exactly that size display edge to edge without scaling. Grayscale images look best on e-ink.
- **Adding or removing images:** just copy or delete files. Reverie notices the change at the next wake and starts a fresh shuffle that includes new images.
- **Replacing an image:** overwrite the file in `Pictures` with the same name. Onyx keeps its own copy of each image it has shown and displays that copy. Because Reverie hands Onyx files that already live in `Pictures`, overwriting there updates what you see.

---

## Using Reverie Home

The bottom-middle button can run any of these on single, double or long press:

| Action | What it does |
|---|---|
| Do nothing | Ignores that kind of press. |
| Back | Normal Back. |
| Home | Goes to the home screen. |
| Recents | Opens the recent-apps screen. |
| Previous app | Switches to the app you used before this one. |
| Full screen refresh | Full e-ink refresh to clear ghosting. |
| Notifications | Opens the notification shade. |
| Quick settings | Opens quick settings. |
| Screenshot | Takes a screenshot. |
| Lock screen (sleep) | Puts the reader to sleep. |
| Power menu | Shows the power-off / restart menu. |
| Open *app name* | Opens any installed app. |

Timing: a long press is 0.5 s. When **Double press** has an action, each single press waits 0.3 s to see whether a second press follows. Set Double press to **Do nothing** if you want single presses to respond instantly.

Only the physical button is affected. Back on the navigation ball and in apps works as usual.

---

## Troubleshooting

**"App not installed" when installing.** An older Reverie is installed. Uninstall it (long-press the icon → Uninstall), then install again.

**"Open" is greyed out after installing, or there is no Reverie icon.** You installed a very old version that had no screen. Uninstall it and install the APK from the Releases page.

**Double and long press stop working after a restart.** Do [step 5](#5-keep-reverie-home-on-after-restarts-important). Then turn Reverie Home back on: open Reverie → **Accessibility: Reverie Home** → **Turn on**.

**The sleep image never changes.**
- Check that **Notification access** is ✓ in Reverie's Setup.
- Check that your images are directly in `Pictures` and are PNG, JPG or BMP.
- Check that **Onyx ksync installed** is ✓. On this firmware Onyx only accepts new sleep images while `com.onyx.android.ksync` is installed. If you removed it while debloating, restore it from a computer: `adb shell cmd package install-existing com.onyx.android.ksync`.
- If it still doesn't change, restart the reader. On rare occasions Onyx's launcher gets stuck applying an image; restarting clears it (or, with adb: `adb shell am force-stop com.onyx`).

**The side buttons don't turn pages in Kindle.**
- Open Reverie and check that Kindle is listed under **Page buttons as volume keys**.
- Check Kindle's own **Turn pages with volume controls** setting.
- Check that **Accessibility: Reverie Home** is ✓.

**A "set successfully" message pops up every time the reader wakes.** That's Onyx's launcher confirming each new sleep image; this firmware shows it regardless of what Reverie asks. You can hide it from a computer with `adb shell cmd appops set com.onyx TOAST_WINDOW deny`. This also hides the launcher's other pop-up messages. Undo with `... TOAST_WINDOW allow`.

---

## Uninstalling

Long-press the Reverie icon → **Uninstall**. Your images in `Pictures` are not touched. The last sleep image stays set until you choose another one in Onyx's settings.

---

## How it works

### Rotating sleep images

- Onyx's launcher (`com.onyx`) accepts a new standby image through the broadcast `onyx.action.SCREENSAVER`, with the extras `type` (16 = sleep, 17 = power-off), `file` (path) and `show_result_hint`. This was first documented in the [Babel KOReader plugin](https://github.com/Jouskaio/Babel/pull/33).
- On firmware 3.2.2, Onyx only applies the broadcast while `com.onyx.android.ksync` is installed.
- Reverie's `ShuffleListener` is a `NotificationListenerService`. Android rebinds a notification listener after every boot, even after Onyx force-stops third-party apps, which makes it a reliable way to stay alive without a foreground notification. It ignores all notifications. On every `SCREEN_ON`, it gives Onyx the next image for the next sleep and a different one for the next power-off, using a shuffle bag so every image appears once before any repeat.
- Images are read from `Pictures` because Onyx copies every image it's given into `Pictures/<name>`, shows that copy, and never overwrites an existing copy. Handing it files that already live there avoids keeping a duplicate of every image.

### Reverie Home

- `KeyService` is an `AccessibilityService` that filters key events. It only handles the hardware Back key (events with a scan code), so the Back events it sends itself pass through untouched.
- Actions use `performGlobalAction`. The full refresh sends `onyx.android.intent.action.REFRESH_SCREEN`, which Onyx's SystemUI handles.

### Page buttons as volume keys

- Onyx's framework (`PhoneWindowHelper.sendKeyEventBySwitch`) converts `PAGE_UP`/`PAGE_DOWN` into `VOLUME_UP`/`VOLUME_DOWN` whenever the system setting `switch_key` is `1`. On this firmware nothing sets it per app; the per-app `usePageKeyAsVolumeKey` flag in Onyx's optimization config is ignored.
- `KeyService` watches window changes and sets `switch_key` to `1` while a chosen app is in front, and back to `0` otherwise. A normal app cannot send volume key presses to another app, so this is the only no-root route.

### Why the "older Android" warning

Android only lets apps that target Android 5.1 (API 22) or lower write non-public system settings such as `switch_key`. Reverie therefore targets API 22. As a result, Android shows the "built for an older version of Android" warning and a permission review the first time it opens, and an update from a copy that targeted a newer API is refused until that copy is uninstalled.

### Why the Optimization Switch matters

When Onyx's optimization service starts at boot (`OECService.eacEnableStatusChangedImpl`), it force-stops every running app whose per-app optimization is enabled, so those apps restart with their display settings. Only the current keyboard is exempt. Android then removes a force-stopped app's accessibility service from the enabled list. With optimization switched off for Reverie, Onyx skips it.

### Optional: automatic re-enable (adb)

As a fallback, Reverie can re-enable Reverie Home by itself if it is ever switched off, but only if you grant it a permission from a computer:

```
adb shell pm grant local.boox.rotator android.permission.WRITE_SECURE_SETTINGS
```

With that grant, `A11yKeeper` watches the accessibility setting for the first 3 minutes after boot and on screen wake, and puts Reverie Home back if it's missing. It makes at most 4 changes per boot, then does nothing until the next restart. Without the grant it does nothing.

---

## Building from source

The build uses Ubuntu's packaged Android tools instead of Gradle or the Google SDK.

```
sudo apt install openjdk-21-jdk-headless android-sdk-platform-23 aapt apksigner dalvik-exchange zipalign
./build.sh
```

The APK is written to `build/Reverie.apk`. The first build creates a signing key, `debug.keystore`, which is kept out of the repository. A build you make yourself is signed with your own key, so it cannot update a release APK, and the reverse is also true: uninstall one before installing the other.

Install with adb:

```
adb install -r build/Reverie.apk
```

Then open Reverie on the device and complete the [Setup checklist](#4-complete-the-setup-checklist) and [step 5](#5-keep-reverie-home-on-after-restarts-important).

`python3 tools_icons.py res` regenerates the launcher icon (needs Pillow).

### Project layout

```
AndroidManifest.xml
build.sh                     build script (aapt, javac, dx, zipalign, apksigner)
res/                         icon, strings, accessibility service config
src/local/boox/rotator/
  ShuffleListener.java       stays alive across boots, rotates images on screen wake
  Shuffler.java              shuffle bag, sends onyx.action.SCREENSAVER
  KeyService.java            Reverie Home (accessibility): button actions, switch_key per app
  Actions.java               the actions a press can run
  SettingsActivity.java      the app screen: setup checklist and settings
  A11yKeeper.java            optional re-enable of Reverie Home (needs the adb grant)
tools_icons.py               icon generator
```

---

## License

Personal, non-commercial use only. Download Reverie only from this repository; redistribution and selling are not allowed. See [LICENSE](LICENSE).

Onyx and BOOX are trademarks of their respective owner. Reverie is not affiliated with Onyx.
