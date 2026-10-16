/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.extension.youtube.patches.watchlater;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Carries identity through child contexts; ambiguous shared template paths never select a video. */
public final class WatchLaterCardIdentity {
    private record Binding(String path, String videoId) {}
    private final Map<Object, Binding> scopes = new WeakHashMap<>();
    private static final Pattern CARD = Pattern.compile(
            "(?:^|\\|)(?:home_video_with_context|search_video_with_context|"
                    + "related_video_with_context|video_with_context|"
                    + "video_lockup_with_attachment|compact_video)\\.e[^|\\s]*\\|([^|]+)(?:\\||$)");
    private final Map<String, String> cards = new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, String> entry) {
            return size() > 512;
        }
    };

    private static String key(CharSequence path) {
        Matcher match = CARD.matcher(path);
        String key = null;
        while (match.find()) {
            String hash = match.group(1);
            // Without the per-component hash, two siblings have the same path. Do not guess.
            if (!hash.endsWith("Type") && !hash.contains(".e")) {
                key = path.subSequence(0, match.end()).toString();
                if (!key.endsWith("|")) key += "|";
            }
        }
        return key;
    }

    public synchronized void observe(CharSequence path, String videoId) {
        observe(null, null, path, videoId);
    }

    public synchronized void observe(Object context, Object builder, CharSequence path, String videoId) {
        String key = key(path);
        if (key == null) return;
        boolean root = path.length() == key.length()
                || path.length() + 1 == key.length();
        // Descendant buffers often contain only a title or avatar. They must not erase the
        // thumbnail identity observed on the parent before text creation runs.
        if (root && videoId == null) cards.put(key, null);
        if (videoId != null) {
            Binding binding = new Binding(key, videoId);
            if (context != null) scopes.put(context, binding);
            if (builder != null) scopes.put(builder, binding);
            // Template hashes can be shared. Do not replace another card's identity on a
            // string-key match: only an exact context/builder can disambiguate those cards.
            if (!cards.containsKey(key)) cards.put(key, videoId);
            else if (!videoId.equals(cards.get(key))) cards.put(key, null);
        } else if (root) {
            if (context != null) scopes.remove(context);
            if (builder != null) scopes.remove(builder);
        }
    }

    public synchronized String resolve(CharSequence path) {
        return resolve(null, null, path);
    }

    public synchronized String resolve(Object context, Object builder, CharSequence path) {
        String key = key(path);
        if (key == null) return null;
        Binding binding = scopes.get(context);
        if (binding != null && key.equals(binding.path)) return binding.videoId;
        binding = scopes.get(builder);
        if (binding != null && key.equals(binding.path)) return binding.videoId;
        return cards.get(key);
    }
}
