package com.example.restrictiondetector;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "settings";
    private static final String KEY_NIGHT = "night_mode";

    private String lastResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView results = findViewById(R.id.txtResults);
        MaterialButton grabBtn = findViewById(R.id.btnGrab);
        MaterialButton copyBtn = findViewById(R.id.btnCopy);
        SwitchMaterial themeSwitch = findViewById(R.id.switchTheme);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        themeSwitch.setChecked(prefs.getBoolean(KEY_NIGHT, isSystemInDarkMode()));

        themeSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_NIGHT, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        grabBtn.setOnClickListener(v -> {
            results.setText("Requesting root, running dumpsys device_policy...");
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    String raw = RootPolicyScanner.dump();
                    return RestrictionOutputParser.extractRestrictionNames(raw);
                }

                @Override
                protected void onPostExecute(String parsed) {
                    lastResult = parsed;
                    results.setText(parsed.isEmpty() ? "(no restrictions found)" : parsed);
                }
            }.execute();
        });

        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("restrictions", lastResult);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.contains(KEY_NIGHT)) {
            boolean night = prefs.getBoolean(KEY_NIGHT, false);
            AppCompatDelegate.setDefaultNightMode(
                    night ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private boolean isSystemInDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }
}
