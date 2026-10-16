package app.morphe.extension.youtube.patches.utils.requests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class SaveToWatchLaterRequest {
    public static final List<String> ids = new ArrayList<>();
    public static SaveToWatchLaterRequest fetchRequest(String id, Map<String, String> headers) {
        ids.add(id);
        return new SaveToWatchLaterRequest();
    }
    public Boolean getResult() { return true; }
}
