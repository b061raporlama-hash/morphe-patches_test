# Cep Patcher

Independent Android patch manager derived from Morphe Manager, bundled with this repository's Cep Patches.
See [UPSTREAM.md](UPSTREAM.md) for origin and modifications, [LICENSE](LICENSE) and [NOTICE](NOTICE) for terms,
and [build and installation instructions](../docs/cep-patcher.md).

Open this directory in Android Studio. Build the root patch project first, copy its `.mpp` to
`app/src/main/assets/cep-patches.mpp`, then run `./gradlew :app:assembleRelease`.
The GitHub Actions workflow automates these steps, tests, signing and distribution.
