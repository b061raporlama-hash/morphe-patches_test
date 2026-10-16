/* Copyright 2026 Cep Patcher contributors. GPL-3.0-only. */
package app.morphe.manager;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.facebook.litho.ComponentHost;
import java.util.ArrayList;
import java.util.List;
import app.morphe.extension.shared.patches.components.ContextInterface;
import app.morphe.extension.youtube.patches.WatchLaterShortcutsPatch;
import app.morphe.extension.youtube.patches.utils.requests.SaveToWatchLaterRequest;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.ShortsPlayerState;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Uses production identity/span/overlay/save sources; only Litho mount data and network are fixtures. */
@RunWith(AndroidJUnit4.class)
public class WatchLaterOverlayTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Activity activity;
    private FrameLayout screen;
    private ComponentHost measured;
    private ComponentHost first;
    private ComponentHost second;
    private int cardHeight;

    private static ContextInterface context(String path) {
        return new ContextInterface() {
            final StringBuilder builder = new StringBuilder(path);
            public String patch_getIdentifier() { return "home_video_with_context.e"; }
            public StringBuilder patch_getPathBuilder() { return builder; }
            public Integer patch_getHeightConstraint() { return null; }
            public Object patch_getHorizontalCollectionSwipeProtector() { return null; }
        };
    }

    private static CharSequence title(String hash, String id) {
        String path = "feed|home_video_with_context.eml-fe|" + hash + "|";
        WatchLaterShortcutsPatch.bindContext(context(path), id);
        // A child has a NEW conversion context and builder, and no thumbnail in its buffer.
        ContextInterface child = context(path + "ContainerType|TextType|");
        WatchLaterShortcutsPatch.bindContext(child, null);
        return WatchLaterShortcutsPatch.onLithoTextLoaded(child, "Title " + hash);
    }

    private ComponentHost card(int top, CharSequence text) {
        ComponentHost card = new ComponentHost(activity);
        card.setBackgroundColor(top == 0 ? Color.LTGRAY : Color.GRAY);
        card.texts = List.of(text);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, cardHeight);
        params.topMargin = top;
        measured.addView(card, params);
        return card;
    }

    private void draw() {
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            View root = screen.getRootView();
            root.getViewTreeObserver().dispatchOnPreDraw();
            root.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(root.getHeight(), View.MeasureSpec.EXACTLY));
            root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
            root.getViewTreeObserver().dispatchOnPreDraw();
        });
        instrumentation.waitForIdleSync();
    }

    private static void buttons(View view, List<ImageButton> result) {
        if (view instanceof ImageButton button && button.getVisibility() == View.VISIBLE) result.add(button);
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            buttons(group.getChildAt(i), result);
        }
    }

    private List<ImageButton> visibleButtons() {
        List<ImageButton> result = new ArrayList<>();
        buttons(screen.getRootView(), result);
        return result;
    }

    @Test public void mountedCardsShowClocksSaveTheirOwnIdsAndFollowRecycling() throws Exception {
        activity = instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),
                WatchLaterFixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                Settings.WATCH_LATER_SHORTCUTS.value = true;
                PlayerType.maximized = false;
                ShortsPlayerState.open = false;
                SaveToWatchLaterRequest.ids.clear();
                screen = new FrameLayout(activity);
                activity.setContentView(screen);
            });
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                int width = screen.getWidth();
                assertTrue(width > 0);
                cardHeight = width * 9 / 16 + Math.round(64 * screen.getResources().getDisplayMetrics().density);
                measured = new ComponentHost(activity);
                measured.nullContent = true; // Not every mount host provides TextContent.
                screen.addView(measured, new FrameLayout.LayoutParams(-1, cardHeight * 2));
                first = card(0, title("card-A", "abcdefghijk"));
                second = card(cardHeight, title("card-B", "12345678901"));
                WatchLaterShortcutsPatch.onLithoViewMeasured(measured);
            });
            draw();
            int[] touch = new int[2];
            instrumentation.runOnMainSync(() -> {
                assertTrue(screen.hasWindowFocus());
                List<ImageButton> buttons = visibleButtons();
                assertEquals("Each nested card needs a clock; the measured parent contains multiple IDs", 2, buttons.size());
                Rect host = new Rect(), clock = new Rect();
                first.getGlobalVisibleRect(host);
                assertTrue(buttons.get(0).getGlobalVisibleRect(clock));
                assertTrue("Clock must be inside its thumbnail", host.contains(clock));
                try (var output = new java.io.FileOutputStream(new java.io.File(activity.getFilesDir(), "watch-later-overlay.png"))) {
                    android.graphics.Bitmap screenshot = android.graphics.Bitmap.createBitmap(screen.getRootView().getWidth(),
                            screen.getRootView().getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
                    screen.getRootView().draw(new android.graphics.Canvas(screenshot));
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
                    screenshot.recycle();
                } catch (java.io.IOException ex) { throw new AssertionError(ex); }
                touch[0] = clock.centerX();
                touch[1] = clock.centerY();
            });
            long down = android.os.SystemClock.uptimeMillis();
            android.view.MotionEvent press = android.view.MotionEvent.obtain(down, down,
                    android.view.MotionEvent.ACTION_DOWN, touch[0], touch[1], 0);
            android.view.MotionEvent release = android.view.MotionEvent.obtain(down, down + 50,
                    android.view.MotionEvent.ACTION_UP, touch[0], touch[1], 0);
            instrumentation.sendPointerSync(press);
            instrumentation.sendPointerSync(release);
            press.recycle();
            release.recycle();
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                List<ImageButton> buttons = visibleButtons();
                assertEquals("Overlay must receive a real screen tap", List.of("abcdefghijk"), SaveToWatchLaterRequest.ids);
                buttons.get(1).performClick();
                assertEquals(List.of("abcdefghijk", "12345678901"), SaveToWatchLaterRequest.ids);
                assertEquals("morphe_watch_later_shortcuts_saved", buttons.get(0).getContentDescription());
                first.texts = List.of(title("card-C", "abc_def-123"));
            });
            draw();
            instrumentation.runOnMainSync(() -> {
                List<ImageButton> buttons = visibleButtons();
                assertEquals(2, buttons.size());
                assertEquals("morphe_watch_later_shortcuts_add", buttons.get(0).getContentDescription());
                buttons.get(0).performClick();
                assertEquals("abc_def-123", SaveToWatchLaterRequest.ids.get(2));
                first.texts = List.of(title("card-D", null));
            });
            draw();
            instrumentation.runOnMainSync(() -> assertEquals("Unidentified recycled card must hide its clock", 1, visibleButtons().size()));
            instrumentation.runOnMainSync(() -> second.setTranslationY(screen.getHeight()));
            draw();
            instrumentation.runOnMainSync(() -> assertEquals("Clipped/off-screen cards must not leave a floating clock", 0, visibleButtons().size()));
            instrumentation.runOnMainSync(() -> second.setTranslationY(0));
            draw();
            instrumentation.runOnMainSync(() -> assertEquals(1, visibleButtons().size()));
            instrumentation.runOnMainSync(() -> PlayerType.maximized = true);
            draw();
            instrumentation.runOnMainSync(() -> assertEquals(0, visibleButtons().size()));
            instrumentation.runOnMainSync(() -> { PlayerType.maximized = false; ShortsPlayerState.open = true; });
            draw();
            instrumentation.runOnMainSync(() -> assertEquals(0, visibleButtons().size()));
            instrumentation.runOnMainSync(() -> { ShortsPlayerState.open = false; Settings.WATCH_LATER_SHORTCUTS.value = false; });
            draw();
            instrumentation.runOnMainSync(() -> assertEquals(0, visibleButtons().size()));
        } finally {
            instrumentation.runOnMainSync(() -> { Settings.WATCH_LATER_SHORTCUTS.value = true; activity.finish(); });
        }
    }
}
