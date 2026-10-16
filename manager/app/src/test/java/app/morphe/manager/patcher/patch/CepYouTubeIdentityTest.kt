/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager.patcher.patch

import app.morphe.manager.util.Options
import kotlin.test.Test
import kotlin.test.assertEquals

class CepYouTubeIdentityTest {
    private fun options(value: String, uid: Int = 0): Options = mapOf(
        uid to mapOf("Clone app" to mapOf("packageName" to value, "updateProviders" to true),
            "Other patch" to mapOf("flag" to true))
    )

    @Test fun `old explicit Morphe identity migrates while other options are preserved`() {
        val old = options("app.morphe.android.youtube")
        val result = CepYouTubeIdentity.migrateLegacyOptions("com.google.android.youtube", old)
        assertEquals(CepYouTubeIdentity.PACKAGE_NAME, result[0]!!["Clone app"]!!["packageName"])
        assertEquals(true, result[0]!!["Clone app"]!!["updateProviders"])
        assertEquals(old[0]!!["Other patch"], result[0]!!["Other patch"])
        assertEquals("app.morphe.android.youtube", old[0]!!["Clone app"]!!["packageName"])
    }

    @Test fun `explicit private clone identities and other sources remain unchanged`() {
        for (value in listOf("Default", "app.private.youtube", CepYouTubeIdentity.PACKAGE_NAME)) {
            assertEquals(options(value), CepYouTubeIdentity.migrateLegacyOptions("com.google.android.youtube", options(value)))
        }
        assertEquals(options("app.morphe.android.youtube", 5),
            CepYouTubeIdentity.migrateLegacyOptions("com.google.android.youtube", options("app.morphe.android.youtube", 5)))
        assertEquals(options("app.morphe.android.youtube"),
            CepYouTubeIdentity.migrateLegacyOptions("com.google.android.apps.youtube.music", options("app.morphe.android.youtube")))
    }
}
