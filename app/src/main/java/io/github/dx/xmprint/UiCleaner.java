package io.github.dx.xmprint;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.Map;

/** Small, version-specific UI cleanup helpers for com.mi.print 4.1.713. */
public final class UiCleaner {
    private static final String TARGET_PACKAGE = "com.mi.print";

    private UiCleaner() {
    }

    public static boolean cleanMain(Activity activity) {
        if (activity == null) {
            return false;
        }

        int studyId = activity.getResources().getIdentifier(
                "rb_main_study", "id", TARGET_PACKAGE);
        int homeId = activity.getResources().getIdentifier(
                "rb_main_home", "id", TARGET_PACKAGE);
        int groupId = activity.getResources().getIdentifier(
                "rg_bottom_menu", "id", TARGET_PACKAGE);
        if (studyId == 0 || homeId == 0 || groupId == 0) {
            return false;
        }

        View studyView = activity.findViewById(studyId);
        View homeView = activity.findViewById(homeId);
        View groupView = activity.findViewById(groupId);
        if (!(studyView instanceof RadioButton)
                || !(homeView instanceof RadioButton)
                || !(groupView instanceof RadioGroup)) {
            return false;
        }

        RadioButton study = (RadioButton) studyView;
        RadioButton home = (RadioButton) homeView;
        RadioGroup group = (RadioGroup) groupView;
        if (group.indexOfChild(study) < 0 || group.indexOfChild(home) < 0) {
            return false;
        }

        if (study.isChecked()) {
            group.check(homeId);
        }
        study.setVisibility(View.GONE);
        return true;
    }

    public static boolean cleanHome(View root) {
        if (root == null) {
            return false;
        }

        int webId = root.getResources().getIdentifier(
                "web_view", "id", TARGET_PACKAGE);
        int loadingId = root.getResources().getIdentifier(
                "loading_view", "id", TARGET_PACKAGE);
        int errorId = root.getResources().getIdentifier(
                "ll_error", "id", TARGET_PACKAGE);
        if (webId == 0 || loadingId == 0 || errorId == 0) {
            return false;
        }

        View webView = root.findViewById(webId);
        View loadingView = root.findViewById(loadingId);
        View errorView = root.findViewById(errorId);
        if (!(webView instanceof WebView) || loadingView == null || errorView == null) {
            return false;
        }

        ViewGroup parent = (webView.getParent() instanceof ViewGroup)
                ? (ViewGroup) webView.getParent() : null;
        if (!(parent instanceof FrameLayout)
                || loadingView.getParent() != parent
                || errorView.getParent() != parent
                || parent.getChildCount() != 3) {
            return false;
        }

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child != webView && child != loadingView && child != errorView) {
                return false;
            }
        }

        parent.setVisibility(View.GONE);
        return true;
    }

    public static boolean cleanUserCenter(View root) {
        if (root == null) {
            return false;
        }

        int memberId = targetId(root, "ll_user_member_center");
        int rateId = targetId(root, "ll_user_rate");
        int accountId = targetId(root, "ll_user_info");
        int deviceId = targetId(root, "ll_user_my_device");
        int consumablesId = targetId(root, "ll_user_shop");
        int settingsId = targetId(root, "ll_user_setting");
        if (memberId == 0 || rateId == 0 || accountId == 0 || deviceId == 0
                || consumablesId == 0 || settingsId == 0) {
            return false;
        }

        View member = root.findViewById(memberId);
        View rate = root.findViewById(rateId);
        if (!(member instanceof LinearLayout) || !(rate instanceof LinearLayout)
                || root.findViewById(accountId) == null
                || root.findViewById(deviceId) == null
                || root.findViewById(consumablesId) == null
                || root.findViewById(settingsId) == null
                || member.getParent() != rate.getParent()
                || !(member.getParent() instanceof LinearLayout)) {
            return false;
        }

        member.setVisibility(View.GONE);
        rate.setVisibility(View.GONE);
        return true;
    }

    /** Builds a responsive task dashboard while retaining the four official parent views. */
    public static boolean arrangeTaskHome(View root,
            Map<String, View.OnClickListener> listeners) {
        if (!(root instanceof ViewGroup) || listeners == null) {
            return false;
        }
        GridApi api = GridApi.create(root);
        if (api == null || !api.isLinear(root)) {
            return false;
        }
        ViewGroup rootGroup = (ViewGroup) root;
        String[] parentNames = {
                "function_doc_print", "function_image_print", "function_shoot", "function_scan"
        };
        View[] parentButtons = new View[parentNames.length];
        for (int i = 0; i < parentNames.length; i++) {
            int id = targetId(root, parentNames[i]);
            parentButtons[i] = id == 0 ? null : root.findViewById(id);
            if (!(parentButtons[i] instanceof ViewGroup) || !api.isLinear(parentButtons[i])) {
                return false;
            }
        }
        if (isExistingTaskHome(root, parentButtons)) {
            return true;
        }
        ViewGroup container = originalHomeContainer(parentButtons);
        if (container == null || !api.isLinear(container)
                || api.orientation(container) != LinearLayout.HORIZONTAL
                || container.getChildCount() != parentButtons.length
                || container.getParent() != root) {
            return false;
        }
        for (int i = 0; i < parentButtons.length; i++) {
            if (container.getChildAt(i) != parentButtons[i]
                    || !parentButtons[i].isAttachedToWindow()
                    || !parentButtons[i].hasOnClickListeners()) {
                return false;
            }
        }

        int headerId = targetId(root, "homepage_header_bar_layout");
        int webId = targetId(root, "web_view");
        int loadingId = targetId(root, "loading_view");
        int errorId = targetId(root, "ll_error");
        View header = headerId == 0 ? null : root.findViewById(headerId);
        View web = webId == 0 ? null : root.findViewById(webId);
        View loading = loadingId == 0 ? null : root.findViewById(loadingId);
        View error = errorId == 0 ? null : root.findViewById(errorId);
        ViewGroup frame = web != null && web.getParent() instanceof ViewGroup
                ? (ViewGroup) web.getParent() : null;
        if (header == null || !(web instanceof WebView) || loading == null || error == null
                || !(frame instanceof FrameLayout) || loading.getParent() != frame
                || error.getParent() != frame || frame.getChildCount() != 3
                || header.getParent() != root || frame.getParent() != root
                || rootGroup.indexOfChild(header) != 0
                || rootGroup.indexOfChild(container) <= rootGroup.indexOfChild(header)
                || rootGroup.indexOfChild(frame) <= rootGroup.indexOfChild(container)) {
            return false;
        }

        String[][] actionSpecs = {
                {
                        "doc-photo|fun_document_photo|图片文档",
                        "doc-local|fun_document_local|本地文档",
                        "doc-mine|fun_document_scan|我的文档"
                },
                {
                        "photo-album|fun_local_album|本地相册",
                        "photo-puzzle|fun_puzzle|拼图",
                        "photo-id|function_image_print|证件照"
                },
                {
                        "camera-doc|fun_picture_scan|文档拍摄",
                        "camera-word|fun_to_word|图片转 Word",
                        "camera-excel|fun_to_excel|图片转 Excel",
                        "camera-id|fun_idcard|身份证拍摄"
                },
                {
                        "scan-doc|fun_document_device_scan|文档扫描",
                        "scan-id|function_scan|证件扫描",
                        "my-device|ic_my_device|我的设备"
                }
        };
        int widthDp = root.getResources().getConfiguration().screenWidthDp;
        boolean twoColumns = widthDp >= HomeLayoutPolicy.SIDE_BY_SIDE_MIN_WIDTH_DP;
        boolean compactNarrow = !twoColumns;
        String[] titles = {"文档", "照片", "拍摄识别", "扫描与设备"};
        LinearLayout[] groups = new LinearLayout[titles.length];
        for (int i = 0; i < titles.length; i++) {
            groups[i] = taskGroup(root, titles[i], actionSpecs[i], listeners, compactNarrow);
            if (groups[i] == null) {
                return false;
            }
        }

        ViewGroup.LayoutParams[] originalButtonParams =
                new ViewGroup.LayoutParams[parentButtons.length];
        for (int i = 0; i < parentButtons.length; i++) {
            originalButtonParams[i] = parentButtons[i].getLayoutParams();
            if (originalButtonParams[i] == null) {
                return false;
            }
        }
        OriginalState originalState = new OriginalState(api.orientation(container),
                container.getPaddingLeft(), container.getPaddingTop(),
                container.getPaddingRight(), container.getPaddingBottom(),
                container.getLayoutParams(), originalButtonParams);
        ViewGroup.LayoutParams fillParams = api.layoutParams(
                rootGroup, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
        if (fillParams == null) {
            return false;
        }
        copyMargins(container.getLayoutParams(), fillParams);

        float density = root.getResources().getDisplayMetrics().density;
        int outer = Math.round((compactNarrow ? 8 : 12) * density);
        int gap = Math.round((compactNarrow ? 8 : 10) * density);
        LinearLayout stash = new LinearLayout(root.getContext());
        stash.setVisibility(View.GONE);
        stash.setTag("xmprint:official-parent-stash");
        try {
            container.removeAllViews();
            api.setOrientation(container, LinearLayout.VERTICAL);
            container.setPadding(outer, outer, outer, outer);
            container.setLayoutParams(fillParams);
            container.addView(stash, new LinearLayout.LayoutParams(0, 0));
            for (View button : parentButtons) {
                stash.addView(button, new LinearLayout.LayoutParams(0, 0));
            }
            if (twoColumns) {
                LinearLayout firstRow = taskRow(root);
                LinearLayout secondRow = taskRow(root);
                addTaskGroup(firstRow, groups[0], false, gap);
                addTaskGroup(firstRow, groups[1], true, gap);
                addTaskGroup(secondRow, groups[2], false, gap);
                addTaskGroup(secondRow, groups[3], true, gap);
                ViewGroup.LayoutParams firstRowParams = api.layoutParams(container,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
                ViewGroup.LayoutParams secondRowParams = api.layoutParams(container,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
                if (firstRowParams == null || secondRowParams == null
                        || !(secondRowParams instanceof ViewGroup.MarginLayoutParams)) {
                    throw new IllegalStateException("missing compatible row layout params");
                }
                ((ViewGroup.MarginLayoutParams) secondRowParams).topMargin = gap;
                container.addView(firstRow, firstRowParams);
                container.addView(secondRow, secondRowParams);
            } else {
                for (int i = 0; i < groups.length; i++) {
                    ViewGroup.LayoutParams groupParams = api.layoutParams(container,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
                    if (groupParams == null
                            || !(groupParams instanceof ViewGroup.MarginLayoutParams)) {
                        throw new IllegalStateException("missing compatible group layout params");
                    }
                    if (i > 0) {
                        ((ViewGroup.MarginLayoutParams) groupParams).topMargin = gap;
                    }
                    container.addView(groups[i], groupParams);
                }
            }
            if (!isTaskHome(container, parentButtons, groups, twoColumns)) {
                throw new IllegalStateException("task home validation failed");
            }
            return true;
        } catch (RuntimeException | LinkageError e) {
            restoreOriginalHome(api, container, parentButtons, originalState);
            return false;
        }
    }

    public static boolean arrangeResponsiveHome(View root,
            View.OnClickListener myDocumentsClick, View.OnClickListener myDevicesClick) {
        if (!(root instanceof ViewGroup) || myDocumentsClick == null || myDevicesClick == null) {
            return false;
        }
        GridApi api = GridApi.create(root);
        if (api == null || !api.isLinear(root)) {
            return false;
        }
        ViewGroup rootGroup = (ViewGroup) root;

        int headerId = targetId(root, "homepage_header_bar_layout");
        int webId = targetId(root, "web_view");
        int loadingId = targetId(root, "loading_view");
        int errorId = targetId(root, "ll_error");
        String[] names = {
                "function_doc_print", "function_image_print", "function_shoot", "function_scan"
        };
        View[] buttons = new View[names.length];
        if (headerId == 0 || webId == 0 || loadingId == 0 || errorId == 0) {
            return false;
        }
        for (int i = 0; i < names.length; i++) {
            int id = targetId(root, names[i]);
            if (id == 0) {
                return false;
            }
            buttons[i] = root.findViewById(id);
            if (!(buttons[i] instanceof ViewGroup) || !api.isLinear(buttons[i])) {
                return false;
            }
        }

        View header = root.findViewById(headerId);
        View webView = root.findViewById(webId);
        View loadingView = root.findViewById(loadingId);
        View errorView = root.findViewById(errorId);
        if (header == null || !(webView instanceof WebView)
                || loadingView == null || errorView == null) {
            return false;
        }
        ViewGroup frame = (webView.getParent() instanceof ViewGroup)
                ? (ViewGroup) webView.getParent() : null;
        if (!(frame instanceof FrameLayout) || loadingView.getParent() != frame
                || errorView.getParent() != frame || frame.getChildCount() != 3
                || header.getParent() != root || frame.getParent() != root) {
            return false;
        }
        for (int i = 0; i < frame.getChildCount(); i++) {
            View child = frame.getChildAt(i);
            if (child != webView && child != loadingView && child != errorView) {
                return false;
            }
        }

        ViewGroup container = originalHomeContainer(buttons);
        boolean alreadyArranged = false;
        if (container == null) {
            container = responsiveHomeContainer(buttons);
            alreadyArranged = container != null;
        }
        if (container == null || !api.isLinear(container)
                || container.getParent() != root
                || rootGroup.indexOfChild(header) != 0
                || rootGroup.indexOfChild(container) <= rootGroup.indexOfChild(header)
                || rootGroup.indexOfChild(frame) <= rootGroup.indexOfChild(container)) {
            return false;
        }
        // NetworkState.register() inserts a GONE NetStateLayout after the header.
        // Keep that banner: invisible children are still part of the real hierarchy.
        for (int i = 0; i < rootGroup.getChildCount(); i++) {
            View child = rootGroup.getChildAt(i);
            if (child != header && child != container && child != frame
                    && !"com.hannto.mires.widget.netstate.NetStateLayout".equals(child.getClass().getName())) {
                return false;
            }
        }

        int widthDp = root.getResources().getConfiguration().screenWidthDp;
        if (widthDp <= 0) {
            float density = root.getResources().getDisplayMetrics().density;
            widthDp = density > 0f
                    ? Math.round(root.getResources().getDisplayMetrics().widthPixels / density) : 0;
        }
        HomeLayoutPolicy.Mode mode = HomeLayoutPolicy.forWidthDp(widthDp);
        if (alreadyArranged) {
            if (!isResponsiveHome(container, buttons)) {
                return false;
            }
            ResponsiveParams responsiveParams = responsiveParams(
                    api, rootGroup, container, mode, 3);
            return responsiveParams != null
                    && applyResponsiveMode(api, container, responsiveParams);
        }

        if (api.orientation(container) != LinearLayout.HORIZONTAL
                || container.getChildCount() != buttons.length) {
            return false;
        }
        for (int i = 0; i < buttons.length; i++) {
            if (container.getChildAt(i) != buttons[i]
                    || buttons[i].getParent() != container) {
                return false;
            }
            ViewGroup button = (ViewGroup) buttons[i];
            if (button.getChildCount() != 2
                    || !(button.getChildAt(0) instanceof ImageView)
                    || !(button.getChildAt(1) instanceof TextView)) {
                return false;
            }
        }

        float density = root.getResources().getDisplayMetrics().density;
        int outerPadding = Math.round(16 * density);
        int groupPadding = Math.round(16 * density);
        int actionGap = Math.round(8 * density);
        int actionHeight = Math.round(104 * density);

        LinearLayout printGroup = group(root, "打印");
        LinearLayout scanGroup = group(root, "扫描");
        LinearLayout libraryGroup = group(root, "资料与设备");
        LinearLayout printActions = actionRow(root);
        LinearLayout scanActions = actionRow(root);
        LinearLayout libraryActions = actionRow(root);
        printActions.setMinimumHeight(actionHeight);
        scanActions.setMinimumHeight(actionHeight);
        libraryActions.setMinimumHeight(actionHeight);
        printGroup.addView(printActions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scanGroup.addView(scanActions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        libraryGroup.addView(libraryActions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout myDocuments = shortcutAction(
                root, "ic_user_doc", "我的文档", "xmprint:my-documents", myDocumentsClick);
        LinearLayout myDevices = shortcutAction(
                root, "ic_my_device", "我的设备", "xmprint:my-devices", myDevicesClick);
        if (myDocuments == null || myDevices == null) {
            return false;
        }

        LinearLayout.LayoutParams[] buttonParams = new LinearLayout.LayoutParams[buttons.length];
        ViewGroup.LayoutParams[] originalButtonParams = new ViewGroup.LayoutParams[buttons.length];
        for (int i = 0; i < buttons.length; i++) {
            originalButtonParams[i] = buttons[i].getLayoutParams();
            buttonParams[i] = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if ((i & 1) == 1) {
                buttonParams[i].leftMargin = actionGap;
            }
            if (originalButtonParams[i] == null) {
                return false;
            }
        }
        printGroup.setPadding(groupPadding, groupPadding, groupPadding, groupPadding);
        scanGroup.setPadding(groupPadding, groupPadding, groupPadding, groupPadding);
        libraryGroup.setPadding(groupPadding, groupPadding, groupPadding, groupPadding);
        LinearLayout.LayoutParams firstShortcutParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams secondShortcutParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        secondShortcutParams.leftMargin = actionGap;
        ResponsiveParams responsiveParams = responsiveParams(api, rootGroup, container, mode, 3);
        if (responsiveParams == null) {
            return false;
        }
        OriginalState originalState = new OriginalState(api.orientation(container),
                container.getPaddingLeft(), container.getPaddingTop(),
                container.getPaddingRight(), container.getPaddingBottom(),
                container.getLayoutParams(), originalButtonParams);
        return commitResponsiveHome(api, container, buttons, printGroup, scanGroup,
                libraryGroup, printActions, scanActions, libraryActions,
                myDocuments, myDevices, buttonParams,
                firstShortcutParams, secondShortcutParams, responsiveParams,
                originalState, outerPadding);
    }

    private static ViewGroup originalHomeContainer(View[] buttons) {
        if (!(buttons[0].getParent() instanceof ViewGroup)) {
            return null;
        }
        ViewGroup parent = (ViewGroup) buttons[0].getParent();
        for (View button : buttons) {
            if (button.getParent() != parent) {
                return null;
            }
        }
        return parent;
    }

    private static ViewGroup responsiveHomeContainer(View[] buttons) {
        if (!(buttons[0].getParent() instanceof LinearLayout)
                || buttons[1].getParent() != buttons[0].getParent()
                || !(buttons[2].getParent() instanceof LinearLayout)
                || buttons[3].getParent() != buttons[2].getParent()) {
            return null;
        }
        View printGroup = ((View) buttons[0].getParent()).getParent() instanceof View
                ? (View) ((View) buttons[0].getParent()).getParent() : null;
        View scanGroup = ((View) buttons[2].getParent()).getParent() instanceof View
                ? (View) ((View) buttons[2].getParent()).getParent() : null;
        if (!(printGroup instanceof LinearLayout) || !(scanGroup instanceof LinearLayout)
                || printGroup.getParent() == null || printGroup.getParent() != scanGroup.getParent()
                || !(printGroup.getParent() instanceof ViewGroup)) {
            return null;
        }
        return (ViewGroup) printGroup.getParent();
    }

    private static boolean isResponsiveHome(ViewGroup container, View[] buttons) {
        if (container.getChildCount() != 3
                || !(container.getChildAt(0) instanceof LinearLayout)
                || !(container.getChildAt(1) instanceof LinearLayout)
                || !(container.getChildAt(2) instanceof LinearLayout)) {
            return false;
        }
        LinearLayout printGroup = (LinearLayout) container.getChildAt(0);
        LinearLayout scanGroup = (LinearLayout) container.getChildAt(1);
        LinearLayout libraryGroup = (LinearLayout) container.getChildAt(2);
        if (printGroup.getOrientation() != LinearLayout.VERTICAL
                || scanGroup.getOrientation() != LinearLayout.VERTICAL
                || libraryGroup.getOrientation() != LinearLayout.VERTICAL
                || printGroup.getChildCount() != 2 || scanGroup.getChildCount() != 2
                || libraryGroup.getChildCount() != 2
                || !(printGroup.getChildAt(0) instanceof TextView)
                || !(scanGroup.getChildAt(0) instanceof TextView)
                || !(libraryGroup.getChildAt(0) instanceof TextView)
                || !(printGroup.getChildAt(1) instanceof LinearLayout)
                || !(scanGroup.getChildAt(1) instanceof LinearLayout)
                || !(libraryGroup.getChildAt(1) instanceof LinearLayout)) {
            return false;
        }
        LinearLayout printActions = (LinearLayout) printGroup.getChildAt(1);
        LinearLayout scanActions = (LinearLayout) scanGroup.getChildAt(1);
        LinearLayout libraryActions = (LinearLayout) libraryGroup.getChildAt(1);
        return printActions.getOrientation() == LinearLayout.HORIZONTAL
                && scanActions.getOrientation() == LinearLayout.HORIZONTAL
                && libraryActions.getOrientation() == LinearLayout.HORIZONTAL
                && printActions.getChildCount() == 2 && scanActions.getChildCount() == 2
                && libraryActions.getChildCount() == 2
                && printActions.getChildAt(0) == buttons[0]
                && printActions.getChildAt(1) == buttons[1]
                && scanActions.getChildAt(0) == buttons[2]
                && scanActions.getChildAt(1) == buttons[3]
                && "xmprint:my-documents".equals(libraryActions.getChildAt(0).getTag())
                && "xmprint:my-devices".equals(libraryActions.getChildAt(1).getTag())
                && libraryActions.getChildAt(0).hasOnClickListeners()
                && libraryActions.getChildAt(1).hasOnClickListeners();
    }

    private static ResponsiveParams responsiveParams(GridApi api, ViewGroup root,
            ViewGroup container, HomeLayoutPolicy.Mode mode, int groupCount) {
        ViewGroup.LayoutParams containerParams = api.layoutParams(root,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
        if (containerParams == null) {
            return null;
        }
        copyMargins(container.getLayoutParams(), containerParams);

        int gap = Math.round(12 * root.getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams[] groupParams = new ViewGroup.LayoutParams[groupCount];
        for (int i = 0; i < groupCount; i++) {
            if (mode == HomeLayoutPolicy.Mode.SIDE_BY_SIDE) {
                groupParams[i] = api.layoutParams(
                        container, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (i > 0 && groupParams[i] instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) groupParams[i]).leftMargin = gap;
                }
            } else {
                groupParams[i] = api.layoutParams(container, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
                if (i > 0 && groupParams[i] instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) groupParams[i]).topMargin = gap;
                }
            }
            if (groupParams[i] == null) {
                return null;
            }
        }
        int orientation = mode == HomeLayoutPolicy.Mode.SIDE_BY_SIDE
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;
        return new ResponsiveParams(orientation, containerParams, groupParams);
    }

    private static boolean applyResponsiveMode(GridApi api, ViewGroup container,
            ResponsiveParams params) {
        int oldOrientation = api.orientation(container);
        ViewGroup.LayoutParams oldContainerParams = container.getLayoutParams();
        ViewGroup.LayoutParams[] oldGroupParams = new ViewGroup.LayoutParams[container.getChildCount()];
        for (int i = 0; i < oldGroupParams.length; i++) {
            oldGroupParams[i] = container.getChildAt(i).getLayoutParams();
        }
        try {
            applyResponsiveModeUnchecked(api, container, params);
            return true;
        } catch (RuntimeException | LinkageError e) {
            try {
                api.setOrientation(container, oldOrientation);
                for (int i = 0; i < oldGroupParams.length; i++) {
                    container.getChildAt(i).setLayoutParams(oldGroupParams[i]);
                }
                container.setLayoutParams(oldContainerParams);
            } catch (RuntimeException | LinkageError ignored) {
                // The outer Protective hook records the original failure.
            }
            return false;
        }
    }

    private static void applyResponsiveModeUnchecked(GridApi api, ViewGroup container,
            ResponsiveParams params) {
        api.setOrientation(container, params.orientation);
        if (container.getChildCount() != params.groupParams.length) {
            throw new IllegalStateException("responsive group count mismatch");
        }
        for (int i = 0; i < params.groupParams.length; i++) {
            container.getChildAt(i).setLayoutParams(params.groupParams[i]);
        }
        container.setLayoutParams(params.containerParams);
    }

    private static boolean commitResponsiveHome(GridApi api, ViewGroup container,
            View[] buttons, LinearLayout printGroup, LinearLayout scanGroup,
            LinearLayout libraryGroup, LinearLayout printActions, LinearLayout scanActions,
            LinearLayout libraryActions, LinearLayout myDocuments, LinearLayout myDevices,
            LinearLayout.LayoutParams[] buttonParams,
            LinearLayout.LayoutParams firstShortcutParams,
            LinearLayout.LayoutParams secondShortcutParams, ResponsiveParams responsiveParams,
            OriginalState originalState, int outerPadding) {
        try {
            container.removeAllViews();
            container.setPadding(outerPadding, outerPadding, outerPadding, outerPadding);
            container.addView(printGroup);
            container.addView(scanGroup);
            container.addView(libraryGroup);
            for (int i = 0; i < buttons.length; i++) {
                LinearLayout row = i < 2 ? printActions : scanActions;
                row.addView(buttons[i], buttonParams[i]);
            }
            libraryActions.addView(myDocuments, firstShortcutParams);
            libraryActions.addView(myDevices, secondShortcutParams);
            applyResponsiveModeUnchecked(api, container, responsiveParams);
            if (!isResponsiveHome(container, buttons)) {
                throw new IllegalStateException("responsive home validation failed");
            }
            return true;
        } catch (RuntimeException | LinkageError e) {
            restoreOriginalHome(api, container, buttons, originalState);
            return false;
        }
    }

    private static void restoreOriginalHome(GridApi api, ViewGroup container,
            View[] buttons, OriginalState state) {
        try {
            for (View button : buttons) {
                if (button.getParent() instanceof ViewGroup) {
                    ((ViewGroup) button.getParent()).removeView(button);
                }
            }
            container.removeAllViews();
            api.setOrientation(container, state.orientation);
            container.setPadding(state.paddingLeft, state.paddingTop,
                    state.paddingRight, state.paddingBottom);
            for (int i = 0; i < buttons.length; i++) {
                container.addView(buttons[i], state.buttonParams[i]);
            }
            container.setLayoutParams(state.containerParams);
        } catch (RuntimeException | LinkageError ignored) {
            // Best effort: the outer Protective hook prevents a target-process crash.
        }
    }

    private static final class ResponsiveParams {
        final int orientation;
        final ViewGroup.LayoutParams containerParams;
        final ViewGroup.LayoutParams[] groupParams;

        ResponsiveParams(int orientation, ViewGroup.LayoutParams containerParams,
                ViewGroup.LayoutParams[] groupParams) {
            this.orientation = orientation;
            this.containerParams = containerParams;
            this.groupParams = groupParams;
        }
    }

    private static final class OriginalState {
        final int orientation;
        final int paddingLeft;
        final int paddingTop;
        final int paddingRight;
        final int paddingBottom;
        final ViewGroup.LayoutParams containerParams;
        final ViewGroup.LayoutParams[] buttonParams;

        OriginalState(int orientation, int paddingLeft, int paddingTop,
                int paddingRight, int paddingBottom,
                ViewGroup.LayoutParams containerParams,
                ViewGroup.LayoutParams[] buttonParams) {
            this.orientation = orientation;
            this.paddingLeft = paddingLeft;
            this.paddingTop = paddingTop;
            this.paddingRight = paddingRight;
            this.paddingBottom = paddingBottom;
            this.containerParams = containerParams;
            this.buttonParams = buttonParams;
        }
    }

    private static void copyMargins(ViewGroup.LayoutParams from, ViewGroup.LayoutParams to) {
        if (from instanceof ViewGroup.MarginLayoutParams
                && to instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams oldMargins = (ViewGroup.MarginLayoutParams) from;
            ((ViewGroup.MarginLayoutParams) to).setMargins(oldMargins.leftMargin,
                    oldMargins.topMargin, oldMargins.rightMargin, oldMargins.bottomMargin);
        }
    }

    private static LinearLayout taskGroup(View root, String titleText, String[] specs,
            Map<String, View.OnClickListener> listeners, boolean compact) {
        LinearLayout result = group(root, titleText, compact);
        LinearLayout actions = actionRow(root);
        actions.setGravity(Gravity.CENTER);
        float density = root.getResources().getDisplayMetrics().density;
        int groupPadding = Math.round((compact ? 10 : 14) * density);
        result.setPadding(groupPadding, groupPadding, groupPadding, groupPadding);
        result.setMinimumHeight(Math.round((compact ? 132 : 168) * density));
        int actionGap = Math.round((compact ? 2 : 4) * density);
        for (int i = 0; i < specs.length; i++) {
            String[] fields = specs[i].split("\\|", -1);
            if (fields.length != 3) {
                return null;
            }
            View.OnClickListener listener = listeners.get(fields[0]);
            if (listener == null) {
                return null;
            }
            LinearLayout action = shortcutAction(root, fields[1], fields[2],
                    "xmprint:task:" + fields[0], listener, compact);
            if (action == null) {
                return null;
            }
            action.setMinimumHeight(Math.round((compact ? 72 : 96) * density));
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                actionParams.leftMargin = actionGap;
            }
            actions.addView(action, actionParams);
        }
        result.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return result;
    }

    private static LinearLayout taskRow(View root) {
        LinearLayout row = new LinearLayout(root.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private static void addTaskGroup(LinearLayout row, LinearLayout group,
            boolean withStartMargin, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (withStartMargin) {
            params.leftMargin = gap;
        }
        row.addView(group, params);
    }

    private static boolean isTaskHome(ViewGroup container, View[] parentButtons,
            LinearLayout[] groups, boolean twoColumns) {
        if (container.getChildCount() != (twoColumns ? 3 : 5)
                || !(container.getChildAt(0) instanceof LinearLayout)
                || !"xmprint:official-parent-stash".equals(container.getChildAt(0).getTag())) {
            return false;
        }
        LinearLayout stash = (LinearLayout) container.getChildAt(0);
        if (stash.getChildCount() != parentButtons.length || stash.getVisibility() != View.GONE) {
            return false;
        }
        for (int i = 0; i < parentButtons.length; i++) {
            if (stash.getChildAt(i) != parentButtons[i] || !parentButtons[i].hasOnClickListeners()) {
                return false;
            }
        }
        for (LinearLayout taskGroup : groups) {
            if (taskGroup.getChildCount() != 2
                    || !(taskGroup.getChildAt(0) instanceof TextView)
                    || !(taskGroup.getChildAt(1) instanceof LinearLayout)) {
                return false;
            }
            LinearLayout actions = (LinearLayout) taskGroup.getChildAt(1);
            if (actions.getChildCount() < 2) {
                return false;
            }
            for (int i = 0; i < actions.getChildCount(); i++) {
                View action = actions.getChildAt(i);
                if (!(action instanceof LinearLayout) || action.getId() != View.NO_ID
                        || !action.hasOnClickListeners()
                        || !(action.getTag() instanceof String)
                        || !((String) action.getTag()).startsWith("xmprint:task:")) {
                    return false;
                }
            }
        }
        if (twoColumns) {
            if (!(container.getChildAt(1) instanceof LinearLayout)
                    || !(container.getChildAt(2) instanceof LinearLayout)) {
                return false;
            }
            LinearLayout first = (LinearLayout) container.getChildAt(1);
            LinearLayout second = (LinearLayout) container.getChildAt(2);
            return first.getChildCount() == 2 && second.getChildCount() == 2
                    && first.getChildAt(0) == groups[0] && first.getChildAt(1) == groups[1]
                    && second.getChildAt(0) == groups[2] && second.getChildAt(1) == groups[3];
        }
        for (int i = 0; i < groups.length; i++) {
            if (container.getChildAt(i + 1) != groups[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isExistingTaskHome(View root, View[] parentButtons) {
        if (!(parentButtons[0].getParent() instanceof LinearLayout)) {
            return false;
        }
        LinearLayout stash = (LinearLayout) parentButtons[0].getParent();
        if (!"xmprint:official-parent-stash".equals(stash.getTag())
                || stash.getVisibility() != View.GONE
                || stash.getChildCount() != parentButtons.length
                || !(stash.getParent() instanceof ViewGroup)
                || ((ViewGroup) stash.getParent()).getParent() != root) {
            return false;
        }
        for (int i = 0; i < parentButtons.length; i++) {
            if (stash.getChildAt(i) != parentButtons[i]
                    || parentButtons[i].getParent() != stash
                    || !parentButtons[i].hasOnClickListeners()) {
                return false;
            }
        }
        ViewGroup container = (ViewGroup) stash.getParent();
        return (container.getChildCount() == 3 || container.getChildCount() == 5)
                && countTaskActions(container) == 13;
    }

    private static int countTaskActions(View view) {
        int count = view.getTag() instanceof String
                && ((String) view.getTag()).startsWith("xmprint:task:") ? 1 : 0;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countTaskActions(group.getChildAt(i));
            }
        }
        return count;
    }

    private static LinearLayout group(View root, String titleText) {
        return group(root, titleText, false);
    }

    private static LinearLayout group(View root, String titleText, boolean compact) {
        LinearLayout group = new LinearLayout(root.getContext());
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(groupBackground(root));
        TextView title = new TextView(root.getContext());
        title.setText(titleText);
        title.setTextColor(0xFF1F2937);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 16 : 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        int bottom = Math.round((compact ? 6 : 12)
                * root.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = bottom;
        group.addView(title, titleParams);
        return group;
    }

    private static LinearLayout actionRow(View root) {
        LinearLayout row = new LinearLayout(root.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private static LinearLayout shortcutAction(View root, String iconName, String labelText,
            String marker, View.OnClickListener listener) {
        return shortcutAction(root, iconName, labelText, marker, listener, false);
    }

    private static LinearLayout shortcutAction(View root, String iconName, String labelText,
            String marker, View.OnClickListener listener, boolean compact) {
        int iconId = root.getResources().getIdentifier(iconName, "mipmap", TARGET_PACKAGE);
        if (iconId == 0) {
            return null;
        }
        float density = root.getResources().getDisplayMetrics().density;
        int gap = Math.round((compact ? 4 : 8) * density);
        int iconSize = Math.round((compact ? 32 : 40) * density);

        LinearLayout action = new LinearLayout(root.getContext());
        action.setOrientation(LinearLayout.VERTICAL);
        action.setGravity(Gravity.CENTER);
        action.setPadding(gap, gap, gap, gap);
        action.setTag(marker);
        action.setClickable(true);
        action.setFocusable(true);
        action.setOnClickListener(listener);

        ImageView icon = new ImageView(root.getContext());
        icon.setImageResource(iconId);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        action.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView label = new TextView(root.getContext());
        label.setText(labelText);
        label.setTextColor(0xFF666666);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 12 : 14);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = gap;
        action.addView(label, labelParams);
        return action;
    }

    static boolean performActivityViewClick(View source, String targetIdName) {
        if (source == null || targetIdName == null) {
            return false;
        }
        Activity activity = findActivity(source.getContext());
        if (activity == null) {
            return false;
        }
        int targetId = source.getResources().getIdentifier(
                targetIdName, "id", TARGET_PACKAGE);
        if (targetId == 0) {
            return false;
        }
        View target = activity.findViewById(targetId);
        return target != null && target != source && target.isAttachedToWindow()
                && target.isEnabled() && target.hasOnClickListeners() && target.performClick();
    }

    static Activity findActivity(Context context) {
        Context current = context;
        for (int i = 0; i < 12 && current != null; i++) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            if (!(current instanceof ContextWrapper)) {
                return null;
            }
            Context next = ((ContextWrapper) current).getBaseContext();
            if (next == current) {
                return null;
            }
            current = next;
        }
        return null;
    }

    private static final class GridApi {
        private static final String COMPAT_NAME = "androidx.appcompat.widget.LinearLayoutCompat";
        private static final String LAYOUT_PARAMS_NAME =
                "androidx.appcompat.widget.LinearLayoutCompat$LayoutParams";

        private final Class<?> compatClass;
        private final java.lang.reflect.Constructor<?> layoutParamsConstructor;
        private final java.lang.reflect.Method getOrientation;
        private final java.lang.reflect.Method setOrientation;

        private GridApi(Class<?> compatClass,
                java.lang.reflect.Constructor<?> layoutParamsConstructor,
                java.lang.reflect.Method getOrientation,
                java.lang.reflect.Method setOrientation) {
            this.compatClass = compatClass;
            this.layoutParamsConstructor = layoutParamsConstructor;
            this.getOrientation = getOrientation;
            this.setOrientation = setOrientation;
        }

        static GridApi create(View root) {
            try {
                ClassLoader hostLoader = root.getClass().getClassLoader();
                Class<?> compat = Class.forName(COMPAT_NAME, false, hostLoader);
                Class<?> params = Class.forName(LAYOUT_PARAMS_NAME, false, hostLoader);
                return new GridApi(compat, params.getConstructor(int.class, int.class, float.class),
                        compat.getMethod("getOrientation"),
                        compat.getMethod("setOrientation", int.class));
            } catch (ReflectiveOperationException | LinkageError e) {
                return null;
            }
        }

        boolean isLinear(View view) {
            return view instanceof LinearLayout || compatClass.isInstance(view);
        }

        int orientation(ViewGroup view) {
            if (view instanceof LinearLayout) {
                return ((LinearLayout) view).getOrientation();
            }
            try {
                return ((Number) getOrientation.invoke(view)).intValue();
            } catch (ReflectiveOperationException e) {
                return -1;
            }
        }

        void setOrientation(ViewGroup view, int orientation) {
            if (view instanceof LinearLayout) {
                ((LinearLayout) view).setOrientation(orientation);
                return;
            }
            invoke(setOrientation, view, orientation);
        }

        ViewGroup.LayoutParams layoutParams(ViewGroup parent, int width, int height, float weight) {
            if (parent instanceof LinearLayout) {
                return new LinearLayout.LayoutParams(width, height, weight);
            }
            try {
                return (ViewGroup.LayoutParams) layoutParamsConstructor.newInstance(width, height, weight);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private static void invoke(java.lang.reflect.Method method, ViewGroup view, int value) {
            try {
                method.invoke(view, value);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static int targetId(View root, String name) {
        return root.getResources().getIdentifier(name, "id", TARGET_PACKAGE);
    }

    private static GradientDrawable groupBackground(View root) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFFFFFFFF);
        float density = root.getResources().getDisplayMetrics().density;
        drawable.setStroke(Math.max(1, Math.round(density)), 0xFFE5E7EB);
        drawable.setCornerRadius(20 * density);
        return drawable;
    }

}
