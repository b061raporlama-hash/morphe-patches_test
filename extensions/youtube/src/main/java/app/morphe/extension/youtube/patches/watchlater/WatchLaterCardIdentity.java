/* Copyright 2026 Cep Patcher contributors. See NOTICE for GPLv3 Section 7 terms. */
package app.morphe.extension.youtube.patches.watchlater;

import app.morphe.extension.shared.patches.components.ContextInterface;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Identity follows context copies even when YouTube binds the parent after copying its children. */
public final class WatchLaterCardIdentity {
    private static final Pattern CARD = Pattern.compile(
        "(?:^|\\|)(?:home_video_with_context|search_video_with_context|related_video_with_context|video_with_context|video_lockup_with_attachment|compact_video)\\.e[^|\\s]*\\|([^|]+)(?:\\||$)");
    private static final int DESCENDANT = 0, BARE_ROOT = 1, CELL_ROOT = 2;

    private static final class PathInfo {
        final String key;
        final int stage;
        PathInfo(String key, int stage) { this.key = key; this.stage = stage; }
    }

    private static final class Binding {
        final String path;
        final boolean root;
        // null = not observed yet; empty = conflicting/ambiguous; otherwise the video ID.
        volatile String value;
        Binding(String path, boolean root) { this.path = path; this.root = root; }
        synchronized void observe(String id) {
            if (id == null || "".equals(value)) return;
            value = value != null && !value.equals(id) ? "" : id;
        }
        synchronized void invalidate() { value = ""; }
    }

    /** Holds no Android context or view. Every copy gets its own node, linked to its source. */
    private static final class Node {
        final Node parent;
        final String key;
        final int stage;
        volatile Binding binding;
        boolean rootObserved;
        Scope cachedScope;
        Node(Node parent, PathInfo path) {
            this.parent = parent;
            key = path.key;
            stage = path.stage;
        }
    }

    /** A mounted span keeps its node and expected card key; late ancestor bindings remain visible. */
    public static final class Scope {
        private final Node origin;
        private final String path;
        private Scope(Node origin, String path) { this.origin = origin; this.path = path; }
        public String videoId() {
            String id = null;
            for (Node node = origin; node != null; node = node.parent) {
                if (!path.equals(node.key)) return null;
                Binding binding = node.binding;
                if (binding == null || !path.equals(binding.path)) continue;
                String value = binding.value;
                if ("".equals(value)) return null;
                if (value != null) {
                    if (id != null && !id.equals(value)) return null;
                    id = value;
                }
                if (binding.root) break;
            }
            return id;
        }
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

    private static PathInfo pathInfo(CharSequence path) {
        String key = key(path);
        int stage = DESCENDANT;
        if (key != null) {
            if (path.length() == key.length() || path.length() + 1 == key.length()) stage = BARE_ROOT;
            else if (path.length() == key.length() + "CellType|".length()
                    && "CellType|".contentEquals(path.subSequence(key.length(), path.length()))) stage = CELL_ROOT;
        }
        return new PathInfo(key, stage);
    }

    private static PathInfo contextPath(Object context) {
        if (!(context instanceof ContextInterface owner)) return null; // Builder has no path accessor.
        StringBuilder path = owner.patch_getPathBuilder();
        return path == null ? null : pathInfo(path);
    }

    private static Node node(WatchLaterScopeCarrier carrier, PathInfo path, boolean create) {
        synchronized (carrier) {
            if (path != null && path.key == null) {
                carrier.patch_setWatchLaterScope(null);
                return null;
            }
            Object value = carrier.patch_getWatchLaterScope();
            Node node = value instanceof Node existing ? existing : null;
            if (create && path != null && (node == null || !path.key.equals(node.key))) {
                node = new Node(null, path);
                carrier.patch_setWatchLaterScope(node);
            }
            return node;
        }
    }

    /** Injected after Object.<init> and at the builder's build return; does not copy a stale snapshot. */
    public static void copy(Object source, Object destination) {
        if (!(source instanceof WatchLaterScopeCarrier from) || !(destination instanceof WatchLaterScopeCarrier to)) return;
        PathInfo sourcePath = contextPath(source), destinationPath = contextPath(destination);
        Node parent = node(from, sourcePath, sourcePath != null && sourcePath.key != null);
        if (sourcePath == null && parent != null) sourcePath = new PathInfo(parent.key, parent.stage);
        PathInfo path = destinationPath != null ? destinationPath : sourcePath;
        Node child = null;
        if (path != null && path.key != null) {
            // New cards enter from the bare prefix, then append CellType. Same-CellType
            // clones are branches of this card and retain the parent for late binding.
            boolean boundary = destinationPath != null && path.stage != DESCENDANT
                    && (sourcePath == null || !path.key.equals(sourcePath.key)
                        || (sourcePath.stage == BARE_ROOT && path.stage == CELL_ROOT));
            child = new Node(boundary ? null : parent, path);
        }
        synchronized (to) { to.patch_setWatchLaterScope(child); }
    }

    public void observe(Object context, CharSequence path, String videoId) {
        observe(context, path, videoId, false);
    }

    public void observe(Object context, CharSequence path, String videoId, boolean ambiguous) {
        if (!(context instanceof WatchLaterScopeCarrier carrier)) return;
        PathInfo info = pathInfo(path);
        if (info.key == null) return;
        synchronized (carrier) {
            Node node = node(carrier, info, true);
            Node owner = node;
            while (owner.parent != null && info.key.equals(owner.parent.key)) owner = owner.parent;
            boolean root = info.stage != DESCENDANT;
            Binding old = owner.binding;
            // A repeated observation of a clone belongs to this card. Reusing the
            // entry context for another card starts a generation and preserves old spans.
            if (root && node == owner && node.rootObserved && !ambiguous
                    && old != null && old.value != null && !old.value.equals(videoId)) {
                owner = node = new Node(null, info);
                carrier.patch_setWatchLaterScope(node);
            }
            if (root) node.rootObserved = true;
            if (root || videoId != null || ambiguous) {
                synchronized (owner) {
                    Binding binding = owner.binding;
                    if (binding == null) owner.binding = binding = new Binding(info.key, true);
                    if (ambiguous) binding.invalidate();
                    else binding.observe(videoId);
                }
            }
        }
    }

    public Scope resolve(Object context, CharSequence path) {
        if (!(context instanceof WatchLaterScopeCarrier carrier)) return null;
        PathInfo info = pathInfo(path);
        if (info.key == null) return null;
        Node node = node(carrier, null, false);
        if (node != null && !info.key.equals(node.key)) return null;
        if (node == null) node = node(carrier, info, true);
        synchronized (node) {
            if (node.cachedScope == null || !info.key.equals(node.cachedScope.path))
                node.cachedScope = new Scope(node, info.key);
            return node.cachedScope;
        }
    }
}
