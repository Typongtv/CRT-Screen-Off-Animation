package com.typongtv.crtscreenoff

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalConfiguration

class MainActivity : ComponentActivity() {

    companion object {
        internal var hasShownSplash = false

        internal const val DISCHARGE_AGSL = """
            uniform shader image;
            uniform float chromaticOffset;

            vec4 main(vec2 fragCoord) {
                if (chromaticOffset < 0.001) {
                    return image.eval(fragCoord);
                }
                vec4 r = image.eval(fragCoord - vec2(chromaticOffset, 0.0));
                vec4 g = image.eval(fragCoord);
                vec4 b = image.eval(fragCoord + vec2(chromaticOffset, 0.0));
                return vec4(r.r, g.g, b.b, max(max(r.a, g.a), b.a));
            }
        """
    }

    override fun onDestroy() {
        super.onDestroy()
        hasShownSplash = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock phones to portrait for ergonomic dashboard layout, allow tablets/foldables to freely autorotate
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        if (!isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        val prefs = getSharedPreferences("crt_prefs", Context.MODE_PRIVATE)

        setContent {
            val configuration = LocalConfiguration.current
            val isLandscapeTablet = configuration.screenWidthDp >= 840 &&
                    configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isLandscapeTablet) {
                TabletDashboard(
                    prefs = prefs,
                    onMoveTaskToBack = { moveTaskToBack(true) }
                )
            } else {
                PhoneDashboard(
                    prefs = prefs,
                    onMoveTaskToBack = { moveTaskToBack(true) }
                )
            }
        }
    }
}
