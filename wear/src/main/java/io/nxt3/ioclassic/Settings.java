package io.nxt3.ioclassic;


import android.content.Context;
import android.content.SharedPreferences;

import io.nxt3.ioclassic.model.PrefKey;

/*
 * A Singleton for managing SharedPreferences.
 *
 * IMPORTANT: The class is not thread safe. It should work fine in most
 * circumstances since the write and read operations are fast. However
 * if you call edit for bulk updates and do not commit your changes
 * there is a possibility of data loss if a background thread has modified
 * preferences at the same time.
 *
 * Usage:
 *
 * int sampleInt = SettingsSharedPrefs.getInstance(context).getInt(PrefKey.SAMPLE_INT);
 * SettingsSharedPrefs.getInstance(context).set(PrefKey.SAMPLE_INT, sampleInt);
 *
 */
public class Settings {

    private static final String SETTINGS_NAME = "ioclassic_settings";
    private static Settings sSettings;
    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;
    private boolean mBulkUpdate = false;

    private Settings(Context context) {
        mPref = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets an instance of the SharedPrefs
     * If null, create an instance
     *
     * @param context of SharedPrefs
     * @return the SharedPrefs
     */
    public static Settings getInstance(Context context) {
        if (sSettings == null) {
            sSettings = new Settings(context.getApplicationContext());
        }
        return sSettings;
    }

    /************ SETTERS ************/

    /**
     * Convenience method for storing ints
     *
     * @param prefKey The enum of the preference to store
     * @param val The new value for the preference
     */
    public void put(PrefKey prefKey, int val) {
        doEdit();
        mEditor.putInt(prefKey.name(), val);
        doCommit();
    }

    /**
     * Convenience method for storing booleans
     *
     * @param prefKey The enum of the preference to store
     * @param val     The new value for the preference
     */
    public void put(PrefKey prefKey, boolean val) {
        doEdit();
        mEditor.putBoolean(prefKey.name(), val);
        doCommit();
    }


    /************ GETTERS ************/

    /**
     * Convenience method for retrieving ints
     *
     * @param prefKey The enum of the preference to fetch
     */
    public int getInt(PrefKey prefKey) {
        return mPref.getInt(prefKey.name(), 0);
    }

    /**
     * Convenience method for retrieving ints
     *
     * @param prefKey          The enum of the preference to fetch
     * @param defaultValue to return if the prefKey doesn't have a value
     */
    public int getInt(PrefKey prefKey, int defaultValue) {
        return mPref.getInt(prefKey.name(), defaultValue);
    }

    /**
     * Convenience method for retrieving booleans
     *
     * @param prefKey The enum of the preference to fetch
     */
    public boolean getBoolean(PrefKey prefKey) {
        return mPref.getBoolean(prefKey.name(), false);
    }

    /**
     * Convenience method for retrieving booleans
     *
     * @param prefKey      The enum of the preference to fetch
     * @param defaultValue to return if the prefKey doesn't have a value
     */
    public boolean getBoolean(PrefKey prefKey, boolean defaultValue) {
        return mPref.getBoolean(prefKey.name(), defaultValue);
    }


    /**
     * Remove prefKeys from SharedPreferences
     *
     * @param prefKeys The enum of the key(s) to be removed
     */
    public void remove(PrefKey... prefKeys) {
        doEdit();

        for (PrefKey prefKey : prefKeys) {
            mEditor.remove(prefKey.name());
        }
        doCommit();
    }

    /**
     * Remove all keys from SharedPreferences
     */
    public void clear() {
        doEdit();
        mEditor.clear();
        doCommit();
    }

    /**
     * Create a new SharedPreferences.Editor for this pref
     */
    public void edit() {
        mBulkUpdate = true;
        mEditor = mPref.edit();
    }

    /**
     * Asynchronously commits the changes back to SharedPrefs
     */
    public void commit() {
        mBulkUpdate = false;
        mEditor.apply();
        mEditor = null;
    }

    /**
     * Perform edit() on the SharedPrefs
     * This allows us to atomically commit changes back to SharedPrefs
     */
    private void doEdit() {
        if (!mBulkUpdate && mEditor == null) {
            mEditor = mPref.edit();
        }
    }

    /**
     * Asynchronously commits the changes to SharedPrefs
     */
    private void doCommit() {
        if (!mBulkUpdate && mEditor != null) {
            mEditor.apply();
            mEditor = null;
        }
    }
}
