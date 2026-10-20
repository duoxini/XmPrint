package io.github.dx.xmprint;

import android.app.Activity;

final class TargetProfile {
    static final String PACKAGE = "com.mi.print";
    static final long VERSION_CODE = 458;
    static final String VERSION_NAME = "4.1.713";
    static final String MAIN = "com.mi.print.MainActivity";

    static boolean followsRotation(Activity activity) {
        // Include bundled SDK screens, but never another application's activity.
        return PACKAGE.equals(activity.getPackageName());
    }

    private TargetProfile() {}
}
