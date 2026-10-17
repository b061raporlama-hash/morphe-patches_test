/* Copyright 2026 Morphe. See NOTICE for GPLv3 Section 7 terms. */
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;

/** Runs the actual extension sources with deterministic network/thread fixtures; needs only a JDK. */
class WatchLaterRegression {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("watch-later-tests");
        try {
            Map<String, String> fixtures = Map.of(
                "android/text/TextUtils.java", """
                    package android.text;
                    public class TextUtils { public static boolean isEmpty(String s) { return s == null || s.isEmpty(); } }
                    """,
                "app/morphe/extension/shared/StringRef.java", """
                    package app.morphe.extension.shared;
                    public class StringRef { public static String str(String s) { return s; } }
                    """,
                "app/morphe/extension/shared/patches/components/ContextInterface.java", """
                    package app.morphe.extension.shared.patches.components;
                    public interface ContextInterface { StringBuilder patch_getPathBuilder(); }
                    """,
                "app/morphe/extension/shared/Logger.java", """
                    package app.morphe.extension.shared;
                    import java.util.function.Supplier;
                    public class Logger { public static void printException(Supplier<String> s, Exception e) {} }
                    """,
                "app/morphe/extension/shared/Utils.java", """
                    package app.morphe.extension.shared;
                    import java.util.*;
                    public class Utils {
                        public static final List<Runnable> workers = new ArrayList<>(), main = new ArrayList<>();
                        public static boolean onMain;
                        public static void runOnBackgroundThread(Runnable r) { workers.add(r); }
                        public static void runOnMainThread(Runnable r) { main.add(r); }
                        public static void showToastShort(String s) {}
                        public static void drain() {
                            while (!workers.isEmpty()) workers.remove(0).run();
                            onMain = true;
                            try { while (!main.isEmpty()) main.remove(0).run(); }
                            finally { onMain = false; }
                        }
                    }
                    """,
                "app/morphe/extension/shared/innertube/utils/AuthUtils.java", """
                    package app.morphe.extension.shared.innertube.utils;
                    import java.util.Map;
                    public class AuthUtils {
                        public static boolean loggedOut;
                        public static boolean isNotLoggedIn() { return loggedOut; }
                        public static Map<String,String> getRequestHeader() { return Map.of(); }
                    }
                    """,
                "app/morphe/extension/youtube/patches/utils/requests/SaveToWatchLaterRequest.java", """
                    package app.morphe.extension.youtube.patches.utils.requests;
                    import java.util.*;
                    public class SaveToWatchLaterRequest {
                        public static Boolean response = true;
                        public static boolean fail;
                        public static final List<String> ids = new ArrayList<>();
                        public static SaveToWatchLaterRequest fetchRequest(String id, Map<String,String> headers) {
                            ids.add(id);
                            if (fail) throw new IllegalStateException("Network fixture");
                            return new SaveToWatchLaterRequest();
                        }
                        public Boolean getResult() { return response; }
                    }
                    """,
                "Scenario.java", """
                    import java.nio.charset.StandardCharsets;
                    import java.util.*;
                    import app.morphe.extension.shared.Utils;
                    import app.morphe.extension.shared.innertube.utils.AuthUtils;
                    import app.morphe.extension.youtube.patches.SaveToWatchLaterPatch;
                    import app.morphe.extension.youtube.patches.watchlater.WatchLaterVideoId;
                    import app.morphe.extension.youtube.patches.watchlater.WatchLaterCardIdentity;
                    import app.morphe.extension.youtube.patches.watchlater.WatchLaterScopeCarrier;
                    import app.morphe.extension.youtube.patches.utils.requests.SaveToWatchLaterRequest;
                    public class Scenario {
                        static class Carrier implements WatchLaterScopeCarrier {
                            Object scope;
                            public Object patch_getWatchLaterScope() { return scope; }
                            public void patch_setWatchLaterScope(Object value) { scope = value; }
                        }
                        static final class TypedCarrier extends Carrier implements app.morphe.extension.shared.patches.components.ContextInterface {
                            final StringBuilder path;
                            TypedCarrier(String path) { this.path = new StringBuilder(path); }
                            public StringBuilder patch_getPathBuilder() { return path; }
                        }
                        static int assertions;
                        static final List<Boolean> results = new ArrayList<>();
                        static void check(boolean condition) {
                            assertions++;
                            if (!condition) throw new AssertionError("Assertion " + assertions);
                        }
                        static String id(String s) { return WatchLaterVideoId.fromBuffer(s.getBytes(StandardCharsets.ISO_8859_1)); }
                        static void save(String id) {
                            SaveToWatchLaterPatch.saveVideo(id, ok -> { check(Utils.onMain); results.add(ok); });
                        }
                        public static void run() {
                            check("abcdefghijk".equals(id("https://i.ytimg.com/vi/abcdefghijk/hq720.jpg")));
                            check("abc_def-123".equals(id("https://i1.ytimg.com/vi_webp/abc_def-123/hqdefault.webp")));
                            check("abcdefghijk".equals(id("https://img.youtube.com/vi/abcdefghijk/0.jpg")));
                            check("abcdefghijk".equals(id("https://i.ytimg.com/vi/abcdefghijk/a.jpg https://i.ytimg.com/vi/abcdefghijk/b.jpg")));
                            check(id("https://i.ytimg.com/vi/abcdefghijk/a.jpg https://i.ytimg.com/vi/12345678901/b.jpg") == null);
                            check(id("https://evil.test/vi/abcdefghijk/a.jpg") == null);
                            check(id("https://yt3.ggpht.com/avatar") == null);
                            check(id("https://i.ytimg.com/vi/abcdefghijkl/a.jpg") == null);
                            check(WatchLaterVideoId.fromBuffer(null) == null);
                            check(id("") == null);
                            check(!WatchLaterVideoId.observe(null).ambiguous());
                            check(WatchLaterVideoId.observe(("https://i.ytimg.com/vi/abcdefghijk/a.jpg "
                                + "https://i.ytimg.com/vi/12345678901/b.jpg").getBytes(StandardCharsets.ISO_8859_1)).ambiguous());
                            var cards = new WatchLaterCardIdentity();
                            String card = "feed|video_lockup_with_attachment.eml-fe|shared-template|";
                            String child = card + "ContainerType|TextType|";
                            List<Carrier> roots = new ArrayList<>(), children = new ArrayList<>();
                            for (int i = 0; i < 40; i++) {
                                String video = String.format("%011d", i);
                                Carrier root = new Carrier(), builder = new Carrier(), text = new Carrier();
                                cards.observe(root, card, video);
                                WatchLaterCardIdentity.copy(root, builder);
                                WatchLaterCardIdentity.copy(builder, text);
                                cards.observe(text, new StringBuilder(child), null);
                                check(video.equals(cards.resolve(text, child).videoId()));
                                roots.add(root); children.add(text);
                            }
                            for (int i = 0; i < 40; i++) {
                                check(String.format("%011d", i).equals(cards.resolve(children.get(i), child).videoId()));
                            }
                            check(cards.resolve(new Carrier(), child).videoId() == null); // No hash-only guessing.
                            Carrier late = new Carrier(), lateChild = new Carrier();
                            cards.observe(late, card, null);
                            WatchLaterCardIdentity.copy(late, lateChild);
                            var lateScope = cards.resolve(lateChild, child);
                            check(lateScope.videoId() == null);
                            cards.observe(lateChild, child, "abcdefghijk");
                            check("abcdefghijk".equals(lateScope.videoId())); // Mounted span sees late identity.
                            cards.observe(lateChild, child, "12345678901");
                            check(lateScope.videoId() == null); // Conflicting data invalidates all siblings.
                            cards.observe(lateChild, child, "abcdefghijk");
                            check(lateScope.videoId() == null);
                            Carrier reused = roots.get(0);
                            var oldScope = cards.resolve(reused, card);
                            cards.observe(reused, card, "abc_def-123");
                            check(cards.resolve(reused, card) != oldScope);
                            check("00000000000".equals(oldScope.videoId()));
                            WatchLaterCardIdentity.copy(reused, children.get(0));
                            check("abc_def-123".equals(cards.resolve(children.get(0), child).videoId()));
                            cards.observe(reused, card, null);
                            check(cards.resolve(reused, card).videoId() == null); // No stale recycled ID.
                            cards.observe(reused, card, null, true);
                            WatchLaterCardIdentity.copy(reused, children.get(0));
                            cards.observe(children.get(0), child, "abcdefghijk");
                            check(cards.resolve(children.get(0), child).videoId() == null);
                            check(cards.resolve(reused, "feed|shorts_shelf.e|shared-template|TextType|") == null);
                            check(cards.resolve(reused, "feed|video_lockup_with_attachment.eml-fe|other-template|TextType|") == null);
                            // Real 21.20.405 callbacks start at CellType, after copying the
                            // preceding context. No callback is made at the bare card prefix.
                            String cellRoot = card + "CellType|";
                            String cellText = cellRoot + "video_lockup.eml-fe|metadata|TextType|";
                            Carrier previous = new TypedCarrier(cellRoot);
                            cards.observe(previous, cellRoot, "00000000099");
                            List<WatchLaterCardIdentity.Scope> cellScopes = new ArrayList<>();
                            for (int i = 0; i < 30; i++) {
                                Carrier entry = new TypedCarrier(card), next = new TypedCarrier(cellRoot);
                                Carrier builder = new Carrier(), text = new TypedCarrier(cellText);
                                WatchLaterCardIdentity.copy(previous, entry);
                                WatchLaterCardIdentity.copy(entry, builder);
                                WatchLaterCardIdentity.copy(builder, next);
                                String video = String.format("%011d", i + 100);
                                WatchLaterCardIdentity.copy(next, text);
                                cards.observe(text, cellText, null);
                                var scope = cards.resolve(text, cellText);
                                check(scope.videoId() == null); // New card cannot inherit the previous video's ID.
                                cards.observe(next, cellRoot, video); // Parent is observed AFTER its child was copied.
                                check(video.equals(scope.videoId()));
                                cellScopes.add(scope);
                                previous = next;
                            }
                            for (int i = 0; i < 30; i++) {
                                check(String.format("%011d", i + 100).equals(cellScopes.get(i).videoId()));
                            }
                            Carrier entry = new TypedCarrier(card), rootCell = new TypedCarrier(cellRoot), builderCell = new Carrier();
                            WatchLaterCardIdentity.copy(entry, builderCell);
                            WatchLaterCardIdentity.copy(builderCell, rootCell);
                            Carrier branch = new TypedCarrier(cellRoot), leaf = new TypedCarrier(cellText);
                            WatchLaterCardIdentity.copy(rootCell, branch);
                            WatchLaterCardIdentity.copy(branch, leaf);
                            var prebuilt = cards.resolve(leaf, cellText);
                            check(prebuilt.videoId() == null);
                            cards.observe(branch, cellRoot, "abcdefghijk"); // Sibling root callback precedes parent callback.
                            check("abcdefghijk".equals(prebuilt.videoId()));
                            cards.observe(rootCell, cellRoot, "abcdefghijk");
                            check("abcdefghijk".equals(prebuilt.videoId()));
                            cards.observe(branch, cellText, null, true);
                            check(prebuilt.videoId() == null); // Ambiguity invalidates the entire card family.
                            save("abcdefghijk"); save("abcdefghijk"); save("12345678901");
                            check(Utils.workers.size() == 2);
                            Utils.drain();
                            check(SaveToWatchLaterRequest.ids.equals(List.of("abcdefghijk", "12345678901")));
                            check(results.equals(List.of(false, true, true)));
                            results.clear(); SaveToWatchLaterRequest.fail = true;
                            save("abcdefghijk"); Utils.drain();
                            check(results.equals(List.of(false)));
                            results.clear(); SaveToWatchLaterRequest.fail = false;
                            save("abcdefghijk"); Utils.drain();
                            check(results.equals(List.of(true)));
                            results.clear(); SaveToWatchLaterRequest.response = null;
                            save("abcdefghijk"); Utils.drain(); check(results.equals(List.of(false)));
                            results.clear(); SaveToWatchLaterRequest.response = false;
                            save("abcdefghijk"); Utils.drain(); check(results.equals(List.of(true)));
                            results.clear(); AuthUtils.loggedOut = true;
                            int count = SaveToWatchLaterRequest.ids.size();
                            save("abcdefghijk"); Utils.drain(); check(results.equals(List.of(false)));
                            check(SaveToWatchLaterRequest.ids.size() == count);
                            results.clear(); AuthUtils.loggedOut = false;
                            save(null); save(""); Utils.drain(); check(results.equals(List.of(false, false)));
                            check(SaveToWatchLaterRequest.ids.size() == count);
                            System.out.println("PASS: " + assertions + " assertions (identity, ambiguity, rapid taps, retry, auth, UI callbacks)");
                        }
                    }
                    """
            );
            List<String> compile = new ArrayList<>(List.of("-d", temp.toString(), "--release", "17"));
            for (var fixture : fixtures.entrySet()) {
                Path file = temp.resolve(fixture.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, fixture.getValue());
                compile.add(file.toString());
            }
            Path source = Path.of("extensions/youtube/src/main/java/app/morphe/extension/youtube/patches");
            compile.add(source.resolve("SaveToWatchLaterPatch.java").toString());
            compile.add(source.resolve("watchlater/WatchLaterVideoId.java").toString());
            compile.add(source.resolve("watchlater/WatchLaterCardIdentity.java").toString());
            compile.add(source.resolve("watchlater/WatchLaterScopeCarrier.java").toString());
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException("A JDK is required");
            if (compiler.run(null, null, null, compile.toArray(String[]::new)) != 0) {
                throw new AssertionError("Extension source compilation failed");
            }
            try (var loader = new URLClassLoader(new java.net.URL[]{temp.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                loader.loadClass("Scenario").getMethod("run").invoke(null);
            }
        } finally {
            try (var files = Files.walk(temp)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }
}
