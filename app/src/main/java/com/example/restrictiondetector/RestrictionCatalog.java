package com.example.restrictiondetector;

import android.os.UserManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RestrictionCatalog {

    private RestrictionCatalog() {}

    public static List<String> allKnownRestrictions() {
        List<String> result = new ArrayList<>();
        for (Field f : UserManager.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() != String.class) continue;
            String name = f.getName();
            if (!name.startsWith("DISALLOW_")) continue;
            try {
                Object value = f.get(null);
                if (value instanceof String) {
                    result.add((String) value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        Collections.sort(result);
        return result;
    }
}
