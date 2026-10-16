"""Build minimal Android fixtures; no proprietary application code or signing keys are used."""
import os
from pathlib import Path
import subprocess
import sys
import zipfile

out = Path(sys.argv[1]).resolve()
out.mkdir(parents=True, exist_ok=True)
sdk = Path(os.environ["ANDROID_HOME"])
tools = sorted((sdk / "build-tools").iterdir(), key=lambda p: tuple(int(n) for n in p.name.split('.') if n.isdigit()))[-1]
platform = sorted((sdk / "platforms").glob("android-*/android.jar"))[-1]
source = out / "Provider.java"
resources = out / "res"
(resources / "values").mkdir(parents=True, exist_ok=True)
(resources / "values" / "strings.xml").write_text('<resources><string name="app_name">Coexistence fixture</string></resources>')
(resources / "values" / "ids.xml").write_text('<resources><item type="id" name="fixture_view"/></resources>')
source.write_text("""package probe;
public final class Provider extends android.content.ContentProvider {
 public boolean onCreate() { return true; }
 public android.database.Cursor query(android.net.Uri u, String[] p, String s, String[] a, String o) { return null; }
 public String getType(android.net.Uri u) { return null; }
 public android.net.Uri insert(android.net.Uri u, android.content.ContentValues v) { return null; }
 public int delete(android.net.Uri u, String s, String[] a) { return 0; }
 public int update(android.net.Uri u, android.content.ContentValues v, String s, String[] a) { return 0; }
}
""")
classes = out / "classes"
classes.mkdir(exist_ok=True)
subprocess.run(["javac", "--release", "8", "-cp", str(platform), "-d", str(classes), str(source)], check=True)
subprocess.run(["jar", "cf", str(out / "probe.jar"), "-C", str(classes), "."], check=True)
subprocess.run([str(tools / "d8"), "--min-api", "26", "--lib", str(platform), "--output", str(out), str(out / "probe.jar")], check=True)
for filename, package in [("original", "com.google.android.youtube"),
                           ("old-youtube", "app.morphe.android.youtube"),
                           ("old-manager", "app.morphe.manager")]:
    manifest = out / "AndroidManifest.xml"
    manifest.write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}" android:versionCode="1" android:versionName="1.0">
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/>
<permission android:name="{package}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" android:protectionLevel="signature"/>
<uses-permission android:name="{package}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>
<permission android:name="{package}.permission.C2D_MESSAGE" android:protectionLevel="signature"/>
<uses-permission android:name="{package}.permission.C2D_MESSAGE"/>
<application android:label="@string/app_name" android:allowBackup="false">
<provider android:name="probe.Provider" android:authorities="{package}.fixture" android:exported="false"/>
</application></manifest>''')
    apk = out / f"{filename}.apk"
    subprocess.run([str(tools / "aapt"), "package", "-f", "-M", str(manifest), "-S", str(resources), "-I", str(platform), "-F", str(apk)], check=True)
    with zipfile.ZipFile(apk, "a") as archive:
        archive.write(out / "classes.dex", "classes.dex")
print("Built original, Morphe YouTube and Morphe Manager fixtures")
