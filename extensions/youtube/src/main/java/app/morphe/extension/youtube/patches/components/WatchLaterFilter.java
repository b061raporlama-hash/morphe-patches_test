/*
 * Copyright 2026 Morphe.
 * See the included NOTICE file for GPLv3 Section 7 terms.
 */
package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.BufferAsciiStrings;
import app.morphe.extension.shared.patches.components.ContextInterface;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.patches.WatchLaterShortcutsPatch;
import app.morphe.extension.youtube.patches.watchlater.WatchLaterVideoId;
import app.morphe.extension.youtube.settings.Settings;

/** Observes video cards without filtering or changing their content. */
@SuppressWarnings("unused")
public final class WatchLaterFilter extends Filter {
    public WatchLaterFilter() {
        addPathCallbacks(new StringFilterGroup(Settings.WATCH_LATER_SHORTCUTS,
                "home_video_with_context.e", "video_with_context.e",
                "search_video_with_context.e", "related_video_with_context.e",
                "video_lockup_with_attachment.e", "compact_video.e"));
    }

    @Override
    public boolean isFiltered(ContextInterface context, String identifier, String accessibility,
                              CharSequence path, byte[] buffer, BufferAsciiStrings asciiStrings,
                              StringFilterGroup group, FilterContentType type, int index) {
        WatchLaterShortcutsPatch.bindContext(context, WatchLaterVideoId.fromBuffer(buffer));
        return false;
    }
}
