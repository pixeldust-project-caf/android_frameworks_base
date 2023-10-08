/*
 * Copyright (C) 2022 Paranoid Android
 * Copyright (C) 2022 StatiXOS
 * Copyright (C) 2023 the RisingOS Android Project
 *           (C) 2023 ArrowOS
 *           (C) 2023 The LibreMobileOS Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.ComponentName;
import android.content.res.Resources;
import android.os.Build;
import android.os.Binder;
import android.os.Process;
import android.os.Build.VERSION;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PropImitationHooks {

    private static final String TAG = "PropImitationHooks";
    private static final boolean DEBUG = false;
    
    private static final String PRODUCT_DEVICE = "ro.product.device";

    private static final String sMainFP = "google/husky/husky:14/UD1A.230803.041/10808477:user/release-keys";
    private static final String sStockFp = SystemProperties.get("ro.vendor.build.fingerprint");

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";
    private static final String PACKAGE_GCAM = "com.google.android.GoogleCamera";
    private static final String PACKAGE_SNAPCHAT = "com.snapchat.android";

    private static final String PACKAGE_SUBSCRIPTION_RED = "com.google.android.apps.subscriptions.red";
    private static final String PACKAGE_TURBO = "com.google.android.apps.turbo";
    private static final String PACKAGE_VELVET = "com.google.android.googlequicksearchbox";
    private static final String PACKAGE_GBOARD = "com.google.android.inputmethod.latin";
    private static final String PACKAGE_SETUPWIZARD = "com.google.android.setupwizard";
    
    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static final Map<String, Object> sP8ProProps = createGoogleSpoofProps("husky", "Pixel 8 Pro", sMainFP);
    private static final Map<String, Object> gPhotosProps = createGoogleSpoofProps("marlin", "Pixel XL", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");

    private static Map<String, Object> createGameProps(String model, String manufacturer) {
        Map<String, Object> props = new HashMap<>();
        props.put("MODEL", model);
        props.put("MANUFACTURER", manufacturer);
        return props;
    }

    private static Map<String, Object> createGoogleSpoofProps(String device, String model, String fingerprint) {
        Map<String, Object> props = new HashMap<>();
        props.put("BRAND", "google");
        props.put("MANUFACTURER", "Google");
        props.put("ID", getBuildID(fingerprint));
        props.put("DEVICE", device);
        props.put("PRODUCT", device);
        props.put("MODEL", model);
        props.put("FINGERPRINT", fingerprint);
        props.put("TYPE", "user");
        props.put("TAGS", "release-keys");
        return props;
    }

    private static String getBuildID(String fingerprint) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9]+\\.\\d+\\.\\d+\\.\\w+)");
        Matcher matcher = pattern.matcher(fingerprint);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static volatile boolean sIsGms, sIsFinsky;
    private static volatile String sProcessName;

    public static void setProps(Application app) {
        final String packageName = app.getPackageName();
        final String processName = app.getProcessName();
        if (app == null || packageName == null || processName == null) {
            return;
        }
        sProcessName = processName;
        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);
        if (sIsGms) {
            dlog("Setting Pixel 2 fingerprint for: " + packageName);
            setCertifiedPropsForGms();
        } else if (sIsFinsky) {
            dlog("Setting certified fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", sMainFP);
        } else {
            switch (packageName) {
                case PACKAGE_SUBSCRIPTION_RED:
                case PACKAGE_TURBO:
                case PACKAGE_VELVET:
                case PACKAGE_GBOARD:
                case PACKAGE_SETUPWIZARD:
                case PACKAGE_GMS:
                case PACKAGE_GCAM:
                    if (packageName.equals(PACKAGE_GCAM) && !SystemProperties.getBoolean("persist.sys.pixelprops.gcam", false)) {
                        dlog("Not spoofing as Pixel 8 Pro for: " + packageName);
                        break;
                    }
                    dlog("Spoofing as Pixel 8 Pro for: " + packageName);
                    sP8ProProps.forEach((k, v) -> setPropValue(k, v));
                    break;
                case PACKAGE_ARCORE:
                    dlog("Setting stock fingerprint for: " + packageName);
                    setPropValue("FINGERPRINT", sStockFp);
                    break;
                case PACKAGE_SNAPCHAT:
                    dlog("Spoofing as Pixel 2 for: " + packageName);
                    spoofBuildGms();
                    break;
                case PACKAGE_GPHOTOS:
                    if (SystemProperties.getBoolean("persist.sys.pixelprops.gphotos", true)) {
                        dlog("Spoofing as Pixel XL for: " + packageName);
                        gPhotosProps.forEach((k, v) -> setPropValue(k, v));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void setCertifiedPropsForGms() {
        final boolean was = isGmsAddAccountActivityOnTop();
        final TaskStackListener taskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                final boolean is = isGmsAddAccountActivityOnTop();
                if (is ^ was) {
                    dlog("GmsAddAccountActivityOnTop is:" + is + " was:" + was +
                            ", killing myself!"); // process will restart automatically later
                    Process.killProcess(Process.myPid());
                }
            }
        };
        if (!was) {
            dlog("Spoofing build for GMS");
            spoofBuildGms();
        } else {
            dlog("Skip spoofing build for GMS, because GmsAddAccountActivityOnTop");
        }
        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener!", e);
        }
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final String callingPackage = context.getPackageManager().getNameForUid(callingUid);
        dlog("shouldBypassTaskPermission: callingPackage:" + callingPackage);
        return callingPackage != null && callingPackage.toLowerCase().contains("google");
    }

    private static void setPropValue(String key, Object value) {
        try {
            dlog("Setting prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setVersionField(String key, Integer value) {
        try {
            // Unlock
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            // Edit
            field.set(null, value);
            // Lock
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void spoofBuildGms() {
        // Alter most build properties for cts profile match checks
        setPropValue("BRAND", "google");
        setPropValue("PRODUCT", "walleye");
        setPropValue("MODEL", "Pixel 2");
    	setPropValue("MANUFACTURER", "Google");
        setPropValue("DEVICE", "walleye");
        setPropValue("FINGERPRINT", "google/walleye/walleye:8.1.0/OPM1.171019.011/4448085:user/release-keys");
        setPropValue("ID", "OPM1.171019.011");
        setPropValue("TYPE", "user");
        setPropValue("TAGS", "release-keys");
        setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.O_MR1);
        setVersionFieldString("SECURITY_PATCH", "2017-12-05");
    }

    private static void setVersionFieldString(String key, String value) {
        try {
            // Unlock
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);

            // Edit
            field.set(null, value);

            // Lock
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            dlog("Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }

    public static void dlog(String msg) {
      if (DEBUG) Log.d(TAG, "[" + sProcessName + "] " + msg);
    }
}
