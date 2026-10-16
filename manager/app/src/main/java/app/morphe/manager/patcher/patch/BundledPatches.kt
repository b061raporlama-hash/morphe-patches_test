/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager.patcher.patch

import java.io.File
import java.io.InputStream

/** Atomic installation also satisfies Android 14+'s read-only dynamic code requirement. */
object BundledPatches {
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
