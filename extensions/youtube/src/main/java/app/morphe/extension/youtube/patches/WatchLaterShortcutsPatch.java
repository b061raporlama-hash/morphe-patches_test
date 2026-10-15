/*
 * Copyright 2026 Morphe.
 * See the included NOTICE file for GPLv3 Section 7 terms.
 */
package app.morphe.extension.youtube.patches;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.litho.ComponentHost;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.patches.components.ContextInterface;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.ShortsPlayerState;

/** Uses the card's own conversion context; never falls back to the active player. */
@SuppressWarnings("unused")
public final class WatchLaterShortcutsPatch {
    private record VideoIdSpan(String videoId) {}

    // Conversion and text creation are multithreaded. Weak keys do not retain old feeds.
    private static final Map<ContextInterface, String> contexts =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<StringBuilder, String> pathContexts =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<FrameLayout, WeakReference<Overlay>> overlays = new WeakHashMap<>();
    private static final Map<View, Boolean> observedHosts = new WeakHashMap<>();

    public static boolean isPatchIncluded() {
        return false; // Modified during patching.
    }

    public static boolean isEnabled() {
        return isPatchIncluded() && Settings.WATCH_LATER_SHORTCUTS.get();
    }

    public static void bindContext(ContextInterface context, String videoId) {
        StringBuilder path = context.patch_getPathBuilder();
        if (videoId == null) {
            contexts.remove(context);
            pathContexts.remove(path);
        } else {
            contexts.put(context, videoId);
            // Child conversion contexts may share the card's path builder.
            pathContexts.put(path, videoId);
        }
    }

    /** Carries identity with mounted text, including when Litho reuses cached spans. */
    public static CharSequence onLithoTextLoaded(ContextInterface context, CharSequence text) {
        if (text == null || text.length() == 0) return text;
        String id = isEnabled() ? contexts.get(context) : null;
        if (id == null && isEnabled()) id = pathContexts.get(context.patch_getPathBuilder());
        VideoIdSpan[] old = text instanceof Spanned spanned
                ? spanned.getSpans(0, text.length(), VideoIdSpan.class) : new VideoIdSpan[0];
        if (id == null && old.length == 0) return text;
        SpannableString marked = new SpannableString(text);
        for (VideoIdSpan span : old) marked.removeSpan(span);
        if (id != null) {
            marked.setSpan(new VideoIdSpan(id), 0, marked.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return marked;
    }

    public static void onLithoViewMeasured(View view) {
        if (!isEnabled() || !(view instanceof ComponentHost)) return;
        if (observedHosts.put(view, Boolean.TRUE) == null) {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) { v.post(() -> registerHost(v)); }
                @Override public void onViewDetachedFromWindow(View v) { /* removed on next draw */ }
            });
        }
        if (view.isAttachedToWindow()) view.post(() -> registerHost(view));
    }

    private static void registerHost(View host) {
        if (!(host.getRootView() instanceof FrameLayout root)) return;
        WeakReference<Overlay> ref = overlays.get(root);
        Overlay overlay = ref == null ? null : ref.get();
        if (overlay == null) {
            overlay = new Overlay(root);
            overlays.put(root, new WeakReference<>(overlay));
        }
        overlay.track(host);
    }

    /** Called before the Shorts hide setting so that the replacement stays visible. */
    public static boolean bindNavigationTab(View tab) {
        if (!isEnabled()) return false;
        Utils.runOnMainThread(() -> {
            tab.setVisibility(View.VISIBLE);
            tab.setContentDescription(str("morphe_watch_later_shortcuts_label"));
            tab.setOnClickListener(v -> openWatchLater(v));
            updateNavigationAppearance(tab);
            // YouTube can bind icons and labels again after creating the tab.
            tab.getViewTreeObserver().addOnPreDrawListener(() -> {
                updateNavigationAppearance(tab);
                return true;
            });
        });
        return true;
    }

    private static void updateNavigationAppearance(View view) {
        if (view instanceof ImageView image && !(image.getDrawable() instanceof ClockDrawable)) {
            image.setImageDrawable(new ClockDrawable());
            if (image.getImageTintList() == null) {
                TypedArray colors = view.getContext().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
                try {
                    image.setImageTintList(colors.getColorStateList(0));
                } finally {
                    colors.recycle();
                }
            }
        } else if (view instanceof TextView text) {
            String label = str("morphe_watch_later_shortcuts_label");
            if (!label.contentEquals(text.getText())) text.setText(label);
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) updateNavigationAppearance(group.getChildAt(i));
        }
    }

    private static void openWatchLater(View view) {
        if (AuthUtils.isNotLoggedIn()) {
            Utils.showToastShort(str("morphe_queue_manager_check_failed_auth"));
            return;
        }
        try {
            view.getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/playlist?list=WL"))
                    .setPackage(view.getContext().getPackageName()));
        } catch (Exception ex) {
            Logger.printException(() -> "Could not open Watch later", ex);
            Utils.showToastShort(str("morphe_watch_later_shortcuts_open_error"));
        }
    }

    private static String videoId(ComponentHost host) {
        Set<String> ids = new HashSet<>();
        collectVideoIds(host, ids);
        return ids.size() == 1 ? ids.iterator().next() : null;
    }

    private static void collectVideoIds(View view, Set<String> ids) {
        if (ids.size() > 1) return;
        if (view instanceof ComponentHost host) {
            for (CharSequence text : host.getTextContent().getTextItems()) {
                if (!(text instanceof Spanned spanned)) continue;
                for (VideoIdSpan span : spanned.getSpans(0, text.length(), VideoIdSpan.class)) {
                    ids.add(span.videoId());
                }
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount() && ids.size() <= 1; i++) {
                collectVideoIds(group.getChildAt(i), ids);
            }
        }
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static final class Card {
        final WeakReference<View> host;
        final ImageButton button;
        String videoId;
        String savedVideoId;
        String pendingVideoId;

        Card(View host, FrameLayout layer) {
            this.host = new WeakReference<>(host);
            button = new ImageButton(layer.getContext());
            button.setImageDrawable(new ClockDrawable());
            button.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            int padding = dp(layer, 8);
            button.setPadding(padding, padding, padding, padding);
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xB3181818);
            background.setCornerRadius(dp(layer, 8));
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x66FFFFFF), background, null));
            button.setContentDescription(str("morphe_watch_later_shortcuts_add"));
            button.setVisibility(View.GONE);
            button.setOnClickListener(v -> {
                String requestedId = videoId;
                if (requestedId == null || requestedId.equals(pendingVideoId)) return;
                pendingVideoId = requestedId;
                button.setEnabled(false);
                SaveToWatchLaterPatch.saveVideo(requestedId, success -> {
                    if (requestedId.equals(pendingVideoId)) pendingVideoId = null;
                    if (success) savedVideoId = requestedId;
                    // The host may have been recycled while the request was running.
                    button.setEnabled(videoId != null && !videoId.equals(pendingVideoId));
                    updateIcon();
                });
            });
            layer.addView(button, new FrameLayout.LayoutParams(dp(layer, 48), dp(layer, 48), Gravity.TOP | Gravity.LEFT));
        }

        void updateIcon() {
            boolean saved = videoId != null && videoId.equals(savedVideoId);
            if (button.getDrawable() instanceof ClockDrawable clock) clock.setSaved(saved);
            button.setContentDescription(str(saved
                    ? "morphe_watch_later_shortcuts_saved" : "morphe_watch_later_shortcuts_add"));
        }
    }

    /** A separate touch layer keeps custom children out of Litho's mount bookkeeping. */
    private static final class Overlay {
        final WeakReference<FrameLayout> root;
        final FrameLayout layer;
        final List<Card> cards = new ArrayList<>();
        final Rect visible = new Rect();
        final int[] rootLocation = new int[2];
        final int[] hostLocation = new int[2];

        Overlay(FrameLayout root) {
            this.root = new WeakReference<>(root);
            layer = new FrameLayout(root.getContext());
            layer.setClickable(false);
            layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            layer.setTag(this); // Root owns the controller; the static map holds only a weak reference.
            root.addView(layer, new FrameLayout.LayoutParams(-1, -1));
            root.getViewTreeObserver().addOnPreDrawListener(this::draw);
        }

        void track(View host) {
            for (Card card : cards) if (card.host.get() == host) return;
            cards.add(new Card(host, layer));
        }

        boolean draw() {
            try {
                FrameLayout window = root.get();
                boolean show = window != null && window.hasWindowFocus() && isEnabled()
                        && !PlayerType.getCurrent().isMaximizedOrFullscreen() && !ShortsPlayerState.isOpen();
                if (window != null) window.getLocationOnScreen(rootLocation);
                List<Rect> occupied = new ArrayList<>();
                for (Iterator<Card> iterator = cards.iterator(); iterator.hasNext();) {
                    Card card = iterator.next();
                    View host = card.host.get();
                    if (host == null || !host.isAttachedToWindow()) {
                        layer.removeView(card.button);
                        iterator.remove();
                        continue;
                    }
                    String id = show && host.isShown() ? videoId((ComponentHost) host) : null;
                    // Full-width, landscape feed cards. Never guess bounds for compact rows or Shorts.
                    boolean fullCard = host.getWidth() >= dp(host, 160)
                            && host.getHeight() >= host.getWidth() * 9 / 16 + dp(host, 16);
                    if (id == null || !fullCard || !host.getGlobalVisibleRect(visible)) {
                        card.videoId = null;
                        card.button.setVisibility(View.GONE);
                        continue;
                    }
                    host.getLocationOnScreen(hostLocation);
                    int left = hostLocation[0] + dp(host, 8);
                    int top = hostLocation[1] + dp(host, 8);
                    Rect target = new Rect(left, top, left + dp(host, 48), top + dp(host, 48));
                    boolean overlaps = false;
                    for (Rect previous : occupied) overlaps |= Rect.intersects(previous, target);
                    if (!visible.contains(target) || overlaps) {
                        card.videoId = null;
                        card.button.setVisibility(View.GONE);
                        continue;
                    }
                    occupied.add(target);
                    card.videoId = id;
                    card.button.setEnabled(!id.equals(card.pendingVideoId));
                    card.updateIcon();
                    card.button.setTranslationX(left - rootLocation[0]);
                    card.button.setTranslationY(top - rootLocation[1]);
                    card.button.setVisibility(View.VISIBLE);
                }
            } catch (Exception ex) {
                for (Card card : cards) card.button.setVisibility(View.GONE);
                Logger.printException(() -> "Watch later overlay failure", ex);
            }
            return true;
        }
    }

    private static final class ClockDrawable extends Drawable {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boolean saved;

        void setSaved(boolean saved) {
            if (this.saved != saved) {
                this.saved = saved;
                invalidateSelf();
            }
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            int save = canvas.save();
            canvas.translate(bounds.left, bounds.top);
            canvas.scale(bounds.width() / 24f, bounds.height() / 24f);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            if (saved) {
                canvas.drawLine(5, 12, 10, 17, paint);
                canvas.drawLine(10, 17, 19, 7, paint);
            } else {
                canvas.drawCircle(12, 12, 9, paint);
                canvas.drawLine(12, 6, 12, 12, paint);
                canvas.drawLine(12, 12, 16, 14, paint);
            }
            canvas.restoreToCount(save);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return 24; }
        @Override public int getIntrinsicHeight() { return 24; }
    }
}
