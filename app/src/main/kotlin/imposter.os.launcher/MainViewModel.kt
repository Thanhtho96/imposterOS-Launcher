package imposter.os.launcher

import android.content.Intent
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import imposter.os.launcher.model.AppInfo
import imposter.os.launcher.model.SeatType
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress("Unused")
private const val TAG = "MainViewModel"

class MainViewModel : AndroidViewModel(App.instance) {
    companion object {
        private const val INVALID_INDEX = -1
        private const val HOT_SEAT_NUM = MainActivity.COLUMN_ITEM_COUNT
    }

    private val context = getApplication<App>()

    var chosenApp by mutableStateOf<AppInfo?>(null)
        private set

    val listApp = mutableStateListOf<AppInfo>()

    var isEditMode by mutableStateOf(false)
        private set

    var focusDragOffset by mutableStateOf(IntOffset.Zero)
        private set

    fun loadApps(iconPerPage: Int, iconSize: Int, pageInfo: (pageCount: Int) -> Unit) {
        val pm = context.packageManager
        val blankDrawable = ShapeDrawable(OvalShape()).apply {
//            paint.color = context.getColor(android.R.color.transparent)
        }.toBitmap(iconSize, iconSize).asImageBitmap()

        val allApps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            0
        )
//            .filterNot { it.activityInfo.packageName == context.packageName }
            .sortedBy { it.loadLabel(pm).toString() }

        val pageSize = (allApps.size - HOT_SEAT_NUM).toFloat() / iconPerPage
        val pageCount = pageSize.toInt() + if (pageSize % 1 == 0f) 0 else 1
        pageInfo.invoke(pageCount)

        val fillApp = pageCount * iconPerPage + HOT_SEAT_NUM

        listApp.addAll(
            (0 until fillApp).map { index ->
                val resolveInfo = allApps.getOrNull(index)

                val appInfo =
                    resolveInfo?.let {
                        AppInfo(
                            position = index,
                            label = it.loadLabel(pm).trim(),
                            packageName = it.activityInfo.packageName,
                            icon = it.activityInfo.loadIcon(pm)
                                .toBitmap(iconSize, iconSize)
                                .asImageBitmap(),
                        )
                    } ?: AppInfo(
                        position = index,
                        label = "",
                        packageName = "",
                        icon = blankDrawable,
                        seatType = SeatType.BLANK
                    )

                when {
                    resolveInfo == null -> {
                        appInfo
                    }

                    index <= HOT_SEAT_NUM - 1 -> {
                        appInfo.copy(seatType = SeatType.HOT)
                    }

                    else -> {
                        appInfo
                    }
                }
            }
        )
    }

    fun updateChosenApp(chosenApp: AppInfo? = null) {
        this.chosenApp = chosenApp?.let { listApp[it.position] }
    }

    fun updateListOffset(appInfo: AppInfo, offset: Offset) {
        listApp[appInfo.position] = appInfo.copy(offset = offset)
    }

    fun toggleEditMode() {
        if (chosenApp != null) return

        isEditMode = !isEditMode
    }

    fun updateEditMode(isEdit: Boolean) {
        isEditMode = isEdit
    }

    var newOffset = Offset.Zero

    fun updateFocusDragOffset(offset: Offset) {
        focusDragOffset += IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
    }

    fun checkNewPosition() {
        val closestApp =
            listApp.filter { it.seatType != SeatType.BLANK }
                .minByOrNull { abs(newOffset.getDistanceSquared() - it.offset.getDistanceSquared()) }
                ?: return

        Log.d(TAG, "newOffset: $newOffset")
        Log.d(TAG, "closestApp: $closestApp")
        Log.d(TAG, "chosenApp: $chosenApp")

        val i = closestApp.position
        val j = chosenApp?.position ?: INVALID_INDEX

        if (i == j || i == INVALID_INDEX || j == INVALID_INDEX) return

        listApp[i] = chosenApp!!.copy(
            position = i,
            seatType = closestApp.seatType,
            offset = closestApp.offset
        )

        listApp[j] = closestApp.copy(
            position = j,
            seatType = chosenApp!!.seatType,
            offset = chosenApp!!.offset
        )
    }

    fun resetFocusDragOffset() {
        focusDragOffset = IntOffset.Zero
        newOffset = Offset.Zero
    }
}