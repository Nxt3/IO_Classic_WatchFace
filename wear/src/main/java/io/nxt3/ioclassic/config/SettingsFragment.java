package io.nxt3.ioclassic.config;


import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import io.nxt3.ioclassic.IOClassicWatchFaceService;
import io.nxt3.ioclassic.R;

import static android.app.Activity.RESULT_OK;
import static io.nxt3.ioclassic.config.SettingsActivity.donate;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Request codes for the color settings
     */
    private final int HOUR_HAND_COLOR_REQ = 10;
    private final int MINUTE_HAND_COLOR_REQ = 11;
    private final int SECOND_HAND_COLOR_REQ = 12;
    private final int CENTER_CIRCLE_COLOR_REQ = 13;
    private final int CIRCLE_AND_TICKS_COLOR_REQ = 14;
    private final int OUTER_CIRCLE_COLOR_REQ = 15;
    private final int COMPLICATION_COLOR_REQ = 16;

    private ProviderInfoRetriever mProviderInfoRetriever;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        updateAll();
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

        mProviderInfoRetriever = new ProviderInfoRetriever(getContext(), executor);

        mProviderInfoRetriever.init();
        mProviderInfoRetriever.retrieveProviderInfo(callback,
                new ComponentName(getContext(), IOClassicWatchFaceService.class),
                IOClassicWatchFaceService.COMPLICATION_IDS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProviderInfoRetriever.release();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Bundle extras = preference.getExtras();
        Intent intent;

        SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();

        //Default colors
        final String DEFAULT_WHITE = "#98A4A3"; //hours, minutes, ticks, and circle
        final String DEFAULT_RED = "#AA5B34"; //seconds
        final String DEFAULT_CENTER = "#1E2327"; //center circle
        final String DEFAULT_OUTER = "#20272A"; //outer circle

        switch (preference.getKey()) {
            case "settings_top_complication":
            case "settings_left_complication":
            case "settings_right_complication":
            case "settings_bottom_complication":
                final int id = extras.getInt("id");
                startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                getContext(),
                                new ComponentName(getContext().getApplicationContext(),
                                        IOClassicWatchFaceService.class),
                                id, IOClassicWatchFaceService.COMPLICATION_SUPPORTED_TYPES[id]), id);
                break;

            case "settings_complication_color":
                intent = new Intent(getContext(), ColorActivity.class);
                intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_color_value", Color.parseColor(DEFAULT_WHITE)));
                intent.putExtra("color_names_id", R.array.color_names);
                intent.putExtra("color_values_id", R.array.color_values);
                startActivityForResult(intent, COMPLICATION_COLOR_REQ);
                break;
            
            case "settings_hour_hand_color":
                intent = new Intent(getContext(), ColorActivity.class);
                intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_color_value", Color.parseColor(DEFAULT_WHITE)));
                intent.putExtra("color_names_id", R.array.color_names);
                intent.putExtra("color_values_id", R.array.color_values);
                startActivityForResult(intent, HOUR_HAND_COLOR_REQ);
                break;

            case "settings_minute_hand_color":
                intent = new Intent(getContext(), ColorActivity.class);
                intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_color_value", Color.parseColor(DEFAULT_WHITE)));
                intent.putExtra("color_names_id", R.array.color_names);
                intent.putExtra("color_values_id", R.array.color_values);
                startActivityForResult(intent, MINUTE_HAND_COLOR_REQ);
                break;

            case "settings_second_hand_color":
                intent = new Intent(getContext(), ColorActivity.class);
                intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_color_value", Color.parseColor(DEFAULT_RED)));
                intent.putExtra("color_names_id", R.array.color_names);
                intent.putExtra("color_values_id", R.array.color_values);
                startActivityForResult(intent, SECOND_HAND_COLOR_REQ);
                break;

            case "settings_center_circle_color":
                intent = new Intent(getContext(), ColorActivity.class);
                intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_background_color_value", Color.parseColor(DEFAULT_CENTER)));
                intent.putExtra("color_names_id", R.array.background_color_names);
                intent.putExtra("color_values_id", R.array.background_color_values);
                startActivityForResult(intent, CENTER_CIRCLE_COLOR_REQ);
                break;

            case "settings_circle_ticks_color":
                intent = new Intent(getContext(), ColorActivity.class);
                intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_color_value", Color.parseColor(DEFAULT_WHITE)));
                intent.putExtra("color_names_id", R.array.color_names);
                intent.putExtra("color_values_id", R.array.color_values);
                startActivityForResult(intent, CIRCLE_AND_TICKS_COLOR_REQ);
                break;

            case "settings_outer_circle_color":
                intent = new Intent(getContext(), ColorActivity.class);
                intent.putExtra("color", getPreferenceScreen().getSharedPreferences().getInt("settings_background_color_value", Color.parseColor(DEFAULT_OUTER)));
                intent.putExtra("color_names_id", R.array.background_color_names);
                intent.putExtra("color_values_id", R.array.background_color_values);
                startActivityForResult(intent, OUTER_CIRCLE_COLOR_REQ);
                break;
            
            case "time_format":
                startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
                break;

            case "settings_reset_hand_colors":
                editor.putString("settings_hour_hand_color", getString(R.string.settings_default_hands));
                editor.putInt("settings_hour_hand_color_value", Color.parseColor(DEFAULT_WHITE));

                editor.putString("settings_minute_hand_color", getString(R.string.settings_default_hands));
                editor.putInt("settings_minute_hand_color_value", Color.parseColor(DEFAULT_WHITE)).apply();

                editor.putString("settings_second_hand_color", getString(R.string.settings_default_seconds));
                editor.putInt("settings_second_hand_color_value", Color.parseColor(DEFAULT_RED)).apply();

                editor.apply();
                setSummary("settings_hour_hand_color");
                setSummary("settings_minute_hand_color");
                setSummary("settings_second_hand_color");

                Toast.makeText(getContext(),
                        getString(R.string.settings_confirmation_hands_reset),
                        Toast.LENGTH_SHORT).show();

                break;

            case "settings_reset_background_colors":
                editor.putString("settings_center_circle_color", getString(R.string.settings_default_center_circle));
                editor.putInt("settings_center_circle_color_value", Color.parseColor(DEFAULT_CENTER));

                editor.putString("settings_circle_ticks_color", getString(R.string.settings_default_hands));
                editor.putInt("settings_circle_ticks_color_value", Color.parseColor(DEFAULT_WHITE));

                editor.putString("settings_outer_circle_color", getString(R.string.settings_default_outer_circle));
                editor.putInt("settings_outer_circle_color_value", Color.parseColor(DEFAULT_OUTER));

                editor.putString("settings_complication_color", getString(R.string.settings_default_hands));
                editor.putInt("settings_complication_color_value", Color.parseColor(DEFAULT_WHITE));

                editor.apply();
                setSummary("settings_center_circle_color");
                setSummary("settings_circle_ticks_color");
                setSummary("settings_outer_circle_color");
                setSummary("settings_complication_color");

                Toast.makeText(getContext(),
                        getString(R.string.settings_confirmation_background_reset),
                        Toast.LENGTH_SHORT).show();

                break;
            
            case "donation_1":
            case "donation_3":
            case "donation_5":
            case "donation_10":
            case "donation_20":
                donate(getActivity(), preference.getKey());
                break;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                case 1:
                case 2:
                case 3:
                    setComplicationSummary(requestCode, data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO));
                    break;

                case HOUR_HAND_COLOR_REQ:
                    editor.putString("settings_hour_hand_color", data.getStringExtra("color_name"));
                    editor.putInt("settings_hour_hand_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    setSummary("settings_hour_hand_color");
                    break;

                case MINUTE_HAND_COLOR_REQ:
                    editor.putString("settings_minute_hand_color", data.getStringExtra("color_name"));
                    editor.putInt("settings_minute_hand_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    setSummary("settings_minute_hand_color");
                    break;

                case SECOND_HAND_COLOR_REQ:
                    editor.putString("settings_second_hand_color", data.getStringExtra("color_name"));
                    editor.putInt("settings_second_hand_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    setSummary("settings_second_hand_color");
                    break;

                case CENTER_CIRCLE_COLOR_REQ:
                    editor.putString("settings_center_circle_color", data.getStringExtra("color_name"));
                    editor.putInt("settings_center_circle_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    setSummary("settings_center_circle_color");
                    break;

                case CIRCLE_AND_TICKS_COLOR_REQ:
                    editor.putString("settings_circle_ticks_color", data.getStringExtra("color_name"));
                    editor.putInt("settings_circle_ticks_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    setSummary("settings_circle_ticks_color");
                    break;

                case OUTER_CIRCLE_COLOR_REQ:
                    editor.putString("settings_outer_circle_color", data.getStringExtra("color_name"));
                    editor.putInt("settings_outer_circle_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    setSummary("settings_outer_circle_color");
                    break;

                case COMPLICATION_COLOR_REQ:
                    editor.putString("settings_complication_color", data.getStringExtra("color_name"));
                    editor.putInt("settings_complication_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    setSummary("settings_complication_color");
                    break;
            }
        }
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

            int prefCount = prefGroup.getPreferenceCount();

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
        ArrayList<Preference> preferences = getPreferenceList(getPreferenceScreen(), new ArrayList<>());

        for (Preference preference : preferences) {
            final Drawable icon = preference.getIcon();

            if (icon != null) {
                setStyleIcon(preference, icon);
            }

            onSharedPreferenceChanged(getPreferenceScreen().getSharedPreferences(), preference.getKey());
        }
    }

    /**
     * Sets the icon styles for the preferences
     *
     * @param preference belonging to the icon
     * @param icon       to set the styles of
     */
    private void setStyleIcon(Preference preference, Drawable icon) {
        LayerDrawable layerDrawable = (LayerDrawable) getContext().getDrawable(R.drawable.config_icon);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);

        if (preference != null) {
            Bundle extras = preference.getExtras();

            if (preference instanceof ListPreference) {
                String name = extras.getString("icons");

                if (name != null) {
                    String value = sharedPreferences.getString(key, null);
                    int id = getResources().getIdentifier(name, "array", getActivity().getPackageName());

                    final TypedArray icons = getResources().obtainTypedArray(id);
                    final CharSequence[] entryValues = ((ListPreference) preference).getEntryValues();

                    for (int x = 0; x < entryValues.length; x++) {
                        if (value != null && value.equals(entryValues[x])) {
                            setStyleIcon(preference, getResources().getDrawable(icons.getResourceId(x, 0)));
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
        Preference preference = findPreference(key);

        if (preference != null) {
            Bundle extras = preference.getExtras();

            String def = extras.getString("default");
            String value = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(key, def);

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

        Preference preference = findPreference(key);

        if (preference != null) {
            final String providerName = providerInfo != null
                    ? providerInfo.providerName : getString(R.string.settings_empty);
            preference.setSummary(providerName);
        }
    }
}
