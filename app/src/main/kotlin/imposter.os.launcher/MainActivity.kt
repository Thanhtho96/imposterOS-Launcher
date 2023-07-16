package imposter.os.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import imposter.os.launcher.model.AppInfo
import imposter.os.launcher.model.SeatType
import imposter.os.launcher.ui.theme.imposterOSLauncherTheme
import imposter.os.launcher.util.gridSizeX
import imposter.os.launcher.util.roundedCornerHotSeat
import imposter.os.launcher.util.screenWidthInPx
import imposter.os.launcher.util.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun iconSize() = (gridSizeX() - 47.dp).toPx().toInt()

@Suppress("Unused")
private const val TAG = "MainActivity"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {
    companion object {
        const val COLUMN_ITEM_COUNT = 4
        val hotSeatPadding = 10.dp
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val viewModel = koinViewModel<MainViewModel>()
            val listHotSeat = viewModel.listApp.filter { it.seatType == SeatType.HOT }
            val listNormalApp = viewModel.listApp.filter { it.seatType != SeatType.HOT }
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
            val iconSize = iconSize()

            LaunchedEffect(listNormalApp) {
                listNormalApp
                    .filter { it.offset != Offset.Zero && it.seatType != SeatType.BLANK }
                    .forEach {
//                        Log.d(TAG, "appInfo: $it")
                    }
            }

            imposterOSLauncherTheme {
                BackHandler(enabled = true) {

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
                                }
                                .requiredHeight(
                                    height = gridSizeX()
                                ),
                            columns = GridCells.Fixed(COLUMN_ITEM_COUNT),
                            userScrollEnabled = false,
                            content = {
                                items(
                                    items = listHotSeat,
                                    key = { it.position }
                                ) {
                                    Greeting(appInfo = it, context = context)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(viewModel.isEditMode) {
                            if (viewModel.isEditMode) {
                                detectTapGestures(
                                    onTap = {
                                        if (viewModel.isEditMode) {
                                            viewModel.updateEditMode(false)
                                        }
                                    }
                                )
                            } else {
                                detectTapGestures(
                                    onLongPress = {
                                        viewModel.toggleEditMode()
                                    },
                                    onTap = {
                                        if (viewModel.isEditMode) {
                                            viewModel.updateEditMode(false)
                                        }
                                    }
                                )
                            }
                        },
                    backgroundColor = Color.Transparent,
                    content = { paddingValues ->
                        HorizontalPager(
                            pageCount = pageCount,
                            state = pagerState,
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

                                    viewModel.loadApps(iconPerPage, iconSize) {
                                        pageCount = it
                                    }
                                }
                        ) { page ->
                            if (pageCount == 0) return@HorizontalPager

                            val startIndex = page * iconPerPage
                            val endIndex = (page + 1) * iconPerPage

                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Fixed(COLUMN_ITEM_COUNT),
                                verticalArrangement = Arrangement.SpaceEvenly,
                                userScrollEnabled = false,
                                content = {
                                    items(
                                        items = listNormalApp.subList(startIndex, endIndex),
                                        key = { it.position }
                                    ) {
                                        Greeting(appInfo = it, context = context)
                                    }
                                }
                            )
                        }
                    }
                )

                viewModel.chosenApp?.let {
                    Image(
                        modifier = Modifier
                            .offset { viewModel.focusDragOffset }
                            .graphicsLayer {
                                this.translationX = it.offset.x
                                this.translationY = it.offset.y
                            }
                            .onGloballyPositioned { layoutCoordinates ->
                                if (layoutCoordinates.isAttached) {
                                    viewModel.newOffset = layoutCoordinates.positionInWindow()
                                }
                            },
                        bitmap = it.icon,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    appInfo: AppInfo,
    context: Context,
    viewModel: MainViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleScope = rememberCoroutineScope()
    var alpha by remember { mutableStateOf(1f) }
    val screenWidth = screenWidthInPx() - (MainActivity.hotSeatPadding * 2).toPx()
    var viewOffset by remember { mutableStateOf(Offset.Zero) }

    BackHandler(enabled = viewModel.isEditMode) {
        viewModel.updateEditMode(false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch {
                    viewModel.updateEditMode(false)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .shake(
                enabled = viewModel.isEditMode,
                scope = lifecycleScope
            )
            .pointerInput(viewModel.isEditMode) {
                if (viewModel.isEditMode) {
                    if (appInfo.seatType == SeatType.BLANK) return@pointerInput

                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            viewModel.updateChosenApp(appInfo)
                            alpha = 0F
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            viewModel.updateFocusDragOffset(dragAmount)
                        },
                        onDragEnd = {
                            viewModel.checkNewPosition()
                            viewModel.updateChosenApp(null)
                            viewModel.resetFocusDragOffset()
                            alpha = 1F
                        }
                    )
                } else {
                    detectTapGestures(
                        onTap = {
                            if (viewModel.isEditMode) return@detectTapGestures

                            val packageName = appInfo.packageName.toString()

                            val launchIntent =
                                context.packageManager.getLaunchIntentForPackage(packageName)
                                    ?: return@detectTapGestures

                            context.startActivity(
                                launchIntent,
                                Bundle().apply {
                                    putString("android:activity.packageName", packageName)
                                    putInt("android:activity.animStartX", viewOffset.x.toInt())
                                    putInt("android:activity.animStartY", viewOffset.y.toInt())
                                    putInt("android:activity.animWidth", appInfo.icon.width)
                                    putInt("android:activity.animHeight", appInfo.icon.height)
                                    putInt("android:activity.animType", 11)
                                }
                            )
                        },
                        onLongPress = {
                            viewModel.toggleEditMode()
                        }
                    )
                }
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
                            val appOffset =
                                if (layoutCoordinates.positionInWindow().x < screenWidth) {
                                    layoutCoordinates.positionInWindow()
                                } else {
                                    layoutCoordinates
                                        .positionInWindow()
                                        .copy(x = layoutCoordinates.positionInWindow().x - screenWidth)
                                }
                            viewModel.updateListOffset(
                                appInfo = appInfo,
                                offset = appOffset
                            )
                            viewOffset = appOffset
                        }
                    },
                bitmap = appInfo.icon,
                contentDescription = null
            )
            if (appInfo.seatType != SeatType.NORMAL) return@Box
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                    },
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

private fun Modifier.shake(
    enabled: Boolean,
    scope: CoroutineScope
) = composed(
    factory = {
        val minPivot = 0.3f
        val maxPivot = 0.7f
        val pivotX = remember { minPivot + Random.nextFloat() * (maxPivot - minPivot) }
        val pivotY = remember { minPivot + Random.nextFloat() * (maxPivot - minPivot) }
        val animSpec = remember { tween<Float>(durationMillis = 70, easing = LinearEasing) }
        val angle = remember { Animatable(0f) }

        LaunchedEffect(enabled) {
            scope.launch {
                if (enabled) {
                    while (true) {
                        angle.animateTo(targetValue = -3.5f, animationSpec = animSpec)
                        angle.animateTo(targetValue = 3.5f, animationSpec = animSpec)
                    }
                } else {
                    angle.animateTo(targetValue = 0F, animationSpec = animSpec)
                }
            }
        }

        Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(
                pivotFractionX = pivotX,
                pivotFractionY = pivotY
            )
            rotationZ = angle.value
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "shake"
        properties["enabled"] = enabled
    }
)
