package io.nxt3.ioclassic;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class IOClassicWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "IOClassic";
    //Update rate in milliseconds for interactive mode. We update once a second to advance the second hand.
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    //Left/Right dial support types
    public static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_LONG_TEXT, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON}
    };
    private static final int TOP_DIAL_COMPLICATION = 0;
    private static final int LEFT_DIAL_COMPLICATION = 1;
    private static final int RIGHT_DIAL_COMPLICATION = 2;
    private static final int BOTTOM_DIAL_COMPLICATION = 3;
    public static final int[] COMPLICATION_IDS = {
            TOP_DIAL_COMPLICATION,
            LEFT_DIAL_COMPLICATION,
            RIGHT_DIAL_COMPLICATION,
            BOTTOM_DIAL_COMPLICATION
    };

    @Override
    public Engine onCreateEngine() {
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

        //SharedPrefs for getting settings
        private SharedPreferences mPrefs;

        //Booleans for various ambient mode styles
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

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

        //Paint objects for each component
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mBackgroundPaint;
        private Paint mCircleAndTickPaint;
        private Paint mOuterBackgroundPaint;

        //Colors for each complication component
        private int mTertiaryColor;
        private int mQuaternaryColor;

        //Paint objects for complication components
        private Paint mComplicationArcValuePaint;
        private Paint mComplicationArcPaint;
        private Paint mComplicationCirclePaint;
        private TextPaint mComplicationPrimaryLongTextPaint;
        private Paint mComplicationPrimaryTextPaint;
        private Paint mComplicationTextPaint;
        private TextPaint mComplicationLongTextPaint;

        //Complication stuff
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
        private RectF[] mComplicationTapBoxes = new RectF[COMPLICATION_IDS.length];
        private final float COMPLICATION_RADIUS = 4.6f;

        //Fonts for complications
        private Typeface mFontLight;
        private Typeface mFontBold;
        private Typeface mFont;

        //Other settings
        private boolean mComplicationBorder;


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
                    .setAcceptsTapEvents(true)
                    .build());

            loadSavedPrefs();
            initializeBackground();
            initializeComplications();
            initializeWatchFace();
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

            mOuterBackgroundPaint = new Paint();
            mOuterBackgroundPaint.setColor(mOuterCircleColor);
            mOuterBackgroundPaint.setAntiAlias(true);
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

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mSecondHandColor);
            mSecondPaint.setStyle(Paint.Style.STROKE);
            mSecondPaint.setStrokeWidth(THIN_STROKE);
            mSecondPaint.setAntiAlias(true);
        }

        /**
         * Init watch face complications components
         */
        private void initializeComplications() {
            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            setActiveComplications(COMPLICATION_IDS);

            /* Set defaults for fonts */
            mFontLight = Typeface.create("sans-serif-light", Typeface.NORMAL);
            mFontBold = Typeface.create("sans-serif", Typeface.BOLD);
            mFont = Typeface.create("sans-serif", Typeface.NORMAL);

            mComplicationArcValuePaint = new Paint();
            mComplicationArcValuePaint.setColor(mCircleAndTickColor);
            mComplicationArcValuePaint.setStrokeWidth(4f);
            mComplicationArcValuePaint.setAntiAlias(true);
            mComplicationArcValuePaint.setStrokeCap(Paint.Cap.SQUARE);
            mComplicationArcValuePaint.setStyle(Paint.Style.STROKE);

            mComplicationArcPaint = new Paint();
            mComplicationArcPaint.setColor(mTertiaryColor);
            mComplicationArcPaint.setStrokeWidth(4f);
            mComplicationArcPaint.setAntiAlias(true);
            mComplicationArcPaint.setStrokeCap(Paint.Cap.SQUARE);
            mComplicationArcPaint.setStyle(Paint.Style.STROKE);

            mComplicationCirclePaint = new Paint();
            mComplicationCirclePaint.setColor(mQuaternaryColor);
            mComplicationCirclePaint.setStrokeWidth(3f);
            mComplicationCirclePaint.setAntiAlias(true);
            mComplicationCirclePaint.setStrokeCap(Paint.Cap.SQUARE);
            mComplicationCirclePaint.setStyle(Paint.Style.STROKE);

            mComplicationPrimaryLongTextPaint = new TextPaint();
            mComplicationPrimaryLongTextPaint.setColor(mCircleAndTickColor);
            mComplicationPrimaryLongTextPaint.setAntiAlias(true);
            mComplicationPrimaryLongTextPaint.setTypeface(mFontBold);

            mComplicationPrimaryTextPaint = new Paint();
            mComplicationPrimaryTextPaint.setColor(mCircleAndTickColor);
            mComplicationPrimaryTextPaint.setAntiAlias(true);
            mComplicationPrimaryTextPaint.setTypeface(mFontBold);

            mComplicationTextPaint = new Paint();
            mComplicationTextPaint.setColor(mTertiaryColor);
            mComplicationTextPaint.setAntiAlias(true);
            mComplicationTextPaint.setTypeface(mFontBold);

            mComplicationLongTextPaint = new TextPaint();
            mComplicationLongTextPaint.setColor(mTertiaryColor);
            mComplicationLongTextPaint.setAntiAlias(true);
            mComplicationLongTextPaint.setTypeface(mFontBold);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);

            final float offset = -10; //offset for complications

            drawWatchFace(canvas, bounds);
            drawComplication(canvas, now, TOP_DIAL_COMPLICATION, mCenterX, mCenterY / 2 - offset);
            drawComplication(canvas, now, LEFT_DIAL_COMPLICATION, mCenterX / 2 - offset, mCenterY);
            drawComplication(canvas, now, BOTTOM_DIAL_COMPLICATION, mCenterX, mCenterY * 1.5f + offset);
            drawComplication(canvas, now, RIGHT_DIAL_COMPLICATION, mCenterX * 1.5f + offset, mCenterY);
        }

        /**
         * Handles drawing the background
         *
         * @param canvas to draw to
         */
        private void drawBackground(Canvas canvas) {
            if (mAmbient && (!mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawColor(mCenterCircleColor);
            }
        }

        /**
         * Handles drawing the complications
         *
         * @param canvas            to draw to
         * @param currentTimeMillis current time
         * @param id                of the complication
         * @param centerX           x coordinate of the center
         * @param centerY           y coordinate of the center
         */
        private void drawComplication(Canvas canvas, long currentTimeMillis, int id, float centerX, float centerY) {
            ComplicationData complicationData = mActiveComplicationDataSparseArray.get(id);

            if ((complicationData != null) && (complicationData.isActive(currentTimeMillis))) {
                switch (complicationData.getType()) {
                    case ComplicationData.TYPE_RANGED_VALUE:
                        drawRangeComplication(canvas,
                                complicationData,
                                currentTimeMillis,
                                id);
                        break;
                    case ComplicationData.TYPE_SMALL_IMAGE:
                        drawSmallImageComplication(canvas,
                                complicationData,
                                centerX,
                                centerY,
                                id);
                        break;
                    case ComplicationData.TYPE_LONG_TEXT:
                        drawLongTextComplication(canvas,
                                complicationData,
                                currentTimeMillis,
                                centerX,
                                centerY,
                                id);
                        break;
                    case ComplicationData.TYPE_SHORT_TEXT:
                        drawShortTextComplication(canvas,
                                complicationData,
                                currentTimeMillis,
                                centerX,
                                centerY,
                                id);
                        break;
                    case ComplicationData.TYPE_ICON:
                        drawIconComplication(canvas,
                                complicationData,
                                centerX,
                                centerY,
                                id);
                        break;
                }
            }
        }

        /**
         * Handles drawing the watch face components
         * Draws the hour, minute, and second hands
         * Draws the circle w/ ticks
         *
         * @param canvas to draw to
         * @param bounds of the device screen
         */
        private void drawWatchFace(Canvas canvas, Rect bounds) {
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
                canvas.drawColor(mOuterCircleColor);
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

        /**
         * Handles shortening the complication numbers to readable strings
         * If a number is large enough, we'll round and make it shorter (i.e. 1323 becomes 1.3k)
         *
         * @param val to shorten and round
         * @return the number as an easily digestible String
         */
        private String complicationNumberString(float val) {
            if (val > 100000) {
                return String.valueOf(Math.round(val / 100000) + "m");
            } else if (val > 1000) {
                return String.valueOf(Math.round(val / 1000) + "k");
            } else {
                return String.valueOf(Math.round(val));
            }
        }

        /**
         * Handles drawing range complications
         *
         * @param canvas            to draw to
         * @param data              of the complication
         * @param currentTimeMillis current time
         * @param id                of the the complication
         */
        private void drawRangeComplication(Canvas canvas, ComplicationData data,
                                           long currentTimeMillis, int id) {
            float min = data.getMinValue();
            float max = data.getMaxValue();
            float val = data.getValue();

            ComplicationData bottomComplicationData = mActiveComplicationDataSparseArray.get(BOTTOM_DIAL_COMPLICATION);

            float centerX;
            float centerY;
            float radius;
            float startAngle = -90;

            /*
            If bottom complication data exists then only the right space is available
            instead of the bottom right space.
            */
            float offset = 0;

            if (bottomComplicationData != null &&
                    bottomComplicationData.getType() != ComplicationData.TYPE_EMPTY &&
                    bottomComplicationData.getType() != ComplicationData.TYPE_NO_DATA &&
                    bottomComplicationData.isActive(currentTimeMillis)) {
                centerX = mCenterX * 1.5f + offset;
                centerY = mCenterY;
                radius = mCenterX / 4;
            } else {
                centerX = mCenterX + mCenterX / 4 + 10 + offset * 1.3f;
                centerY = mCenterY + mCenterY / 4 + 10 + offset * 0.3f;
                radius = mCenterX / 2;
                radius -= 20;
            }

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            Bitmap arcBitmap = Bitmap.createBitmap((int) radius * 2 + 4, (int) radius * 2 + 4, Bitmap.Config.ARGB_8888);
            Canvas arcCanvas = new Canvas(arcBitmap);
            Path path = new Path();
            path.addArc(2, 2, radius * 2 + 2, radius * 2 + 2,
                    -90 + (val - min) / (max - min) * 270,
                    270 - (val - min) / (max - min) * 270);

            int complicationSteps = 10;
            for (int tickIndex = 1; tickIndex < complicationSteps; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 3 / 2 / complicationSteps - startAngle / 180 * Math.PI - Math.PI / 2);
                float innerX = (float) Math.sin(tickRot) * (radius - 4 - (0.05f * mCenterX));
                float innerY = (float) -Math.cos(tickRot) * (radius - 4 - (0.05f * mCenterX));
                float outerX = (float) Math.sin(tickRot) * (radius - 4);
                float outerY = (float) -Math.cos(tickRot) * (radius - 4);
                path.moveTo(radius + innerX + 2, radius + innerY + 2);
                path.lineTo(radius + outerX + 2, radius + outerY + 2);
            }
            arcCanvas.drawPath(path, mComplicationArcPaint);

            float valRot = (float) ((val - min) * Math.PI * 3 / 2 / (max - min) - startAngle / 180 * Math.PI - Math.PI / 2);
            Path valuePath = new Path();
            valuePath.addArc(2, 2, radius * 2 + 2, radius * 2 + 2,
                    -90, (val - min) / (max - min) * 270 + 0.0001f);

            valuePath.lineTo((float) Math.sin(valRot) * (radius - (0.15f * mCenterX)) + radius + 2, (float) -Math.cos(valRot) * (radius - (0.15f * mCenterX)) + radius + 2);

            mComplicationArcValuePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            arcCanvas.drawPath(valuePath, mComplicationArcValuePaint);

            mComplicationArcValuePaint.setXfermode(null);
            arcCanvas.drawPath(valuePath, mComplicationArcValuePaint);

            canvas.drawBitmap(arcBitmap, centerX - radius - 2, centerY - radius - 2, null);

            mComplicationTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(complicationNumberString(min),
                    centerX + -6,
                    centerY - radius - mComplicationTextPaint.descent() - mComplicationTextPaint.ascent(),
                    mComplicationTextPaint);

            mComplicationTextPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(complicationNumberString(max),
                    centerX - radius - 4,
                    centerY - 6,
                    mComplicationTextPaint);

            Icon icon = mAmbient && mBurnInProtection ? data.getBurnInProtectionIcon() : data.getIcon();
            if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());

                if (drawable != null) {
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setTint(mComplicationArcValuePaint.getColor());
                    drawable.setBounds(Math.round(centerX - size / 2), Math.round(centerY - size / 2), Math.round(centerX + size / 2), Math.round(centerY + size / 2));
                    drawable.draw(canvas);
                }
            } else {
                mComplicationPrimaryTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(complicationNumberString(val),
                        centerX,
                        centerY - (mComplicationPrimaryTextPaint.descent() + mComplicationPrimaryTextPaint.ascent()) / 2,
                        mComplicationPrimaryTextPaint);
            }
        }

        /**
         * Handles drawing long text complications
         *
         * @param canvas            to draw to
         * @param data              of the complication
         * @param currentTimeMillis current time
         * @param centerX           x coordinate of the center
         * @param centerY           y coordinate of the center
         * @param id                of the complication
         */
        private void drawLongTextComplication(Canvas canvas, ComplicationData data,
                                              long currentTimeMillis, float centerX, float centerY,
                                              int id) {
            ComplicationText text = data.getLongText();
            String textText = text != null ? text.getText(getApplicationContext(), currentTimeMillis).toString() : null;

            ComplicationText title = data.getLongTitle();
            String titleText = title != null ? title.getText(getApplicationContext(), currentTimeMillis).toString() : null;

            Icon icon = mBurnInProtection && mAmbient && data.getBurnInProtectionIcon() != null ? data.getBurnInProtectionIcon() : data.getIcon();
            Icon image = data.getSmallImage();

            float height = mCenterY / 4;
            float width = mCenterX * 1.2f;
            float maxWidth = width;

            Rect bounds = new Rect();
            Rect bounds2 = new Rect();

            float textWidth = 0;
            float titleWidth = 0;

            if (textText != null) {
                mComplicationPrimaryLongTextPaint.getTextBounds(textText, 0, textText.length(), bounds);
                textWidth = bounds.width() + height / 2;
            }

            if (titleText != null) {
                mComplicationLongTextPaint.getTextBounds(titleText, 0, titleText.length(), bounds2);
                titleWidth = bounds2.width() + height / 2;
            }

            if (textWidth > titleWidth && textWidth > 0) {
                width = textWidth;
            }

            if (textWidth < titleWidth && titleWidth > 0) {
                width = titleWidth;
            }

            if (image != null && !(mAmbient && mBurnInProtection)) {
                width += height + 8;
            } else if (icon != null) {
                width += height;
            }
            boolean ellipsize = false;

            if (width > maxWidth) {
                width = maxWidth;
                ellipsize = true;
            }

            RectF tapBox = new RectF(centerX - width / 2,
                    centerY - height / 2,
                    centerX + width / 2,
                    centerY + height / 2);

            mComplicationTapBoxes[id] = tapBox;

            if (mComplicationBorder) {
                Path path = new Path();
                path.moveTo(tapBox.left + height / 2, tapBox.top);

                path.lineTo(tapBox.right - height / 2, tapBox.top);
                path.arcTo(tapBox.right - height, tapBox.top, tapBox.right, tapBox.bottom, -90, 180, false);

                path.lineTo(tapBox.left + height / 2, tapBox.bottom);
                path.arcTo(tapBox.left, tapBox.top, tapBox.left + height, tapBox.bottom, 90, 180, false);

                canvas.drawPath(path, mComplicationCirclePaint);
            }

            float textY = centerY - (mComplicationPrimaryLongTextPaint.descent() + mComplicationPrimaryLongTextPaint.ascent() / 2);
            float textX = tapBox.left + height / 4;
            float textW = width - height / 4;

            if (image != null && !(mAmbient && mBurnInProtection)) {
                Drawable drawable = image.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    if (mAmbient) {
                        drawable = convertToGrayscale(drawable);
                    }
                    drawable = convertToCircle(drawable);
                    drawable.setBounds(Math.round(tapBox.left + 2),
                            Math.round(tapBox.top + 2),
                            Math.round(tapBox.left + height - 2),
                            Math.round(tapBox.bottom - 2));
                    drawable.draw(canvas);

                    textX = tapBox.left + height + 8;
                    textW = width - (textX - tapBox.left) - height / 4;
                }
            } else if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    drawable.setTint(mComplicationPrimaryLongTextPaint.getColor());
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setBounds(Math.round(tapBox.left + height / 2 - size / 2),
                            Math.round(tapBox.top + height / 2 - size / 2),
                            Math.round(tapBox.left + height / 2 + size / 2),
                            Math.round(tapBox.top + height / 2 + size / 2));
                    drawable.draw(canvas);

                    textX = tapBox.left + height;
                    textW = width - (textX - tapBox.left) - height / 4;
                }
            }

            if (title != null) {
                canvas.drawText(
                        ellipsize ? TextUtils.ellipsize(
                                titleText,
                                mComplicationLongTextPaint,
                                textW,
                                TextUtils.TruncateAt.END
                        ).toString() : titleText,
                        textX,
                        centerY - mComplicationLongTextPaint.descent() - mComplicationLongTextPaint.ascent() + 4,
                        mComplicationLongTextPaint);
                textY = centerY - 4;
            }

            canvas.drawText(
                    ellipsize ? TextUtils.ellipsize(
                            textText,
                            mComplicationPrimaryLongTextPaint,
                            textW,
                            TextUtils.TruncateAt.END
                    ).toString() : textText,
                    textX,
                    textY,
                    mComplicationPrimaryLongTextPaint);
        }

        /**
         * Handles drawing short text complications
         *
         * @param canvas            to draw to
         * @param data              of the complication
         * @param currentTimeMillis current time
         * @param centerX           x coordinate of the center
         * @param centerY           y coordinate of the center
         * @param id                of the complication
         */
        private void drawShortTextComplication(Canvas canvas, ComplicationData data,
                                               long currentTimeMillis, float centerX,
                                               float centerY, int id) {
            ComplicationText title = data.getShortTitle();
            ComplicationText text = data.getShortText();
            Icon icon = mBurnInProtection && mAmbient && data.getBurnInProtectionIcon() != null ? data.getBurnInProtectionIcon() : data.getIcon();

            float radius = mCenterX / COMPLICATION_RADIUS;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            if (mComplicationBorder) {
                canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
            }

            mComplicationPrimaryTextPaint.setTextAlign(Paint.Align.CENTER);
            mComplicationTextPaint.setTextAlign(Paint.Align.CENTER);

            float textY = centerY - (mComplicationPrimaryTextPaint.descent() + mComplicationPrimaryTextPaint.ascent() / 2);

            if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    drawable.setTint(mComplicationPrimaryTextPaint.getColor());
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setBounds(Math.round(centerX - size / 2), Math.round(centerY - size - 2), Math.round(centerX + size / 2), Math.round(centerY - 2));
                    drawable.draw(canvas);

                    textY = centerY - mComplicationPrimaryTextPaint.descent() - mComplicationPrimaryTextPaint.ascent() + 4;
                }
            } else if (title != null) {
                canvas.drawText(title.getText(getApplicationContext(), currentTimeMillis).toString().toUpperCase(),
                        centerX,
                        centerY - mComplicationTextPaint.descent() - mComplicationTextPaint.ascent() + 4,
                        mComplicationTextPaint);
                textY = centerY - 4;
            }

            canvas.drawText(text.getText(getApplicationContext(), currentTimeMillis).toString(),
                    centerX,
                    textY,
                    mComplicationPrimaryTextPaint);
        }

        /**
         * Handles drawing icon complications
         *
         * @param canvas  to draw to
         * @param data    of the complication
         * @param centerX x coordinate of the center
         * @param centerY y coordinate of the center
         * @param id      of the complication
         */
        private void drawIconComplication(Canvas canvas, ComplicationData data,
                                          float centerX, float centerY, int id) {
            float radius = mCenterX / COMPLICATION_RADIUS;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            Icon icon = mAmbient && mBurnInProtection ? data.getBurnInProtectionIcon() : data.getSmallImage();
            if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setTint(mComplicationPrimaryTextPaint.getColor());
                    drawable.setBounds(Math.round(centerX - size), Math.round(centerY - size), Math.round(centerX + size), Math.round(centerY + size));
                    drawable.draw(canvas);
                    if (mComplicationBorder) {
                        canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
                    }
                }
            }
        }

        /**
         * Handles drawing small image complications
         *
         * @param canvas  to draw to
         * @param data    of the complication
         * @param centerX x coordinate of the center
         * @param centerY y coordinate of the center
         * @param id      of the complication
         */
        private void drawSmallImageComplication(Canvas canvas,ComplicationData data,
                                                float centerX, float centerY, int id) {
            float radius = mCenterX / COMPLICATION_RADIUS;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            Icon smallImage = data.getSmallImage();
            if (smallImage != null && !(mAmbient && mBurnInProtection)) {
                Drawable drawable = smallImage.loadDrawable(getApplicationContext());

                if (drawable != null) {
                    if (mAmbient) {
                        drawable = convertToGrayscale(drawable);
                    }
                    int size = Math.round(radius - mComplicationCirclePaint.getStrokeWidth() / 2);
                    if (data.getImageStyle() == ComplicationData.IMAGE_STYLE_ICON) {
                        size = (int) Math.round(0.15 * mCenterX);
                    } else {
                        drawable = convertToCircle(drawable);
                    }

                    drawable.setBounds(Math.round(centerX - size), Math.round(centerY - size), Math.round(centerX + size), Math.round(centerY + size));
                    drawable.draw(canvas);

                    if (mComplicationBorder) {
                        canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
                    }
                }
            }
        }

        /**
         * Converts a drawable into a grayscale image
         *
         * @param drawable to convert
         * @return the drawable as a grayscale image
         */
        private Drawable convertToGrayscale(Drawable drawable) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

            drawable.setColorFilter(filter);

            return drawable;
        }

        /**
         * Converts a drawable to a Bitmap
         *
         * @param drawable to convert
         * @return the drawable as a Bitmap image
         */
        private Bitmap drawableToBitmap(Drawable drawable) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }

            int width = drawable.getIntrinsicWidth();
            width = width > 0 ? width : 1;
            int height = drawable.getIntrinsicHeight();
            height = height > 0 ? height : 1;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }

        /**
         * Converts a drawable to a circle
         *
         * @param drawable to convert
         * @return the drawable encapsulated as a circle
         */
        private Drawable convertToCircle(Drawable drawable) {
            Bitmap bitmap = drawableToBitmap(drawable);
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            canvas.drawCircle(bitmap.getWidth() / 2,
                    bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return new BitmapDrawable(output);
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
                updateWatchPaintStyles();
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
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    for (int i = 0; i < mComplicationTapBoxes.length; i++) {
                        if (mComplicationTapBoxes[i] != null && mComplicationTapBoxes[i].contains(x, y)) {
                            onComplicationTapped(i);
                        }
                    }
                    break;
            }
            invalidate();
        }

        /**
         * Handles what to do once a complication is tapped
         *
         * @param id of the complication tapped
         */
        private void onComplicationTapped(int id) {
            ComplicationData complicationData = mActiveComplicationDataSparseArray.get(id);

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
         * Called when there is updated data for the complication
         *
         * @param complicationId   id of the complication to update data for
         * @param complicationData data to update the complication with
         */
        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);
            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */

            mCenterX = width / 2;
            mCenterY = height / 2;

            mComplicationPrimaryLongTextPaint.setTextSize(width / 23);
            mComplicationPrimaryTextPaint.setTextSize(width / 18);
            mComplicationLongTextPaint.setTextSize(width / 25);
            mComplicationTextPaint.setTextSize(width / 20);
//            mNotificationTextPaint.setTextSize(width / 25);

//            int gradientColor = Color.argb(128, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor));
//            Shader shader = new LinearGradient(0, height - height / 4, 0, height, Color.TRANSPARENT, gradientColor, Shader.TileMode.CLAMP);
//            mNotificationBackgroundPaint.setShader(shader);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        /**
         * Update the watch paint styles when changing between Ambient and Non-Ambient modes
         */
        private void updateWatchPaintStyles() {

            mComplicationArcValuePaint.setColor(mCircleAndTickColor);
            mComplicationArcPaint.setColor(mTertiaryColor);
            mComplicationCirclePaint.setColor(mQuaternaryColor);
            mComplicationPrimaryLongTextPaint.setColor(mCircleAndTickColor);
            mComplicationPrimaryTextPaint.setColor(mCircleAndTickColor);
            mComplicationTextPaint.setColor(mTertiaryColor);
            mComplicationLongTextPaint.setColor(mTertiaryColor);

            if (mAmbient) {
                mBackgroundPaint.setColor(Color.BLACK);
                mOuterBackgroundPaint.setColor(Color.BLACK);

                mHourPaint.setColor(Color.WHITE);
                mHourPaint.setStrokeWidth(THIN_STROKE);

                mMinutePaint.setColor(Color.WHITE);
                mMinutePaint.setStrokeWidth(THIN_STROKE);

                mSecondPaint.setColor(Color.WHITE);

                mCircleAndTickPaint.setColor(Color.WHITE);
                mCircleAndTickPaint.setStrokeWidth(THIN_STROKE);

                if (mLowBitAmbient) {
                    mComplicationArcValuePaint.setColor(Color.WHITE);
                    mComplicationPrimaryLongTextPaint.setColor(Color.WHITE);
                    mComplicationPrimaryTextPaint.setColor(Color.WHITE);
                }

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
            } else {
                mBackgroundPaint.setColor(mCenterCircleColor);
                mOuterBackgroundPaint.setColor(mOuterCircleColor);

                mHourPaint.setColor(mHourHandColor);
                mHourPaint.setStrokeWidth(THICK_STROKE);

                mMinutePaint.setColor(mMinuteHandColor);
                mMinutePaint.setStrokeWidth(THICK_STROKE);

                mSecondPaint.setColor(mSecondHandColor);

                mCircleAndTickPaint.setColor(mCircleAndTickColor);
                mCircleAndTickPaint.setStrokeWidth(THICK_STROKE);

                mComplicationArcValuePaint.setColor(mCircleAndTickColor);
                mComplicationPrimaryLongTextPaint.setColor(mCircleAndTickColor);
                mComplicationPrimaryTextPaint.setColor(mCircleAndTickColor);

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
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            final String DEFAULT_WHITE = "#98A4A3"; //hours, minutes, ticks, and circle
            final String DEFAULT_RED = "#AA5B34"; //seconds
            final String DEFAULT_CENTER = "#1E2327"; //center circle
            final String DEFAULT_OUTER = "#20272A"; //outer circle

            mHourHandColor = mPrefs.getInt("settings_hour_hand_color_value", Color.parseColor(DEFAULT_WHITE));
            mMinuteHandColor = mPrefs.getInt("settings_minute_hand_color_value", Color.parseColor(DEFAULT_WHITE));
            mSecondHandColor = mPrefs.getInt("settings_second_hand_color_value", Color.parseColor(DEFAULT_RED));

            mCenterCircleColor = mPrefs.getInt("settings_center_circle_color_value", Color.parseColor(DEFAULT_CENTER));
            mCircleAndTickColor = mPrefs.getInt("settings_circle_ticks_color_value", Color.parseColor(DEFAULT_WHITE));
            mOuterCircleColor = mPrefs.getInt("settings_outer_circle_color_value", Color.parseColor(DEFAULT_OUTER));

            //Accent colors for complications
            mTertiaryColor = Color.argb(Math.round(152), Color.red(mCircleAndTickColor), Color.green(mCircleAndTickColor), Color.blue(mCircleAndTickColor));
            mQuaternaryColor = Color.argb(Math.round(48), Color.red(mCircleAndTickColor), Color.green(mCircleAndTickColor), Color.blue(mCircleAndTickColor));

            mComplicationBorder = mPrefs.getBoolean("settings_complication_border", true);
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