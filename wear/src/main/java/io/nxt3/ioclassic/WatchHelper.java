package io.nxt3.ioclassic;


import android.content.Context;
import android.util.TypedValue;

public class WatchHelper {

    static float dpToPx(Context context, final int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
