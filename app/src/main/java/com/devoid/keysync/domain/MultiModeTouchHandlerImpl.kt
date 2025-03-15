package com.devoid.keysync.domain

import android.view.KeyEvent
import android.view.MotionEvent
import com.devoid.keysync.model.EventInjector
import com.devoid.keysync.model.KeyMap
import com.devoid.keysync.model.MultiModeTouchHandler

//inject and release touch events on click at primary key(1st click) at cancel key (2nd click)
class TapModeTouchHandler(private val eventInjector: EventInjector):MultiModeTouchHandler {
    override fun handleTouchEvent(
        keyEvent: KeyEvent,
        isPressed: Boolean,
        pointerId: Int,
        keyMap: KeyMap
    ): Boolean {
        if (keyEvent.action==MotionEvent.ACTION_DOWN){
            eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
            eventInjector.releasePointer(pointerId)
            return !isPressed
        }
        return isPressed
    }
}
//inject and release touch events on click at primary key(on key Press) at cancel key (on key Release)
class HoldModeTouchHandler(private val eventInjector: EventInjector):MultiModeTouchHandler{
    override fun handleTouchEvent(
        keyEvent: KeyEvent,
        isPressed: Boolean,
        pointerId: Int,
        keyMap: KeyMap
    ): Boolean {
        if (keyEvent.action==MotionEvent.ACTION_DOWN||keyEvent.action==MotionEvent.ACTION_UP){
            eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
            eventInjector.releasePointer(pointerId)
        }
        return keyEvent.action==MotionEvent.ACTION_DOWN
    }
}
class MixedModeTouchHandler(private val eventInjector: EventInjector):MultiModeTouchHandler{
    private var lastKeyDownMap = hashMapOf<Int,Long>()
    private  val minDifference = 200
    override fun handleTouchEvent(
        keyEvent: KeyEvent,
        isPressed: Boolean,
        pointerId: Int,
        keyMap: KeyMap
    ): Boolean {
       if (keyEvent.action==MotionEvent.ACTION_DOWN){
           lastKeyDownMap[pointerId] = System.currentTimeMillis()
           eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
           eventInjector.releasePointer(pointerId)
           return !isPressed
       }else if (keyEvent.action==MotionEvent.ACTION_UP){
           if (!isPressed)
               return false
           val difference =System.currentTimeMillis()- (lastKeyDownMap[pointerId] ?: 0)
           if (difference>minDifference){
               eventInjector.injectPointer(pointerId, keyMap.position, keyMap.end!!)
               eventInjector.releasePointer(pointerId)
               return false
           }
       }
        return isPressed
    }

}