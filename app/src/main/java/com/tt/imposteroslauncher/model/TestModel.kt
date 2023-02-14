package com.tt.imposteroslauncher.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TestModel(@PrimaryKey val id: Int)

@Immutable
@Stable
data class AppInfo(
    val label: CharSequence,
    val packageName: CharSequence,
    val icon: Drawable
)