/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches;

import static app.morphe.extension.shared.StringRef.str;

import android.text.TextUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.youtube.patches.utils.requests.SaveToWatchLaterRequest;

@SuppressWarnings("unused")
public final class SaveToWatchLaterPatch {
    private static final Set<String> pendingVideos = ConcurrentHashMap.newKeySet();

    public static boolean isPatchIncluded() {
        return false; // Modified during patching.
    }

    public static void saveVideo(String videoId) {
        saveVideo(videoId, success -> {});
    }

    /** The callback always runs on the main thread; only a confirmed response counts as saved. */
    public static void saveVideo(String videoId, Consumer<Boolean> callback) {
        if (AuthUtils.isNotLoggedIn()) {
            Utils.showToastShort(str("morphe_queue_manager_check_failed_auth"));
            complete(callback, false);
            return;
        }
        if (TextUtils.isEmpty(videoId)) {
            Utils.showToastShort(str("morphe_queue_manager_check_failed_video_id"));
            complete(callback, false);
            return;
        }
        // Suppress repeated taps on the same card without dropping saves of other videos.
        if (!pendingVideos.add(videoId)) {
            complete(callback, false);
            return;
        }
        try {
            Utils.runOnBackgroundThread(() -> {
                boolean success = false;
                try {
                    SaveToWatchLaterRequest request = SaveToWatchLaterRequest.fetchRequest(
                            videoId, AuthUtils.getRequestHeader());
                    Boolean result = request.getResult();
                    success = result != null;
                    if (success) {
                        Utils.showToastShort(str(result
                                ? "morphe_save_to_watch_later_success_toast"
                                : "morphe_save_to_watch_later_already_exists_toast"));
                    } else {
                        Utils.showToastShort(str("morphe_save_to_watch_later_error_toast"));
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "Could not save video to Watch later", ex);
                    Utils.showToastShort(str("morphe_save_to_watch_later_error_toast"));
                } finally {
                    pendingVideos.remove(videoId);
                    complete(callback, success);
                }
            });
        } catch (Exception ex) {
            pendingVideos.remove(videoId);
            Logger.printException(() -> "Could not schedule Watch later request", ex);
            Utils.showToastShort(str("morphe_save_to_watch_later_error_toast"));
            complete(callback, false);
        }
    }

    private static void complete(Consumer<Boolean> callback, boolean success) {
        Utils.runOnMainThread(() -> callback.accept(success));
    }
}
