/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.patches.youtube

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass.Companion.toMutable
import app.morphe.patches.youtube.interaction.savetowatchlater.addWatchLaterScopeCarrier
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import kotlin.test.Test
import kotlin.test.assertTrue

class WatchLaterCarrierTest {
    @Test fun `scope field and accessors survive DEX serialization with preloaded member views`() {
        val clazz = ImmutableClassDef("Lfixture/Context;", AccessFlags.PUBLIC.value,
            "Ljava/lang/Object;", emptyList(), null, emptyList(), emptyList(), emptyList()).toMutable()
        // Other patches can materialize all these lazy member views before our patch runs.
        clazz.fields.size
        clazz.instanceFields.size
        clazz.methods.size
        clazz.virtualMethods.size
        addWatchLaterScopeCarrier(clazz)
        assertTrue(clazz.fields.any { it.name == "cep_watchLaterScope" })
        val store = MemoryDataStore()
        DexPool(Opcodes.getDefault()).apply { internClass(clazz); writeTo(store) }
        val serialized = DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(store.data)).classes.single()
        assertTrue(serialized.fields.any { it.name == "cep_watchLaterScope" })
        assertTrue(serialized.methods.any { it.name == "patch_getWatchLaterScope" })
        assertTrue(serialized.methods.any { it.name == "patch_setWatchLaterScope" })
    }
}
