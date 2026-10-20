package io.github.dx.xmprint;

import android.app.Activity;
import android.os.SystemClock;
import android.view.View;

/** One-shot handoff from a homepage shortcut to an official FunctionActivity fragment view. */
final class HomeTaskNavigator {
    private static final long UNBOUND_MAX_AGE_MS = 120000L;
    private static final long BOUND_MAX_AGE_MS = 10000L;
    private static final Object LOCK = new Object();
    private static Pending pending;

    interface ResultLogger {
        void log(String marker, boolean forwarded);
    }

    private HomeTaskNavigator() {
    }

    static boolean start(View source, String parentHomeId, String functionType,
            String fragmentClass, String targetIdName) {
        if (source == null || parentHomeId == null || fragmentClass == null
                || targetIdName == null || functionType == null) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        Pending request = new Pending(functionType, fragmentClass, targetIdName, now);
        synchronized (LOCK) {
            if (pending != null && !pending.expired(now)) {
                return false;
            }
            pending = request;
        }
        boolean parentForwarded = UiCleaner.performActivityViewClick(source, parentHomeId);
        if (!parentForwarded) {
            clear(request);
        }
        return parentForwarded;
    }

    static boolean openParent(View source, String parentHomeId) {
        synchronized (LOCK) {
            pending = null;
        }
        return UiCleaner.performActivityViewClick(source, parentHomeId);
    }

    static boolean bind(Activity activity) {
        if (activity == null) {
            return false;
        }
        Object type = activity.getIntent().getSerializableExtra("FUNCTION_TYPE");
        long now = SystemClock.elapsedRealtime();
        synchronized (LOCK) {
            if (pending == null) {
                return false;
            }
            if (pending.expired(now)) {
                pending = null;
                return false;
            }
            if (!pending.functionType.equals(String.valueOf(type))
                    || pending.boundActivity != null) {
                return false;
            }
            pending.boundActivity = activity;
            pending.boundAtMs = now;
            return true;
        }
    }

    static void dispatch(String fragmentClass, View root, ResultLogger logger) {
        if (fragmentClass == null || root == null || logger == null) {
            return;
        }
        Pending request;
        Activity activity = UiCleaner.findActivity(root.getContext());
        if (activity == null) {
            return;
        }
        synchronized (LOCK) {
            request = pending;
            if (request == null) {
                return;
            }
            if (request.expired(SystemClock.elapsedRealtime())) {
                pending = null;
                return;
            }
            if (request.boundActivity != activity
                    || !request.fragmentClass.equals(fragmentClass)) {
                return;
            }
        }
        Pending matched = request;
        root.post(() -> {
            boolean forwarded = false;
            synchronized (LOCK) {
                if (pending != matched) {
                    return;
                }
                pending = null;
            }
            try {
                int targetId = root.getResources().getIdentifier(
                        matched.targetIdName, "id", "com.mi.print");
                if (targetId != 0) {
                    View target = root.findViewById(targetId);
                    forwarded = target != null && target.isAttachedToWindow()
                            && target.isEnabled() && target.hasOnClickListeners()
                            && target.performClick();
                }
                logger.log(matched.targetIdName, forwarded);
            } catch (RuntimeException | LinkageError e) {
                logger.log(matched.targetIdName, false);
            }
        });
    }

    private static void clear(Pending request) {
        synchronized (LOCK) {
            if (pending == request) {
                pending = null;
            }
        }
    }

    private static final class Pending {
        final String functionType;
        final String fragmentClass;
        final String targetIdName;
        final long createdAtMs;
        Activity boundActivity;
        long boundAtMs;

        Pending(String functionType, String fragmentClass,
                String targetIdName, long createdAtMs) {
            this.functionType = functionType;
            this.fragmentClass = fragmentClass;
            this.targetIdName = targetIdName;
            this.createdAtMs = createdAtMs;
        }

        boolean expired(long now) {
            return boundActivity == null
                    ? now - createdAtMs > UNBOUND_MAX_AGE_MS
                    : now - boundAtMs > BOUND_MAX_AGE_MS;
        }
    }
}
