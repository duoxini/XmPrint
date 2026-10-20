package io.github.dx.xmprint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.CompoundButton;
import android.widget.TextView;
import io.github.libxposed.service.XposedService;

public final class StatusActivity extends Activity implements ModuleApplication.ServiceStateListener {
    private static final int COLOR_PAGE = 0xFFF7F8FA;
    private static final int COLOR_SURFACE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF1D2129;
    private static final int COLOR_SECONDARY = 0xFF667085;
    private static final int COLOR_PRIMARY = 0xFF2563EB;
    private static final int COLOR_BORDER = 0xFFE4E7EC;
    private static final int COLOR_SUCCESS = 0xFF16845B;

    private TextView status;
    private TextView configStatus;
    private TextView frameworkChip;
    private LsposedSwitch adaptationSwitch;
    private boolean suppressSwitchListener;
    private boolean activityStarted;
    private boolean featureEnabled = ModuleConfig.DEFAULT_TABLET_ADAPTATION_ENABLED;
    private boolean configurationLoaded;
    private int requestGeneration;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        setContentView(buildPage());
    }

    private View buildPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_PAGE);

        LinearLayout viewport = new LinearLayout(this);
        viewport.setOrientation(LinearLayout.VERTICAL);
        viewport.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        int pageInset = dp(getResources().getConfiguration().screenWidthDp >= 600 ? 24 : 16);
        int contentTopGap = dp(16);
        viewport.setPadding(pageInset, contentTopGap, pageInset, dp(32));
        viewport.setOnApplyWindowInsetsListener((view, insets) -> {
            int statusBarInset = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? insets.getInsets(WindowInsets.Type.statusBars()).top
                    : insets.getSystemWindowInsetTop();
            view.setPadding(pageInset, statusBarInset + contentTopGap, pageInset, dp(32));
            return insets;
        });
        scroll.addView(viewport, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int width = getResources().getConfiguration().screenWidthDp >= 600
                ? dp(720) : ViewGroup.LayoutParams.MATCH_PARENT;
        viewport.addView(body, new LinearLayout.LayoutParams(width,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(buildHeader());
        body.addView(sectionLabel("功能"), topMargin(dp(24)));
        body.addView(buildFeatureCard(), topMargin(dp(8)));
        body.addView(sectionLabel("运行状态"), topMargin(dp(24)));
        body.addView(buildStatusCard(), topMargin(dp(8)));
        body.addView(sectionLabel("快捷操作"), topMargin(dp(24)));
        body.addView(buildActionsCard(), topMargin(dp(8)));
        body.addView(sectionLabel("使用说明"), topMargin(dp(24)));
        body.addView(buildHelpCard(), topMargin(dp(8)));
        return scroll;
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_printer);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setBackground(rounded(0xFFEAF1FF, 16, 0, 0));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titlesParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titlesParams.leftMargin = dp(14);
        row.addView(titles, titlesParams);
        titles.addView(text("小米打印界面精简", 22, COLOR_TEXT, Typeface.BOLD));
        titles.addView(text("LSPosed 模块  ·  v" + BuildConfig.VERSION_NAME,
                13, COLOR_SECONDARY, Typeface.NORMAL), topMargin(dp(4)));

        frameworkChip = text("等待框架", 12, COLOR_SECONDARY, Typeface.BOLD);
        frameworkChip.setGravity(Gravity.CENTER);
        frameworkChip.setPadding(dp(11), dp(7), dp(11), dp(7));
        frameworkChip.setBackground(rounded(0xFFEEF0F3, 99, 0, 0));
        row.addView(frameworkChip, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View buildFeatureCard() {
        LinearLayout card = card();
        LinearLayout settingRow = new LinearLayout(this);
        settingRow.setTag("xmprint:settings:master-switch-row");
        settingRow.setOrientation(LinearLayout.HORIZONTAL);
        settingRow.setGravity(Gravity.CENTER_VERTICAL);
        settingRow.setMinimumHeight(dp(76));
        settingRow.setPadding(dp(18), dp(14), dp(12), dp(14));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        settingRow.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(text("启用界面适配", 16, COLOR_TEXT, Typeface.BOLD));
        copy.addView(text("响应式首页、方向适配与页面精简",
                13, COLOR_SECONDARY, Typeface.NORMAL), topMargin(dp(4)));

        adaptationSwitch = new LsposedSwitch(this);
        adaptationSwitch.setTag("xmprint:settings:master-switch");
        adaptationSwitch.setContentDescription("启用界面适配");
        adaptationSwitch.setChecked(featureEnabled);
        adaptationSwitch.setEnabled(false);
        adaptationSwitch.setOnCheckedChangeListener(
                (button, checked) -> onFeatureToggleChanged(checked));
        settingRow.addView(adaptationSwitch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
        settingRow.setOnClickListener(view -> {
            if (adaptationSwitch.isEnabled()) adaptationSwitch.performClick();
        });
        card.addView(settingRow);
        card.addView(divider(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        configStatus = text("正在连接框架并读取配置…", 13,
                COLOR_SECONDARY, Typeface.NORMAL);
        configStatus.setTag("xmprint:settings:config-status");
        configStatus.setPadding(dp(18), dp(14), dp(18), dp(16));
        card.addView(configStatus);
        return card;
    }

    private View buildStatusCard() {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(12), dp(12));
        header.addView(text("LSPosed 状态", 16, COLOR_TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView refresh = compactButton("刷新", false);
        refresh.setTag("xmprint:settings:refresh");
        refresh.setContentDescription("刷新 LSPosed 框架状态");
        refresh.setOnClickListener(v -> loadCurrentServiceState());
        header.addView(refresh);
        card.addView(header);

        status = text("等待框架服务…", 14, COLOR_SECONDARY, Typeface.NORMAL);
        status.setTag("xmprint:settings:framework-status");
        status.setLineSpacing(dp(3), 1f);
        status.setPadding(dp(18), 0, dp(18), dp(18));
        card.addView(status);
        return card;
    }

    private View buildActionsCard() {
        LinearLayout card = card();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(70));
        row.setPadding(dp(18), dp(12), dp(12), dp(12));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(text("打开小米打印", 16, COLOR_TEXT, Typeface.BOLD));
        copy.addView(text("查看适配后的首页与官方功能",
                13, COLOR_SECONDARY, Typeface.NORMAL), topMargin(dp(4)));

        TextView open = compactButton("打开", true);
        open.setTag("xmprint:settings:open-print");
        open.setContentDescription("打开小米打印");
        open.setOnClickListener(v -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage(TargetProfile.PACKAGE);
            if (intent != null) startActivity(intent);
        });
        row.addView(open);
        card.addView(row);
        return card;
    }

    private View buildHelpCard() {
        LinearLayout card = card();
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.addView(text("修改总开关后，需强行停止并重新打开小米打印才会生效。",
                14, COLOR_TEXT, Typeface.NORMAL));
        card.addView(text("系统单应用方向兼容项独立于模块总开关，关闭模块不会改动系统方向设置。",
                14, COLOR_SECONDARY, Typeface.NORMAL), topMargin(dp(10)));
        return card;
    }

    private LinearLayout card() {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setBackground(rounded(COLOR_SURFACE, 18, COLOR_BORDER, 1));
        result.setClipToOutline(true);
        return result;
    }

    private TextView sectionLabel(String value) {
        TextView label = text(value, 13, COLOR_SECONDARY, Typeface.BOLD);
        label.setPadding(dp(4), 0, 0, 0);
        return label;
    }

    private TextView compactButton(String value, boolean primary) {
        TextView button = text(value, 14, primary ? Color.WHITE : COLOR_PRIMARY, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(64));
        button.setMinHeight(dp(40));
        button.setPadding(dp(16), dp(9), dp(16), dp(9));
        button.setBackground(rounded(primary ? COLOR_PRIMARY : 0xFFEAF1FF, 12, 0, 0));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private View divider() {
        View result = new View(this);
        result.setBackgroundColor(COLOR_BORDER);
        return result;
    }

    private TextView text(String value, float sizeSp, int color, int style) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextSize(sizeSp);
        result.setTextColor(color);
        result.setTypeface(Typeface.DEFAULT, style);
        return result;
    }

    private LinearLayout.LayoutParams topMargin(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = margin;
        return params;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class LsposedSwitch extends CompoundButton {
        private final Drawable switchDrawable;
        private final int switchWidth;
        private final int switchHeight;
        private final int touchHeight;

        LsposedSwitch(Context context) {
            super(context);
            float density = context.getResources().getDisplayMetrics().density;
            switchWidth = Math.round(52 * density);
            switchHeight = Math.round(32 * density);
            touchHeight = Math.round(48 * density);
            switchDrawable = context.getDrawable(R.drawable.xm_switch_full);
            setButtonDrawable((Drawable) null);
            setClickable(true);
            setFocusable(true);
            setMinWidth(switchWidth);
            setMinHeight(touchHeight);
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(resolveSize(switchWidth, widthMeasureSpec),
                    resolveSize(touchHeight, heightMeasureSpec));
        }

        @Override protected void onDraw(Canvas canvas) {
            int left = (getWidth() - switchWidth) / 2;
            int top = (getHeight() - switchHeight) / 2;
            switchDrawable.setBounds(left, top, left + switchWidth, top + switchHeight);
            switchDrawable.setState(getDrawableState());
            switchDrawable.draw(canvas);
        }

        @Override protected void drawableStateChanged() {
            super.drawableStateChanged();
            if (switchDrawable != null) switchDrawable.setState(getDrawableState());
            invalidate();
        }

        @Override protected boolean verifyDrawable(Drawable who) {
            return who == switchDrawable || super.verifyDrawable(who);
        }

        @Override public void jumpDrawablesToCurrentState() {
            super.jumpDrawablesToCurrentState();
            if (switchDrawable != null) switchDrawable.jumpToCurrentState();
        }

        @Override public CharSequence getAccessibilityClassName() {
            return "android.widget.Switch";
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(COLOR_PAGE);
        window.setNavigationBarColor(COLOR_PAGE);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    @Override protected void onStart() {
        super.onStart();
        activityStarted = true;
        ModuleApplication.addServiceStateListener(this);
    }

    @Override protected void onStop() {
        activityStarted = false;
        requestGeneration++;
        ModuleApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override public void onServiceChanged(XposedService current) {
        loadServiceState(current);
    }

    private void loadCurrentServiceState() {
        loadServiceState(ModuleApplication.getService());
    }

    private void loadServiceState(XposedService current) {
        final int generation = ++requestGeneration;
        configurationLoaded = false;
        adaptationSwitch.setEnabled(false);
        if (current == null) {
            setFrameworkChip("等待框架", false);
            status.setText("未连接\n请在 LSPosed 中启用模块，并确认已勾选小米打印作用域。");
            configStatus.setText("框架服务未连接，当前不能修改总开关。");
            return;
        }
        if (ModuleApplication.isRemotePreferenceWriteUncertain(current)) {
            showUncertainWriteState();
            return;
        }

        setFrameworkChip("正在读取", false);
        status.setText("框架服务已连接，正在读取状态…");
        configStatus.setText("正在读取总开关配置…");
        ModuleApplication.executeConfigTask(() -> {
            if (ModuleApplication.isRemotePreferenceWriteUncertain(current)) {
                runOnUiThread(() -> {
                    if (!activityStarted || generation != requestGeneration) return;
                    if (ModuleApplication.getService() != current) {
                        loadCurrentServiceState();
                        return;
                    }
                    showUncertainWriteState();
                });
                return;
            }

            String frameworkStatus;
            boolean frameworkReadSucceeded = false;
            try {
                boolean scoped = current.getScope().contains(TargetProfile.PACKAGE);
                frameworkStatus = "框架    " + current.getFrameworkName() + " "
                        + current.getFrameworkVersion()
                        + "\nAPI       " + current.getApiVersion()
                        + "\n作用域    " + (scoped ? "小米打印已勾选" : "小米打印未勾选");
                frameworkReadSucceeded = true;
            } catch (RuntimeException e) {
                frameworkStatus = "框架服务已连接，但读取状态失败："
                        + e.getClass().getSimpleName();
            }

            boolean readSucceeded = false;
            boolean enabled = featureEnabled;
            String preferenceStatus;
            try {
                SharedPreferences preferences = current.getRemotePreferences(
                        ModuleConfig.REMOTE_PREFERENCE_GROUP);
                enabled = ModuleConfig.readTabletAdaptationEnabled(preferences);
                readSucceeded = true;
                preferenceStatus = "配置已同步。修改后重启小米打印生效；缺少配置项时默认开启。";
            } catch (RuntimeException e) {
                preferenceStatus = "读取配置失败（" + e.getClass().getSimpleName()
                        + "），开关保持不可编辑。目标进程读取异常时会跳过全部 Hook。";
            }

            final String finalFrameworkStatus = frameworkStatus;
            final String finalPreferenceStatus = preferenceStatus;
            final boolean finalFrameworkReadSucceeded = frameworkReadSucceeded;
            final boolean finalReadSucceeded = readSucceeded;
            final boolean finalEnabled = enabled;
            runOnUiThread(() -> {
                if (!activityStarted || generation != requestGeneration) return;
                if (ModuleApplication.getService() != current) {
                    loadCurrentServiceState();
                    return;
                }
                if (ModuleApplication.isRemotePreferenceWriteUncertain(current)) {
                    showUncertainWriteState();
                    return;
                }
                setFrameworkChip(finalFrameworkReadSucceeded ? "已连接" : "读取异常",
                        finalFrameworkReadSucceeded);
                status.setText(finalFrameworkStatus);
                configStatus.setText(finalPreferenceStatus);
                if (finalReadSucceeded) {
                    featureEnabled = finalEnabled;
                    configurationLoaded = true;
                    setSwitchChecked(featureEnabled);
                    adaptationSwitch.setEnabled(true);
                }
            });
        });
    }

    private void onFeatureToggleChanged(boolean enabled) {
        if (suppressSwitchListener) return;
        final boolean previousValue = featureEnabled;
        final XposedService current = ModuleApplication.getService();
        if (!activityStarted || !configurationLoaded || current == null) {
            setSwitchChecked(previousValue);
            adaptationSwitch.setEnabled(false);
            configStatus.setText("框架服务或配置不可用，未保存更改。");
            return;
        }

        final int generation = requestGeneration;
        adaptationSwitch.setEnabled(false);
        configStatus.setText("正在提交总开关配置…");
        ModuleApplication.executeConfigTask(() -> {
            boolean committed = false;
            RuntimeException failure = null;
            try {
                SharedPreferences preferences = current.getRemotePreferences(
                        ModuleConfig.REMOTE_PREFERENCE_GROUP);
                committed = ModuleConfig.writeTabletAdaptationEnabled(preferences, enabled);
            } catch (RuntimeException e) {
                failure = e;
            }
            final boolean finalCommitted = committed;
            final RuntimeException finalFailure = failure;
            if (!finalCommitted) ModuleApplication.markRemotePreferenceWriteUncertain(current);
            runOnUiThread(() -> {
                if (!activityStarted) return;
                if (generation != requestGeneration || ModuleApplication.getService() != current) {
                    loadCurrentServiceState();
                    return;
                }
                if (finalCommitted) {
                    featureEnabled = enabled;
                    setSwitchChecked(featureEnabled);
                    configStatus.setText("配置已保存。强行停止并重新打开小米打印后生效。");
                    adaptationSwitch.setEnabled(true);
                } else {
                    configurationLoaded = false;
                    setSwitchChecked(previousValue);
                    String reason = finalFailure == null ? "commit() 返回 false"
                            : finalFailure.getClass().getSimpleName();
                    configStatus.setText("配置提交失败（" + reason
                            + "），已恢复原显示并锁定开关。请重开模块 App，或等待框架重连后再试。");
                }
            });
        });
    }

    private void setSwitchChecked(boolean checked) {
        suppressSwitchListener = true;
        adaptationSwitch.setChecked(checked);
        suppressSwitchListener = false;
    }

    private void setFrameworkChip(String label, boolean connected) {
        frameworkChip.setText(label);
        frameworkChip.setTextColor(connected ? COLOR_SUCCESS : COLOR_SECONDARY);
        frameworkChip.setBackground(rounded(connected ? 0xFFE4F6EE : 0xFFEEF0F3,
                99, 0, 0));
    }

    private void showUncertainWriteState() {
        configurationLoaded = false;
        adaptationSwitch.setEnabled(false);
        setFrameworkChip("需要重连", false);
        status.setText("框架服务仍是上次保存失败时的连接实例。");
        configStatus.setText("上次提交失败，本地缓存状态可能已变化。请强行停止模块 App 后重新打开，或等待框架服务重新连接。");
    }
}
