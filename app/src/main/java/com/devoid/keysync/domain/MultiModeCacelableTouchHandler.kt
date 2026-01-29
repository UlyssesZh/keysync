package com.devoid.keysync.domain


import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import com.devoid.keysync.model.EventInjector
import com.devoid.keysync.model.KeyMap
import com.devoid.keysync.model.MultiModeTouchHandler

//cancelable pointers are difrent then regular one because u cant inject pointer at primary position for too loong
//rather single tap primary position and cancel position


//primary position must be provided when $isPressed is false and cancel position must be provided when $isPressed is true
class CancelableTapModeTouchHandler(private val eventInjector: EventInjector) : MultiModeTouchHandler {
    override fun handleTouchEvent(
        keyEvent: KeyEvent,
        isPressed: Boolean,
        pointerId: Int,
        keyMap: KeyMap
    ): Boolean {
        if (keyEvent.action == MotionEvent.ACTION_DOWN) {
            eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
            eventInjector.releasePointer(pointerId)
            return !isPressed
        }
        return isPressed
    }
}

//primary position must be provided when $isPressed is false and cancel position must be provided when $isPressed is true
class CancelableHoldModeTouchHandler(private val eventInjector: EventInjector) : MultiModeTouchHandler {
    override fun handleTouchEvent(
        keyEvent: KeyEvent,
        isPressed: Boolean,
        pointerId: Int,
        keyMap: KeyMap
    ): Boolean {
        if (keyEvent.action == MotionEvent.ACTION_DOWN || keyEvent.action == MotionEvent.ACTION_UP) {
            eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
            eventInjector.releasePointer(pointerId)
        }
        return keyEvent.action == MotionEvent.ACTION_DOWN
    }
}

//primary position must be provided when $isPressed is false and cancel position must be provided when $isPressed is true
class CancelableMixedModeTouchHandler(private val eventInjector: EventInjector) : MultiModeTouchHandler {
    private var lastKeyDownMap = hashMapOf<Int, Long>()
    private val minDifference = 200
    override fun handleTouchEvent(
        keyEvent: KeyEvent,
        isPressed: Boolean,
        pointerId: Int,
        keyMap: KeyMap
    ): Boolean {
        if (keyEvent.action == MotionEvent.ACTION_DOWN) {
            lastKeyDownMap[pointerId] = System.currentTimeMillis()
            eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
            eventInjector.releasePointer(pointerId)
            return !isPressed
        } else if (keyEvent.action == MotionEvent.ACTION_UP) {
            if (!isPressed)
                return false
            val difference = System.currentTimeMillis() - (lastKeyDownMap[pointerId] ?: 0)
            if (difference > minDifference) {
               eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
                eventInjector.releasePointer(pointerId)
                return false
            }
        }
        return isPressed
    }

}