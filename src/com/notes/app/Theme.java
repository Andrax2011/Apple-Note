package com.notes.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

public class Theme {

    public final boolean dark;
    public final int bg;
    public final int card;
    public final int searchBg;
    public final int barBg;
    public final int divider;
    public final int textPrimary;
    public final int textSecondary;
    public final int textHint;
    public final int accent;
    public final int danger;
    public final int ripple;

    private Theme(boolean dark) {
        this.dark = dark;
        if (dark) {
            bg            = Color.parseColor("#000000");
            card          = Color.parseColor("#1C1C1E");
            searchBg      = Color.parseColor("#1C1C1E");
            barBg         = Color.parseColor("#121214");
            divider       = Color.parseColor("#2C2C2E");
            textPrimary   = Color.parseColor("#FFFFFF");
            textSecondary = Color.parseColor("#8E8E93");
            textHint      = Color.parseColor("#636366");
            accent        = Color.parseColor("#FFD60A");
            danger        = Color.parseColor("#FF453A");
            ripple        = Color.parseColor("#28FFFFFF");
        } else {
            bg            = Color.parseColor("#F2F2F7");
            card          = Color.parseColor("#FFFFFF");
            searchBg      = Color.parseColor("#E5E5EA");
            barBg         = Color.parseColor("#F9F9FB");
            divider       = Color.parseColor("#DCDCE0");
            textPrimary   = Color.parseColor("#000000");
            textSecondary = Color.parseColor("#6C6C70");
            textHint      = Color.parseColor("#8E8E93");
            accent        = Color.parseColor("#C98A00");
            danger        = Color.parseColor("#FF3B30");
            ripple        = Color.parseColor("#18000000");
        }
    }

    public static Theme of(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("notes_ui", Context.MODE_PRIVATE);
        int mode = prefs.getInt("theme_mode", 0);
        boolean isDark;
        if (mode == 1) {
            isDark = true;
        } else if (mode == 2) {
            isDark = false;
        } else {
            int night = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isDark = (night == Configuration.UI_MODE_NIGHT_YES);
        }
        return new Theme(isDark);
    }

    public static void toggle(Context ctx) {
        boolean currentlyDark = of(ctx).dark;
        ctx.getSharedPreferences("notes_ui", Context.MODE_PRIVATE)
                .edit()
                .putInt("theme_mode", currentlyDark ? 2 : 1)
                .apply();
    }

    @SuppressWarnings("deprecation")
    public void applySystemBars(Activity activity) {
        Window w = activity.getWindow();
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(barBg);

        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController ctrl = w.getInsetsController();
            if (ctrl != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                ctrl.setSystemBarsAppearance(dark ? 0 : mask, mask);
            }
        } else {
            int flags = 0;
            if (!dark) {
                flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }

    public int dialogTheme() {
        return dark
                ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert;
    }

    public static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radiusPx);
        return gd;
    }

    public static Drawable rippleRounded(int color, int rippleColor, float radiusPx) {
        GradientDrawable content = rounded(color, radiusPx);
        GradientDrawable mask = rounded(Color.WHITE, radiusPx);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }
}
