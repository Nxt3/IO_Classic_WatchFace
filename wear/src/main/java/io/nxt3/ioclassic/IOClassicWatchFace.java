package io.nxt3.ioclassic;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class IOClassicWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new IOClassicWatchFaceEngine();
    }

    /**
     * The engine responsible for the Drawing of the watch face and receives events from the system
     */
    private class IOClassicWatchFaceEngine extends CanvasWatchFaceService.Engine {

        private TextPaint timeText;
        private TextPaint dateText;

        private SimpleDateFormat timeFormat;
        private SimpleDateFormat dateFormat;

        /**
         * Called when the watch face service is created for the first time
         * We will initialize our drawing components here
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            timeFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());
            dateFormat = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

            final Context context = getApplicationContext();

            timeText = new TextPaint();
            timeText.setTextSize(WatchHelper.dpToPx(context, 32));
            timeText.setColor(Color.BLACK);
            timeText.setAntiAlias(true);

            dateText = new TextPaint();
            dateText.setTextSize(WatchHelper.dpToPx(context, 24));
            dateText.setColor(Color.BLACK);
            dateText.setAntiAlias(true);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
//            canvas.drawColor(Color.CYAN); //TODO, make background color a setting

            // We draw the watch face here
            long time = System.currentTimeMillis();
            String timeString = timeFormat.format(time);
            String dateString = dateFormat.format(time);

            Rect timeTextBounds = new Rect();
            timeText.getTextBounds(timeString, 0, timeString.length(), timeTextBounds);

            Rect dateTextBounds = new Rect();
            dateText.getTextBounds(dateString, 0, dateString.length(), dateTextBounds);

            final float marginPercent = 0.02f;
            final int timeX = Math.abs(bounds.centerX() - timeTextBounds.centerX());
            final int timeY = Math.round(Math.abs(bounds.centerY()) - (bounds.height() * marginPercent));

            final int dateX = Math.abs(bounds.centerX() - dateTextBounds.centerX());
            final int dateY = Math.round(bounds.centerY() + dateTextBounds.height() + (bounds.height() * marginPercent));

            canvas.drawText(timeString, timeX, timeY, timeText);
            canvas.drawText(dateString, dateX, dateY, dateText);
        }
    }
}
