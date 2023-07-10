package com.tt.imposteroslauncher

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
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
import com.tt.imposteroslauncher.model.SeatType
import com.tt.imposteroslauncher.ui.theme.imposterOSLauncherTheme
import com.tt.imposteroslauncher.util.gridSizeX
import com.tt.imposteroslauncher.util.roundedCornerHotSeat
import com.tt.imposteroslauncher.util.screenHeightInPx
import com.tt.imposteroslauncher.util.screenWidthInPx
import com.tt.imposteroslauncher.util.toPx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun centerX() = screenWidthInPx() / 2f - (gridSizeX().toPx() / 4f)

@Composable
fun centerY() = screenHeightInPx() / 2f - (gridSizeX().toPx() / 2f)

@Composable
fun iconSize() = (gridSizeX() - 47.dp).toPx().toInt()

@Suppress("Unused")
private const val TAG = "MainActivity"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {
    companion object {
        private const val COLUMN_ITEM_COUNT = 4
        const val HOT_SEAT_NUM = COLUMN_ITEM_COUNT
        val hotSeatPadding = 10.dp
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val viewModel = koinViewModel<MainViewModel>()
            val listAllApp = viewModel.listApp.filter { it.seatType != SeatType.BLANK }
            val listNormalApp = viewModel.listApp.filter { it.seatType == SeatType.NORMAL }
            val listHotSeat = viewModel.listApp.filter { it.seatType == SeatType.HOT }
            val systemUiController = rememberSystemUiController()
            val pagerState = rememberPagerState()
            var pageCount by remember { mutableStateOf(0) }
            var iconPerPage by remember { mutableStateOf(0) }

            SideEffect {
                // Update all of the system bar colors to be transparent, and use
                // dark icons if we're in light theme
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = false
                )
            }

            val gridSize = gridSizeX().toPx()

            imposterOSLauncherTheme {
                BackHandler(enabled = true) {

                }

                val transition = updateTransition(viewModel.boxState, label = "")

                val center = Pair(
                    first = centerX(),
                    second = centerY()
                )

                LaunchedEffect(transition.currentState) {
                    if (transition.isRunning) return@LaunchedEffect
                    when (transition.currentState) {
                        BoxState.Collapsed -> {
                            viewModel.updateChosenApp(null)
                        }

                        BoxState.Expanded -> {
                            val packageName =
                                viewModel.chosenApp?.packageName?.toString()
                                    ?: return@LaunchedEffect

                            val launchIntent =
                                context.packageManager.getLaunchIntentForPackage(packageName)
                                    ?: return@LaunchedEffect
                            context.startActivity(
                                launchIntent.apply {
                                    flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                                }
                            )
                        }
                    }
                }

                val blur by transition.animateDp(
                    transitionSpec = transition(),
                    label = ""
                ) { state ->
                    when (state) {
                        BoxState.Collapsed -> 0.dp
                        BoxState.Expanded -> 7.dp
                    }
                }

                val alpha by transition.animateFloat(
                    transitionSpec = transition(),
                    label = ""
                ) { state ->
                    when (state) {
                        BoxState.Collapsed -> 1f
                        BoxState.Expanded -> 0.3f
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

                Scaffold(
                    topBar = {
                        Spacer(modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues()))
                    },
                    bottomBar = {
                        LazyVerticalGrid(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(hotSeatPadding)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.3f),
                                        cornerRadius = CornerRadius(
                                            roundedCornerHotSeat(
                                                padding = hotSeatPadding
                                                    .toPx()
                                                    .roundToInt(),
                                                insets = window.decorView.rootWindowInsets
                                            ).toFloat()
                                        )
                                    )
                                },
                            columns = GridCells.Fixed(COLUMN_ITEM_COUNT),
                            userScrollEnabled = false,
                            content = {
                                items(
                                    items = listHotSeat,
                                    key = { it.packageName }
                                ) {
                                    Greeting(appInfo = it)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blur)
                        .graphicsLayer {
                            this.alpha = alpha
                        },
                    backgroundColor = Color.Transparent,
                    content = { paddingValues ->
                        HorizontalPager(
                            pageCount = pageCount,
                            state = pagerState,
                            userScrollEnabled = viewModel.chosenApp == null,
                            contentPadding = PaddingValues(horizontal = hotSeatPadding),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = paddingValues.calculateTopPadding(),
                                    bottom = paddingValues.calculateBottomPadding()
                                )
                                .onGloballyPositioned { layoutCoordinates ->
                                    if (layoutCoordinates.isAttached.not() || pageCount != 0) {
                                        return@onGloballyPositioned
                                    }

                                    val rowItemCount =
                                        (layoutCoordinates.size.height / gridSize).toInt()
                                    iconPerPage = rowItemCount * COLUMN_ITEM_COUNT
                                    val pageSize = listNormalApp.size.toFloat() / iconPerPage
                                    pageCount = pageSize.toInt() + if (pageSize % 1 == 0f) 0 else 1
                                }
                        ) { page ->
                            if (pageCount == 0) return@HorizontalPager

                            val startIndex = page * iconPerPage
                            val endIndex = min((page + 1) * iconPerPage, listNormalApp.size)

                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Fixed(COLUMN_ITEM_COUNT),
                                userScrollEnabled = false,
                                content = {
                                    items(
                                        items = listNormalApp.subList(startIndex, endIndex),
                                        key = { it.packageName }
                                    ) {
                                        Greeting(appInfo = it)
                                    }
                                }
                            )
                        }
                    }
                )

                viewModel.listApp.forEach { appInfo ->
                    val offsetAnim by transition.animateOffset(
                        transitionSpec = transition(),
                        label = ""
                    ) { state ->
                        when (state) {
                            BoxState.Collapsed -> appInfo.offset
                            BoxState.Expanded -> Offset(
                                x = center.first,
                                y = center.second
                            )
                        }
                    }

                    if (viewModel.chosenApp?.packageName == appInfo.packageName) {
                        Image(
                            modifier = Modifier
                                .graphicsLayer {
                                    this.scaleX = scaleIcon
                                    this.scaleY = scaleIcon
                                    this.translationX = offsetAnim.x
                                    this.translationY = offsetAnim.y
                                },
                            bitmap = appInfo.icon.toBitmap(
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
    appInfo: AppInfo,
    viewModel: MainViewModel = koinViewModel()
) {
    val interactionSource = remember { MutableInteractionSource() }
    var alpha by remember { mutableStateOf(1f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleScope = rememberCoroutineScope()
    val screenWidth = screenWidthInPx() - (MainActivity.hotSeatPadding * 2).toPx()

    BackHandler(enabled = viewModel.boxState == BoxState.Expanded) {
        viewModel.updateBoxState(BoxState.Collapsed)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch {
                    delay(300)
                    viewModel.updateBoxState(BoxState.Collapsed)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (viewModel.chosenApp == null) alpha = 1F

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (viewModel.chosenApp != null) return@clickable

                viewModel.updateChosenApp(chosenApp = appInfo)
                viewModel.updateBoxState(BoxState.Expanded)
                alpha = 0f
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
                    .graphicsLayer {
                        this.alpha = alpha
                    }
                    .onGloballyPositioned { layoutCoordinates ->
                        if (layoutCoordinates.isAttached && appInfo.offset == Offset.Zero) {
                            viewModel.updateListOffset(
                                appInfo,
                                if (layoutCoordinates.positionInWindow().x < screenWidth) {
                                    layoutCoordinates.positionInWindow()
                                } else {
                                    layoutCoordinates
                                        .positionInWindow()
                                        .copy(x = layoutCoordinates.positionInWindow().x - screenWidth)
                                }
                            )
                        }
                    },
                bitmap = appInfo.icon.toBitmap(
                    width = iconSize(),
                    height = iconSize()
                ).asImageBitmap(),
                contentDescription = null
            )
            if (appInfo.seatType != SeatType.NORMAL) return@Box
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
        spring(dampingRatio = 0.9F, stiffness = 300F)
    }

enum class BoxState {
    Collapsed,
    Expanded
}