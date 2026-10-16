/* Copyright 2026 Morphe. Modified 2026-10-16 by Cep Patcher contributors.
 * https://github.com/MorpheApp/morphe-patches
 * Original hard forked code: https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 * Extracted from GmsCoreSupportPatch. GPL-3.0-only; see repository NOTICE for Section 7 terms.
 */
package app.morphe.patches.shared.misc.gms

/** Shared by the actual GmsCore resource patch and the install-coexistence probe. */
internal fun remapGmsManifestIdentity(manifest: String, fromPackageName: String, toPackageName: String): String {
    val transformations = mapOf(
        "package=\"$fromPackageName" to "package=\"$toPackageName",
        "android:authorities=\"$fromPackageName" to "android:authorities=\"$toPackageName",
        "$fromPackageName.permission.C2D_MESSAGE" to "$toPackageName.permission.C2D_MESSAGE",
        "$fromPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" to "$toPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        "com.google.android.c2dm" to "$GMS_CORE_VENDOR_GROUP_ID.android.c2dm",
        "com.google.android.libraries.photos.api.mars" to "$GMS_CORE_VENDOR_GROUP_ID.android.apps.photos.api.mars",
        "</queries>" to "</queries><uses-permission android:name=\"android.permission.REQUEST_DELETE_PACKAGES\"/>",
    )
    return transformations.entries.fold(manifest) { text, (from, to) -> text.replace(from, to) }
}
