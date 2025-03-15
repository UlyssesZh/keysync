package com.devoid.keysync.domain

import android.util.SparseIntArray
import android.view.Display
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.devoid.keysync.model.EventManager

class EventManagerImpl : EventManager {
    private val maxPointers = 10//only this no of pointers can be active at a time
    private val pointerIndexMap = SparseIntArray(maxPointers)
    private var activePointerCount = 0

    private val propsArray = Array(maxPointers) { MotionEvent.PointerProperties() }
    private val coordsArray = Array(maxPointers) { MotionEvent.PointerCoords() }


    override suspend fun addPointer(id: Int, x: Float, y: Float): Int {
        if (activePointerCount >= maxPointers) return -1
        var index = pointerIndexMap[id, -1]
        if (index != -1) {//existing event
            coordsArray[index].also { coords ->
                coords.x = x
                coords.y = y
            }
            return -2
        }
        index = activePointerCount
        pointerIndexMap.put(id, index)

        // Initialize
        propsArray[index].also { props ->
            props.id = id
            props.toolType = MotionEvent.TOOL_TYPE_FINGER
        }

        coordsArray[index].also { coords ->
            coords.x = x
            coords.y = y
            coords.pressure = 1.0f
            coords.size = 1.0f
        }

        activePointerCount++
        return index
    }

    override suspend fun updatePointer(id: Int, x: Float, y: Float): Boolean {
        val index = pointerIndexMap[id, -1]
        if (index == -1) return false

        coordsArray[index].also { coords ->
            coords.x = x
            coords.y = y
        }
        return true
    }

    override suspend fun removePointer(id: Int): Int {
        val removedIndex = pointerIndexMap[id, -1]
        if (removedIndex == -1) return -1
        for (i in removedIndex until activePointerCount - 1) {
            // Copy next pointer
            propsArray[i].copyFrom(propsArray[i + 1])
            coordsArray[i].copyFrom(coordsArray[i + 1])

            val nextId = propsArray[i + 1].id
            pointerIndexMap.put(nextId, i)
        }
        pointerIndexMap.delete(id)
        activePointerCount--
        return removedIndex
    }

    override suspend fun createMotionEvent(action: Int, downTime: Long): MotionEvent? {
        if (activePointerCount <= 0)
            return null
        val event = MotionEvent.obtain(
            downTime,
            downTime,
            action,
            activePointerCount,
            propsArray.sliceArray(0 until activePointerCount),
            coordsArray.sliceArray(0 until activePointerCount),
            0,
            0,
            1f,
            1f,
            Display.DEFAULT_DISPLAY,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )
        return event
    }

    override suspend fun offsetPointer(id: Int, offset: Offset): Boolean {
        val index = pointerIndexMap[id, -1]
        if (index == -1) return false

        coordsArray[index].also { coords ->
            coords.x += offset.x
            coords.y += offset.y
        }
        return true
    }

    override suspend fun getPointerAction(
        pointerId: Int,
        isUp: Boolean
    ): Int {//make sure to add pointer first
        val index = pointerIndexMap[pointerId, -1]
        if (index == -1) return -1
        return if (activePointerCount <= 1) {
            if (isUp) MotionEvent.ACTION_UP else MotionEvent.ACTION_DOWN
        } else {
            val action =
                if (isUp) MotionEvent.ACTION_POINTER_UP else MotionEvent.ACTION_POINTER_DOWN
            (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT) or action
        }
    }

    override suspend fun getPointerLocation(pointerId: Int): Offset? {
        val index = pointerIndexMap[pointerId, -1]
        return if (index == -1) null else {
            val coords = coordsArray[index]
            Offset(coords.x, coords.y)
        }
    }

    override suspend fun clear(): Boolean {
        if (pointerIndexMap.size() == 0)
            return false
        pointerIndexMap.clear()
        activePointerCount = 0
        return true
    }
}