package com.example.restrictiondetector;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView results = findViewById(R.id.txtResults);
        Button scanBtn = findViewById(R.id.btnScan);

        scanBtn.setOnClickListener(v -> {
            RestrictionScanner.Report report = RestrictionScanner.scan(getApplicationContext());

            StringBuilder sb = new StringBuilder();
            sb.append("ACTIVE (").append(report.active.size()).append(")\n");
            sb.append("--------------------\n");
            for (String key : report.active) {
                sb.append("\u2713 ").append(key).append('\n');
            }
            sb.append("\nINACTIVE (").append(report.inactive.size()).append(")\n");
            sb.append("--------------------\n");
            for (String key : report.inactive) {
                sb.append("\u2717 ").append(key).append('\n');
            }
            results.setText(sb.toString());
        });
    }
}
