/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.extension.youtube.patches.watchlater;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Per-conversion identity carried by context -> builder -> context copies. */
public final class WatchLaterCardIdentity {
    private static final Pattern CARD = Pattern.compile(
        "(?:^|\\|)(?:home_video_with_context|search_video_with_context|related_video_with_context|video_with_context|video_lockup_with_attachment|compact_video)\\.e[^|\\s]*\\|([^|]+)(?:\\||$)");
    /** A generation belongs to one card, even when its template is shared. */
    public static final class Scope {
        private final String path;
        private volatile String videoId;
        private boolean ambiguous;
        private Scope(String path) { this.path = path; }
        private synchronized void observe(String id) {
            if (id == null || ambiguous) return;
            if (videoId != null && !videoId.equals(id)) {
                ambiguous = true;
                videoId = null;
            } else videoId = id;
        }
        public String videoId() { return videoId; }
        private synchronized void invalidate() { ambiguous = true; videoId = null; }
    }
    private static String key(CharSequence path) {
        Matcher match = CARD.matcher(path);
        String key = null;
        while (match.find()) {
            String hash = match.group(1);
            if (!hash.endsWith("Type") && !hash.contains(".e")) {
                key = path.subSequence(0, match.end()).toString();
                if (!key.endsWith("|")) key += "|";
            }
        }
        return key;
    }
    /** Injected into the real context-copy constructor and builder build return. */
    public static void copy(Object source, Object destination) {
        if (source instanceof WatchLaterScopeCarrier from && destination instanceof WatchLaterScopeCarrier to)
            to.patch_setWatchLaterScope(from.patch_getWatchLaterScope());
    }
    public void observe(Object context, CharSequence path, String videoId) {
        observe(context, path, videoId, false);
    }
    public void observe(Object context, CharSequence path, String videoId, boolean ambiguous) {
        if (!(context instanceof WatchLaterScopeCarrier carrier)) return;
        String key = key(path);
        if (key == null) return;
        boolean root = path.length() == key.length() || path.length() + 1 == key.length();
        Scope scope = resolve(context, path);
        if (root || scope == null) {
            scope = new Scope(key);
            carrier.patch_setWatchLaterScope(scope);
        }
        if (ambiguous) scope.invalidate();
        else scope.observe(videoId);
    }
    public Scope resolve(Object context, CharSequence path) {
        if (!(context instanceof WatchLaterScopeCarrier carrier)) return null;
        Object value = carrier.patch_getWatchLaterScope();
        return value instanceof Scope scope && scope.path.equals(key(path)) ? scope : null;
    }
}
