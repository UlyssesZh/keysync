package com.devoid.keysync.model

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset

interface EventManager {
    suspend fun addPointer(id:Int,x:Float,y:Float):Int
    suspend fun updatePointer(id: Int,x: Float,y: Float):Boolean
    suspend fun removePointer(id: Int):Int
    suspend fun createMotionEvent(action:Int,downTime:Long=SystemClock.uptimeMillis()):MotionEvent?
    suspend fun offsetPointer(id: Int,offset: Offset):Boolean
    suspend fun getPointerAction(pointerId: Int, isUp: Boolean):Int
    suspend fun getPointerLocation(pointerId: Int):Offset?
    suspend fun clear():Boolean
}