package com.devoid.keysync.model

import android.view.KeyEvent

interface MultiModeTouchHandler {
    //return if pointer is pressed
    fun handleTouchEvent(keyEvent: KeyEvent,isPressed:Boolean,pointerId:Int,keyMap: KeyMap):Boolean
}