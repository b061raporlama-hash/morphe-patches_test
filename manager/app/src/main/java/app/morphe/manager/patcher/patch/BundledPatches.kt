/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager.patcher.patch

import java.io.File
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipInputStream
import io.github.z4kn4fein.semver.Version

/** Atomic installation also satisfies Android 14+'s read-only dynamic code requirement. */
object BundledPatches {
    fun version(input: InputStream): String = input.use { stream ->
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.equals("META-INF/MANIFEST.MF", ignoreCase = true)) {
                    return Manifest(zip).mainAttributes.getValue("Version")
                        ?: error("Bundled patches have no version")
                }
                entry = zip.nextEntry
            }
            error("Bundled patches have no manifest")
        }
    }

    fun shouldInstall(bundled: String, installed: String?): Boolean {
        val available = Version.parse(bundled, strict = false)
        val current = installed?.let { runCatching { Version.parse(it, strict = false) }.getOrNull() }
        return current == null || available > current
    }

    /** Refresh offline on manager upgrade; never downgrade a newer downloaded source. */
    fun installIfNewer(openAsset: () -> InputStream, destination: File): Boolean {
        val bundled = version(openAsset())
        val installed = runCatching {
            JarFile(destination).use { it.manifest.mainAttributes.getValue("Version") }
        }.getOrNull()
        if (!shouldInstall(bundled, installed)) return false
        install(openAsset(), destination)
        return true
    }

    fun install(input: InputStream, destination: File) {
        destination.parentFile!!.mkdirs()
        val staging = File(destination.parentFile, "bundled-patches.tmp")
        try {
            staging.delete()
            input.use { source ->
                staging.outputStream().use { output ->
                    check(staging.setReadOnly()) { "Cannot protect bundled patches" }
                    source.copyTo(output)
                }
            }
            check(staging.length() > 0) { "Empty bundled patches" }
            check(staging.renameTo(destination)) { "Cannot install bundled patches" }
        } finally {
            staging.delete()
        }
    }
}
