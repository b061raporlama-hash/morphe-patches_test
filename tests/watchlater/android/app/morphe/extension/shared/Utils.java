package app.morphe.extension.shared;
public class Utils {
    public static void runOnMainThread(Runnable task) { task.run(); }
    public static void runOnBackgroundThread(Runnable task) { task.run(); }
    public static void showToastShort(String key) { }
}
