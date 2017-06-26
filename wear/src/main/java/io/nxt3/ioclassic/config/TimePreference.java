package io.nxt3.ioclassic.config;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import io.nxt3.ioclassic.R;

public class TimePreference extends DialogPreference {
    private Calendar mCalendar;
    private TimePicker mPicker = null;

    public TimePreference(Context context) {
        this(context, null);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setPositiveButtonText(R.string.settings_set);
        setNegativeButtonText(R.string.settings_cancel);

        mCalendar = new GregorianCalendar();
    }

    @Override
    protected View onCreateDialogView() {
        mPicker = new TimePicker(getContext());
        return (mPicker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mPicker.setCurrentHour(mCalendar.get(Calendar.HOUR_OF_DAY));
        mPicker.setCurrentMinute(mCalendar.get(Calendar.MINUTE));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mCalendar.set(Calendar.HOUR_OF_DAY, mPicker.getCurrentHour());
            mCalendar.set(Calendar.MINUTE, mPicker.getCurrentMinute());

            setSummary(getSummary());

            if (callChangeListener(mCalendar.getTimeInMillis())) {
                persistLong(mCalendar.getTimeInMillis());
                notifyChanged();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int index) {
        return (typedArray.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            if (defaultValue == null) {
                mCalendar.setTimeInMillis(getPersistedLong(System.currentTimeMillis()));
            } else {
                mCalendar.setTimeInMillis(Long.parseLong(getPersistedString((String) defaultValue)));
            }
        } else {
            if (defaultValue == null) {
                mCalendar.setTimeInMillis(System.currentTimeMillis());
            } else {
                mCalendar.setTimeInMillis(Long.parseLong((String) defaultValue));
            }
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        return (mCalendar == null) ? null
                : DateFormat.getTimeFormat(getContext()).format(new Date(mCalendar.getTimeInMillis()));
    }
}