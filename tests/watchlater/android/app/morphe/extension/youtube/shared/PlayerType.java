package app.morphe.extension.youtube.shared;
public class PlayerType {
    public static boolean maximized;
    public static PlayerType getCurrent() { return new PlayerType(); }
    public boolean isMaximizedOrFullscreen() { return maximized; }
}
