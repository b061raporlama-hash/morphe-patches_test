/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.patches.youtube

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patches.youtube.interaction.savetowatchlater.addWatchLaterScopeCarrier
import app.morphe.patches.youtube.interaction.savetowatchlater.cloneWatchLaterBuilderMethod
import app.morphe.patches.youtube.interaction.savetowatchlater.hookWatchLaterContextConstructor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WatchLaterCarrierTest {
    @Test fun `serialized constructor copies scope before the incoming parameter becomes a primitive`() {
        val dex = DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(watchLaterDexFixture()))
        val ctor = dex.classes.single { it.type == "Lcep/fixture/Builder;" }.directMethods.single()
        val instructions = ctor.implementation!!.instructions.toList()
        assertEquals(3, ctor.implementation!!.registerCount)
        assertEquals(Opcode.INVOKE_DIRECT, instructions[0].opcode)
        assertEquals(Opcode.INVOKE_STATIC, instructions[1].opcode)
        val copy = instructions[1] as com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
        assertEquals(2, copy.registerC) // original p1, before const/16 overwrites it
        assertEquals(1, copy.registerD) // initialized p0
        assertEquals(Opcode.CONST_16, instructions[instructions.size - 3].opcode)
        assertEquals(Opcode.RETURN_VOID, instructions.last().opcode)
        assertEquals(1, instructions.count { it.opcode == Opcode.INVOKE_STATIC })
    }

    @Test fun `unrecognized constructor entry fails instead of emitting unsafe bytecode`() {
        val method = ImmutableMethod("Lfixture/Builder;", "<init>",
            listOf(com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter("Ljava/lang/Object;", null, null)),
            "V", AccessFlags.PUBLIC.value or AccessFlags.CONSTRUCTOR.value, null, null,
            MutableMethodImplementation(3)).toMutable().apply {
            addInstructions(0, "nop\ninvoke-direct { p0 }, Ljava/lang/Object;-><init>()V\nreturn-void")
        }
        assertFailsWith<IllegalStateException> { hookWatchLaterContextConstructor(method) }
    }

    @Test fun `builder parameter clone replaces the serialized virtual method`() {
        val type = "Lfixture/Builder;"
        val method = ImmutableMethod(type, "build", emptyList(), "Ljava/lang/Object;",
            AccessFlags.PUBLIC.value, null, null, MutableMethodImplementation(2)).toMutable().apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
        }
        val clazz = ImmutableClassDef(type, AccessFlags.PUBLIC.value, "Ljava/lang/Object;",
            emptyList(), null, emptyList(), emptyList(), listOf(method)).toMutable()
        addWatchLaterScopeCarrier(clazz) // Materializes virtualMethods before cloning build.
        val original = clazz.methods.single { it.name == "build" }
        val clone = cloneWatchLaterBuilderMethod(original, clazz)
        val store = MemoryDataStore()
        DexPool(Opcodes.getDefault()).apply { internClass(clazz); writeTo(store) }
        val written = DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(store.data))
            .classes.single().methods.single { it.name == "build" }
        assertTrue(clone.implementation!!.registerCount > original.implementation!!.registerCount)
        assertEquals(clone.implementation!!.registerCount, written.implementation!!.registerCount)
    }

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
