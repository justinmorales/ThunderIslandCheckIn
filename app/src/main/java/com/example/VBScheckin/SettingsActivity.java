package com.example.VBScheckin;

import static android.content.ContentValues.TAG;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            // Get references to each SwitchPreference
            SwitchPreference day1 = findPreference("day1");
            SwitchPreference day2 = findPreference("day2");
            SwitchPreference day3 = findPreference("day3");
            SwitchPreference day4 = findPreference("day4");
            SwitchPreference day5 = findPreference("day5");

            // Create a listener to ensure only one switch is enabled at a time
            Preference.OnPreferenceChangeListener listener = (preference, newValue) -> {
                if ((boolean) newValue) {
                    // Turn off other switches if this one is turned on
                    if (preference != day1) day1.setChecked(false);
                    if (preference != day2) day2.setChecked(false);
                    if (preference != day3) day3.setChecked(false);
                    if (preference != day4) day4.setChecked(false);
                    if (preference != day5) day5.setChecked(false);
                }
                return true;
            };

            // Set the listener to each SwitchPreference
            if (day1 != null) day1.setOnPreferenceChangeListener(listener);
            if (day2 != null) day2.setOnPreferenceChangeListener(listener);
            if (day3 != null) day3.setOnPreferenceChangeListener(listener);
            if (day4 != null) day4.setOnPreferenceChangeListener(listener);
            if (day5 != null) day5.setOnPreferenceChangeListener(listener);

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}