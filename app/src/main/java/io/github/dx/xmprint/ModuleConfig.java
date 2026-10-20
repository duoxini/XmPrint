package io.github.dx.xmprint;

import android.content.SharedPreferences;

/** Shared configuration schema used by the module process and its settings app. */
final class ModuleConfig {
    static final String REMOTE_PREFERENCE_GROUP = "io.github.dx.xmprint";
    static final String KEY_TABLET_ADAPTATION_ENABLED = "tablet_adaptation_enabled";
    static final boolean DEFAULT_TABLET_ADAPTATION_ENABLED = true;

    private ModuleConfig() {}

    static boolean readTabletAdaptationEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(KEY_TABLET_ADAPTATION_ENABLED, DEFAULT_TABLET_ADAPTATION_ENABLED);
    }

    static boolean writeTabletAdaptationEnabled(SharedPreferences preferences, boolean enabled) {
        return preferences.edit().putBoolean(KEY_TABLET_ADAPTATION_ENABLED, enabled).commit();
    }
}
