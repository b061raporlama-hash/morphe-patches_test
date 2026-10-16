/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager.patcher.patch

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BundledPatchesTest {
    @Test fun `upgrade migrates offline and numeric prerelease counters sort correctly`() {
        assertTrue(BundledPatches.shouldInstall("1.47.0-cep.4", "1.47.0-cep.3"))
        assertTrue(BundledPatches.shouldInstall("1.47.0-cep.10", "1.47.0-cep.9"))
        assertTrue(BundledPatches.shouldInstall("1.47.0-cep.4", null))
        assertTrue(BundledPatches.shouldInstall("1.47.0-cep.4", "damaged"))
    }

    @Test fun `reload keeps the installed bundle including newer network releases`() {
        assertFalse(BundledPatches.shouldInstall("1.47.0-cep.4", "1.47.0-cep.4"))
        assertFalse(BundledPatches.shouldInstall("1.47.0-cep.4", "1.47.0-cep.5"))
        assertFalse(BundledPatches.shouldInstall("1.47.0-cep.4", "1.48.0-cep.1"))
    }
}
