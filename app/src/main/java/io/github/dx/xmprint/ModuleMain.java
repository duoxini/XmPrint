package io.github.dx.xmprint;

import android.util.Log;
import android.content.SharedPreferences;
import io.github.libxposed.api.XposedModule;

public final class ModuleMain extends XposedModule {
    private String processName;
    private boolean configSnapshotTaken;

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (configSnapshotTaken || !param.isFirstPackage()
                || !TargetProfile.PACKAGE.equals(param.getPackageName())
                || !TargetProfile.PACKAGE.equals(processName)) return;

        // Take one cold-start snapshot. A preference change takes effect after
        // Xiaomi Print is force-stopped and started again.
        configSnapshotTaken = true;
        final boolean enabled;
        try {
            SharedPreferences preferences = getRemotePreferences(ModuleConfig.REMOTE_PREFERENCE_GROUP);
            enabled = ModuleConfig.readTabletAdaptationEnabled(preferences);
        } catch (RuntimeException e) {
            String message = "[feature-config][read] target=" + TargetProfile.PACKAGE
                    + " process=" + processName
                    + " failed; skipping all hooks for this process (missing key alone defaults to true); error="
                    + e.getClass().getSimpleName();
            Log.e(RuntimeState.TAG, message, e);
            log(Log.ERROR, RuntimeState.TAG, message, e);
            return;
        }

        if (!enabled) {
            String message = "[feature-config][snapshot] target=" + TargetProfile.PACKAGE
                    + " process=" + processName + " enabled=false; skipped all hooks";
            Log.i(RuntimeState.TAG, message);
            log(Log.INFO, RuntimeState.TAG, message);
            return;
        }

        String message = "[feature-config][snapshot] target=" + TargetProfile.PACKAGE
                + " process=" + processName + " enabled=true; installing hooks";
        Log.i(RuntimeState.TAG, message);
        log(Log.INFO, RuntimeState.TAG, message);
        new HookInstaller(this).install(param.getClassLoader());
    }
}
