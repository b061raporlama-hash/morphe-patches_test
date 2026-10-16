/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.patches.youtube.interaction.savetowatchlater

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.litho.context.conversionContextClassDef
import app.morphe.patches.shared.misc.litho.context.conversionContextPatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneParameters
import app.morphe.util.findFreeRegister
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val CARRIER = "Lapp/morphe/extension/youtube/patches/watchlater/WatchLaterScopeCarrier;"
private const val IDENTITY = "Lapp/morphe/extension/youtube/patches/watchlater/WatchLaterCardIdentity;"

internal fun addWatchLaterScopeCarrier(clazz: MutableClass) {
    val field = ImmutableField(clazz.type, "cep_watchLaterScope", "Ljava/lang/Object;",
        AccessFlags.PRIVATE.value or AccessFlags.VOLATILE.value, null, null, null).toMutable()
    // MutableClass caches the full and filtered sets separately. Keep both views
    // consistent even if another patch already materialized instanceFields.
    clazz.fields.add(field)
    clazz.instanceFields.add(field)
    clazz.interfaces.add(CARRIER)
    clazz.methods.add(ImmutableMethod(clazz.type, "patch_getWatchLaterScope", emptyList(),
        "Ljava/lang/Object;", AccessFlags.PUBLIC.value or AccessFlags.FINAL.value, null, null,
        MutableMethodImplementation(2)).toMutable().apply {
        addInstructions(0, "iget-object v0, p0, $field\nreturn-object v0")
    }.also { clazz.virtualMethods.add(it) })
    clazz.methods.add(ImmutableMethod(clazz.type, "patch_setWatchLaterScope",
        listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value, null, null,
        MutableMethodImplementation(2)).toMutable().apply {
        addInstructions(0, "iput-object p1, p0, $field\nreturn-void")
    }.also { clazz.virtualMethods.add(it) })
}

/** cloneParameters replaces methods; synchronize the cached virtual-method view as well. */
internal fun cloneWatchLaterBuilderMethod(method: MutableMethod, owner: MutableClass): MutableMethod =
    method.cloneParameters(owner).also { replacement ->
        owner.virtualMethods.remove(method)
        owner.virtualMethods.add(replacement)
    }

/** Preserve a per-card scope through real ConversionContext copies, independent of path hashes. */
internal val watchLaterContextPatch = bytecodePatch {
    dependsOn(sharedExtensionPatch, conversionContextPatch)
    execute {
        val context = conversionContextClassDef
        // AutoValue-style builder: identified structurally, never by an obfuscated name.
        val builder = mutableClassDefBy { clazz ->
            clazz.methods.any { it.name == "<init>" && it.parameterTypes == listOf(context.type) } &&
                clazz.methods.any { it.parameterTypes.isEmpty() && it.returnType == context.type }
        }

        addWatchLaterScopeCarrier(context)
        addWatchLaterScopeCarrier(builder)

        builder.methods.single { it.name == "<init>" && it.parameterTypes == listOf(context.type) }.apply {
            // Constructor in the supported APK has one local plus p0/p1.
            check(implementation!!.registerCount <= 16) { "Unsupported context builder constructor registers" }
            val exits = implementation!!.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }
            exits.asReversed().forEach { (index, _) ->
                addInstructionsAtControlFlowLabel(index,
                    "invoke-static { p1, p0 }, $IDENTITY->copy(Ljava/lang/Object;Ljava/lang/Object;)V")
            }
        }
        val build = builder.methods.single { it.parameterTypes.isEmpty() && it.returnType == context.type }
        cloneWatchLaterBuilderMethod(build, builder).apply {
            val exits = implementation!!.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_OBJECT }
            exits.asReversed().forEach { (index, _) ->
                val result = getInstruction<OneRegisterInstruction>(index).registerA
                val source = findFreeRegister(index, result)
                check(result < 16 && source < 16) { "Unsupported context builder return registers" }
                addInstructionsAtControlFlowLabel(index, """
                    move-object/from16 v$source, p0
                    invoke-static { v$source, v$result }, $IDENTITY->copy(Ljava/lang/Object;Ljava/lang/Object;)V
                """)
            }
        }
    }
}
