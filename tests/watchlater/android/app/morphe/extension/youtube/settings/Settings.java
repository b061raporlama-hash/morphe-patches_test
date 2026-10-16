package app.morphe.extension.youtube.settings;
public class Settings {
    public static final Toggle WATCH_LATER_SHORTCUTS = new Toggle();
    public static class Toggle {
        public boolean value = true;
        public boolean get() { return value; }
    }
}
