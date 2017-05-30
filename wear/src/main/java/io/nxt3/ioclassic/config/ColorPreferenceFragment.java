package io.nxt3.ioclassic.config;


import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import io.nxt3.ioclassic.R;

public class ColorPreferenceFragment extends PreferenceFragment {

    private int colorNamesId;
    private int colorValuesId;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        colorNamesId = bundle.getInt("color_names_id");
        colorValuesId = bundle.getInt("color_values_id");

        PreferenceScreen preferenceScreen
                = getPreferenceManager().createPreferenceScreen(getContext());
        setPreferenceScreen(preferenceScreen);
    }

    @Override
    public void onStart() {
        super.onStart();

        getPreferenceScreen().removeAll();

        String[] colorNames = getResources().getStringArray(colorNamesId);
        TypedArray colorValues = getResources().obtainTypedArray(colorValuesId);

        for (int x = 0; x < colorNames.length; x++) {
            Preference preference = new Preference(getContext());

            final String name = colorNames[x];
            final int color = colorValues.getColor(x, 0);

            preference.setOnPreferenceClickListener(p -> {
                Intent intent = new Intent();
                intent.putExtra("color_name", name);
                intent.putExtra("color_value", color);
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
                return false;
            });

            preference.setTitle(name);
            setStyleIcon(preference, getContext().getDrawable(R.drawable.config_icon).mutate(), color);

            getPreferenceScreen().addPreference(preference);
        }
        colorValues.recycle();
    }

    private void setStyleIcon(Preference preference, Drawable icon, int color) {
        LayerDrawable layerDrawable = (LayerDrawable) getContext().getDrawable(R.drawable.config_icon);
        icon.setTint(color);
        if (layerDrawable != null && layerDrawable.setDrawableByLayerId(R.id.nested_icon, icon)) {
            preference.setIcon(layerDrawable);
        }
    }
}
