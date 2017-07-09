package io.nxt3.ioclassic.config;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.widget.Toast;

import com.google.android.wearable.intent.RemoteIntent;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import io.nxt3.ioclassic.BuildConfig;
import io.nxt3.ioclassic.IOClassicWatchFaceService;
import io.nxt3.ioclassic.R;

import static android.app.Activity.RESULT_OK;


public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = "Settings";

    private Context mContext;

    //Normal color request codes
    private final int HOUR_HAND_COLOR_REQ = 10;
    private final int MINUTE_HAND_COLOR_REQ = 11;
    private final int SECOND_HAND_COLOR_REQ = 12;
    private final int CENTER_CIRCLE_COLOR_REQ = 13;
    private final int CIRCLE_AND_TICKS_COLOR_REQ = 14;
    private final int OUTER_CIRCLE_COLOR_REQ = 15;
    private final int COMPLICATION_COLOR_REQ = 16;
    private final int HOUR_LABELS_COLOR_REQ = 17;

    //Night mode request codes
    private final int HOUR_HAND_NIGHT_MODE_COLOR_REQ = 18;
    private final int MINUTE_HAND_NIGHT_MODE_COLOR_REQ = 19;
    private final int SECOND_HAND_NIGHT_MODE_COLOR_REQ = 20;
    private final int CENTER_CIRCLE_NIGHT_MODE_COLOR_REQ = 21;
    private final int CIRCLE_AND_TICKS_NIGHT_MODE_COLOR_REQ = 22;
    private final int OUTER_CIRCLE_NIGHT_MODE_COLOR_REQ = 23;
    private final int COMPLICATION_NIGHT_MODE_COLOR_REQ = 24;
    private final int HOUR_LABELS_NIGHT_MODE_COLOR_REQ = 25;

    private boolean mClassicModeStatus;
    private String mNumberHourLabels = "";

    private boolean mAutoNightModeEnabled;
    private boolean mManualNightModeEnabled;

    private ProviderInfoRetriever mProviderInfoRetriever;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();

        addPreferencesFromResource(R.xml.settings);
        updateAll();

        //only enable the "Number of hour labels" setting if Classic mode is enabled
        mClassicModeStatus = getPreferenceScreen().getSharedPreferences()
                .getBoolean("settings_classic_mode", false);
        getPreferenceScreen().findPreference("settings_number_hour_labels")
                .setEnabled(mClassicModeStatus);
        //Get the setting to reset to if the user toggles "Classic mode" in the same session
        mNumberHourLabels = getPreferenceScreen().getSharedPreferences()
                .getString("settings_number_hour_labels",
                        getString(R.string.settings_0));

        //Disable the inverse of each of these Night mode settings
        //(only one can be enabled at any time)
        mAutoNightModeEnabled = getPreferenceScreen().getSharedPreferences()
                .getBoolean("settings_night_mode_enabled", false);
        mManualNightModeEnabled = getPreferenceScreen().getSharedPreferences()
                .getBoolean("settings_night_mode_manual_enabled", false);
        findPreference("settings_night_mode_manual_enabled")
                .setEnabled(!mAutoNightModeEnabled);
        findPreference("settings_night_mode_enabled")
                .setEnabled(!mManualNightModeEnabled);

        //Set the current version
        getPreferenceScreen().findPreference("app_version").setSummary(BuildConfig.VERSION_NAME);
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);

        Executor executor = (runnable) -> new Thread(runnable).start();

        ProviderInfoRetriever.OnProviderInfoReceivedCallback callback = new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
            @Override
            public void onProviderInfoReceived(int i, @Nullable ComplicationProviderInfo complicationProviderInfo) {
                setComplicationSummary(i, complicationProviderInfo);
            }
        };

        mProviderInfoRetriever = new ProviderInfoRetriever(mContext, executor);

        mProviderInfoRetriever.init();
        mProviderInfoRetriever.retrieveProviderInfo(callback,
                new ComponentName(mContext, IOClassicWatchFaceService.class),
                IOClassicWatchFaceService.COMPLICATION_IDS);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Bundle extras = preference.getExtras();

        final SharedPreferences.Editor editor
                = getPreferenceScreen().getSharedPreferences().edit();

        //Default colors
        final int defaultHands
                = mContext.getColor(R.color.default_hands); //hours, minutes, ticks, and circle
        final int defaultSeconds
                = mContext.getColor(R.color.default_seconds); //seconds
        final int defaultCenter
                = mContext.getColor(R.color.default_center_circle); //center circle
        final int defaultOuter
                = mContext.getColor(R.color.default_outer_circle); //outer circle

        //Default night mode colors
        final int defaultNightModeHands
                = mContext.getColor(R.color.default_hands); //hours, minutes, ticks, and circle
        final int defaultNightModeSeconds
                = mContext.getColor(R.color.gray); //seconds
        final int defaultNightModeCenter
                = mContext.getColor(R.color.default_center_circle); //center circle
        final int defaultNightModeOuter
                = defaultNightModeCenter; //outer circle

        switch (preference.getKey()) {
            case "settings_top_complication":
            case "settings_left_complication":
            case "settings_right_complication":
            case "settings_bottom_complication":
                final int id = extras.getInt("id");
                startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                mContext,
                                new ComponentName(mContext.getApplicationContext(),
                                        IOClassicWatchFaceService.class),
                                id,
                                IOClassicWatchFaceService.COMPLICATION_SUPPORTED_TYPES[id]), id);
                break;

            case "settings_complication_color":
                createColorPreferenceActivityIntent(mContext, "settings_complication_color_value",
                        defaultHands, COMPLICATION_COLOR_REQ);
                break;

            case "settings_complication_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_complication_night_mode_color_value",
                        defaultNightModeHands, COMPLICATION_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_hour_hand_color":
                createColorPreferenceActivityIntent(mContext, "settings_hour_hand_color_value",
                        defaultHands, HOUR_HAND_COLOR_REQ);
                break;

            case "settings_hour_hand_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_hour_hand_night_mode_color_value",
                        defaultNightModeHands, HOUR_HAND_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_minute_hand_color":
                createColorPreferenceActivityIntent(mContext, "settings_minute_hand_color_value",
                        defaultHands, MINUTE_HAND_COLOR_REQ);
                break;

            case "settings_minute_hand_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_minute_hand_night_mode_color_value",
                        defaultNightModeHands, MINUTE_HAND_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_second_hand_color":
                createColorPreferenceActivityIntent(mContext, "settings_second_hand_color_value",
                        defaultSeconds, SECOND_HAND_COLOR_REQ);
                break;

            case "settings_second_hand_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_second_hand_night_mode_color_value",
                        defaultNightModeSeconds, SECOND_HAND_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_center_circle_color":
                createColorPreferenceActivityIntent(mContext, "settings_center_circle_color_value",
                        defaultCenter, CENTER_CIRCLE_COLOR_REQ);
                break;

            case "settings_center_circle_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_center_circle_night_mode_color_value",
                        defaultNightModeCenter, CENTER_CIRCLE_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_circle_ticks_color":
                createColorPreferenceActivityIntent(mContext, "settings_circle_ticks_color_value",
                        defaultHands, CIRCLE_AND_TICKS_COLOR_REQ);
                break;

            case "settings_circle_ticks_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_circle_ticks_night_mode_color_value",
                        defaultNightModeHands, CIRCLE_AND_TICKS_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_outer_circle_color":
                createColorPreferenceActivityIntent(mContext, "settings_outer_circle_color_value",
                        defaultOuter, OUTER_CIRCLE_COLOR_REQ);
                break;

            case "settings_outer_circle_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_outer_circle_night_mode_color_value",
                        defaultNightModeOuter, OUTER_CIRCLE_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_hour_labels_color":
                createColorPreferenceActivityIntent(mContext, "settings_hour_labels_color_value",
                        defaultHands, HOUR_LABELS_COLOR_REQ);
                break;

            case "settings_hour_labels_night_mode_color":
                createColorPreferenceActivityIntent(mContext,
                        "settings_hour_labels_night_mode_color_value",
                        defaultNightModeHands, HOUR_LABELS_NIGHT_MODE_COLOR_REQ);
                break;

            case "settings_night_mode_enabled":
                //If auto night mode is enabled, disable manual night mode
                mAutoNightModeEnabled = getPreferenceScreen().getSharedPreferences()
                        .getBoolean("settings_night_mode_enabled", false);
                findPreference("settings_night_mode_manual_enabled")
                        .setEnabled(!mAutoNightModeEnabled);

                editor.putBoolean("force_night_mode", false).apply();
                editor.putBoolean("settings_night_mode_manual_enabled", false).commit();

                break;

            case "settings_night_mode_manual_enabled":
                //If manual night mode is enabled, disable auto night mode
                mManualNightModeEnabled = getPreferenceScreen().getSharedPreferences()
                        .getBoolean("settings_night_mode_manual_enabled", false);
                findPreference("settings_night_mode_enabled").setEnabled(!mManualNightModeEnabled);

                editor.putBoolean("force_night_mode", false).apply();
                editor.putBoolean("settings_night_mode_enabled", false).commit();

                break;

            case "settings_classic_mode":
                //only enable the "Show hour labels" setting if Classic mode is enabled
                mClassicModeStatus = getPreferenceScreen().getSharedPreferences()
                        .getBoolean("settings_classic_mode", false);
                findPreference("settings_number_hour_labels").setEnabled(mClassicModeStatus);

                /*
                  This will all handle a user toggling "Classic mode" in the same session.
                  Instead of losing the value for settings_number_hour_labels, we retain
                  it and restore it if necessary. This helps make the UI look responsive af.
                 */
                if (!mClassicModeStatus) {
                    mNumberHourLabels = getPreferenceScreen().getSharedPreferences()
                            .getString("settings_number_hour_labels",
                                    getString(R.string.settings_0));

                    boolean committed = editor.remove("settings_number_hour_labels").commit();
                    if (committed) {
                        editor.putString("settings_number_hour_labels",
                                getString(R.string.settings_0)).commit();
                    }
                } else {
                    if (!mNumberHourLabels.equals(getString(R.string.settings_0))) {
                        editor.putString("settings_number_hour_labels", mNumberHourLabels).commit();
                    }
                }
                break;

            case "settings_reset_hand_colors":
                editor.putString("settings_hour_hand_color", getString(R.string.settings_default_hands));
                editor.putInt("settings_hour_hand_color_value", defaultHands);

                editor.putString("settings_minute_hand_color", getString(R.string.settings_default_hands));
                editor.putInt("settings_minute_hand_color_value", defaultHands);

                editor.putString("settings_second_hand_color", getString(R.string.settings_default_seconds));
                editor.putInt("settings_second_hand_color_value", defaultSeconds);

                editor.apply();
                setSummary("settings_hour_hand_color");
                setSummary("settings_minute_hand_color");
                setSummary("settings_second_hand_color");

                Toast.makeText(mContext,
                        getString(R.string.settings_confirmation_hands_reset),
                        Toast.LENGTH_SHORT).show();
                break;

            case "settings_reset_background_colors":
                editor.putString("settings_center_circle_color",
                        getString(R.string.settings_default_center_circle));
                editor.putInt("settings_center_circle_color_value", defaultCenter);

                editor.putString("settings_circle_ticks_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_circle_ticks_color_value", defaultHands);

                editor.putString("settings_outer_circle_color",
                        getString(R.string.settings_default_outer_circle));
                editor.putInt("settings_outer_circle_color_value", defaultOuter);

                editor.putString("settings_complication_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_complication_color_value", defaultHands);

                editor.putString("settings_hour_labels_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_hour_labels_color_value", defaultHands);

                editor.apply();
                setSummary("settings_center_circle_color");
                setSummary("settings_circle_ticks_color");
                setSummary("settings_outer_circle_color");
                setSummary("settings_complication_color");
                setSummary("settings_hour_labels_color");

                Toast.makeText(mContext,
                        getString(R.string.settings_confirmation_background_reset_toast),
                        Toast.LENGTH_SHORT).show();
                break;

            case "settings_reset_night_mode_colors":
                //Night mode hands
                editor.putString("settings_hour_hand_night_mode_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_hour_hand_night_mode_color_value",
                        defaultNightModeHands);

                editor.putString("settings_minute_hand_night_mode_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_minute_hand_night_mode_color_value",
                        defaultNightModeHands);

                editor.putString("settings_second_hand_night_mode_color",
                        getString(R.string.settings_gray));
                editor.putInt("settings_second_hand_night_mode_color_value",
                        defaultNightModeSeconds);

                //Night mode background
                editor.putString("settings_center_circle_night_mode_color",
                        getString(R.string.settings_default_outer_circle));
                editor.putInt("settings_center_circle_night_mode_color_value",
                        defaultNightModeCenter);

                editor.putString("settings_circle_ticks_night_mode_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_circle_ticks_night_mode_color_value",
                        defaultNightModeHands);

                editor.putString("settings_outer_circle_night_mode_color",
                        getString(R.string.settings_default_outer_circle));
                editor.putInt("settings_outer_circle_night_mode_color_value",
                        defaultNightModeOuter);

                editor.putString("settings_complication_night_mode_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_complication_night_mode_color_value",
                        defaultNightModeHands);

                editor.putString("settings_hour_labels_night_mode_color",
                        getString(R.string.settings_default_hands));
                editor.putInt("settings_hour_labels_night_mode_color_value",
                        defaultNightModeHands);

                editor.apply();
                setSummary("settings_hour_hand_night_mode_color");
                setSummary("settings_minute_hand_night_mode_color");
                setSummary("settings_second_hand_night_mode_color");

                setSummary("settings_center_circle_night_mode_color");
                setSummary("settings_circle_ticks_night_mode_color");
                setSummary("settings_outer_circle_night_mode_color");
                setSummary("settings_complication_night_mode_color");
                setSummary("settings_hour_labels_night_mode_color");

                Toast.makeText(mContext,
                        getString(R.string.settings_confirmation_night_mode_reset_toast),
                        Toast.LENGTH_SHORT).show();
                break;

            case "donation_1":
            case "donation_3":
            case "donation_5":
            case "donation_10":
                getSettingsActivity().donate(getActivity(), preference.getKey());
                break;

            case "settings_open_changelog":
                Intent openChangelogIntent
                        = new Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://github.com/Nxt3/IO_Classic_WatchFace/blob/master/CHANGELOG.md"));

                RemoteIntent.startRemoteActivity(mContext, openChangelogIntent, null);
                Toast.makeText(mContext, getString(R.string.settings_about_opening_toast),
                        Toast.LENGTH_LONG).show();
                break;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Creates an intent to start the ColorPreference Activity
     *
     * @param context      for the intent
     * @param key          for the color preference
     * @param defaultColor for the preference
     * @param reqCode      request code for the intent, used in onActivityResult()
     */
    private void createColorPreferenceActivityIntent(Context context, String key, int defaultColor,
                                                     int reqCode) {
        Intent intent = new Intent(context, ColorActivity.class);
        intent.putExtra("color",
                getPreferenceScreen().getSharedPreferences().getInt(key,
                        defaultColor));
        intent.putExtra("color_names_id", R.array.color_names);
        intent.putExtra("color_values_id", R.array.color_values);
        startActivityForResult(intent, reqCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final SharedPreferences.Editor editor
                = getPreferenceScreen().getSharedPreferences().edit();

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                case 1:
                case 2:
                case 3:
                    setComplicationSummary(requestCode,
                            data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO));
                    break;

                case HOUR_HAND_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_hour_hand_color");
                    break;

                case HOUR_HAND_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_hour_hand_night_mode_color");
                    break;

                case MINUTE_HAND_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_minute_hand_color");
                    break;

                case MINUTE_HAND_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_minute_hand_night_mode_color");
                    break;

                case SECOND_HAND_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_second_hand_color");
                    break;

                case SECOND_HAND_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_second_hand_night_mode_color");
                    break;

                case CENTER_CIRCLE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_center_circle_color");
                    break;

                case CENTER_CIRCLE_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_center_circle_night_mode_color");
                    break;

                case CIRCLE_AND_TICKS_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_circle_ticks_color");
                    break;

                case CIRCLE_AND_TICKS_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_circle_ticks_night_mode_color");
                    break;

                case OUTER_CIRCLE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_outer_circle_color");
                    break;

                case OUTER_CIRCLE_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_outer_circle_night_mode_color");
                    break;

                case HOUR_LABELS_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_hour_labels_color");
                    break;

                case HOUR_LABELS_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_hour_labels_night_mode_color");
                    break;

                case COMPLICATION_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_complication_color");
                    break;

                case COMPLICATION_NIGHT_MODE_COLOR_REQ:
                    handleActivityOnResult(editor, data, "settings_complication_night_mode_color");
                    break;
            }
        }
    }

    /**
     * Handles the onActivityResult for ColorPreference
     *
     * @param editor SharedPrefs editor to store the selected colors
     * @param data   intent to handle the result for
     * @param key    of the preference to handle
     */
    private void handleActivityOnResult(SharedPreferences.Editor editor, Intent data, String key) {
        editor.putString(key,
                data.getStringExtra("color_name"));
        editor.putInt(key.concat("_value"),
                data.getIntExtra("color_value", 0));

        editor.apply();
        setSummary(key);
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProviderInfoRetriever.release();
    }

    /**
     * Gets the SettingsActivity so that we can call donate()
     *
     * @return SettingsActivity
     */
    private SettingsActivity getSettingsActivity() {
        return (SettingsActivity) getActivity();
    }

    /**
     * Gets the list of preferences in a PreferenceScreen
     *
     * @param p    preference to add to the list
     * @param list of preferences in the PreferenceScreen
     * @return a list of all the preferences
     */
    private ArrayList<Preference> getPreferenceList(Preference p, ArrayList<Preference> list) {
        if (p instanceof PreferenceCategory || p instanceof PreferenceScreen) {
            PreferenceGroup prefGroup = (PreferenceGroup) p;

            final int prefCount = prefGroup.getPreferenceCount();

            for (int i = 0; i < prefCount; i++) {
                getPreferenceList(prefGroup.getPreference(i), list);
            }
        }

        if (!(p instanceof PreferenceCategory)) {
            list.add(p);
        }

        return list;
    }

    /**
     * Updates all of the preferences
     */
    private void updateAll() {
        final ArrayList<Preference> preferences
                = getPreferenceList(getPreferenceScreen(), new ArrayList<>());

        for (Preference preference : preferences) {
            final Drawable icon = preference.getIcon();

            if (icon != null) {
                setStyleIcon(preference, icon);
            }

            onSharedPreferenceChanged(getPreferenceScreen().getSharedPreferences(),
                    preference.getKey());
        }
    }

    /**
     * Sets the icon styles for the preferences
     *
     * @param preference belonging to the icon
     * @param icon       to set the styles of
     */
    private void setStyleIcon(Preference preference, Drawable icon) {
        final LayerDrawable layerDrawable
                = (LayerDrawable) mContext.getDrawable(R.drawable.config_icon);
        icon.setTint(Color.WHITE);

        if (layerDrawable != null && layerDrawable.setDrawableByLayerId(R.id.nested_icon, icon)) {
            preference.setIcon(layerDrawable);
        }
    }

    /**
     * Handles what to do when a preference is altered
     *
     * @param sharedPreferences to observe
     * @param key               of the pref that was altered
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Preference preference = findPreference(key);

        if (preference != null) {
            final Bundle extras = preference.getExtras();

            if (preference instanceof ListPreference) {
                final String name = extras.getString("icons");

                if (name != null) {
                    final String value = sharedPreferences.getString(key, null);
                    final int id = getResources()
                            .getIdentifier(name, "array", getActivity().getPackageName());

                    final TypedArray icons = getResources().obtainTypedArray(id);
                    final CharSequence[] entryValues
                            = ((ListPreference) preference).getEntryValues();

                    for (int x = 0; x < entryValues.length; x++) {
                        if (value != null && value.equals(entryValues[x])) {
                            setStyleIcon(preference, getResources()
                                    .getDrawable(icons.getResourceId(x, 0)));
                        }
                    }
                    icons.recycle();
                }
            } else if (preference.getSummary() != null && preference.getSummary().equals("%s")) {
                setSummary(key);
            }
        }
    }

    /**
     * Handles setting the summary after an new selection has been made
     *
     * @param key of the setting to update its summary for
     */
    private void setSummary(String key) {
        final Preference preference = findPreference(key);

        if (preference != null) {
            Bundle extras = preference.getExtras();

            final String defaultValue = extras.getString("default");

            final String value = PreferenceManager
                    .getDefaultSharedPreferences(mContext).getString(key, defaultValue);

            preference.setSummary(value);
        }
    }

    /**
     * Sets the summary for the complication selections
     *
     * @param id           of the complication
     * @param providerInfo provider which returns the name of the selected complication in the slot
     */
    private void setComplicationSummary(int id, ComplicationProviderInfo providerInfo) {
        String key;

        switch (id) {
            case 0:
                key = "settings_top_complication";
                break;
            case 1:
                key = "settings_left_complication";
                break;
            case 2:
                key = "settings_right_complication";
                break;
            case 3:
                key = "settings_bottom_complication";
                break;
            default:
                return;
        }

        final Preference preference = findPreference(key);

        if (preference != null) {
            final String providerName = (providerInfo != null)
                    ? providerInfo.providerName : getString(R.string.settings_empty);
            preference.setSummary(providerName);
        }
    }
}
