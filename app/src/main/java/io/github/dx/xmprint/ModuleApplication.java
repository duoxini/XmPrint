package io.github.dx.xmprint;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ModuleApplication extends Application {
    interface ServiceStateListener {
        void onServiceChanged(XposedService service);
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService CONFIG_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "XmPrint-RemotePreferences");
        thread.setDaemon(true);
        return thread;
    });
    private static final List<WeakReference<ServiceStateListener>> LISTENERS = new ArrayList<>();
    private static volatile XposedService service;
    private static volatile XposedService uncertainWriteService;

    private final XposedServiceHelper.OnServiceListener frameworkListener = new XposedServiceHelper.OnServiceListener() {
        @Override public void onServiceBind(XposedService value) {
            if (service != value && uncertainWriteService != value) uncertainWriteService = null;
            service = value;
            dispatchServiceChanged(value);
        }

        @Override public void onServiceDied(XposedService value) {
            if (service == value) {
                service = null;
                dispatchServiceChanged(null);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(frameworkListener);
    }

    static XposedService getService() {
        return service;
    }

    static void markRemotePreferenceWriteUncertain(XposedService value) {
        uncertainWriteService = value;
    }

    static boolean isRemotePreferenceWriteUncertain(XposedService value) {
        return value != null && uncertainWriteService == value;
    }

    static void executeConfigTask(Runnable task) {
        CONFIG_EXECUTOR.execute(task);
    }

    static void addServiceStateListener(ServiceStateListener listener) {
        synchronized (LISTENERS) {
            removeListenerLocked(listener);
            LISTENERS.add(new WeakReference<>(listener));
        }
        dispatchToListener(listener, service);
    }

    static void removeServiceStateListener(ServiceStateListener listener) {
        synchronized (LISTENERS) {
            removeListenerLocked(listener);
        }
    }

    private static void removeListenerLocked(ServiceStateListener listener) {
        Iterator<WeakReference<ServiceStateListener>> iterator = LISTENERS.iterator();
        while (iterator.hasNext()) {
            ServiceStateListener current = iterator.next().get();
            if (current == null || current == listener) iterator.remove();
        }
    }

    private static void dispatchServiceChanged(XposedService current) {
        MAIN_HANDLER.post(() -> {
            List<ServiceStateListener> snapshot = new ArrayList<>();
            synchronized (LISTENERS) {
                Iterator<WeakReference<ServiceStateListener>> iterator = LISTENERS.iterator();
                while (iterator.hasNext()) {
                    ServiceStateListener listener = iterator.next().get();
                    if (listener == null) iterator.remove();
                    else snapshot.add(listener);
                }
            }
            for (ServiceStateListener listener : snapshot) listener.onServiceChanged(current);
        });
    }

    private static void dispatchToListener(ServiceStateListener listener, XposedService current) {
        MAIN_HANDLER.post(() -> {
            synchronized (LISTENERS) {
                if (!containsListenerLocked(listener)) return;
            }
            listener.onServiceChanged(current);
        });
    }

    private static boolean containsListenerLocked(ServiceStateListener listener) {
        Iterator<WeakReference<ServiceStateListener>> iterator = LISTENERS.iterator();
        while (iterator.hasNext()) {
            ServiceStateListener current = iterator.next().get();
            if (current == null) iterator.remove();
            else if (current == listener) return true;
        }
        return false;
    }
}
