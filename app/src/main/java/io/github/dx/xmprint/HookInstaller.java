package io.github.dx.xmprint;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HookInstaller {
    private final XposedModule module;
    private final RuntimeState state;
    private final List<HookHandle> handles = new ArrayList<>();

    HookInstaller(XposedModule module) {
        this.module = module;
        state = new RuntimeState(module);
    }

    void install(ClassLoader loader) {
        state.info("module", "loaded module=" + BuildConfig.VERSION_NAME + " framework=" + module.getFrameworkName()
                + " version=" + module.getFrameworkVersion() + " api=" + module.getApiVersion());
        installOrientation();
        installTaskForwarding(loader);
        installCleanHome(loader);
        installNavigation(loader);
        installUserCenter(loader);
    }

    private void installOrientation() {
        state.guard("orientation-install", () -> {
            try {
                Method application = Instrumentation.class.getDeclaredMethod(
                        "callApplicationOnCreate", Application.class);
                handles.add(module.hook(application).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    state.guard("profile-early", () -> state.verify((Application) chain.getArg(0)));
                    return chain.proceed();
                }));
                Method create = Instrumentation.class.getDeclaredMethod(
                        "callActivityOnCreate", Activity.class, Bundle.class);
                handles.add(module.hook(create).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    Activity activity = (Activity) chain.getArg(0);
                    state.guard("orientation-create", () -> {
                        state.verify(activity);
                        if (state.supported()
                                && "com.hannto.functioncore.activity.FunctionActivity"
                                .equals(activity.getClass().getName())
                                && HomeTaskNavigator.bind(activity)) {
                            state.info("home-task-bind", "activity="
                                    + Integer.toHexString(System.identityHashCode(activity)));
                        }
                        if (state.supported() && TargetProfile.followsRotation(activity)) {
                            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_USER) {
                                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                            }
                            state.once("rotation:" + activity.getClass().getName(),
                                    "follow-user activity=" + activity.getClass().getName());
                        }
                    });
                    Object result = chain.proceed();
                    cleanMain(activity);
                    return result;
                }));
                Method orientation = Activity.class.getDeclaredMethod("setRequestedOrientation", int.class);
                handles.add(module.hook(orientation).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    Activity activity = (Activity) chain.getThisObject();
                    if (state.supported() && TargetProfile.followsRotation(activity)) {
                        return chain.proceed(new Object[] {ActivityInfo.SCREEN_ORIENTATION_USER});
                    }
                    return chain.proceed();
                }));
                Method resume = Instrumentation.class.getDeclaredMethod("callActivityOnResume", Activity.class);
                handles.add(module.hook(resume).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    Object result = chain.proceed();
                    cleanMain((Activity) chain.getArg(0));
                    return result;
                }));
                state.info("orientation-install", "success");
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        });
    }

    private void installCleanHome(ClassLoader loader) {
        state.guard("home-install", () -> {
            try {
                Class<?> home = loader.loadClass("com.mi.print.fragment.DeviceHomePageFragment");
                Method created = home.getDeclaredMethod("onViewCreated", View.class, Bundle.class);
                handles.add(module.hook(created).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    Object result = chain.proceed();
                    View root = (View) chain.getArg(0);
                    state.verify(root.getContext());
                    if (state.supported()) state.guard("home-ui", () -> {
                        state.info("home-ui", "onViewCreated task-dashboard="
                                + applyTaskHome(root));
                    });
                    return result;
                }));
                Method getView = home.getMethod("getView");
                Method hidden = home.getDeclaredMethod("onHiddenChanged", boolean.class);
                handles.add(module.hook(hidden).setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            if (!((Boolean) chain.getArg(0))) {
                                state.guard("home-visible", () -> {
                                try {
                                    View root = (View) getView.invoke(chain.getThisObject());
                                    if (root != null) {
                                        state.verify(root.getContext());
                                    }
                                    if (root != null && state.supported()) {
                                        state.info("home-visible", "task-dashboard="
                                                + applyTaskHome(root));
                                    }
                                } catch (ReflectiveOperationException e) {
                                    throw new IllegalStateException(e);
                                }
                                });
                            }
                            return result;
                        }));
                Class<?> learn = loader.loadClass("com.hannto.learn.web.HanntoLearnWeb");
                Method load = learn.getDeclaredMethod("g", boolean.class);
                if (load.getReturnType() != void.class) throw new NoSuchMethodException("learn loader type");
                handles.add(module.hook(load).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    if (state.supported()) {
                        state.once("course-load", "blocked homepage/resource course loader");
                        return null;
                    }
                    return chain.proceed();
                }));
                state.info("home-install", "success");
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        });
    }

    private void installTaskForwarding(ClassLoader loader) {
        state.guard("task-forward-install", () -> {
            try {
                String[] fragments = {
                        "com.hannto.functioncore.fragment.DocumentFragment",
                        "com.hannto.functioncore.fragment.PhotoFragment",
                        "com.hannto.functioncore.fragment.PictureFragment",
                        "com.hannto.functioncore.fragment.ScanFragment"
                };
                for (String fragmentName : fragments) {
                    Class<?> fragment = loader.loadClass(fragmentName);
                    Method created = fragment.getDeclaredMethod(
                            "onViewCreated", View.class, Bundle.class);
                    handles.add(module.hook(created).setExceptionMode(ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                                Object result = chain.proceed();
                                View root = (View) chain.getArg(0);
                                state.verify(root.getContext());
                                if (state.supported()) state.guard("task-forward", () ->
                                        HomeTaskNavigator.dispatch(fragmentName, root,
                                                (marker, forwarded) -> state.info(
                                                        "home-task-" + marker,
                                                        "forwarded=" + forwarded)));
                                return result;
                            }));
                }
                state.info("task-forward-install", "success");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private View.OnClickListener deepLink(String marker, String parentHomeId,
            String fragmentClass, String targetIdName) {
        String functionType;
        if ("function_doc_print".equals(parentHomeId)) {
            functionType = "DOCUMENT";
        } else if ("function_image_print".equals(parentHomeId)) {
            functionType = "PHOTO";
        } else if ("function_shoot".equals(parentHomeId)) {
            functionType = "CAMERA_SCAN";
        } else if ("function_scan".equals(parentHomeId)) {
            functionType = "DEVICE_SCAN";
        } else {
            throw new IllegalArgumentException("unknown parent home action");
        }
        return view -> state.guard("home-task-" + marker, () -> state.info(
                "home-task-" + marker, "parent-forwarded="
                        + HomeTaskNavigator.start(
                                view, parentHomeId, functionType,
                                fragmentClass, targetIdName)));
    }

    private View.OnClickListener parentLink(String marker, String parentHomeId) {
        return view -> state.guard("home-task-" + marker, () -> state.info(
                "home-task-" + marker, "parent-forwarded="
                        + HomeTaskNavigator.openParent(view, parentHomeId)));
    }

    private View.OnClickListener activityLink(String marker, String targetIdName) {
        return view -> state.guard("home-task-" + marker, () -> state.info(
                "home-task-" + marker, "forwarded="
                        + UiCleaner.performActivityViewClick(view, targetIdName)));
    }

    private boolean applyTaskHome(View root) {
        boolean cleaned = UiCleaner.cleanHome(root);
        Map<String, View.OnClickListener> actions = new LinkedHashMap<>();
        actions.put("doc-photo", deepLink("doc-photo", "function_doc_print",
                "com.hannto.functioncore.fragment.DocumentFragment", "ll_photo_document"));
        actions.put("doc-local", deepLink("doc-local", "function_doc_print",
                "com.hannto.functioncore.fragment.DocumentFragment", "ll_local_document"));
        actions.put("doc-mine", deepLink("doc-mine", "function_doc_print",
                "com.hannto.functioncore.fragment.DocumentFragment", "ll_scanned"));
        actions.put("photo-album", deepLink("photo-album", "function_image_print",
                "com.hannto.functioncore.fragment.PhotoFragment", "local_album_layout"));
        actions.put("photo-puzzle", deepLink("photo-puzzle", "function_image_print",
                "com.hannto.functioncore.fragment.PhotoFragment", "puzzle_layout"));
        actions.put("photo-id", parentLink("photo-id", "function_image_print"));
        actions.put("camera-doc", deepLink("camera-doc", "function_shoot",
                "com.hannto.functioncore.fragment.PictureFragment", "document_picture_layout"));
        actions.put("camera-word", deepLink("camera-word", "function_shoot",
                "com.hannto.functioncore.fragment.PictureFragment", "img2word_layout"));
        actions.put("camera-excel", deepLink("camera-excel", "function_shoot",
                "com.hannto.functioncore.fragment.PictureFragment", "img2excel_layout"));
        actions.put("camera-id", deepLink("camera-id", "function_shoot",
                "com.hannto.functioncore.fragment.PictureFragment", "idcard_layout"));
        actions.put("scan-doc", deepLink("scan-doc", "function_scan",
                "com.hannto.functioncore.fragment.ScanFragment", "document_scan_layout"));
        actions.put("scan-id", parentLink("scan-id", "function_scan"));
        actions.put("my-device", activityLink("my-device", "ll_user_my_device"));
        boolean arranged = UiCleaner.arrangeTaskHome(root, actions);
        state.info("home-clean", "course-container-hidden=" + cleaned);
        return arranged;
    }

    private boolean applyTaskHome(Activity activity) {
        int headerId = activity.getResources().getIdentifier(
                "homepage_header_bar_layout", "id", TargetProfile.PACKAGE);
        View header = headerId == 0 ? null : activity.findViewById(headerId);
        if (header == null || !(header.getParent() instanceof View)) {
            return false;
        }
        return applyTaskHome((View) header.getParent());
    }

    private void installNavigation(ClassLoader loader) {
        state.guard("navigation-install", () -> {
            try {
                Class<?> main = loader.loadClass(TargetProfile.MAIN);
                Method created = main.getDeclaredMethod("onCreate", Bundle.class);
                handles.add(module.hook(created).setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            Activity activity = (Activity) chain.getThisObject();
                            activity.getWindow().getDecorView().post(() ->
                                    state.guard("home-main-created", () -> {
                                        state.verify(activity);
                                        if (state.supported()) {
                                            state.info("home-main-created", "task-dashboard="
                                                    + applyTaskHome(activity));
                                        }
                                    }));
                            return result;
                        }));
                Method selected = main.getDeclaredMethod(
                        "onCheckedChanged", RadioGroup.class, int.class);
                handles.add(module.hook(selected).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    boolean[] redirected = {false};
                    RadioGroup group = (RadioGroup) chain.getArg(0);
                    state.verify(group.getContext());
                    if (state.supported()) state.guard("navigation-restore", () -> {
                        int study = group.getResources().getIdentifier("rb_main_study", "id", TargetProfile.PACKAGE);
                        int home = group.getResources().getIdentifier("rb_main_home", "id", TargetProfile.PACKAGE);
                        if (study != 0 && home != 0 && ((Integer) chain.getArg(1)) == study
                                && group.findViewById(home) != null) {
                            group.check(home);
                            redirected[0] = true;
                        }
                    });
                    Object result = redirected[0] ? null : chain.proceed();
                    int home = group.getResources().getIdentifier(
                            "rb_main_home", "id", TargetProfile.PACKAGE);
                    if (!redirected[0] && ((Integer) chain.getArg(1)) == home) {
                        group.post(() -> state.guard("home-selected", () -> {
                            Activity activity = UiCleaner.findActivity(group.getContext());
                            if (activity != null) {
                                state.verify(activity);
                            }
                            if (activity != null && state.supported()) {
                                state.info("home-selected", "task-dashboard="
                                        + applyTaskHome(activity));
                            }
                        }));
                    }
                    return result;
                }));
                state.info("navigation-install", "success");
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        });
    }

    private void cleanMain(Activity activity) {
        state.verify(activity);
        if (!state.supported() || !TargetProfile.MAIN.equals(activity.getClass().getName())) return;
        state.guard("navigation-ui", () -> state.once("navigation-ui",
                "resource-tab-hidden=" + UiCleaner.cleanMain(activity)));
    }

    private void installUserCenter(ClassLoader loader) {
        state.guard("user-center-install", () -> {
            try {
                Class<?> user = loader.loadClass("com.hannto.usercenter.fragment.UserCenterFragment");
                Method created = user.getDeclaredMethod("onViewCreated", View.class, Bundle.class);
                handles.add(module.hook(created).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    Object result = chain.proceed();
                    View root = (View) chain.getArg(0);
                    state.verify(root.getContext());
                    if (state.supported()) state.guard("user-center-ui", () ->
                            state.once("user-center-ui", "membership-and-rating-hidden="
                                    + UiCleaner.cleanUserCenter(root)));
                    return result;
                }));
                Method getView = user.getMethod("getView");
                Method resume = user.getDeclaredMethod("onResume");
                handles.add(module.hook(resume).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                    Object result = chain.proceed();
                    if (state.supported()) state.guard("user-center-resume", () -> {
                        try { UiCleaner.cleanUserCenter((View) getView.invoke(chain.getThisObject())); }
                        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                    });
                    return result;
                }));
                state.info("user-center-install", "success");
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        });
    }
}
