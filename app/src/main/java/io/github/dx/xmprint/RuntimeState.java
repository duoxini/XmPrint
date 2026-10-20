package io.github.dx.xmprint;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import java.util.HashSet;
import java.util.Set;

final class RuntimeState {
    static final String TAG = "XmPrintTablet";
    private final XposedModule module;
    private final Set<String> reported = new HashSet<>();
    private boolean verified;
    private volatile boolean supported;

    RuntimeState(XposedModule module) { this.module = module; }

    synchronized void verify(Context context) {
        if (verified) return;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(TargetProfile.PACKAGE, 0);
            supported = info.getLongVersionCode() == TargetProfile.VERSION_CODE
                    && TargetProfile.VERSION_NAME.equals(info.versionName);
            verified = true;
            info("profile", "package=com.mi.print process=com.mi.print version="
                    + info.versionName + " code=" + info.getLongVersionCode()
                    + " supported=" + supported);
        } catch (Exception e) {
            failure("profile", e);
        }
    }

    boolean supported() { return supported; }

    void guard(String feature, Runnable action) {
        try { action.run(); }
        catch (Exception | LinkageError e) { failure(feature, e); }
    }

    synchronized void once(String feature, String message) {
        if (reported.add(feature)) info(feature, message);
    }

    void info(String feature, String message) {
        String text = "[" + feature + "] " + message;
        Log.i(TAG, text);
        module.log(Log.INFO, TAG, text);
    }

    private synchronized void failure(String feature, Throwable error) {
        if (!reported.add("error:" + feature)) return;
        // Exception messages can contain host file names; report type only.
        info(feature, "disabled/pass-through error=" + error.getClass().getSimpleName());
    }
}
