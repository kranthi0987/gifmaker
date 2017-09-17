package com.sanjay.gifmaker.PrefHelper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.sanjay.gifmaker.R;

/**
 * Created by fen on 8/3/16.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    //private CheckBoxPreference mCheckBox;
    private EditTextPreference mEditText;
    private NumberPickerPreference mNumberPicker;
    private Preference mVersion;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //getDelegate().installViewFactory();
        //getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        //mCheckBox = (CheckBoxPreference) findPreference("pref_repeat");
        mEditText = (EditTextPreference) findPreference("pre_default_title");
        mNumberPicker = (NumberPickerPreference) findPreference("pref_compression");

        // TODO: Set Programmatically
//        String vName = BuildConfig.VERSION_NAME;
//        mVersion = findPreference("pref_static_field_key0");
//        mVersion.setSummary("Fissure " + vName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mNumberPicker.setSummary(sharedPreferences.getString("pref_compression", "20"));
        mEditText.setSummary(sharedPreferences.getString("pref_default_title", "fissureGIF"));
    }


}