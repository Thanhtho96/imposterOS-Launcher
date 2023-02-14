package com.tt.imposteroslauncher

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tt.imposteroslauncher.model.AppInfo
import com.tt.imposteroslauncher.ui.theme.imposterOSLauncherTheme
import com.tt.imposteroslauncher.util.gridSizeX
import com.tt.imposteroslauncher.util.screenHeightInPx
import com.tt.imposteroslauncher.util.screenWidthInPx
import com.tt.imposteroslauncher.util.toPx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun centerX() = screenWidthInPx() / 2f - (gridSizeX().toPx() / 4f)

@Composable
fun centerY() = screenHeightInPx() / 2f - (gridSizeX().toPx() / 2f)

@Composable
fun iconSize() = (gridSizeX() - 47.dp).toPx().toInt()

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {
    companion object {
        @Suppress("Unused")
        const val TAG = "MainActivity"
        private const val INVALID_NUM = -1
        private const val ROW_ITEM_COUNT = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            val listApp = viewModel.listApp
            val listOffset = viewModel.listOffset
            val systemUiController = rememberSystemUiController()

            SideEffect {
                // Update all of the system bar colors to be transparent, and use
                // dark icons if we're in light theme
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = false
                )
            }

            imposterOSLauncherTheme {
                BackHandler(enabled = true) {

                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = Color.Transparent,
                    content = {
                        val transition = updateTransition(viewModel.boxState, label = "")

                        val blur by transition.animateDp(
                            transitionSpec = transition(),
                            label = ""
                        ) { state ->
                            when (state) {
                                BoxState.Collapsed -> 0.dp
                                BoxState.Expanded -> 7.dp
                            }
                        }

                        LazyVerticalGrid(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(blur),
                            columns = GridCells.Fixed(ROW_ITEM_COUNT),
                            content = {
                                items(ROW_ITEM_COUNT) {
                                    Spacer(Modifier.statusBarsPadding())
                                }
                                itemsIndexed(
                                    items = listApp,
                                    key = { _, appInfo -> appInfo.packageName }) { index, appInfo ->
                                    Greeting(
                                        index = index,
                                        appInfo = appInfo
                                    )
                                }
                                items(ROW_ITEM_COUNT) {
                                    Spacer(Modifier.navigationBarsPadding())
                                }
                            }
                        )
                    }
                )

                BoxWithConstraints {
                    val center = Pair(
                        first = centerX(),
                        second = centerY()
                    )
                    val transition = updateTransition(viewModel.boxState, label = "")

                    LaunchedEffect(transition.currentState) {
                        if (transition.isRunning) return@LaunchedEffect
                        when (transition.currentState) {
                            BoxState.Collapsed -> {
                                viewModel.updateBoxState(
                                    chosenPos = INVALID_NUM,
                                    isBlockClick = false
                                )
                            }

                            BoxState.Expanded -> {
                            }
                        }
                    }

                    val scaleIcon by transition.animateFloat(
                        transitionSpec = transition(),
                        label = ""
                    ) { state ->
                        when (state) {
                            BoxState.Collapsed -> 1f
                            BoxState.Expanded -> screenWidthInPx() / 2.7f / iconSize()
                        }
                    }

                    listOffset.forEachIndexed { index, offset ->
                        val offsetAnim by transition.animateOffset(
                            transitionSpec = transition(),
                            label = ""
                        ) { state ->
                            when (state) {
                                BoxState.Collapsed -> offset
                                BoxState.Expanded -> Offset(
                                    x = center.first,
                                    y = center.second
                                )
                            }
                        }

                        if (viewModel.chosenPos != index) return@forEachIndexed

                        Image(
                            modifier = Modifier
                                .graphicsLayer {
                                    this.scaleX = scaleIcon
                                    this.scaleY = scaleIcon
                                    this.translationX = offsetAnim.x
                                    this.translationY = offsetAnim.y
                                },
                            bitmap = listApp[index].icon.toBitmap(
                                width = iconSize(),
                                height = iconSize()
                            ).asImageBitmap(),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(
    index: Int,
    appInfo: AppInfo,
    viewModel: MainViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    var currentState by remember(appInfo) { mutableStateOf(BoxState.Collapsed) }
    val transition = updateTransition(currentState, label = "")
    var alpha by remember { mutableStateOf(1f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleScope = rememberCoroutineScope()

    BackHandler(enabled = currentState == BoxState.Expanded) {
        viewModel.updateBoxState(boxState = BoxState.Collapsed)
        currentState = BoxState.Collapsed
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch {
                    delay(300)
                    viewModel.updateBoxState(boxState = BoxState.Collapsed)
                    currentState = BoxState.Collapsed
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(transition.currentState) {
        if (transition.isRunning) return@LaunchedEffect
        when (transition.currentState) {
            BoxState.Collapsed -> {
                alpha = 1F
            }

            BoxState.Expanded -> {
                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(appInfo.packageName.toString())
                        ?: return@LaunchedEffect
                context.startActivity(
                    launchIntent.apply {
                        flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    }
                )
            }
        }
    }

    @Suppress("Unused")
    val zIndex by transition.animateFloat(transitionSpec = transition(), label = "") { state ->
        when (state) {
            BoxState.Collapsed -> 0f
            BoxState.Expanded -> 7f
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (viewModel.isBlockClick.get()) return@clickable

                alpha = 0f
                currentState = BoxState.Expanded
                viewModel.updateBoxState(BoxState.Expanded, chosenPos = index, isBlockClick = true)
            }
            .requiredSize(
                width = gridSizeX(),
                height = gridSizeX()
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .alpha(alpha)
                    .onGloballyPositioned { layoutCoordinates ->
                        if (
                            layoutCoordinates.isAttached &&
                            viewModel.listOffset[index] == Offset.Zero
                        ) {
                            viewModel.updateListOffset(
                                index,
                                layoutCoordinates.positionInWindow()
                            )
                        }
                    },
                bitmap = appInfo.icon.toBitmap(
                    width = iconSize(),
                    height = iconSize()
                ).asImageBitmap(),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                color = Color.White,
                fontSize = 14.sp,
                text = appInfo.label.toString(),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun <T> transition(): @Composable (Transition.Segment<BoxState>.() -> FiniteAnimationSpec<T>) =
    {
        spring(dampingRatio = 0.9F, stiffness = 200F)
    }

enum class BoxState {
    Collapsed,
    Expanded
}