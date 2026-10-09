package com.example.restrictiondetector;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private String lastResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView results = findViewById(R.id.txtResults);
        Button grabBtn = findViewById(R.id.btnGrab);
        Button copyBtn = findViewById(R.id.btnCopy);

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
}
