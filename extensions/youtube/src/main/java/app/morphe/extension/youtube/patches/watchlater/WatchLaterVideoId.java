/*
 * Copyright 2026 Morphe.
 * See the included NOTICE file for GPLv3 Section 7 terms.
 */
package app.morphe.extension.youtube.patches.watchlater;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Does not guess a video from its title or from the currently playing video. */
public final class WatchLaterVideoId {
    private static final Pattern THUMBNAIL = Pattern.compile(
            "https?://(?:i[0-9]?\\.ytimg\\.com|img\\.youtube\\.com)/(?:vi|vi_webp)/([A-Za-z0-9_-]{11})/");

    private WatchLaterVideoId() {}

    public record Observation(String videoId, boolean ambiguous) {}

    /** Returns null for shelves, mixed buffers, avatars, and unrecognized URLs. */
    public static String fromBuffer(byte[] buffer) {
        return observe(buffer).videoId();
    }

    public static Observation observe(byte[] buffer) {
        if (buffer == null || buffer.length == 0) return new Observation(null, false);
        Matcher matcher = THUMBNAIL.matcher(new String(buffer, StandardCharsets.ISO_8859_1));
        String found = null;
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (found != null && !found.equals(candidate)) return new Observation(null, true);
            found = candidate;
        }
        return new Observation(found, false);
    }
}
