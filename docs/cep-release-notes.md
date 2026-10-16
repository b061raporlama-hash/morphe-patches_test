Cep Patcher is an independent, modified build of Morphe Manager with its own name, application ID and pocket/clock icon. It is not an official Morphe release.

Download `cep-patcher.apk` and install on Android 8.0 or newer. The APK includes **Cep Patches**, containing **Watch later shortcuts**. In expert mode select YouTube, choose a supported original APK, then explicitly select **Watch later shortcuts**. Its dependencies are included automatically. Patch and install the result.

Patch updates use this release's `cep-patches-bundle.json` through the repository's latest-release endpoint. No token is needed to use the public release. The APK is signed with the repository's persistent Cep Patcher key.

Automated checks cover patch regression tests, manager unit tests and lint, APK signature/identity, and actual bundle loading with the manager's Android DEX loader on an Android 15 emulator. These do not prove successful YouTube patching or behavior on a physical phone. Feed cards, real account saves, navigation, offline behavior, root installation and device-specific patching still require phone testing.

`cep-patcher-source.tar.gz` provides corresponding source for this release. GPLv3 and upstream Section 7 notices are preserved in the source, APK legal assets and manager license UI. See `manager/UPSTREAM.md` and `docs/cep-patcher.md` for provenance, build instructions and limitations.
