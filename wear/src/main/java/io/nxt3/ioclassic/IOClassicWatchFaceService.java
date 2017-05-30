package io.nxt3.ioclassic;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.nxt3.ioclassic.config.Settings;
import io.nxt3.ioclassic.model.PrefKey;

public class IOClassicWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "IOClassic";
    private static Settings sSettings;
    //Update rate in milliseconds for interactive mode. We update once a second to advance the second hand.
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    //Left/Right dial support types
    public static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_LONG_TEXT, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_LARGE_IMAGE}
    };
    private static final int TOP_DIAL_COMPLICATION = 0;
    private static final int LEFT_DIAL_COMPLICATION = 1;
    private static final int RIGHT_DIAL_COMPLICATION = 2;
    private static final int BOTTOM_DIAL_COMPLICATION = 3;
    private static final int BACKGROUND_COMPLICATION = 4;
    public static final int[] COMPLICATION_IDS = {
            TOP_DIAL_COMPLICATION,
            LEFT_DIAL_COMPLICATION,
            RIGHT_DIAL_COMPLICATION,
            BOTTOM_DIAL_COMPLICATION,
            BACKGROUND_COMPLICATION
    };

    @Override
    public Engine onCreateEngine() {
        sSettings = Settings.getInstance(getApplicationContext()); //get a Settings instance
        return new IOClassicWatchFaceEngine();
    }

    /**
     * The engine responsible for the Drawing of the watch face and receives events from the system
     */
    private class IOClassicWatchFaceEngine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 0;
        private static final float TWO_PI = (float) Math.PI * 2f;
        private static final float THICK_STROKE = 7f;
        private static final float THIN_STROKE = 2f;

        private Calendar mCalendar;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mRegisteredTimeZoneReceiver = false;

        //Colors for each component
        private int mHourHandColor;
        private int mMinuteHandColor;
        private int mSecondHandColor;
        private int mBackgroundColor;
        private int mCircleAndTickColor;
        private int mOuterBackgroundColor;

        //Paint objects for each component
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mBackgroundPaint;
        private Paint mCircleAndTickPaint;
        private Paint mOuterBackgroundPaint;

        /**
         * Handles changing timezones
         */
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        /**
         * Handler to update the time once a second when viewing the watch face
         */
        private final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();

                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);

                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        /**
         * Called when the watch face service is created for the first time
         * We will initialize our drawing components here
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(new WatchFaceStyle.Builder(IOClassicWatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL)
                    .setShowSystemUiTime(false)
                    .build());

            loadSavedPrefs();
            initializeBackground();
            initializeWatchFace();
        }

        private void initializeBackground() {
            Log.d(TAG, "Init background");

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);
            mBackgroundPaint.setAntiAlias(true);

            //Circle that holds the ticks
            mCircleAndTickPaint = new Paint();
            mCircleAndTickPaint.setColor(mCircleAndTickColor);
            mCircleAndTickPaint.setStyle(Paint.Style.STROKE);
            mCircleAndTickPaint.setStrokeWidth(THICK_STROKE);
            mCircleAndTickPaint.setAntiAlias(true);

            mOuterBackgroundPaint = new Paint();
            mOuterBackgroundPaint.setColor(mOuterBackgroundColor);
            mOuterBackgroundPaint.setAntiAlias(true);
        }

        private void initializeWatchFace() {
            Log.d(TAG, "Init watch face components");

            mHourPaint = new Paint();
            mHourPaint.setColor(mHourHandColor);
            mHourPaint.setStyle(Paint.Style.STROKE);
            mHourPaint.setStrokeWidth(THICK_STROKE);
            mHourPaint.setAntiAlias(true);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mMinuteHandColor);
            mMinutePaint.setStyle(Paint.Style.STROKE);
            mMinutePaint.setStrokeWidth(THICK_STROKE);
            mMinutePaint.setAntiAlias(true);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mSecondHandColor);
            mSecondPaint.setStyle(Paint.Style.STROKE);
            mSecondPaint.setStrokeWidth(THIN_STROKE);
            mSecondPaint.setAntiAlias(true);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawWatchFace(canvas, bounds);
        }

        public void drawBackground(Canvas canvas) {
            if (mAmbient && (!mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawColor(mBackgroundColor);
            }
        }

        public void drawWatchFace(Canvas canvas, Rect bounds) {
            final int WIDTH = bounds.width();
            final int HEIGHT = bounds.height();

            final float minHrOverflow = 10f;
            final float secOverflow = 16f;

            //Offset/distance between the edge and the inner circle
            final float circleOffset = 24f;

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = WIDTH / 2f;
            float centerY = HEIGHT / 2f;

            float innerX, innerY, outerX, outerY;

            // draw the clock pointers
            float seconds = mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float secRot = seconds / 60f * TWO_PI;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float minRot = minutes / 60f * TWO_PI;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            float hrRot = hours / 12f * TWO_PI;

            final float hourHandLength = centerX - 95;
            final float minuteHandLength = centerX - 65;
            final float secondHandLength = centerX - 60;

            if (!isInAmbientMode()) {
                //draws backgrounds
                canvas.drawColor(mOuterBackgroundColor);
                canvas.drawCircle(centerX, centerY, WIDTH / 2, mOuterBackgroundPaint);
                canvas.drawCircle(centerX, centerY, WIDTH / 2 - circleOffset - 20.0f, mBackgroundPaint);

                //draws second hand
                float secX = (float) Math.sin(secRot);
                float secY = (float) -Math.cos(secRot);
                canvas.drawLine(centerX - secX * secOverflow, centerY - secY * secOverflow, centerX + secX * secondHandLength, centerY + secY * secondHandLength, mSecondPaint);
            }

            //draws hour hand
            float hrX = (float) Math.sin(hrRot);
            float hrY = (float) -Math.cos(hrRot);
            canvas.drawLine(centerX - hrX * minHrOverflow, centerY - hrY * minHrOverflow, centerX + hrX * hourHandLength, centerY + hrY * hourHandLength, mHourPaint);

            //draws minute hand
            float minX = (float) Math.sin(minRot);
            float minY = (float) -Math.cos(minRot);
            canvas.drawLine(centerX - minX * minHrOverflow, centerY - minY * minHrOverflow, centerX + minX * minuteHandLength, centerY + minY * minuteHandLength, mMinutePaint);

            //draws the tick marks
            float innerTickRadius = centerX - circleOffset - 14;
            float outerTickRadius = centerX - circleOffset - 1;

            for (int tickIndex = 0; tickIndex < 4; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 4);
                innerX = (float) Math.sin(tickRot) * innerTickRadius;
                innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                outerX = (float) Math.sin(tickRot) * outerTickRadius;
                outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, mCircleAndTickPaint);
            }

            //draws circle for the ticks
            canvas.drawArc(circleOffset, circleOffset, WIDTH - circleOffset, HEIGHT - circleOffset, 0, 360, false, mCircleAndTickPaint);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                loadSavedPrefs();
                updateWatchPaintStyles();

                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            updateTimer();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;

            updateWatchPaintStyles();

            //Check and trigger whether or not timer should be running (only in active mode)
            updateTimer();
        }

        /**
         * Update the watch paint styles when changing between Ambient and Non-Ambient modes
         */
        private void updateWatchPaintStyles() {
            if (mAmbient) {
                mBackgroundPaint.setColor(Color.BLACK);

                mHourPaint.setColor(Color.WHITE);
                mHourPaint.setStrokeWidth(THIN_STROKE);

                mMinutePaint.setColor(Color.WHITE);
                mMinutePaint.setStrokeWidth(THIN_STROKE);

                mSecondPaint.setColor(Color.WHITE);

                mCircleAndTickPaint.setColor(Color.WHITE);
                mCircleAndTickPaint.setStrokeWidth(THIN_STROKE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
            } else {
                mBackgroundPaint.setColor(mBackgroundColor);

                mHourPaint.setColor(mHourHandColor);
                mHourPaint.setStrokeWidth(THICK_STROKE);

                mMinutePaint.setColor(mMinuteHandColor);
                mMinutePaint.setStrokeWidth(THICK_STROKE);

                mSecondPaint.setColor(mSecondHandColor);

                mCircleAndTickPaint.setColor(mCircleAndTickColor);
                mCircleAndTickPaint.setStrokeWidth(THICK_STROKE);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mCircleAndTickPaint.setAntiAlias(true);
            }
        }

        /**
         * Loads the user selected colors for each component
         */
        private void loadSavedPrefs() {
            mHourHandColor = sSettings.getInt(PrefKey.HOUR_HAND_COLOR, Color.WHITE);
            mMinuteHandColor = sSettings.getInt(PrefKey.MINUTE_HAND_COLOR, Color.WHITE);
            mSecondHandColor = sSettings.getInt(PrefKey.SECOND_HAND_COLOR, Color.RED);

            mBackgroundColor = sSettings.getInt(PrefKey.BACKGROUND_COLOR, Color.DKGRAY);
            mCircleAndTickColor = sSettings.getInt(PrefKey.CIRCLE_AND_TICKS_COLOR, Color.WHITE);
            mOuterBackgroundColor = sSettings.getInt(PrefKey.OUTER_CIRCLE_COLOR, Color.GRAY);
        }

        /**
         * Register a receiver for handling timezone changes
         */
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            IOClassicWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        /**
         * Unregister a receiver for handling timezone changes
         */
        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            IOClassicWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * @return whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run in active mode
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }
    }
}