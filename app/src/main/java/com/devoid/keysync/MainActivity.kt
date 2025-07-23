package com.devoid.keysync

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devoid.keysync.MainActivityViewModel.UiState
import com.devoid.keysync.model.ThemePreference
import com.devoid.keysync.service.FloatingBubbleService
import com.devoid.keysync.ui.AboutScreen
import com.devoid.keysync.ui.MainScreen
import com.devoid.keysync.ui.SettingsScreen
import com.devoid.keysync.ui.theme.KeySyncTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val INTENT_ACTION_SETTINGS = "navigateToSettings"
    }

    private val TAG = this::class.simpleName
    private val viewModel: MainActivityViewModel by viewModels()
    private val navigateTo = mutableStateOf<Any?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        intent.action?.let {
            if (it == INTENT_ACTION_SETTINGS) {
                navigateTo.value = SettingsScreen
            }
        }
        setContent {
            val uiState by viewModel.shizukuState.collectAsStateWithLifecycle()
            val isServiceRunning by FloatingBubbleService.isRunning.collectAsStateWithLifecycle()
            val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val navController = rememberNavController()
            KeySyncTheme(
                darkTheme = when (appConfig.themePreference) {
                    ThemePreference.LIGHT -> false
                    ThemePreference.DARK -> true
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = appConfig.themePreference == ThemePreference.SYSTEM_DYNAMIC
            ) {
                NavHost(navController = navController, startDestination = MainScreen,
                    enterTransition = {
                        fadeIn(
                            animationSpec = tween(
                                300, easing = LinearEasing
                            )
                        ) + slideIntoContainer(
                            animationSpec = tween(300, easing = EaseIn),
                            towards = AnimatedContentTransitionScope.SlideDirection.Start
                        )
                    },
                    popExitTransition = {
                        fadeOut(
                            animationSpec = tween(
                                300, easing = LinearEasing
                            )
                        ) + slideOutOfContainer(
                            animationSpec = tween(300, easing = EaseOut),
                            towards = AnimatedContentTransitionScope.SlideDirection.End
                        )
                    }) {
                    composable<MainScreen>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None }) {
                        MainScreen(
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState,
                            appName = getString(R.string.app_name),
                            onNavigateToSettings = {
                                navController.navigate(route = SettingsScreen)
                            }
                        ) { packageName ->
                            if (checkUiState(
                                    uiState,
                                    viewModel.viewModelScope,
                                    snackbarHostState
                                )
                            ) {
                                viewModel.launchPackage(packageName,packageManager,isServiceRunning)
//                                launchService()
                            }
                        }
                    }
                    composable<SettingsScreen>(
                        popEnterTransition = { EnterTransition.None }) {
                        SettingsScreen(
                            appConfig = viewModel.appConfig.value,
                            onNavigateBack = navController::popBackStack,
                            onNavigateAbout = { navController.navigate(route = AboutScreen) },
                            onSave = {
                                viewModel.saveKeyConfig(it)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Saved Successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate(route = MainScreen)
                            })
                    }
                    composable<AboutScreen> {
                        AboutScreen(
                            onNavigateBack = navController::popBackStack,
                            onOpenGithub = viewModel::openGithub
                        )
                    }
                }
                if (navigateTo.value != null) {
                    navController.navigate(route = navigateTo.value!!)
                    navigateTo.value = null
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.action?.let {
            if (it == INTENT_ACTION_SETTINGS) {
                navigateTo.value = SettingsScreen
            }
        }
    }

    private fun checkShizukuPermission(): Boolean {
        val isGranted = if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            ContextCompat.checkSelfPermission(
                this,
                ShizukuProvider.PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
        return isGranted
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun checkUiState(
        uiState: UiState,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState
    ): Boolean {
        when (uiState) {
            UiState.ShizukuNotRunning -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Please start Shizuku Service first.")
                }
                return false
            }

            is UiState.ShizukuRunning -> {
                if (!checkOverlayPermission()) {
                    scope.launch {
                        val result = snackbarHostState
                            .showSnackbar(
                                message = "Please grant permissions in settings!",
                                actionLabel = "Settings",
                                duration = SnackbarDuration.Short
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            val settingIntent =
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            val uri =
                                Uri.fromParts("package", packageName, null)
                            settingIntent.setData(uri)
                            startActivity(settingIntent)
                        }
                    }
                    return false
                }
                if (!checkShizukuPermission()) {
                    if (viewModel.requestShizukuPermission()) {
                        return false
                    }
                    scope.launch {
                        val result = snackbarHostState
                            .showSnackbar(
                                message = "Please grant permissions in settings!",
                                actionLabel = "Settings",
                                duration = SnackbarDuration.Short
                            )
                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                val settingIntent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts(
                                    "package",
                                    packageName,
                                    null
                                )
                                settingIntent.setData(uri)
                                startActivity(settingIntent)
                            }

                            else -> {}
                        }
                    }
                }

                return true
            }
        }
    }
}

@Serializable
object MainScreen

@Serializable
object SettingsScreen

@Serializable
object AboutScreen
