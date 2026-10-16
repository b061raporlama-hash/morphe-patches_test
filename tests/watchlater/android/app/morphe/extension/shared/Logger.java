package app.morphe.extension.shared;
import java.util.function.Supplier;
public class Logger {
    public static void printException(Supplier<String> message, Exception ex) {
        throw new AssertionError(message.get(), ex);
    }
}
