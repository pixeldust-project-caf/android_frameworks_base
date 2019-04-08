/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui;

import static android.app.StatusBarManager.DISABLE2_SYSTEM_ICONS;
import static android.app.StatusBarManager.DISABLE_NONE;
import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;
import static android.provider.Settings.System.STATUS_BAR_BATTERY_STYLE;
import static android.provider.Settings.System.TEXT_CHARGING_SYMBOL;
import static android.provider.Settings.System.SHOW_BATTERY_PERCENT_ON_QSB;

import android.animation.ArgbEvaluator;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settingslib.Utils;
import com.android.settingslib.graph.BatteryMeterDrawableBase;
import com.android.settingslib.graph.ThemedBatteryDrawable;
import com.android.systemui.R;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.IconLogger;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.android.systemui.util.Utils.DisableStateTracker;

import java.text.NumberFormat;

public class BatteryMeterView extends LinearLayout implements
        BatteryStateChangeCallback, DarkReceiver, ConfigurationListener {

    private ThemedBatteryDrawable mDrawable;
    private final String mSlotBattery;
    private ImageView mBatteryIconView;
    private final CurrentUserTracker mUserTracker;
    private TextView mBatteryPercentView;
    private String mBatteryEstimate = null;

    private BatteryController mBatteryController;
    private SettingObserver mSettingObserver;
    private int mTextColor;
    private int mLevel;
    private boolean mForceShowPercent;
    private boolean mShowEstimate;
    private boolean mShowBatteryInQsb;

    private int mDarkModeSingleToneColor;
    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;

    private int mLightModeSingleToneColor;
    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;
    private float mDarkIntensity;
    private int mUser;

    private final Context mContext;
    private int mShowPercentOnQSB;

    /**
     * Whether we should use colors that adapt based on wallpaper/the scrim behind quick settings.
     */
    private boolean mUseWallpaperTextColors;

    private int mNonAdaptedSingleToneColor;
    private int mNonAdaptedForegroundColor;
    private int mNonAdaptedBackgroundColor;

    private int mShowBatteryPercent;
    private int mStyle = BatteryMeterDrawableBase.BATTERY_STYLE_PORTRAIT;
    private boolean mCharging;
    private int mTextChargingSymbol;

    private boolean misQsbHeader;
    private boolean mPowerSave;

    private int mPercentageStyleId;
    private int mPercentageSize;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.meter_background_color));
        mPercentageStyleId = atts.getResourceId(R.styleable.BatteryMeterView_textAppearance, 0);
        mPercentageSize = atts.getDimensionPixelSize(R.styleable.BatteryMeterView_textSize, 0);
        mDrawable = new ThemedBatteryDrawable(context, frameColor);
        atts.recycle();

        mSettingObserver = new SettingObserver(new Handler(context.getMainLooper()));

        addOnAttachStateChangeListener(
                new DisableStateTracker(DISABLE_NONE, DISABLE2_SYSTEM_ICONS));

        mSlotBattery = context.getString(
                com.android.internal.R.string.status_bar_battery);

        updateShowPercent();
        setColorsFromContext(context);
        // Init to not dark at all.
        onDarkChanged(new Rect(), 0, DarkIconDispatcher.DEFAULT_ICON_TINT);

        mUserTracker = new CurrentUserTracker(mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                mUser = newUserId;
                mSettingObserver.update();
                updateShowPercent();
            }
        };

        setClipChildren(false);
        setClipToPadding(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setForceShowPercent(boolean show) {
        mForceShowPercent = show;
        updateShowPercent();
    }

    /**
     * Sets whether the battery meter view uses the wallpaperTextColor. If we're not using it, we'll
     * revert back to dark-mode-based/tinted colors.
     *
     * @param shouldUseWallpaperTextColor whether we should use wallpaperTextColor for all
     *                                    components
     */
    public void useWallpaperTextColor(boolean shouldUseWallpaperTextColor) {
        if (shouldUseWallpaperTextColor == mUseWallpaperTextColors) {
            return;
        }

        mUseWallpaperTextColors = shouldUseWallpaperTextColor;

        if (mUseWallpaperTextColors) {
            updateColors(
                    Utils.getColorAttr(mContext, R.attr.wallpaperTextColor),
                    Utils.getColorAttr(mContext, R.attr.wallpaperTextColorSecondary),
                    Utils.getColorAttr(mContext, R.attr.wallpaperTextColor));
        } else {
            updateColors(mNonAdaptedForegroundColor, mNonAdaptedBackgroundColor, mNonAdaptedSingleToneColor);
        }
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }

        Context dualToneDarkTheme = new ContextThemeWrapper(context,
                Utils.getThemeAttr(context, R.attr.darkIconTheme));
        Context dualToneLightTheme = new ContextThemeWrapper(context,
                Utils.getThemeAttr(context, R.attr.lightIconTheme));
        mDarkModeSingleToneColor = Utils.getColorAttr(dualToneDarkTheme, R.attr.singleToneColor);
        mDarkModeBackgroundColor = Utils.getColorAttr(dualToneDarkTheme, R.attr.backgroundColor);
        mDarkModeFillColor = Utils.getColorAttr(dualToneDarkTheme, R.attr.fillColor);
        mLightModeSingleToneColor = Utils.getColorAttr(dualToneLightTheme, R.attr.singleToneColor);
        mLightModeBackgroundColor = Utils.getColorAttr(dualToneLightTheme, R.attr.backgroundColor);
        mLightModeFillColor = Utils.getColorAttr(dualToneLightTheme, R.attr.fillColor);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private boolean forcePercentageQsHeader() {
        if (mShowBatteryInQsb) {
            return ((misQsbHeader && !mShowEstimate) || mPowerSave)
                    && mShowPercentOnQSB == 1
                    && mShowBatteryPercent != 1;
        } else {
            return (misQsbHeader || mPowerSave) && mShowPercentOnQSB == 1
                    && mShowBatteryPercent != 1;
        }
    }

    private void loadImageView() {
        Resources res = getContext().getResources();
        mBatteryIconView = new ImageView(mContext);
        mBatteryIconView.setImageDrawable(mDrawable);
        MarginLayoutParams mlp;

        int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        int marginBottom = res.getDimensionPixelSize(R.dimen.battery_margin_bottom);

        if (isCircleBattery()) {
            batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_circle_battery_icon_height);
            batteryWidth = batteryHeight;
        }

        mlp = new MarginLayoutParams(batteryWidth, batteryHeight);
        mlp.setMargins(0, 0, 0, marginBottom);
        addView(mBatteryIconView, mlp);
        scaleBatteryMeterViews();
    }

    private void reloadImage() {
        final boolean showing = mBatteryIconView != null;
        if (showing) {
            removeView(mBatteryIconView);
            mBatteryIconView = null;
        }
        updateShowImage();
    }

    private void updateShowImage() {
        final boolean showing = mBatteryIconView != null;
        if (mStyle != BatteryMeterDrawableBase.BATTERY_STYLE_HIDDEN &&
                mStyle != BatteryMeterDrawableBase.BATTERY_STYLE_TEXT) {
            if (!showing) {
                loadImageView();
            }
        } else {
            if (showing) {
                removeView(mBatteryIconView);
                mBatteryIconView = null;
            }
        }
        updateVisibility();
    }

    private void updateVisibility() {
        if (mStyle == BatteryMeterDrawableBase.BATTERY_STYLE_HIDDEN && !mForceShowPercent) {
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBatteryController = Dependency.get(BatteryController.class);
        mBatteryController.addCallback(this);
        mUser = ActivityManager.getCurrentUser();
        mSettingObserver.observe();
        mSettingObserver.update();
        updateShowPercent();
        Dependency.get(ConfigurationController.class).addCallback(this);
        mUserTracker.startTracking();
        mSettingObserver.observe();
        mSettingObserver.update();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mUserTracker.stopTracking();
        mBatteryController.removeCallback(this);
        Dependency.get(ConfigurationController.class).removeCallback(this);
        mSettingObserver.unobserve();
        mSettingObserver = null;
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        if (mCharging != pluggedIn) {
            mCharging = pluggedIn;
            mDrawable.setCharging(mCharging);
            if (forcePercentageQsHeader()) {
                setForceShowPercent(true);
            } else {
                setForceShowPercent(mCharging);
            }
        }
        mDrawable.setBatteryLevel(level);
        mDrawable.setCharging(pluggedIn);
        mLevel = level;
        updatePercentText();
        setContentDescription(
                getContext().getString(charging ? R.string.accessibility_battery_level_charging
                        : R.string.accessibility_battery_level, level));
    }

    private boolean isCircleBattery() {
        return mStyle == BatteryMeterDrawableBase.BATTERY_STYLE_CIRCLE
                || mStyle == BatteryMeterDrawableBase.BATTERY_STYLE_DOTTED_CIRCLE;
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        mDrawable.setPowerSaveEnabled(isPowerSave);
        if (mPowerSave != isPowerSave) {
            mPowerSave = isPowerSave;
            updateShowPercent();
        }
    }

    private TextView loadPercentView() {
        return (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.battery_percentage_view, null);
    }

    private void updatePercentText() {
        if (mBatteryController != null && mBatteryPercentView != null) {
            if (!mShowEstimate || mCharging || (!misQsbHeader && mShowBatteryPercent == 1)) {
                setPercentTextAtCurrentLevel();
            } else {
                mBatteryController.getEstimatedTimeRemainingString(this::onEstimateFetchComplete);
            }
        }
    }

    private void updatePercentSize() {
        if (mPercentageSize != 0) {
            mBatteryPercentView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPercentageSize);
        }
    }

    private void onEstimateFetchComplete(String estimate) {
        if (estimate != null) {
            mBatteryPercentView.setText(estimate+" "); //todo: add proper padding
        } else {
            setPercentTextAtCurrentLevel();
        }
    }

    private void setPercentTextAtCurrentLevel() {
        String pct = NumberFormat.getPercentInstance().format(mLevel / 100f);

        if (mCharging && mStyle == BatteryMeterDrawableBase.BATTERY_STYLE_TEXT
                && mTextChargingSymbol > 0) {
            switch (mTextChargingSymbol) {
                case 1:
                default:
                    pct = "⚡️ " + pct;
                   break;
                case 2:
                    pct = "~ " + pct;
                    break;
            }
        }

        if (mBatteryIconView != null) pct = pct + " ";

        mBatteryPercentView.setText(pct);
    }

    public void updateShowPercent() {
        final boolean showing = mBatteryPercentView != null;
        boolean mShow = mForceShowPercent;

        if (mShowBatteryPercent == 1 /*percentage_text_next*/ || mStyle == BatteryMeterDrawableBase.BATTERY_STYLE_TEXT || mForceShowPercent || (mShowEstimate && misQsbHeader)) {
                mShow = true;      
        } else if (mShowBatteryPercent == 2 /*percentage_default*/) {
                mShow = false;
        }

        if (mShow) {
            if (!showing) {
                mBatteryPercentView = loadPercentView();
                addView(mBatteryPercentView,
                        new ViewGroup.LayoutParams(
                                LayoutParams.WRAP_CONTENT,
                                LayoutParams.MATCH_PARENT));
                reloadImage();
            }
            if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
            if (mPercentageStyleId != 0) mBatteryPercentView.setTextAppearance(mPercentageStyleId);
            updatePercentText();
        } else {
            if (showing) {
                removeView(mBatteryPercentView);
                mBatteryPercentView = null;
            }
        }
        mDrawable.setShowPercent(!mCharging && !mShow && mShowBatteryPercent != 2);
        mDrawable.refresh();
    }

    private void createPercentView() {
        if (mBatteryPercentView == null) {
            mBatteryPercentView = loadPercentView();
            if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
            updatePercentText();
            addView(mBatteryPercentView,
                    new ViewGroup.LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.MATCH_PARENT));
            return;
        }
        updatePercentText();
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        scaleBatteryMeterViews();
    }

    /**
     * Looks up the scale factor for status bar icons and scales the battery view by that amount.
     */
    private void scaleBatteryMeterViews() {
        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();

        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();

        int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        int marginBottom = res.getDimensionPixelSize(R.dimen.battery_margin_bottom);

        if (isCircleBattery()) {
            batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_circle_battery_icon_height);
            batteryWidth = batteryHeight;
        }

        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
        scaledLayoutParams.setMargins(0, 0, 0, marginBottom);

        if (mBatteryIconView != null) {
            mBatteryIconView.post(() -> mBatteryIconView.setLayoutParams(scaledLayoutParams));
        }
        if (mBatteryPercentView != null) {
            FontSizeUtils.updateFontSize(mBatteryPercentView, R.dimen.qs_time_expanded_size);
        }
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        mDarkIntensity = darkIntensity;

        float intensity = DarkIconDispatcher.isInArea(area, this) ? darkIntensity : 0;
        mNonAdaptedSingleToneColor = getColorForDarkIntensity(
                intensity, mLightModeSingleToneColor, mDarkModeSingleToneColor);
        mNonAdaptedForegroundColor = getColorForDarkIntensity(
                intensity, mLightModeFillColor, mDarkModeFillColor);
        mNonAdaptedBackgroundColor = getColorForDarkIntensity(
                intensity, mLightModeBackgroundColor,mDarkModeBackgroundColor);

        if (!mUseWallpaperTextColors) {
            updateColors(mNonAdaptedForegroundColor, mNonAdaptedBackgroundColor, mNonAdaptedSingleToneColor);
        }
    }

    private void updateColors(int foregroundColor, int backgroundColor, int singleToneColor) {
        mDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mTextColor = singleToneColor;
        if (mBatteryPercentView != null) {
            mBatteryPercentView.setTextColor(mTextColor);
        }
    }

    public void setFillColor(int color) {
        if (mLightModeFillColor == color) {
            return;
        }
        mLightModeFillColor = color;
        onDarkChanged(new Rect(), mDarkIntensity, DarkIconDispatcher.DEFAULT_ICON_TINT);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }

    public void setShowEstimate(boolean showEstimate) {
        mShowEstimate = showEstimate;
    }

    @Override
    public void onViewAdded(View child) {
        if (child == mBatteryPercentView) {
            post(() -> updatePercentSize());
        }
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STYLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SHOW_BATTERY_PERCENT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.TEXT_CHARGING_SYMBOL),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SHOW_BATTERY_PERCENT_ON_QSB),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SHOW_BATTERY_ESTIMATE),
 	            false, this, UserHandle.USER_CURRENT);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SHOW_BATTERY_INDICATOR_IN_QS),
 	            false, this, UserHandle.USER_CURRENT);
	    update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri.equals(Settings.System.getUriFor(
                Settings.System.SHOW_BATTERY_ESTIMATE))) {
	        setShowEstimate();
            } else if (uri.equals(Settings.System.getUriFor(
                Settings.System.SHOW_BATTERY_INDICATOR_IN_QS))) {
                setShowBatteryInQsb();
            }
	    update();
        }

        protected void unobserve() {
            getContext().getContentResolver().unregisterContentObserver(this);
        }

        public void update() {
            ContentResolver resolver = mContext.getContentResolver();
            mShowBatteryPercent = Settings.System.getIntForUser(resolver,
                SHOW_BATTERY_PERCENT, 0, mUser);
            mStyle = Settings.System.getIntForUser(resolver,
                STATUS_BAR_BATTERY_STYLE, BatteryMeterDrawableBase.BATTERY_STYLE_PORTRAIT, mUser);
            mTextChargingSymbol = Settings.System.getIntForUser(resolver,
                TEXT_CHARGING_SYMBOL, 0, mUser);
            mShowPercentOnQSB = Settings.System.getIntForUser(resolver,
                SHOW_BATTERY_PERCENT_ON_QSB, 1, mUser);
	    setShowEstimate();
            setShowBatteryInQsb();
            updateBatteryStyle();
            mDrawable.refresh();
        }
    }

    public void isQsbHeader() {
        misQsbHeader = true;
    }

    public void setShowEstimate() {
        mShowEstimate = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SHOW_BATTERY_ESTIMATE, 0, UserHandle.USER_CURRENT) == 1;
    }

    public void setShowBatteryInQsb() {
        mShowBatteryInQsb = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SHOW_BATTERY_INDICATOR_IN_QS, 1, UserHandle.USER_CURRENT) == 1;
    }

    public void updateBatteryStyle() {
        final int style = mStyle;

        switch (style) {
            case BatteryMeterDrawableBase.BATTERY_STYLE_HIDDEN:
                if (mBatteryIconView != null) {
                    removeView(mBatteryIconView);
                    mBatteryIconView = null;
                    removeView(mBatteryPercentView);
                    mBatteryPercentView = null;
                }
                break;
            case BatteryMeterDrawableBase.BATTERY_STYLE_TEXT:
                if (mBatteryIconView != null) {
                    removeView(mBatteryIconView);
                    mBatteryIconView = null;
                }
                break;
            default:
                mDrawable.setMeterStyle(style);
                if (mBatteryIconView == null) {
                    mBatteryIconView = new ImageView(mContext);
                    mBatteryIconView.setImageDrawable(mDrawable);
                    final MarginLayoutParams mlp = new MarginLayoutParams(
                            getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_width),
                            getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height));
                    mlp.setMargins(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
                    addView(mBatteryIconView, mlp);
                }
                break;
        }
        updateVisibility();
        if (forcePercentageQsHeader()
                || ((isCircleBattery() || style == BatteryMeterDrawableBase.BATTERY_STYLE_PORTRAIT) && mCharging)) {
            mForceShowPercent = true;
        } else {
            mForceShowPercent = false;
        }
        updateShowPercent();
        onDensityOrFontScaleChanged();
    }
}
