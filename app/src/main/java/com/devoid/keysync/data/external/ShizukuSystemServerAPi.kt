package com.devoid.keysync.data.external

import android.os.IBinder
import android.util.Log
import android.view.InputEvent
import com.devoid.keysync.domain.EventHandler
import com.devoid.keysync.domain.EventInjectorImpl
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

class ShizukuSystemServerAPi {
    private val TAG = "ShizukuSystemServerApi"
    private var injectInputEventFunction : Method? = null
    private  var inputManagerInstance:Any? =null
    init {
        injectInputEventFunction = getInputApi()
    }
        private fun getInputApi(): Method? {
            val inputManagerBinder: IBinder =
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("input"))
            val inputManagerStub = Class.forName("android.hardware.input.IInputManager\$Stub")
            val inputManagerClass = Class.forName("android.hardware.input.IInputManager")
             inputManagerInstance =
                inputManagerStub.getMethod("asInterface", IBinder::class.java)
                    .invoke(null, inputManagerBinder)
            val injectInputEvent = inputManagerClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.java
            )
            return injectInputEvent
        }

    fun getEventHandler(): EventHandler {
        if (injectInputEventFunction==null||inputManagerInstance==null){
            throw IllegalStateException("Input manager Service not connected")
        }
       return EventHandler(object : EventInjectorImpl(){
           override fun inject(event: InputEvent) {
               Log.d(TAG, "inject: injecting: $event")
               injectInputEventFunction?.invoke(inputManagerInstance, event, 1)
           }
       })
    }



}