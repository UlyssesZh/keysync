package com.devoid.keysync.model

import androidx.annotation.UiThread
import androidx.compose.ui.geometry.Offset

interface EventInjector {
    @UiThread
    fun injectGesture(pointerID:Int,o1:Offset, o2:Offset)
    fun transFormGesture(pointerID:Int,position:Offset)
    fun releaseGesture(pointerID: Int)
    fun injectPointer(pointerID: Int,o1: Offset,o2: Offset)
    fun injectPointer(pointerID: Int,position: Offset)
    fun updatePointerPosition(pointerID: Int, position:Offset)
    fun clear()
    fun releasePointer(pointerID: Int)
    suspend fun injected(pointerID: Int):Boolean
}