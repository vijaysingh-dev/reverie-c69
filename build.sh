#!/usr/bin/env bash
# Builds Reverie APK with Ubuntu's packaged Android tools (no Gradle / Google SDK).
set -euo pipefail
cd "$(dirname "$0")"
SDK=/usr/lib/android-sdk; BT=$SDK/build-tools/debian; JAR=$SDK/platforms/android-23/android.jar
rm -rf build && mkdir -p build/classes
aapt package -f -M AndroidManifest.xml -S res -I $JAR -F build/unsigned.apk
javac --release 8 -nowarn -Xlint:-options -cp $JAR -d build/classes $(find src -name "*.java")
java -jar $BT/lib/dx.jar --dex --output=build/classes.dex build/classes
(cd build && cp classes.dex classes.dex.tmp && mv classes.dex.tmp classes.dex && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -f 4 build/unsigned.apk build/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias rotator -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=Reverie" >/dev/null 2>&1
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out build/Reverie.apk build/aligned.apk
echo "built: $(pwd)/build/Reverie.apk"
