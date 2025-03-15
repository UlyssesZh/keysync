package com.devoid.keysync

import android.content.Context
import android.content.Context.INPUT_SERVICE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoid.keysync.data.local.DataStoreManager
import com.devoid.keysync.model.AppConfig
import com.devoid.keysync.model.DraggableItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject


@HiltViewModel
class MainActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) :
    ViewModel(), Shizuku.OnRequestPermissionResultListener {
    private val SHIZUKU_REQ_CODE = 10
    private val _shizukuState = MutableStateFlow<UiState>(UiState.ShizukuNotRunning)
    val shizukuState = _shizukuState.asStateFlow()

    private val _addedPackages = MutableStateFlow<List<String>>(listOf())
    val addedPackages = _addedPackages.asStateFlow()

    private val _appConfig = MutableStateFlow(AppConfig.Default)
    val appConfig = _appConfig.asStateFlow()


    private val _connectedDevices = MutableStateFlow<Map<Int, String>>(hashMapOf())
    val connectedDevices = _connectedDevices.asStateFlow()

    init {
        Shizuku.addRequestPermissionResultListener(this)
        Shizuku.addBinderReceivedListener {
            val binder = Shizuku.getBinder()
            binder?.let {
                _shizukuState.value = UiState.ShizukuRunning(it)
            }
        }
        Shizuku.addBinderDeadListener {
            _shizukuState.value = UiState.ShizukuNotRunning
        }
        getShizukuBinder()
        getConnectedExternalDevices()
        viewModelScope.launch {
            _addedPackages.value = dataStoreManager.getList(DataStoreManager.ADDED_PACKAGES).first()
            _appConfig.value = dataStoreManager.getKeyConfig(DataStoreManager.KEYS_CONFIG).first()
        }
    }

    fun saveKeyConfig(newAppConfig: AppConfig) {
        val newFireKeyCode = newAppConfig.fireKeyCode
        val newShootingModeKeyCode = newAppConfig.shootingModeKeyCode
        if (newFireKeyCode != appConfig.value.fireKeyCode ||
            newShootingModeKeyCode != appConfig.value.shootingModeKeyCode
        ) {
            viewModelScope.launch {
                dataStoreManager.getButtonsConfigKeys().first().forEach {
                    val buttonsConfig = dataStoreManager.getButtons(it).first().toMutableList()
                    buttonsConfig.forEachIndexed { index, value ->
                        if (value is DraggableItem.FixedKey) {
                            if (value.keyCode == appConfig.value.fireKeyCode) {
                                buttonsConfig[index] = value.copy(keyCode = newFireKeyCode)
                            } else if (value.keyCode == appConfig.value.shootingModeKeyCode) {
                                buttonsConfig[index] = value.copy(keyCode = newShootingModeKeyCode)
                            }
                        }
                    }
                    dataStoreManager.save(it, buttonsConfig)
                }
            }
        }
        viewModelScope.launch {
            dataStoreManager.save(DataStoreManager.KEYS_CONFIG, newAppConfig)
            _appConfig.value = newAppConfig
        }
    }

    fun removePackages(packages: Collection<Int>) {
        viewModelScope.launch {
            _addedPackages.value = _addedPackages.value.filterIndexed { index, packageName ->
                if (index !in packages) {
                    true
                } else {
                    if (appConfig.value.deleteDataOnRemove) {
                        dataStoreManager.remove(DataStoreManager.getButtonsConfigKey(packageName))
                    }
                    false
                }
            }
            dataStoreManager.saveList(DataStoreManager.ADDED_PACKAGES, _addedPackages.value)
        }
    }

    fun addPackage(packages: String) {
        if (_addedPackages.value.contains(packages))
            return
        _addedPackages.value = _addedPackages.value.plus(packages)
        viewModelScope.launch {
            dataStoreManager.saveList(DataStoreManager.ADDED_PACKAGES, _addedPackages.value)
        }
    }

    fun getInstalledPackages(): List<PackageInfo> {
        return context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            .filter { it.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
    }

    fun getPackageName(packageName: String): String {
        val packageInfo = context.packageManager.getApplicationInfo(packageName, 0)
        return context.packageManager.getApplicationLabel(packageInfo).toString()
    }

    private fun getConnectedExternalDevices() {
        val inputManager = context.getSystemService(INPUT_SERVICE) as InputManager
        val deviceIds = inputManager.inputDeviceIds
        val devices = HashMap<Int, String>()
        deviceIds.forEach { deviceId ->
            val device = inputManager.getInputDevice(deviceId)
            device?.let {
                if (it.isExternal) {
                    devices[it.id] = getDeviceName(it)
                }
            }
        }
        _connectedDevices.value = devices
        inputManager.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                val device = inputManager.getInputDevice(deviceId)
                device?.let {
                    if (it.isExternal) {
                        _connectedDevices.value =
                            _connectedDevices.value.plus(it.id to getDeviceName(it))
                    }
                }
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                _connectedDevices.value = _connectedDevices.value.minus(deviceId)

            }

            override fun onInputDeviceChanged(deviceId: Int) {
                val device = inputManager.getInputDevice(deviceId)
                device?.let {
                    if (it.isExternal) {
                        _connectedDevices.value =
                            _connectedDevices.value.plus(it.id to getDeviceName(it))
                    }
                }
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun getDeviceName(inputDevice: InputDevice): String {
        return inputDevice.name.takeIf { name -> name.isNotEmpty() } ?: when (inputDevice.sources) {
            InputDevice.SOURCE_MOUSE -> "Mouse"
            InputDevice.SOURCE_KEYBOARD -> "Keyboard"
            else -> "Unknown Device"
        }

    }

    suspend fun getPackageIcon(packageName: String): Drawable {
        return withContext(Dispatchers.IO) {
            return@withContext context.packageManager.getApplicationIcon(packageName)
        }
    }

    private fun getShizukuBinder() {
        Shizuku.getBinder()?.let {
            _shizukuState.value = UiState.ShizukuRunning(it)
        } ?: {
            _shizukuState.value = UiState.ShizukuNotRunning
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        if (granted) {
            Shizuku.getBinder()?.let {
                _shizukuState.value = UiState.ShizukuRunning(it)
            } ?: { _shizukuState.value = UiState.ShizukuNotRunning }
        }
    }

    fun requestShizukuPermission(): Boolean {
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            return false
        }
        // Request the permission
        Shizuku.requestPermission(SHIZUKU_REQ_CODE)
        return true
    }

    fun openGithub() {
        val url = "https://github.com/aka-munan/keysync"
        val i = Intent(Intent.ACTION_VIEW)
        i.setData(Uri.parse(url))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    sealed interface UiState {
        data object ShizukuNotRunning : UiState
        data class ShizukuRunning(val binder: IBinder) : UiState
    }

    override fun onCleared() {
        Shizuku.removeRequestPermissionResultListener(this)
        Log.i("MainActivityViewModel", "onCleared: main viewModelCleared ")
    }

}