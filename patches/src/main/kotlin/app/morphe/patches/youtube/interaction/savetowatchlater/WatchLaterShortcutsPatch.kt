/*
 * Copyright 2026 Morphe.
 * See the included NOTICE file for GPLv3 Section 7 terms.
 */
package app.morphe.patches.youtube.interaction.savetowatchlater

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.litho.filter.addLithoFilter
import app.morphe.patches.shared.misc.litho.relayout.LithoViewOnMeasureFingerprint
import app.morphe.patches.shared.misc.litho.relayout.lithoRelayoutPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.shared.misc.textcomponent.hookLithoSpannableString
import app.morphe.patches.shared.misc.textcomponent.lithoSpannableStringPatch
import app.morphe.patches.youtube.layout.buttons.navigation.navigationBarPatch
import app.morphe.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.util.setExtensionIsPatchIncluded

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/WatchLaterShortcutsPatch;"

@Suppress("unused")
val watchLaterShortcutsPatch = bytecodePatch(
    name = "Watch later shortcuts",
    description = "Adds Watch later buttons to video thumbnails and replaces the Shorts navigation button with Watch later.",
    default = false,
) {
    dependsOn(
        saveToWatchLaterButtonPatch,
        navigationBarPatch,
        lithoFilterPatch,
        lithoSpannableStringPatch,
        lithoRelayoutPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        PreferenceScreen.FEED.addPreferences(
            SwitchPreference("morphe_watch_later_shortcuts", summary = true)
        )
        addLithoFilter("Lapp/morphe/extension/youtube/patches/components/WatchLaterFilter;")
        hookLithoSpannableString(EXTENSION_CLASS)
        LithoViewOnMeasureFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onLithoViewMeasured(Landroid/view/View;)V"
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)
    }
}
