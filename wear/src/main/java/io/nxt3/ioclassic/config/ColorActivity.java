package io.nxt3.ioclassic.config;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ColorActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ColorPreferenceFragment colorPreferenceFragment = new ColorPreferenceFragment();

        Bundle bundle = getIntent().getExtras();
        colorPreferenceFragment.setArguments(bundle);

        getFragmentManager().beginTransaction().replace(android.R.id.content, colorPreferenceFragment).commit();
    }
}
