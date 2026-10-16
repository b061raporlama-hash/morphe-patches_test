/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.patches.youtube

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.interaction.savetowatchlater.addWatchLaterScopeCarrier
import app.morphe.patches.youtube.interaction.savetowatchlater.instrumentWatchLaterContextCopies
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.File

private const val CONTEXT = "Lcep/fixture/Context;"
private const val BUILDER = "Lcep/fixture/Builder;"

/** A small original DEX reproduces YouTube's reuse of p1 as a primitive scratch register. */
internal fun watchLaterDexFixture(broken: Boolean = false): ByteArray {
    fun method(owner: String, name: String, parameters: List<String>, result: String,
               registers: Int, body: String) = ImmutableMethod(owner, name,
        parameters.map { ImmutableMethodParameter(it, null, null) }, result,
        AccessFlags.PUBLIC.value or if (name == "<init>") AccessFlags.CONSTRUCTOR.value else 0,
        null, null, MutableMethodImplementation(registers)).toMutable().apply { addInstructions(0, body) }
    fun field(owner: String, name: String, type: String) = ImmutableField(owner, name, type,
        AccessFlags.PUBLIC.value, null, null, null)
    val context = ImmutableClassDef(CONTEXT, AccessFlags.PUBLIC.value, "Ljava/lang/Object;",
        emptyList(), null, emptyList(), listOf(field(CONTEXT, "payload", "Ljava/lang/Object;")),
        listOf(method(CONTEXT, "<init>", emptyList(), "V", 1, """
            invoke-direct { p0 }, Ljava/lang/Object;-><init>()V
            return-void
        """))).toMutable()
    val builder = ImmutableClassDef(BUILDER, AccessFlags.PUBLIC.value, "Ljava/lang/Object;",
        emptyList(), null, emptyList(), listOf(field(BUILDER, "payload", "Ljava/lang/Object;"),
            field(BUILDER, "flags", "S")), listOf(
            method(BUILDER, "<init>", listOf(CONTEXT), "V", 3, """
                invoke-direct { p0 }, Ljava/lang/Object;-><init>()V
                iget-object v0, p1, $CONTEXT->payload:Ljava/lang/Object;
                iput-object v0, p0, $BUILDER->payload:Ljava/lang/Object;
                const/16 p1, 0x3ff
                iput-short p1, p0, $BUILDER->flags:S
                return-void
            """),
            method(BUILDER, "build", emptyList(), CONTEXT, 3, """
                new-instance v0, $CONTEXT
                invoke-direct { v0 }, $CONTEXT-><init>()V
                iget-object v1, p0, $BUILDER->payload:Ljava/lang/Object;
                iput-object v1, v0, $CONTEXT->payload:Ljava/lang/Object;
                const/16 p0, 0x7ff
                return-object v0
            """))).toMutable()
    if (broken) {
        // Negative control: reproduce the cep.14 constructor hook, not a Java approximation.
        addWatchLaterScopeCarrier(context)
        addWatchLaterScopeCarrier(builder)
        builder.methods.single { it.name == "<init>" }.apply {
            addInstructions(implementation!!.instructions.count() - 1,
                "invoke-static { p1, p0 }, Lapp/morphe/extension/youtube/patches/watchlater/WatchLaterCardIdentity;->copy(Ljava/lang/Object;Ljava/lang/Object;)V")
        }
    } else {
        instrumentWatchLaterContextCopies(context, builder)
    }
    val store = MemoryDataStore()
    DexPool(Opcodes.forApi(26)).apply { internClass(context); internClass(builder); writeTo(store) }
    return store.data
}

fun main(args: Array<String>) {
    val output = File(args.single()).apply { mkdirs() }
    File(output, "watchlater-fixed.dex").writeBytes(watchLaterDexFixture())
    File(output, "watchlater-broken.dex").writeBytes(watchLaterDexFixture(broken = true))
    println("Generated production-transformed DEX and cep.14 negative control for Android ART")
}
