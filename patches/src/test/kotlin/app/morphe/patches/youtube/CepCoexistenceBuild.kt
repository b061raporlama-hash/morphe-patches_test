/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.patches.youtube

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.dex.SdkDexVerifier
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.all.misc.clone.cloneAppPatch
import app.morphe.patches.all.misc.clone.setOrGetFallbackPackageName
import app.morphe.patches.shared.misc.gms.remapGmsManifestIdentity
import app.morphe.patches.youtube.misc.gms.Constants.CEP_YOUTUBE_PACKAGE_NAME
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

/** Runs production clone/fallback/manifest identity code on a redistributable synthetic APK. */
fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: CepCoexistenceBuildKt <original-fixture.apk> <output.apk>" }
    val input = File(args[0]).canonicalFile
    val output = File(args[1]).canonicalFile
    val sdk = File(System.getenv("ANDROID_HOME") ?: error("ANDROID_HOME required"))
    val identityProbe = resourcePatch {
        dependsOn(cloneAppPatch)
        execute {
            val target = setOrGetFallbackPackageName(CEP_YOUTUBE_PACKAGE_NAME)
            val manifest = get("AndroidManifest.xml")
            manifest.writeText(remapGmsManifestIdentity(manifest.readText(), "com.google.android.youtube", target))
        }
    }
    Patcher(PatcherConfig(input, Files.createTempDirectory("cep-coexistence").toFile(),
        verifier = SdkDexVerifier(sdk))).use { patcher ->
        patcher += setOf(identityProbe)
        runBlocking { patcher().collect { result -> result.exception?.let { throw it } } }
        patcher.get().applyTo(output)
    }
    println("Coexistence probe produced through production identity code: $CEP_YOUTUBE_PACKAGE_NAME")
}
