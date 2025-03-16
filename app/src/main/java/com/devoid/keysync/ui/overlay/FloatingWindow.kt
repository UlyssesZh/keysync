package com.devoid.keysync.ui.overlay

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.devoid.keysync.R
import com.devoid.keysync.model.AppConfig
import com.devoid.keysync.model.DraggableItem
import com.devoid.keysync.model.DraggableItemType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


@Composable
fun FloatingBubble(
    expanded: Boolean = false,
    modifier: Modifier = Modifier.size(50.dp),
    onClick: () -> Unit = {}
) {
    Column(modifier = modifier
        .clip(CircleShape)
        .clickable {
            onClick()
        }) {
        if (expanded) {
            Image(
                imageVector = Icons.Rounded.Done,
                contentDescription = "",
                modifier = modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(8.dp)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.logo_raw),
                contentDescription = "",
                modifier = modifier
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun ItemsContainer(
    modifier: Modifier = Modifier,
    containerItems: List<DraggableItem>,
    appConfig: AppConfig = AppConfig.Default,
    onRemove: (Int) -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
    ) {
        containerItems.forEach { item ->
            Log.i("ItemsContainer", "ItemsContainer: added item: $item")
            var position by remember(item.id) { mutableStateOf(item.position) }
            var scale by remember(item.id) { mutableFloatStateOf(if (item is DraggableItem.WASDGroup) item.scale else appConfig.buttonScale) }
            if (item is DraggableItem.CancelableKey) {
                if (item.type == DraggableItemType.BAG_MAP) {
                    var cancelableKeyPosition by remember(item.id) { mutableStateOf(item.cancelPosition) }
                    var hasFocus by remember { mutableStateOf(false) }
                    val keyFocusRequester = remember { FocusRequester() }
                    var keyCode by remember(item.id) { mutableStateOf(item.keyCode) }
                    val text by remember(item.id) {
                        derivedStateOf {
                            keyCode?.let {
                                KeyEvent.keyCodeToString(it).replace("KEYCODE_", "")
                            } ?: "?"
                        }
                    }

                    DraggableItem(
                        scale = scale,
                        id = item.id,
                        offset = position,
                        onOffsetChange = {
                            item.position += it
                            position += it
                        }
                    ) {
                        BagMapKey(
                            modifier = Modifier
                                .onFocusChanged {
                                    hasFocus = it.hasFocus
                                }
                                .onSizeChanged {
                                    item.size = it.width
                                }
                                .onKeyEvent { keyEvent ->
                                    val nativeKeyCode = keyEvent.key.nativeKeyCode
                                    if (nativeKeyCode >= KeyEvent.KEYCODE_DPAD_UP && nativeKeyCode <= KeyEvent.KEYCODE_DPAD_CENTER)
                                        return@onKeyEvent false
                                    item.keyCode = nativeKeyCode
                                    keyCode = nativeKeyCode
                                    true
                                }
                                .focusRequester(keyFocusRequester)
                                .focusable(),
                            text = text,
                            borderColor = if (hasFocus) Color.Cyan else MaterialTheme.colorScheme.primary,
                            onClick = { keyFocusRequester.requestFocus() }) {
                            onRemove(item.id)
                        }
                    }
                    DraggableItem(
                        scale = scale,
                        id = item.id,
                        offset = cancelableKeyPosition,
                        onOffsetChange = {
                            item.cancelPosition += it
                            cancelableKeyPosition += it
                        }
                    ) {
                        CancelKey(
                            borderColor = if (hasFocus) Color.Cyan else MaterialTheme.colorScheme.primary,
                            onClick = { keyFocusRequester.requestFocus() }
                        )
                    }
                }
                return@forEach
            }
            DraggableItem(
                scale = scale,
                id = item.id,
                offset = position,
                onOffsetChange = {
                    item.position += it
                    position += it
                }
            ) {
                when (item) {
                    is DraggableItem.VariableKey -> {
                        var hasFocus by remember { mutableStateOf(false) }
                        val keyFocusRequester = remember { FocusRequester() }
                        var keyCode by remember(item.id) { mutableStateOf(item.keyCode) }
                        val text by remember(item.id) {
                            derivedStateOf {
                                keyCode?.let {
                                    KeyEvent.keyCodeToString(it).replace("KEYCODE_", "")
                                } ?: "?"
                            }
                        }
                        LaunchedEffect(Unit) {
                            keyFocusRequester.requestFocus()
                        }

                        KeyCircular(
                            value = text,
                            onRemove = {
                                onRemove(item.id)
                            },
                            onClick = { keyFocusRequester.requestFocus() },
                            borderColor = if (hasFocus) Color.Cyan else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .onSizeChanged {
                                    item.size = it.width
                                }
                                .onKeyEvent { keyEvent ->
                                    val nativeKeyCode = keyEvent.key.nativeKeyCode
                                    if (nativeKeyCode >= KeyEvent.KEYCODE_DPAD_UP && nativeKeyCode <= KeyEvent.KEYCODE_DPAD_CENTER)
                                        return@onKeyEvent false
                                    item.keyCode = nativeKeyCode
                                    keyCode = nativeKeyCode
                                    true
                                }
                                .onFocusChanged {
                                    hasFocus = it.hasFocus
                                }
                                .focusRequester(keyFocusRequester)
                                .focusable()
                        )
                    }

                    is DraggableItem.WASDGroup -> {
                        var size by remember { mutableIntStateOf(0) }
                        WASDKeysGroup(onRemove = {
                            onRemove(item.id)
                        }, modifier = Modifier
                            .onSizeChanged {
                                size = (it.width * scale).roundToInt()
                            }
                            .onGloballyPositioned { coordinates ->
                                val layoutX = coordinates.positionOnScreen().x
                                val layoutY = coordinates.positionOnScreen().y
                                item.center =
                                    coordinates.positionOnScreen() + Offset(
                                        size / 2f,
                                        size / 2f
                                    )
                                item.w = Offset(item.center.x, layoutY)
                                item.a = Offset(layoutX, item.center.y)
                                item.s = Offset(item.center.x, layoutY + size)
                                item.d = Offset(layoutX + size, item.center.y)

                            }, onPanIconDrag = { dragAmount ->
                            val scaleAmount = scale + (dragAmount.y / 500)
                            scale = max(min(scaleAmount, 1.4f), 0.6f)
                            item.scale = scale
                        })
                    }

                    is DraggableItem.FixedKey -> {
                        val onSizeChangeListener = { intSize: IntSize ->
                            item.size = intSize.width
                        }
                        when (item.type) {
                            DraggableItemType.FIRE -> {
                                FireKey(modifier = Modifier
                                    .onSizeChanged { onSizeChangeListener(it) }) {
                                    onRemove(item.id)
                                }
                            }
                            DraggableItemType.SHOOTING_MODE -> {
                                MousePointer(modifier = Modifier.onSizeChanged {
                                    onSizeChangeListener(it)
                                }) {
                                    onRemove(item.id)
                                }
                            }
                            DraggableItemType.SCOPE -> {
                                IconKey(modifier = Modifier, removable = true, onRemove = {onRemove(item.id)}, onClick = null, borderColor = MaterialTheme.colorScheme.primary) {
                                    Image(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        painter = painterResource(R.drawable.scope),
                                        contentDescription = "pointer",
                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.inverseSurface)
                                    )
                                }
                            }
                            else->{}
                        }
                    }

                    else -> {}
                }

            }

        }
    }
}

@Composable
fun DraggableItem(
    modifier: Modifier = Modifier,
    id: Int,
    offset: Offset,
    scale: Float,
    onOffsetChange: (Offset) -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier
        .offset {
            IntOffset(
                offset.x.roundToInt(),
                offset.y.roundToInt()
            )
        }
        .scale(scale)
        .pointerInput(id) {
            detectDragGestures(onDragEnd = {
            }) { change, dragAmount ->
                change.consume()
                val newOffset = Offset(dragAmount.x, dragAmount.y) * scale
                onOffsetChange(newOffset)
            }
        }) {
        content()
    }
}

@Composable
fun MenuItems(
    modifier: Modifier = Modifier,
    onItemClick: (DraggableItemType) -> Unit
) {
    val state = rememberLazyGridState()
    state.interactionSource.interactions
    Column(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surface,
            CircleShape.copy(CornerSize(10))
        )
    ) {
        Text(
            modifier = Modifier.padding(top = 4.dp, start = 8.dp),
            text = "Add keymapping for:",
            style = MaterialTheme.typography.labelSmall
        )
        LazyVerticalGrid(
            modifier = Modifier
                .heightIn(max = 300.dp)
                .widthIn(max = 170.dp)
                .padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            columns = GridCells.Adaptive(50.dp)
        ) {
            val itemModifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black)
            item(contentType = DraggableItemType.KEY) {
                Column(
                    Modifier.clickable(onClick = { onItemClick(DraggableItemType.KEY) }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(itemModifier, contentAlignment = Alignment.Center) {
                        Text("A", color = Color.Cyan)
                    }
                    Text(
                        "Key",
                        style = TextStyle(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            item(contentType = DraggableItemType.WASD_KEY) {
                Column(
                    Modifier.clickable(onClick = { onItemClick(DraggableItemType.WASD_KEY) }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(itemModifier, contentAlignment = Alignment.Center) {
                        Text(
                            "W\nS",
                            color = Color.Cyan,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "A   D",
                            color = Color.Cyan,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        "Compass",
                        style = TextStyle(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            item(contentType = DraggableItemType.SHOOTING_MODE) {
                Column(
                    Modifier.clickable(onClick = { onItemClick(DraggableItemType.SHOOTING_MODE) }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(itemModifier, contentAlignment = Alignment.Center) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            painter = painterResource(R.drawable.move),
                            contentDescription = "Enter Shooting Mode in FPS games Using Right Mouse Click",
                            colorFilter = ColorFilter.tint(
                                Color.Cyan
                            )
                        )
                    }
                    Text(
                        "Shooting Mode",
                        style = TextStyle(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            item(contentType = DraggableItemType.FIRE) {
                Column(
                    Modifier.clickable(onClick = { onItemClick(DraggableItemType.FIRE) }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(itemModifier, contentAlignment = Alignment.Center) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            painter = painterResource(R.drawable.bullet),
                            contentDescription = "Fire In FPS Games Using Left Mouse Click",
                            colorFilter = ColorFilter.tint(
                                Color.Cyan
                            )
                        )
                    }
                    Text(
                        "Fire",
                        style = TextStyle(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            item(contentType = DraggableItemType.BAG_MAP) {
                Column(
                    Modifier.clickable { onItemClick(DraggableItemType.BAG_MAP) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconKey(
                        modifier = itemModifier,
                        removable = false,
                        background = Color.Black,
                        borderColor = Color.Transparent,
                        onRemove = {}, onClick = { onItemClick(DraggableItemType.BAG_MAP) }) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp),
                            painter = painterResource(R.drawable.bag),
                            contentDescription = "Bag and Map ",
                            colorFilter = ColorFilter.tint(Color.Cyan)
                        )
                    }
                    Text(
                        "Bag and Map",
                        style = TextStyle(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            item(contentType = DraggableItemType.SCOPE) {
                Column(
                    Modifier.clickable { onItemClick(DraggableItemType.SCOPE) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconKey(
                        modifier = itemModifier,
                        removable = false,
                        background = Color.Black,
                        borderColor = Color.Transparent,
                        onRemove = {}, onClick = { onItemClick(DraggableItemType.SCOPE) }) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            painter = painterResource(R.drawable.scope),
                            contentDescription = "Scope",
                            colorFilter = ColorFilter.tint(Color.Cyan)
                        )
                    }
                    Text(
                        "Scope",
                        style = TextStyle(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }


        }
    }

}


@Composable
fun SettingsLayout(
    modifier: Modifier = Modifier,
    pointerSensitivity: Float = 0.5f,
    overlayOpacity: Float = 0.5f,
    onAdvancedSettingsClick: () -> Unit,
    onCloseOverlayClick: () -> Unit,
    onPointerSensChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    Column(
        modifier
            .clip(CircleShape.copy(CornerSize(10)))
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Row {
            Text(
                text = "Pointer Sensitivity: ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            Text(
                "${(pointerSensitivity * 100).roundToInt()} %",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically),
                style = TextStyle(color = MaterialTheme.colorScheme.tertiary)
            )
        }

        Slider(
            value = pointerSensitivity,
            onValueChange = onPointerSensChange,
            valueRange = 0.1f..1.0f,
            steps = 8
        )
        Row {
            Text(
                text = "Overlay Opacity: ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            Text(
                "${(overlayOpacity * 100).roundToInt()} %",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically),
                style = TextStyle(color = MaterialTheme.colorScheme.tertiary)
            )
        }
        Slider(
            value = overlayOpacity,
            onValueChange = onOpacityChange,
            valueRange = 0.0f..1.0f,
            steps = 9
        )
        HorizontalDivider()
        TextButton(onClick = onAdvancedSettingsClick) {
            Icon(Icons.Rounded.Settings, contentDescription = "Advanced Settings")
            Text(
                "Advanced Settings",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        TextButton(onClick = onCloseOverlayClick) {
            Icon(
                Icons.AutoMirrored.Rounded.ExitToApp,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "Close Overlay",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}


@Preview
@Composable
private fun PreviewMenuItems() {
    MenuItems(onItemClick = {})
}

@Preview()
@Composable
private fun PreviewSettingsLayout() {
    SettingsLayout(
        Modifier.clip(
            CircleShape.copy(CornerSize(10))
        ),
        onAdvancedSettingsClick = { },
        onPointerSensChange = { },
        onOpacityChange = { },
        onCloseOverlayClick = {}
    )
}

internal class ServiceLifecycleOwner : SavedStateRegistryOwner {
    private var mSavedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    private var lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    fun setCurrentState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        mSavedStateRegistryController.performRestore(savedState)
    }

    fun performSave(outBundle: Bundle) {
        mSavedStateRegistryController.performSave(outBundle)
    }
}