#!/usr/bin/env bash
set -euo pipefail
dir="$1"
tools=$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)
for key in old cep; do
  keytool -genkeypair -keystore "$dir/$key.jks" -storepass android -keypass android -alias test -keyalg RSA -keysize 2048 -validity 30 -dname "CN=Temporary $key coexistence fixture"
done
sign() {
  local input="$1" output="$2" key="$3"
  "$tools/zipalign" -f 4 "$dir/$input" "$dir/aligned.apk"
  "$tools/apksigner" sign --ks "$dir/$key.jks" --ks-pass pass:android --key-pass pass:android --out "$dir/$output" "$dir/aligned.apk"
}
sign old-youtube.apk morphe-youtube.apk old
sign old-manager.apk morphe-manager.apk old
sign old-youtube.apk conflicting-youtube.apk cep
sign cep-unsigned.apk cep-youtube.apk cep
"$tools/aapt" dump badging "$dir/cep-youtube.apk" | grep "package: name='app.cep.android.youtube'"
"$tools/aapt" dump xmltree "$dir/cep-youtube.apk" AndroidManifest.xml > "$dir/identity.txt"
grep 'app.cep.android.youtube.fixture' "$dir/identity.txt"
grep 'app.cep.android.youtube.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION' "$dir/identity.txt"
grep 'app.cep.android.youtube.permission.C2D_MESSAGE' "$dir/identity.txt"
