/* Android mount fixture; no YouTube code. */
package com.facebook.litho;
import android.content.Context;
import android.widget.FrameLayout;
import java.util.List;
public class ComponentHost extends FrameLayout {
    public List<CharSequence> texts = List.of();
    public boolean nullContent;
    public ComponentHost(Context context) { super(context); }
    public TextContent getTextContent() { return nullContent ? null : () -> texts; }
}
