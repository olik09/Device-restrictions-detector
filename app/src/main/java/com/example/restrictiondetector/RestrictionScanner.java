package com.example.restrictiondetector;

import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.List;

public final class RestrictionScanner {

    public static final class Report {
        public final List<String> active = new ArrayList<>();
        public final List<String> inactive = new ArrayList<>();
    }

    public static Report scan(Context context) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Report report = new Report();
        if (um == null) {
            return report;
        }

        Bundle restrictions = um.getUserRestrictions();
        for (String key : RestrictionCatalog.allKnownRestrictions()) {
            if (restrictions.getBoolean(key, false)) {
                report.active.add(key);
            } else {
                report.inactive.add(key);
            }
        }
        return report;
    }
}
