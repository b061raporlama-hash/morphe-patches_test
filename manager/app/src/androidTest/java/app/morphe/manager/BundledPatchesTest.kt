/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.morphe.manager.patcher.patch.BundledPatches
import app.morphe.manager.patcher.patch.PatchBundle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BundledPatchesTest {
    @Test fun shippedBundleLoadsWithTheManagersActualDexLoader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "cep-loader-test/patches.jar")
        try {
            BundledPatches.install(context.assets.open("cep-patches.mpp"), file)
            val bundle = PatchBundle(file.absolutePath)
            assertEquals("Cep Patches", bundle.manifestAttributes!!.name)
            val patches = PatchBundle.Loader.patches(listOf(bundle), "com.google.android.youtube")
                .getValue(bundle).values
            val shortcut = patches.single { it.name == "Watch later shortcuts" }
            assertFalse(shortcut.default)
            assertTrue(shortcut.dependencies.any { it.name == "Save to Watch later" })
            assertTrue(shortcut.dependencies.any { it.name == "Navigation bar" })
            assertEquals("io.github.b061raporlama.ceppatcher.debug", context.packageName)
        } finally {
            file.delete()
        }
    }
}
