/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.patches.youtube

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.dex.SdkDexVerifier
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.litho.context.conversionContextClassDef
import app.morphe.patches.youtube.interaction.savetowatchlater.watchLaterShortcutsPatch
import app.morphe.patches.youtube.misc.gms.gmsCoreSupportPatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

/** Local only: the supplied proprietary APK is never committed or uploaded. */
fun main(args: Array<String>) {
    require(args.size in 1..2)
    val sdk = System.getenv("ANDROID_HOME") ?: error("Set ANDROID_HOME")
    val probe = bytecodePatch {
        dependsOn(watchLaterShortcutsPatch)
        execute {
            val context = conversionContextClassDef
            val builder = mutableClassDefBy { c -> c.methods.any {
                it.name == "<init>" && it.parameterTypes == listOf(context.type)
            } && c.methods.any { it.returnType == context.type && it.parameterTypes.isEmpty() } }
            for (owner in listOf(context, builder)) {
                check(owner.fields.any { it.name == "cep_watchLaterScope" })
                check(owner.methods.any { it.name == "patch_getWatchLaterScope" })
                check(owner.methods.any { it.name == "patch_setWatchLaterScope" })
            }
            for (method in builder.methods.filter { (it.name == "<init>" &&
                it.parameterTypes == listOf(context.type)) || it.returnType == context.type }) {
                check(method.implementation!!.instructions.any {
                    val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
                    ref?.name == "copy" && ref.definingClass.endsWith("/WatchLaterCardIdentity;")
                }) { "Missing scope-copy hook: $method" }
            }
            println("PASS real APK context/builder copy hooks: ${context.type}, ${builder.type}")
        }
    }
    val workspace = Files.createTempDirectory("cep-watchlater-real-apk")
    Patcher(PatcherConfig(File(args[0]), workspace.toFile(), verifier = SdkDexVerifier(File(sdk)))).use { patcher ->
        patcher += setOf(probe, gmsCoreSupportPatch)
        runBlocking { patcher().collect { result ->
            result.exception?.let { throw it }
            println("Applied: ${result.patch.name}")
        } }
        val result = patcher.get()
        args.getOrNull(1)?.let { output -> result.applyTo(File(output)) }
        println("PASS real YouTube patch application and SDK DEX verification")
    }
}
