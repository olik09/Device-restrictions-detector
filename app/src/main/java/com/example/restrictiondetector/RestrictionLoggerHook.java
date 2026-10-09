package com.example.restrictiondetector;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.UserManager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XCallback;

/**
 * READ-ONLY diagnostic module.
 *
 * Runs inside system_server ("android" package) and logs, via
 * XposedBridge.log, every call that sets or clears a user restriction --
 * which admin component made the call, which restriction key, and the new
 * value -- together with a one-time full dump of current restriction state
 * at boot.
 *
 * This module never:
 *   - calls setUserRestriction / clearUserRestriction itself
 *   - modifies method arguments
 *   - replaces or suppresses the return value of any hooked method
 *   - prevents the original enforcement logic from running
 *
 * Every XC_MethodHook below only reads state in beforeHookedMethod /
 * afterHookedMethod; it never calls param.setResult(...) or
 * param.args[...] = ... Its sole purpose is visibility into the existing
 * policy-enforcement pipeline for defensive/analysis work.
 */
public class RestrictionLoggerHook implements IXposedHookLoadPackage {

    private static final String TAG = "[RestrictionDetector]";

    @Override
    public void handleLoadPackage(
            de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam lpparam) {

        // We only care about system_server, where DevicePolicyManagerService
        // and UserManagerService actually enforce restrictions.
        if (!"android".equals(lpparam.packageName)) {
            return;
        }

        hookRestrictionWrites(lpparam.classLoader);
        hookBootDump(lpparam.classLoader);
    }

    /**
     * Hooks every overload of setUserRestriction / clearUserRestriction on
     * DevicePolicyManagerService, whatever its exact signature on this OS
     * version, and logs the call. Does not alter behavior.
     */
    private void hookRestrictionWrites(ClassLoader classLoader) {
        final String className = "com.android.server.devicepolicy.DevicePolicyManagerService";
        Class<?> dpmsClass;
        try {
            dpmsClass = Class.forName(className, false, classLoader);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " could not find DevicePolicyManagerService on this OS: " + t);
            return;
        }

        int hooked = 0;
        for (Method m : dpmsClass.getDeclaredMethods()) {
            String name = m.getName();
            boolean isRestrictionWrite =
                    name.equals("setUserRestriction")
                            || name.equals("clearUserRestriction")
                            || name.equals("setUserRestrictionInternal")
                            || name.equals("setDeviceOwnerUserRestriction")
                            || name.equals("setProfileOwnerUserRestriction");
            if (!isRestrictionWrite) continue;
            if (Modifier.isAbstract(m.getModifiers())) continue;

            try {
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook(XCallback.PRIORITY_DEFAULT) {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // Read-only: just log. We never touch param.args or
                        // call param.setResult(), so the original policy
                        // logic always runs unmodified.
                        try {
                            XposedBridge.log(TAG + " CALL "
                                    + param.method.getName()
                                    + " args=" + Arrays.toString(param.args));
                        } catch (Throwable ignored) {
                            // Logging must never crash system_server.
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            XposedBridge.log(TAG + " RESULT "
                                    + param.method.getName()
                                    + " -> " + param.getResult());
                        } catch (Throwable ignored) {
                        }
                    }
                });
                hooked++;
            } catch (Throwable t) {
                XposedBridge.log(TAG + " failed to hook " + name + ": " + t);
            }
        }
        XposedBridge.log(TAG + " hooked " + hooked + " restriction-write method(s)");
    }

    /**
     * One-time dump of the full restriction state (active vs inactive) as
     * soon as system_server finishes booting, using the same public
     * UserManager surface the companion app uses, but read directly from
     * the system context so it reflects every user on the device.
     */
    private void hookBootDump(ClassLoader classLoader) {
        try {
            Class<?> smClass = Class.forName(
                    "com.android.server.SystemServiceManager", false, classLoader);
            for (Method m : smClass.getDeclaredMethods()) {
                if (!m.getName().equals("startBootPhase")) continue;
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        // Phase 1000 == PHASE_BOOT_COMPLETED on AOSP.
                        Object phaseArg = param.args.length > 0
                                ? param.args[param.args.length - 1] : null;
                        if (!(phaseArg instanceof Integer) || (Integer) phaseArg < 1000) {
                            return;
                        }
                        dumpOnce();
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " could not hook boot phase, falling back to app-triggered scan only: " + t);
        }
    }

    private void dumpOnce() {
        try {
            Context sysContext = AndroidAppHelper.currentApplication();
            if (sysContext == null) return;
            UserManager um = (UserManager) sysContext.getSystemService(Context.USER_SERVICE);
            if (um == null) return;

            android.os.Bundle restrictions = um.getUserRestrictions();
            StringBuilder active = new StringBuilder();
            StringBuilder inactive = new StringBuilder();
            for (String key : RestrictionCatalog.allKnownRestrictions()) {
                if (restrictions.getBoolean(key, false)) {
                    active.append(key).append(", ");
                } else {
                    inactive.append(key).append(", ");
                }
            }
            XposedBridge.log(TAG + " BOOT DUMP active=[" + active + "]");
            XposedBridge.log(TAG + " BOOT DUMP inactive=[" + inactive + "]");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " boot dump failed: " + t);
        }
    }
}
