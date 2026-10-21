package com.markobl.calllogsync.slice

import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/** Native content remains readable under system bars, cutouts and the IME. */
internal class ActivityInsets private constructor() {
    companion object {
        @JvmStatic
        @Suppress("DEPRECATION")
        fun apply(root: View?) {
            val left = root!!.paddingLeft
            val top = root.paddingTop
            val right = root.paddingRight
            val bottom = root.paddingBottom
            root.setOnApplyWindowInsetsListener { view, insets ->
                val light =
                    (view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                        Configuration.UI_MODE_NIGHT_YES
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars =
                        insets.getInsets(
                            WindowInsets.Type.systemBars() or
                                WindowInsets.Type.displayCutout() or
                                WindowInsets.Type.ime()
                        )
                    view.setPadding(
                        left + bars.left,
                        top + bars.top,
                        right + bars.right,
                        bottom + bars.bottom,
                    )
                    val controller = view.windowInsetsController
                    if (controller != null) {
                        val appearance =
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                        controller.setSystemBarsAppearance(if (light) appearance else 0, appearance)
                    }
                } else {
                    view.setPadding(
                        left + insets.systemWindowInsetLeft,
                        top + insets.systemWindowInsetTop,
                        right + insets.systemWindowInsetRight,
                        bottom + insets.systemWindowInsetBottom,
                    )
                    val appearance =
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    view.systemUiVisibility =
                        if (light) view.systemUiVisibility or appearance
                        else view.systemUiVisibility and appearance.inv()
                }
                insets
            }
            root.requestApplyInsets()
        }
    }
}
