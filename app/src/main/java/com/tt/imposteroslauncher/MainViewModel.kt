package com.tt.imposteroslauncher

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.tt.imposteroslauncher.model.AppInfo
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel : AndroidViewModel(App.instance) {
    private val context = getApplication<App>()

    var boxState by mutableStateOf(BoxState.Collapsed)
        private set

    var chosenPos by mutableStateOf(-1)
        private set

    var isBlockClick by mutableStateOf(AtomicBoolean(false))
        private set

    val listApp = mutableStateListOf<AppInfo>()

    val listOffset = mutableStateListOf<Offset>()

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
        )

        listApp.addAll(
            allApps.map {
                AppInfo(
                    label = it.loadLabel(pm).trim(),
                    packageName = it.activityInfo.packageName,
                    icon = it.activityInfo.loadIcon(pm)
                )
            }
                .sortedBy { it.label.toString() }
        )

        listOffset.addAll(
            allApps.map {
                Offset.Zero
            }
        )
    }

    fun updateBoxState(
        boxState: BoxState? = null,
        chosenPos: Int? = null,
        isBlockClick: Boolean? = null
    ) {
        if (boxState != null) {
            this.boxState = boxState
        }
        if (chosenPos != null) {
            this.chosenPos = chosenPos
        }
        if (isBlockClick != null) {
            this.isBlockClick.set(isBlockClick)
        }
    }

    fun updateListOffset(index: Int, offset: Offset) {
        listOffset[index] = offset
    }
}