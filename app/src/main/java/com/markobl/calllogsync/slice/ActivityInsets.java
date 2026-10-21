package com.markobl.calllogsync.slice;

import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

/** Keep native screen content and system icons readable under enforced edge-to-edge windows. */
final class ActivityInsets {
    private ActivityInsets() {}

    @SuppressWarnings("deprecation") // Legacy insets/system-bar appearance for supported API26–29.
    static void apply(View root) {
        final int left = root.getPaddingLeft(),
                top = root.getPaddingTop(),
                right = root.getPaddingRight(),
                bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener(
                (view, insets) -> {
                    boolean light =
                            (view.getResources().getConfiguration().uiMode
                                            & Configuration.UI_MODE_NIGHT_MASK)
                                    != Configuration.UI_MODE_NIGHT_YES;
                    if (Build.VERSION.SDK_INT >= 30) {
                        android.graphics.Insets bars =
                                insets.getInsets(
                                        WindowInsets.Type.systemBars()
                                                | WindowInsets.Type.displayCutout());
                        view.setPadding(
                                left + bars.left,
                                top + bars.top,
                                right + bars.right,
                                bottom + bars.bottom);
                        WindowInsetsController controller = view.getWindowInsetsController();
                        if (controller != null) {
                            int appearance =
                                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                            | WindowInsetsController
                                                    .APPEARANCE_LIGHT_NAVIGATION_BARS;
                            controller.setSystemBarsAppearance(light ? appearance : 0, appearance);
                        }
                    } else {
                        view.setPadding(
                                left + insets.getSystemWindowInsetLeft(),
                                top + insets.getSystemWindowInsetTop(),
                                right + insets.getSystemWindowInsetRight(),
                                bottom + insets.getSystemWindowInsetBottom());
                        int appearance =
                                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                        view.setSystemUiVisibility(
                                light
                                        ? view.getSystemUiVisibility() | appearance
                                        : view.getSystemUiVisibility() & ~appearance);
                    }
                    return insets;
                });
        root.requestApplyInsets();
    }
}
