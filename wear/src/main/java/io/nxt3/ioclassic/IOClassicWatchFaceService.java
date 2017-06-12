package io.nxt3.ioclassic;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class IOClassicWatchFaceService extends CanvasWatchFaceService {
    private final String TAG = "IOClassic";
    //Update rate in milliseconds for interactive mode. We update once a second to advance the second hand.
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    //Left/Right dial support types
    public static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_LONG_TEXT, ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON}
    };
    private static final int TOP_COMPLICATION_ID = 0;
    private static final int LEFT_COMPLICATION_ID = 1;
    private static final int RIGHT_COMPLICATION_ID = 2;
    private static final int BOTTOM_COMPLICATION_ID = 3;
    public static final int[] COMPLICATION_IDS = {
            TOP_COMPLICATION_ID,
            LEFT_COMPLICATION_ID,
            RIGHT_COMPLICATION_ID,
            BOTTOM_COMPLICATION_ID
    };

    @Override
    public Engine onCreateEngine() {
        return new IOClassicWatchFaceEngine();
    }

    /**
     * The engine responsible for the Drawing of the watch face and receives events from the system
     */
    private class IOClassicWatchFaceEngine extends CanvasWatchFaceService.Engine {
        private final int MSG_UPDATE_TIME = 0;
        private final float TWO_PI = (float) Math.PI * 2f;
        private final float THICK_STROKE = 7f;
        private final float MINUTE_TICK_STROKE = 2f;
        private final float SECOND_HAND_STROKE = 3f;
        private final float AMBIENT_STROKE = 2f;

        //Used for managing the time
        private Calendar mCalendar;

        //SharedPrefs for getting settings
        private SharedPreferences mPrefs;

        //Booleans for various device specific settings
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mHasFlatTire;

        private boolean mRegisteredTimeZoneReceiver = false;

        //Coordinates for center (x, y)
        private int mCenterX;
        private int mCenterY;

        //Colors for each component
        private int mHourHandColor;
        private int mMinuteHandColor;
        private int mSecondHandColor;
        private int mCenterCircleColor;
        private int mCircleAndTickColor;
        private int mOuterCircleColor;
        private int mHourLabelsColor;

        //Paint objects for each component
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mBackgroundPaint;
        private Paint mCircleAndTickPaint;
        private Paint mMinuteTickPaint;
        private Paint mOuterBackgroundPaint;
        private TextPaint mHourLabelTextPaint;

        //Colors for each complication component
        private int mComplicationColor;
        private int mComplicationTitleColor;
        private int mComplicationBorderColor;

        //Complication stuff
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;
        private final float COMPLICATION_RADIUS = 4.5f;

        //Fonts
        private final Typeface mAmbientFont = Typeface.create("sans-serif-light", Typeface.NORMAL);
        private Typeface mHourLabelFont;

        //Other settings
        private boolean mShowComplicationBorder;
        private boolean mShowSecondHand;
        private int mNumberHourTicks;
        private boolean mShowMinuteTicks;
        private boolean mClassicMode;
        private int mNumberHourLabels;


        /**
         * Called when the watch face service is created for the first time
         * We will initialize our drawing components here
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(new WatchFaceStyle.Builder(IOClassicWatchFaceService.this)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL
                            | Gravity.TOP)
                    .setViewProtectionMode(WatchFaceStyle.PROTECT_STATUS_BAR
                            | WatchFaceStyle.PROTECT_HOTWORD_INDICATOR)
                    .setAcceptsTapEvents(true)
                    .build());

            loadSavedPrefs();
            initializeBackground();
            initializeComplications();
            initializeWatchFace();
        }

        /**
         * Init watch face components (hour, minute, second hands)
         */
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

            if (mShowSecondHand) {
                mSecondPaint = new Paint();
                mSecondPaint.setColor(mSecondHandColor);
                mSecondPaint.setStyle(Paint.Style.STROKE);
                mSecondPaint.setStrokeWidth(SECOND_HAND_STROKE);
                mSecondPaint.setAntiAlias(true);
            }
        }

        /**
         * Init the backgrounds and circle
         */
        private void initializeBackground() {
            Log.d(TAG, "Init background");

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mCenterCircleColor);
            mBackgroundPaint.setAntiAlias(true);

            //Circle that holds the ticks
            mCircleAndTickPaint = new Paint();
            mCircleAndTickPaint.setColor(mCircleAndTickColor);
            mCircleAndTickPaint.setStyle(Paint.Style.STROKE);
            mCircleAndTickPaint.setStrokeWidth(THICK_STROKE);
            mCircleAndTickPaint.setAntiAlias(true);

            //Minute ticks
            mMinuteTickPaint = new Paint();
            mMinuteTickPaint.setColor(mCircleAndTickColor);
            mMinuteTickPaint.setStyle(Paint.Style.STROKE);
            mMinuteTickPaint.setStrokeWidth(MINUTE_TICK_STROKE);
            mMinuteTickPaint.setAntiAlias(true);

            mOuterBackgroundPaint = new Paint();
            mOuterBackgroundPaint.setColor(mOuterCircleColor);
            mOuterBackgroundPaint.setAntiAlias(true);

            mHourLabelFont = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            mHourLabelTextPaint = new TextPaint();
            mHourLabelTextPaint.setColor(mHourLabelsColor);
            mHourLabelTextPaint.setTextAlign(Paint.Align.CENTER);
            mHourLabelTextPaint.setTypeface(mHourLabelFont);
            mHourLabelTextPaint.setAntiAlias(true);
        }

        /**
         * Init watch face complications components
         */
        private void initializeComplications() {
            Log.d(TAG, "Init complications");
            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            //create a complication for each complicationId
            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                createComplication(COMPLICATION_ID);
            }
            setActiveComplications(COMPLICATION_IDS);
        }

        /**
         * Creates a ComplicationDrawable for the complicationId
         *
         * @param complicationId to create a ComplicationDrawable for
         */
        private void createComplication(int complicationId) {
            final ComplicationDrawable complicationDrawable
                    = (ComplicationDrawable) getDrawable(R.drawable.complication_styles);

            updateComplicationStyles(complicationDrawable);

            mComplicationDrawableSparseArray.put(complicationId, complicationDrawable);
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas, bounds);

            drawComplications(canvas, now);

            if (mShowSecondHand && !mAmbient) {
                drawSecondHand(canvas);
            }
            drawMinuteHand(canvas);
            drawHourHand(canvas);
        }

        /**
         * Handles drawing the background, tick marks w/ circle, and outer circle
         *
         * @param canvas to draw to
         * @param bounds of the device screen
         */
        private void drawBackground(Canvas canvas, Rect bounds) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawColor(mCenterCircleColor);
            }

            final int width = bounds.width();
            final int height = bounds.height();

            /*
              Offset/distance between the edge and the inner circle; bigger if the device
              has a flat tire.
              If mClassicMode is true, then reduce the offset even further
             */
            final float circleOffset = mClassicMode ? 3f : (mHasFlatTire ? 38f : 24f);

            //draws outer circle
            canvas.drawColor(mOuterCircleColor);
            canvas.drawCircle(mCenterX, mCenterY, width / 2,
                    mOuterBackgroundPaint);
            canvas.drawCircle(mCenterX, mCenterY, width / 2 - circleOffset - 20.0f,
                    mBackgroundPaint);

            //used as the starting point for drawing the ticks (drawn from IN to OUT)
            final float innerTickRadius = mCenterX - circleOffset - 14;

            //if mShowMinuteTicks, make the hour ticks slightly longer
            final float innerHourTickRadius = mShowMinuteTicks
                    ? innerTickRadius - 2.50f : innerTickRadius;

            //used as the stopping point for drawing the ticks
            final float outerTickRadius = mCenterX - circleOffset - 1;

            //draw hour tick marks
            for (int tickIndex = 0; tickIndex < mNumberHourTicks; tickIndex++) {
                final float tickRotation = (float) (tickIndex * Math.PI * 2 / mNumberHourTicks);

                final float innerX = (float) Math.sin(tickRotation) * innerHourTickRadius;
                final float innerY = (float) -Math.cos(tickRotation) * innerHourTickRadius;
                final float outerX = (float) Math.sin(tickRotation) * outerTickRadius;
                final float outerY = (float) -Math.cos(tickRotation) * outerTickRadius;

                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mCircleAndTickPaint);
            }

            //draws minute tick marks
            for (int tickIndex = 0; tickIndex < 60 && mShowMinuteTicks; tickIndex++) {
                final float tickRotation = (float) (tickIndex * Math.PI * 2 / 60);

                final float innerX = (float) Math.sin(tickRotation) * innerTickRadius;
                final float innerY = (float) -Math.cos(tickRotation) * innerTickRadius;
                final float outerX = (float) Math.sin(tickRotation) * outerTickRadius;
                final float outerY = (float) -Math.cos(tickRotation) * outerTickRadius;

                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mMinuteTickPaint);
            }

            //draws hour text labels
            for (int hourIndex = 0; hourIndex < mNumberHourLabels; hourIndex++) {
                final float tickRotation = (float) (hourIndex * Math.PI * 2 / mNumberHourLabels);

                final float textOffset = dpToPx(13); //offset from the hour tick marks
                final float x = (float) Math.sin(tickRotation) * (innerTickRadius - textOffset);
                final float y = (float) -Math.cos(tickRotation) * (innerTickRadius - textOffset);

                final int multiplyOffset = 12 / mNumberHourLabels;
                final int hourLabelNumber = (hourIndex == 0) ? 12 : hourIndex * multiplyOffset;
                final String hourLabelString = getHourLabel(hourLabelNumber);

                if (hourIndex / mNumberHourLabels == 0) {
                    canvas.drawText(hourLabelString,
                            mCenterX + x,
                            mCenterY + y
                                    - (mHourLabelTextPaint.descent()
                                    + mHourLabelTextPaint.ascent()) / 2,
                            mHourLabelTextPaint);
                }
            }

            //draws circle for the ticks
            canvas.drawArc(circleOffset, circleOffset, width - circleOffset,
                    height - circleOffset, 0, 360, false, mCircleAndTickPaint);
        }

        /**
         * Handles drawing the complications
         *
         * @param canvas            to draw to
         * @param currentTimeMillis current time
         */
        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                final ComplicationDrawable complicationDrawable
                        = mComplicationDrawableSparseArray.get(COMPLICATION_ID);

                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        /**
         * Handles drawing the hour hand
         *
         * @param canvas to draw to
         */
        private void drawHourHand(Canvas canvas) {
            final float seconds = mCalendar.get(Calendar.SECOND)
                    + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            final float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            final float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            final float hourRotation = hours / 12f * TWO_PI;

            final float hourOverflow = 10f;
            final float hourHandLength = mCenterX - 95;

            float hourX = (float) Math.sin(hourRotation);
            float hourY = (float) -Math.cos(hourRotation);

            canvas.drawLine(mCenterX - hourX * hourOverflow, mCenterY - hourY * hourOverflow,
                    mCenterX + hourX * hourHandLength, mCenterY + hourY * hourHandLength,
                    mHourPaint);
        }

        /**
         * Handles drawing the minute hand
         *
         * @param canvas to draw to
         */
        private void drawMinuteHand(Canvas canvas) {
            final float seconds = mCalendar.get(Calendar.SECOND)
                    + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            final float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            final float minuteRotation = minutes / 60f * TWO_PI;

            final float minuteOverflow = 10f;
            final float minuteHandLength = mCenterX - 65;

            final float minuteX = (float) Math.sin(minuteRotation);
            final float minuteY = (float) -Math.cos(minuteRotation);

            canvas.drawLine(mCenterX - minuteX * minuteOverflow, mCenterY - minuteY * minuteOverflow,
                    mCenterX + minuteX * minuteHandLength, mCenterY + minuteY * minuteHandLength,
                    mMinutePaint);
        }

        /**
         * Handles drawing the second hand
         *
         * @param canvas to draw to
         */
        private void drawSecondHand(Canvas canvas) {
            float seconds = mCalendar.get(Calendar.SECOND)
                    + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float secondRotation = seconds / 60f * TWO_PI;

            final float secondOverflow = 16f;
            final float secondHandLength = mCenterX - 60;

            final float secondX = (float) Math.sin(secondRotation);
            final float secondY = (float) -Math.cos(secondRotation);

            canvas.drawLine(mCenterX - secondX * secondOverflow, mCenterY - secondY * secondOverflow,
                    mCenterX + secondX * secondHandLength, mCenterY + secondY * secondHandLength,
                    mSecondPaint);
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
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());

                loadSavedPrefs();

                for(int COMPLICATION_ID : COMPLICATION_IDS) {
                    final ComplicationDrawable complicationDrawable
                            = mComplicationDrawableSparseArray.get(COMPLICATION_ID);
                    updateComplicationStyles(complicationDrawable);
                }

                updateWatchStyles();
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

            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                final ComplicationDrawable complicationDrawable
                        = mComplicationDrawableSparseArray.get(COMPLICATION_ID);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            updateWatchStyles();

            //Check and trigger whether or not timer should be running (only in active mode)
            updateTimer();
        }

        /**
         * Captures tap event (and tap type).
         * The {@link android.support.wearable.watchface.WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         *
         * @param tapType   type of tapping the user is performing
         * @param x         coordinate of the tap
         * @param y         coordinate of the tap
         * @param eventTime time the tap took place
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TAP:
                    int tappedComplicationId = getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        onComplicationTapped(tappedComplicationId);
                    }
                    break;
            }
        }

        /**
         * Handles what to do once a complication is tapped
         *
         * @param id of the complication tapped
         */
        private void onComplicationTapped(int id) {
            final ComplicationData complicationData = mActiveComplicationDataSparseArray.get(id);

            if (complicationData != null) {
                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.d(TAG, "Something went wrong with tapping a complication");
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {
                    ComponentName componentName = new ComponentName(
                            getApplicationContext(),
                            WatchFaceService.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }
            }
        }

        /**
         * Determines if a tap was inside a complication area
         *
         * @param x coordinate of tap
         * @param y coordinate of tap
         * @return the id of the complication that was tapped; if no complication was tapped,
         * return -1
         */
        private int getTappedComplicationId(int x, int y) {
            long currentTimeMillis = System.currentTimeMillis();

            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                final ComplicationData complicationData
                        = mActiveComplicationDataSparseArray.get(COMPLICATION_ID);

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    final ComplicationDrawable complicationDrawable
                            = mComplicationDrawableSparseArray.get(COMPLICATION_ID);
                    final Rect complicationBoundingRect = complicationDrawable.getBounds();

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return COMPLICATION_ID;
                        }
                    } else {
                        Log.e(TAG, "Not a recognized complication id.");
                    }
                }
            }
            return -1;
        }

        /**
         * Called when there is updated data for the complication
         *
         * @param complicationId   id of the complication to update data for
         * @param complicationData data to update the complication with
         */
        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            //Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            //Updates correct ComplicationDrawable with updated data.
            final ComplicationDrawable complicationDrawable
                    = mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mCenterX = width / 2;
            mCenterY = height / 2;

            mHourLabelTextPaint.setTextSize(width / 15);

//            drawComplication(canvas, now, TOP_COMPLICATION_ID, mCenterX, mCenterY / 2 - offset);
//            drawComplication(canvas, now, LEFT_COMPLICATION_ID, mCenterX / 2 - offset, mCenterY);
//            drawComplication(canvas, now, BOTTOM_COMPLICATION_ID, mCenterX, mCenterY * 1.5f + offset);
//            drawComplication(canvas, now, RIGHT_COMPLICATION_ID, mCenterX * 1.5f + offset, mCenterY);

            final float offset = mHasFlatTire ? -18 : -10; //offset for complications

            final Rect topBounds = createComplicationRect(mCenterX, mCenterY / 2 - offset);
            final Rect leftBounds = createComplicationRect(mCenterX / 2 - offset, mCenterY);
            final Rect rightBounds = createComplicationRect(mCenterX * 1.5f + offset, mCenterY);
            final Rect bottomBounds = createComplicationRect(mCenterX, mCenterY * 1.5f + offset);

            final ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
            topComplicationDrawable.setBounds(topBounds);

            final ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            final ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);

            final ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);
//            mNotificationTextPaint.setTextSize(width / 25);

//            int gradientColor = Color.argb(128, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor));
//            Shader shader = new LinearGradient(0, height - height / 4, 0, height, Color.TRANSPARENT, gradientColor, Shader.TileMode.CLAMP);
//            mNotificationBackgroundPaint.setShader(shader);
        }

        /**
         * Creates the rectangles objects for the complication to be placed in
         *
         * @param centerX x coordinate of the center
         * @param centerY y coordinate of the center
         * @return the complication rectangle
         */
        private Rect createComplicationRect(float centerX, float centerY) {
            final int radius = Math.round(mCenterX / COMPLICATION_RADIUS);

            final int centerXInt = Math.round(centerX);
            final int centerYInt = Math.round(centerY);

            return new Rect(centerXInt - radius,
                    centerYInt - radius,
                    centerXInt + radius,
                    centerYInt + radius);
        }

        /**
         * Update the watch paint styles when changing between Ambient and Non-Ambient modes
         */
        private void updateWatchStyles() {
            if (mAmbient) {
                mBackgroundPaint.setColor(Color.BLACK);
                mOuterBackgroundPaint.setColor(Color.BLACK);

                mHourPaint.setColor(Color.WHITE);
                mHourPaint.setStrokeWidth(AMBIENT_STROKE);

                mMinutePaint.setColor(Color.WHITE);
                mMinutePaint.setStrokeWidth(AMBIENT_STROKE);

                if (mShowSecondHand) {
                    mSecondPaint.setColor(Color.WHITE);

                    if (mLowBitAmbient) {
                        mSecondPaint.setAntiAlias(false);
                    }
                }

                mCircleAndTickPaint.setColor(Color.WHITE);
                mCircleAndTickPaint.setStrokeWidth(AMBIENT_STROKE);

                mHourLabelTextPaint.setColor(Color.WHITE);
                mHourLabelTextPaint.setTypeface(mAmbientFont);

                mMinuteTickPaint.setColor(Color.WHITE);

                if (mLowBitAmbient) {
                    mHourPaint.setAntiAlias(false);
                    mMinutePaint.setAntiAlias(false);
                    mCircleAndTickPaint.setAntiAlias(false);
                    mMinuteTickPaint.setAntiAlias(false);

                    mHourLabelTextPaint.setAntiAlias(false);
                }
            } else {
                mBackgroundPaint.setColor(mCenterCircleColor);
                mOuterBackgroundPaint.setColor(mOuterCircleColor);

                mHourPaint.setColor(mHourHandColor);
                mHourPaint.setStrokeWidth(THICK_STROKE);

                mMinutePaint.setColor(mMinuteHandColor);
                mMinutePaint.setStrokeWidth(THICK_STROKE);

                if (mShowSecondHand) {
                    mSecondPaint.setColor(mSecondHandColor);

                    if (mLowBitAmbient) {
                        mSecondPaint.setAntiAlias(true);
                    }
                }

                mCircleAndTickPaint.setColor(mCircleAndTickColor);
                mCircleAndTickPaint.setStrokeWidth(THICK_STROKE);

                mHourLabelTextPaint.setColor(mHourLabelsColor);
                mHourLabelTextPaint.setTypeface(mHourLabelFont);

                mMinuteTickPaint.setColor(mCircleAndTickColor);

                if (mLowBitAmbient) {
                    mHourPaint.setAntiAlias(true);
                    mMinutePaint.setAntiAlias(true);

                    mCircleAndTickPaint.setAntiAlias(true);
                    mMinuteTickPaint.setAntiAlias(true);

                    mHourLabelTextPaint.setAntiAlias(true);
                }
            }
        }

        private void updateComplicationStyles(ComplicationDrawable complicationDrawable) {
            final Typeface complicationActiveFont = Typeface.create("sans-serif", Typeface.BOLD);

            //Sets the styles for the complications
            if (complicationDrawable != null) {
                complicationDrawable.setContext(getApplicationContext());
                complicationDrawable.setTextTypefaceActive(complicationActiveFont);
                complicationDrawable.setTitleTypefaceActive(complicationActiveFont);
                complicationDrawable.setTextColorActive(mComplicationColor);
                complicationDrawable.setTitleColorActive(mComplicationTitleColor);
                complicationDrawable.setHighlightColorActive(mComplicationColor);
                complicationDrawable.setHighlightColorAmbient(Color.WHITE);

                //Grayscale images when in Ambient Mode
                final ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                complicationDrawable.setImageColorFilterAmbient(new ColorMatrixColorFilter(matrix));

                if (mShowComplicationBorder) {
                    complicationDrawable.setBorderStyleActive(ComplicationDrawable.BORDER_STYLE_SOLID);
                    complicationDrawable.setBorderColorActive(mComplicationBorderColor);
                    complicationDrawable.setBorderWidthAmbient((int) AMBIENT_STROKE);
                } else {
                    complicationDrawable.setBorderStyleActive(ComplicationDrawable.BORDER_STYLE_NONE);
                }
            }
        }


        /**
         * Loads the user selected colors for each component
         */
        private void loadSavedPrefs() {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            //Default colors
            final int defaultHands = getColor(R.color.default_hands); //hours, minutes, ticks, and circle
            final int defaultSeconds = getColor(R.color.default_seconds); //seconds
            final int defaultCenter = getColor(R.color.default_center_circle); //center circle
            final int defaultOuter = getColor(R.color.default_outer_circle); //outer circle

            //Hand colors
            mHourHandColor = mPrefs.getInt("settings_hour_hand_color_value", defaultHands);
            mMinuteHandColor = mPrefs.getInt("settings_minute_hand_color_value", defaultHands);
            mSecondHandColor = mPrefs.getInt("settings_second_hand_color_value", defaultSeconds);

            //Background colors
            mCenterCircleColor = mPrefs.getInt("settings_center_circle_color_value", defaultCenter);
            mCircleAndTickColor = mPrefs.getInt("settings_circle_ticks_color_value", defaultHands);
            mOuterCircleColor = mPrefs.getInt("settings_outer_circle_color_value", defaultOuter);
            mHourLabelsColor = mPrefs.getInt("settings_hour_labels_color_value", defaultHands);

            //Complication colors
            mComplicationColor = mPrefs.getInt("settings_complication_color_value", defaultHands);
            mComplicationTitleColor = Color.argb(Math.round(152), Color.red(mComplicationColor),
                    Color.green(mComplicationColor), Color.blue(mComplicationColor));
            mComplicationBorderColor = Color.argb(Math.round(48), Color.red(mComplicationColor),
                    Color.green(mComplicationColor), Color.blue(mComplicationColor));

            //Misc settings
            mShowComplicationBorder = mPrefs.getBoolean("settings_complication_border", true);
            mShowSecondHand = mPrefs.getBoolean("settings_show_second_hand", true);

            final String numberHourTicks = mPrefs.getString("settings_number_ticks",
                    getString(R.string.settings_number_ticks_default));
            if (numberHourTicks.equals(getString(R.string.settings_number_ticks_default))) {
                /*
                  This is a workaround for the pref not showing the correct default value upon a
                  fresh install
                 */
                mNumberHourTicks = 4;
            } else {
                mNumberHourTicks = Integer.parseInt(numberHourTicks);
            }

            mShowMinuteTicks = mPrefs.getBoolean("settings_show_minute_ticks", false);
            mClassicMode = mPrefs.getBoolean("settings_classic_mode", false);

            final String numberHourLabels = mPrefs.getString("settings_number_hour_labels",
                    getString(R.string.settings_none));
            if (numberHourLabels.equals(getString(R.string.settings_none))) {
                /*
                  This is a workaround for the pref not showing the correct default value upon a
                  fresh install
                 */
                mNumberHourLabels = 0;
            } else {
                mNumberHourLabels = Integer.parseInt(numberHourLabels);
            }

            mPrefs = null;
        }

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

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                final ComplicationDrawable complicationDrawable
                        = mComplicationDrawableSparseArray.get(COMPLICATION_ID);

                if (complicationDrawable != null) {
                    complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                    complicationDrawable.setBurnInProtection(mBurnInProtection);
                }
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mHasFlatTire = insets.getSystemWindowInsetBottom() > 0;
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
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

        /**
         * Converts density pixels to pixels
         * @param dp desired density pixels
         * @return converted dp to pixels
         */
        private float dpToPx(final int dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    getResources().getDisplayMetrics());
        }

        /**
         * Gets the hour String based on the input hour
         * This is done so that it's possible to translate the hour numbers!
         *
         * @param hour to find the String for
         * @return String of the hour number
         */
        private String getHourLabel(int hour) {
            switch (hour) {
                case 1:
                    return getString(R.string.hour_label_1);
                case 2:
                    return getString(R.string.hour_label_2);
                case 3:
                    return getString(R.string.hour_label_3);
                case 4:
                    return getString(R.string.hour_label_4);
                case 5:
                    return getString(R.string.hour_label_5);
                case 6:
                    return getString(R.string.hour_label_6);
                case 7:
                    return getString(R.string.hour_label_7);
                case 8:
                    return getString(R.string.hour_label_8);
                case 9:
                    return getString(R.string.hour_label_9);
                case 10:
                    return getString(R.string.hour_label_10);
                case 11:
                    return getString(R.string.hour_label_11);
                case 12:
                    return getString(R.string.hour_label_12);
                default:
                    return "";
            }
        }
    }
}