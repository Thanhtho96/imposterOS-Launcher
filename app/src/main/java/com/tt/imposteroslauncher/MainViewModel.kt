package com.tt.imposteroslauncher

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.tt.imposteroslauncher.model.AppInfo
import com.tt.imposteroslauncher.model.SeatType

@Suppress("Unused")
private const val TAG = "MainViewModel"

class MainViewModel : AndroidViewModel(App.instance) {
    companion object {
        private const val INVALID_INDEX = -1
    }

    private val context = getApplication<App>()

    var boxState by mutableStateOf(BoxState.Collapsed)
        private set

    var chosenApp by mutableStateOf<AppInfo?>(null)
        private set

    val listApp = mutableStateListOf<AppInfo>()

    init {
        loadApps()
    }

    private fun loadApps() {
        val pm = context.packageManager

        val allApps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            0
        ).sortedBy { it.loadLabel(pm).toString() }

        listApp.addAll(
            allApps.mapIndexed { index, it ->
                AppInfo(
                    label = it.loadLabel(pm).trim(),
                    packageName = it.activityInfo.packageName,
                    icon = it.activityInfo.loadIcon(pm),
                    seatType = when {
                        index <= MainActivity.HOT_SEAT_NUM - 1 -> SeatType.HOT
                        else -> SeatType.NORMAL
                    }
                )
            }
        )
    }

    fun updateBoxState(boxState: BoxState) {
        this.boxState = boxState
    }

    fun updateChosenApp(chosenApp: AppInfo? = null) {
        this.chosenApp = chosenApp
    }

    fun updateListOffset(appInfo: AppInfo, offset: Offset) {
        val index = listApp.indexOfFirst { it.packageName == appInfo.packageName }
        if (index == INVALID_INDEX) return

        listApp[index] = appInfo.copy(offset = offset)
    }
}