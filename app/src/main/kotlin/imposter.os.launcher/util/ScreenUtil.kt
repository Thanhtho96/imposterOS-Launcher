package imposter.os.launcher.util

import android.content.res.Resources
import android.os.Build
import android.view.RoundedCorner
import androidx.annotation.Px
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun screenWidthInPx() =
    with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx() +
                WindowInsets.systemBars.getLeft(this, LayoutDirection.Ltr) +
                WindowInsets.systemBars.getRight(this, LayoutDirection.Ltr)
    }

@Composable
fun screenHeightInPx() =
    with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx() +
                WindowInsets.systemBars.getTop(this) +
                WindowInsets.systemBars.getBottom(this)
    }

@Composable
fun Dp.toPx() = with(LocalDensity.current) { this@toPx.toPx() }

@Composable
fun Int.toPx() = with(LocalDensity.current) { this@toPx.dp.toPx() }

@Composable
fun Int.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

@Composable
fun Float.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

val Float.toPx get() = this * Resources.getSystem().displayMetrics.density

val Float.toDp get() = this / Resources.getSystem().displayMetrics.density

val Int.toPx get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.toDp get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun roundedCornerHotSeat(@Px padding: Int, insets: android.view.WindowInsets): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
        val bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)

        val bottomRadius = max(bottomLeft?.radius ?: 0, bottomRight?.radius ?: 0)

        if (bottomRadius == 0) {
            return 27.toPx
        }

        return bottomRadius + padding
    } else {
        27.toPx
    }
}
