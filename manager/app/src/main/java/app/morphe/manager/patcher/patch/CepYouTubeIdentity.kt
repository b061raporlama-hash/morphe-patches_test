/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager.patcher.patch

import app.morphe.manager.util.Options

/** Migrate only the old Cep built-in source's explicitly stored Morphe YouTube identity. */
object CepYouTubeIdentity {
    const val PACKAGE_NAME = "app.cep.android.youtube"

    fun migrateLegacyOptions(inputPackage: String, options: Options): Options {
        if (inputPackage != "com.google.android.youtube") return options
        val source = options[0] ?: return options
        val clone = source["Clone app"] ?: return options
        if (clone["packageName"] != "app.morphe.android.youtube") return options
        return options + (0 to (source + ("Clone app" to (clone + ("packageName" to PACKAGE_NAME)))))
    }
}
