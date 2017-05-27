package io.nxt3.ioclassic;


import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.view.SurfaceHolder;

public class IOClassicWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new IOClassicWatchFaceEngine();
    }

    /**
     * The engine responsible for the Drawing of the watch face and receives events from the system
     */
    private class IOClassicWatchFaceEngine extends CanvasWatchFaceService.Engine {
        /**
         * Called when the watch face service is created for the first time
         * We will initialize our drawing components here
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

        }
    }
}
