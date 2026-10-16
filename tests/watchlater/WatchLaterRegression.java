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
                    import app.morphe.extension.youtube.patches.utils.requests.SaveToWatchLaterRequest;
                    public class Scenario {
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
                            var cards = new WatchLaterCardIdentity();
                            String card = "feed|home_video_with_context.e|hash-A|";
                            String child = card + "ContainerType|TextType|";
                            cards.observe(card, "abcdefghijk");
                            cards.observe(new StringBuilder(child), null);
                            check("abcdefghijk".equals(cards.resolve(new StringBuilder(child))));
                            cards.observe("feed|home_video_with_context.e|hash-B|", "12345678901");
                            check("12345678901".equals(cards.resolve("feed|home_video_with_context.e|hash-B|TextType|")));
                            check("abcdefghijk".equals(cards.resolve(child)));
                            cards.observe(child, "12345678901");
                            check(cards.resolve(child) == null);
                            cards.observe(card, "12345678901");
                            check(cards.resolve(child) == null); // A shared template is ambiguous.
                            cards.observe(card, null);
                            check(cards.resolve(child) == null);
                            cards.observe(child, "abcdefghijk");
                            check(cards.resolve(child) == null);
                            cards.observe("feed|home_video_with_context.e|TextType|", "abcdefghijk");
                            check(cards.resolve("feed|home_video_with_context.e|TextType|") == null);
                            check(cards.resolve("feed|shorts_shelf.e|hash-A|TextType|") == null);
                            cards.observe("feed|home_video_with_context.e|hash-C", "abcdefghijk");
                            check("abcdefghijk".equals(cards.resolve("feed|home_video_with_context.e|hash-C|TextType|")));
                            Object rootContext = new Object(), childContext = new Object();
                            StringBuilder sharedPath = new StringBuilder("feed|video_lockup_with_attachment.eml-fe|c0c4a49b6544b5fb|");
                            cards.observe(rootContext, sharedPath, sharedPath, "abcdefghijk");
                            sharedPath.append("ContainerType|TextType|");
                            cards.observe(childContext, sharedPath, sharedPath, null);
                            check("abcdefghijk".equals(cards.resolve(childContext, sharedPath, sharedPath)));
                            String sameTemplate = "feed|video_lockup_with_attachment.eml-fe|c0c4a49b6544b5fb|";
                            StringBuilder otherPath = new StringBuilder(sameTemplate);
                            cards.observe(new Object(), otherPath, otherPath, "12345678901");
                            check("12345678901".equals(cards.resolve(new Object(), otherPath, otherPath)));
                            check("abcdefghijk".equals(cards.resolve(childContext, sharedPath, sharedPath)));
                            check(cards.resolve(new StringBuilder(sameTemplate + "TextType|")) == null);
                            check(cards.resolve(rootContext, sharedPath, "feed|video_lockup_with_attachment.eml-fe|DIFFERENT|TextType|") == null);
                            cards.observe(childContext, sharedPath, sharedPath, "12345678901");
                            check(cards.resolve(childContext, sharedPath, sharedPath) == null);
                            sharedPath.setLength(sameTemplate.length());
                            cards.observe(rootContext, sharedPath, sharedPath, "abc_def-123");
                            sharedPath.append("ContainerType|TextType|");
                            check("abc_def-123".equals(cards.resolve(new Object(), sharedPath, sharedPath)));
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
