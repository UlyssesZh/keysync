package com.devoid.keysync.domain

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import com.devoid.keysync.model.AppConfig
import com.devoid.keysync.model.DraggableItem
import com.devoid.keysync.model.EventInjector
import com.devoid.keysync.model.KeyMap
import com.devoid.keysync.model.KeymapType
import com.devoid.keysync.model.MultiModeTouchHandler
import com.devoid.keysync.model.TouchMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val KEYCODE_WASD = 1001//custom keycode for key groups
const val KEYCODE_LMC = 1002//mouse left click
const val KEYCODE_RMC = 1003//mouse right click
const val KEYCODE_MMC = 1004//mouse middle click
const val KEYCODE_CANCEL = 1005//cancel button for cancelable components

class EventHandler(
    private val eventInjector: EventInjector
) {
    private var items: Map<Set<Int>, KeyMap> = mapOf()

    private var multiModeTouchHandler: MultiModeTouchHandler

    private val pressedKeys =
        mutableMapOf<Int, Boolean>()
    private val pointerIds =
        mutableMapOf<Int, Int?>()
    private val pressedKeysGroup =
        mutableMapOf<Int, Boolean>()//separate map because groups(like "wasd" ,cancellable) are registered under a const in $pressedKeys
    private val _shootingModeState =
        MutableStateFlow<ShootingModeState>(ShootingModeState.Disabled(false))
    val isShootingMode = _shootingModeState.map { it is ShootingModeState.Enabled }.stateIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.Eagerly,
        initialValue = false
    )


    var mousePointerPosition = Offset.Zero

    var appConfig = AppConfig.Default

    init {
        multiModeTouchHandler = TapModeTouchHandler(eventInjector)
    }

    fun setTouchMode(touchMode: TouchMode) {
        multiModeTouchHandler = when (touchMode) {
            TouchMode.TAP -> {
                TapModeTouchHandler(eventInjector)
            }

            TouchMode.HOLD -> {
                HoldModeTouchHandler(eventInjector)
            }

            TouchMode.MIXED -> {
                MixedModeTouchHandler(eventInjector)
            }
        }
    }

    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        val keyCode = if (isWASD(keyEvent)) KEYCODE_WASD else keyEvent.keyCode
        if (pointerIds[keyCode] == null)//no keybinding for $keycode
            return false
        when (keyEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                val pointerId = pointerIds[keyCode]!!
                if (keyCode == KEYCODE_WASD) {
                    if (pressedKeysGroup[keyEvent.keyCode] != true) {
                        pressedKeys[keyCode] = true
                        processWasdEvent(keyEvent, pointerId)
                        pressedKeysGroup[keyEvent.keyCode] = true
                    }
                    return true
                }
                if (pressedKeys[keyEvent.keyCode] != true) {
                    pressedKeys[keyCode] = true
                    processKeyEvent(keyEvent, pointerId)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isWASD(keyEvent)) {
                    val pointerId = pointerIds[KEYCODE_WASD]!!
                    pressedKeysGroup[keyEvent.keyCode] = false
                    processWasdEvent(keyEvent, pointerId)
                    pressedKeys[KEYCODE_WASD] = false
                    return true
                }
                val pointerId = pointerIds[keyEvent.keyCode]!!
                processKeyEvent(keyEvent, pointerId)
                pressedKeys[keyEvent.keyCode] = false
                return true
            }

            else -> {
                return false
            }
        }
    }

    fun handleMotionEvent(motionEvent: MotionEvent) {

    }

    private fun processKeyEvent(keyEvent: KeyEvent, pointerId: Int) {
        if (keyEvent.action == MotionEvent.ACTION_DOWN) {
            val itemKey = setOf(keyEvent.keyCode)
            items[itemKey]?.let {
                if (it.type == KeymapType.CANCELABLE) {
                    val isPressed = pressedKeysGroup[keyEvent.keyCode] ?: false
                    val keyMap =
                        if (isPressed) items[setOf(keyEvent.keyCode, KEYCODE_CANCEL)]!! else it
                    multiModeTouchHandler.handleTouchEvent(
                        keyEvent,
                        isPressed,
                        pointerId,
                        keyMap
                    ).run {
                        pressedKeysGroup[keyEvent.keyCode] = this
                    }
                    toggleShootingMode()
                    return
                }
                eventInjector.injectPointer(pointerId, it.position, it.end!!)
            }
        } else if (keyEvent.action == MotionEvent.ACTION_UP) {
            val itemKey = setOf(keyEvent.keyCode, KEYCODE_CANCEL)
            items[itemKey]?.let {
                val isPressed = pressedKeysGroup[keyEvent.keyCode] ?: false
                multiModeTouchHandler.handleTouchEvent(
                    keyEvent,
                    isPressed,
                    pointerId,
                    it
                ).let { shouldToggleShootingMode->
                    if (isShootingMode.value==shouldToggleShootingMode){
                        pressedKeysGroup[keyEvent.keyCode] =shouldToggleShootingMode
                        toggleShootingMode()
                    }
                }
            } ?: eventInjector.releasePointer(pointerId)
        }
    }

    private fun toggleShootingMode() {
        when(_shootingModeState.value){
            ShootingModeState.Disabled(true) -> {
                _shootingModeState.value= ShootingModeState.Enabled
                val key= setOf(appConfig.shootingModeKeyCode)
                val pointerId = pointerIds[appConfig.shootingModeKeyCode]
                items[key]?.let {
                    eventInjector.injectPointer(pointerId!!,it.position)
                }
            }
            ShootingModeState.Enabled ->{
                _shootingModeState.value= ShootingModeState.Disabled(true)
                val pointerId = pointerIds[appConfig.shootingModeKeyCode]
                eventInjector.releasePointer(pointerId!!)
            }
            else->{}
        }
    }

    private fun processWasdEvent(keyEvent: KeyEvent, pointerId: Int) {
        if (keyEvent.action == MotionEvent.ACTION_DOWN) {
            val itemKey =
                getPressedWasdKey()?.let { setOf(it, keyEvent.keyCode) }
                    ?: setOf(keyEvent.keyCode)
            items[itemKey]?.let {
                if (itemKey.size > 1) {
                    eventInjector.transFormGesture(pointerId, it.position)
                    return
                }
                it.center?.let { center ->
                    eventInjector.injectGesture(
                        pointerId,
                        center,
                        it.position
                    )
                }
            }
        } else if (keyEvent.action == MotionEvent.ACTION_UP) {
            val pressedWASDKey = getPressedWasdKey()
            pressedWASDKey?.let { keyCode ->//check if other keys(W,A,S,D) are pressed
                items[setOf(keyCode)]?.let {
                    eventInjector.transFormGesture(pointerId, it.position)
                }
            } ?: eventInjector.releaseGesture(pointerId)
        }
    }

    private fun getPressedWasdKey(): Int? {
        val wasdKeys = listOf(Key.W, Key.A, Key.S, Key.D)

        wasdKeys.forEach { iKey ->
            if (pressedKeysGroup[iKey.nativeKeyCode] == true) {
                return iKey.nativeKeyCode
            }
        }
        return null
    }

    private fun isWASD(keyEvent: KeyEvent): Boolean {
        when (keyEvent.keyCode) {
            Key.W.nativeKeyCode, Key.A.nativeKeyCode, Key.S.nativeKeyCode, Key.D.nativeKeyCode -> {}
            else -> return false
        }
        return true
    }

    fun handleMouseButton(mouseButton: Int, pressed: Boolean) {
        val keyCode = when (mouseButton) {
            MotionEvent.BUTTON_PRIMARY -> KEYCODE_LMC

            MotionEvent.BUTTON_SECONDARY -> KEYCODE_RMC

            MotionEvent.BUTTON_TERTIARY -> KEYCODE_MMC
            else -> return
        }
        when (keyCode) {
            KEYCODE_LMC, appConfig.fireKeyCode -> {
                if (!isShootingMode.value) {
                    simulateNativeClick(appConfig.fireKeyCode, pressed)
                    return
                }
                val pointerId = pointerIds[KEYCODE_LMC] ?: return
                if (pressed) {
                    val itemKey = setOf(KEYCODE_LMC)
                    items[itemKey]?.let {
                        eventInjector.injectPointer(pointerId, it.position, it.end!!)
                    }
                } else {
                    eventInjector.releasePointer(pointerId)
                }
            }

            appConfig.shootingModeKeyCode -> {
                if (!pressed)
                    return
                val itemKey = setOf(appConfig.shootingModeKeyCode)
                if (items[itemKey] == null)
                    return
                val pointerId = pointerIds[appConfig.shootingModeKeyCode]!!
                if (isShootingMode.value) {
                    items[itemKey]?.let {
                        eventInjector.releasePointer(pointerId)
                    }
                    _shootingModeState.value = ShootingModeState.Disabled(false)
                } else {
                    items[itemKey]?.let {
                        eventInjector.injectPointer(pointerId, it.position, it.end!!)
                    }
                    _shootingModeState.value = ShootingModeState.Enabled
                }
            }
        }
    }


    fun handlePointerMove(position: Offset): Boolean {
        if (!isShootingMode.value) {
            val pointerId = pointerIds[KEYCODE_LMC]!!
            eventInjector.updatePointerPosition(
                pointerID = pointerId,
                position
            )//update the position of visible mouse pressed pointer
            return true
        }
        val pointerId = pointerIds[KEYCODE_RMC]
        pointerId?.let {
            eventInjector.updatePointerPosition(it, position)
            return true
        } ?: return false
    }

    private fun simulateNativeClick(keyCode: Int, isPressed: Boolean) {
        val pointerId = pointerIds[keyCode]!!
        if (isPressed) {
            eventInjector.injectPointer(pointerId, mousePointerPosition)
        } else {
            eventInjector.releasePointer(pointerId)
        }
    }

    fun clear() {
        eventInjector.clear()
        pressedKeys.clear()
        pressedKeysGroup.clear()
        _shootingModeState.value = ShootingModeState.Disabled(false)
    }

    fun updateKeyMapping(items: List<DraggableItem>) {
        val map = hashMapOf<Set<Int>, KeyMap>()
        pointerIds.clear()
        items.forEach {
            when (it) {
                is DraggableItem.WASDGroup -> {
                    map[setOf(Key.W.nativeKeyCode)] = KeyMap(position = it.w, center = it.center)
                    map[setOf(Key.A.nativeKeyCode)] = KeyMap(position = it.a, center = it.center)
                    map[setOf(Key.S.nativeKeyCode)] = KeyMap(position = it.s, center = it.center)
                    map[setOf(Key.D.nativeKeyCode)] = KeyMap(position = it.d, center = it.center)
                    val wd = Offset(
                        x = it.w.x + ((it.d.x - it.w.x) / 2),
                        y = it.w.y + ((it.d.y - it.w.y) / 2)
                    )
                    val wa = Offset(
                        x = it.w.x - ((it.w.x - it.a.x) / 2),
                        y = it.w.y + ((it.a.y - it.w.y) / 2)
                    )
                    val sd = Offset(
                        x = it.s.x + ((it.d.x - it.s.x) / 2),
                        y = it.s.y + ((it.d.y - it.s.y) / 2)
                    )
                    val sa = Offset(
                        x = it.a.x + ((it.s.x - it.a.x) / 2),
                        y = it.a.y + ((it.s.y - it.a.y) / 2)
                    )
                    map[setOf(Key.W.nativeKeyCode, Key.A.nativeKeyCode)] =
                        KeyMap(position = wa, center = it.center)
                    map[setOf(Key.W.nativeKeyCode, Key.D.nativeKeyCode)] =
                        KeyMap(position = wd, center = it.center)
                    map[setOf(Key.S.nativeKeyCode, Key.D.nativeKeyCode)] =
                        KeyMap(position = sd, center = it.center)
                    map[setOf(Key.S.nativeKeyCode, Key.A.nativeKeyCode)] =
                        KeyMap(position = sa, center = it.center)

                    if (!pointerIds.containsKey(KEYCODE_WASD)) {
                        pointerIds[KEYCODE_WASD] = pointerIds.size
                    }
                }

                is DraggableItem.VariableKey -> {
                    it.keyCode?.let { keyCode ->
                        var position =
                            it.position
                        val shrinkSize =
                            it.size / 3f  //shrink the size so that artificial taps can be accurate on circular buttons
                        val end =
                            position + Offset(x = it.size - shrinkSize, y = it.size - shrinkSize)
                        position = Offset(position.x + shrinkSize, position.y + shrinkSize)

                        map[setOf(keyCode)] =
                            KeyMap(position = position, end = end)
                        if (!pointerIds.containsKey(keyCode)) {
                            pointerIds[keyCode] = pointerIds.size
                        }
                    }
                }

                is DraggableItem.FixedKey -> {
                    var position =
                        it.position
                    val shrinkSize =
                        it.size / 3f                                     //shrink the size so that artificial taps can be accurate on circular buttons
                    val end = position + Offset(x = it.size - shrinkSize, y = it.size - shrinkSize)
                    position = Offset(position.x + shrinkSize, position.y + shrinkSize)
                    map[setOf(it.keyCode)] =
                        KeyMap(position = position, end = end)
                    if (!pointerIds.containsKey(it.keyCode)) {
                        pointerIds[it.keyCode] = pointerIds.size
                    }

                }

                is DraggableItem.CancelableKey -> {
                    if (it.keyCode == null)
                        return@forEach
                    var position =
                        it.position
                    var cancelPosition =
                        it.cancelPosition
                    val shrinkSize =
                        it.size / 3f                                     //shrink the size so that artificial taps can be accurate on circular buttons
                    val end = position + Offset(x = it.size - shrinkSize, y = it.size - shrinkSize)
                    val cancelEnd =
                        cancelPosition + Offset(x = it.size - shrinkSize, y = it.size - shrinkSize)
                    position = Offset(position.x + shrinkSize, position.y + shrinkSize)
                    cancelPosition =
                        Offset(cancelPosition.x + shrinkSize, cancelPosition.y + shrinkSize)
                    map[setOf(it.keyCode!!)] =
                        KeyMap(type = KeymapType.CANCELABLE, position = position, end = end)

                    map[setOf(it.keyCode!!, KEYCODE_CANCEL)] =
                        KeyMap(
                            type = KeymapType.CANCELABLE,
                            position = cancelPosition,
                            end = cancelEnd
                        )
                    pointerIds[it.keyCode!!] = pointerIds.size
                }
            }
        }
        if (!pointerIds.containsKey(KEYCODE_LMC)) {
            pointerIds[KEYCODE_LMC] = pointerIds.size
        }
        this.items = map
    }

}