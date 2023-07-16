package imposter.os.launcher.util

import androidx.compose.runtime.Composable

@Composable
fun gridSizeX() = (screenWidthInPx() / 4).toDp()

@Composable
fun gridSizeY() = (screenHeightInPx() / 4).toDp()