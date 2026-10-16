/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.morphe.extension.youtube.patches.watchlater.WatchLaterCardIdentity;
import app.morphe.extension.youtube.patches.watchlater.WatchLaterScopeCarrier;
import dalvik.system.InMemoryDexClassLoader;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Executes the patcher's emitted bytecode on ART, including a known-broken negative control. */
@RunWith(AndroidJUnit4.class)
public class WatchLaterDexTest {
    private ClassLoader load(String asset) throws Exception {
        try (var input = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(asset)) {
            var bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new InMemoryDexClassLoader(ByteBuffer.wrap(bytes.toByteArray()),
                    WatchLaterCardIdentity.class.getClassLoader());
        }
    }

    @Test public void oldReturnHookIsRejectedByArt() throws Exception {
        ClassLoader loader = load("watchlater-broken.dex");
        Throwable rejected = null;
        try {
            // ART can defer verification through loadClass AND getDeclaredConstructors.
            // Invoke the invalid constructor, exactly as YouTube's startup does.
            Class<?> context = loader.loadClass("cep.fixture.Context");
            Object root = context.getConstructor().newInstance();
            loader.loadClass("cep.fixture.Builder").getConstructor(context).newInstance(root);
        } catch (VerifyError expected) {
            rejected = expected;
        } catch (java.lang.reflect.InvocationTargetException wrapped) {
            rejected = wrapped.getCause();
        }
        assertNotNull("ART must reject cep.14's primitive argument to copy(Object,Object)", rejected);
        assertTrue(rejected.toString(), rejected instanceof VerifyError);
        assertTrue(rejected.toString(), rejected.toString().contains("cep.fixture.Builder"));
    }

    @Test public void transformedConstructorAndBuildPreserveScopeDespiteParameterReuse() throws Exception {
        ClassLoader loader = load("watchlater-fixed.dex");
        Class<?> context = loader.loadClass("cep.fixture.Context");
        Class<?> builder = loader.loadClass("cep.fixture.Builder");
        var constructor = builder.getConstructor(context);
        var build = builder.getMethod("build");
        var payloadField = context.getField("payload");
        String path = "feed|home_video_with_context.eml-fe|shared-template|";
        var identity = new WatchLaterCardIdentity();
        Object root = context.getConstructor().newInstance();
        Object payload = new Object();
        payloadField.set(root, payload);
        identity.observe(root, path, "abcdefghijk");
        Object scope = ((WatchLaterScopeCarrier) root).patch_getWatchLaterScope();
        assertNotNull(scope);
        for (int i = 0; i < 40; i++) {
            Object copyBuilder = constructor.newInstance(root);
            assertSame(scope, ((WatchLaterScopeCarrier) copyBuilder).patch_getWatchLaterScope());
            assertEquals(1023, builder.getField("flags").getShort(copyBuilder));
            Object copiedContext = build.invoke(copyBuilder);
            assertSame(scope, ((WatchLaterScopeCarrier) copiedContext).patch_getWatchLaterScope());
            assertSame(payload, payloadField.get(copiedContext));
            assertEquals("abcdefghijk", identity.resolve(copiedContext, path).videoId());
            root = copiedContext;
        }
        Object unbound = context.getConstructor().newInstance();
        Object unboundCopy = build.invoke(constructor.newInstance(unbound));
        assertNull(((WatchLaterScopeCarrier) unboundCopy).patch_getWatchLaterScope());
        // Independent objects with the same template must retain independent scopes.
        identity.observe(unbound, path, "12345678901");
        Object separate = build.invoke(constructor.newInstance(unbound));
        assertNotSame(scope, ((WatchLaterScopeCarrier) separate).patch_getWatchLaterScope());
        assertEquals("12345678901", identity.resolve(separate, path).videoId());
    }
}
