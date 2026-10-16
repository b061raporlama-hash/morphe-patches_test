/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.morphe.manager.patcher.patch.BundledPatches
import app.morphe.manager.patcher.patch.PatchBundle
import app.morphe.manager.domain.bundles.JsonPatchBundle
import app.morphe.manager.domain.bundles.PatchBundleSource
import app.morphe.manager.domain.repository.PatchBundleRepository
import app.morphe.manager.util.SOURCE_RELEASE_JSON_URL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.GlobalContext
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
            // Also exercise first-run Room seeding and the repository metadata path used by the UI.
            val repository = GlobalContext.get().get<PatchBundleRepository>()
            val ready = runBlocking {
                withTimeout(60_000) {
                    repository.reload()
                    repository.bundleState.first { it is PatchBundleRepository.BundleState.Ready }
                        as PatchBundleRepository.BundleState.Ready
                }
            }
            val source = ready.sources.getValue(0) as JsonPatchBundle
            assertEquals(SOURCE_RELEASE_JSON_URL, source.endpoint)
            assertTrue(source.autoUpdate)
            assertTrue(source.enabled)
            assertTrue(source.state is PatchBundleSource.State.Available)
            assertTrue(ready.info.getValue(0).patches.any { it.displayName == "Watch later shortcuts" })
        } finally {
            file.delete()
        }
    }
}
