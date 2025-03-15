package com.devoid.keysync.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.devoid.keysync.model.AppConfig
import com.devoid.keysync.model.ThemePreference
import com.devoid.keysync.model.TouchMode
import com.devoid.keysync.util.capitalizeFirst
import com.devoid.keysync.util.keyCodeToString
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    appConfig: AppConfig = AppConfig.Default,
    onSave: (AppConfig) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateAbout: () -> Unit = {}
) {
    var newConfig by remember { mutableStateOf(appConfig) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Settings")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }, actions = {
                    Button(modifier = Modifier.padding(end = 8.dp), enabled = newConfig != appConfig, onClick = {
                        onSave(newConfig)
                    }) {
                        Text("Save")
                    }
                })
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Box(
            Modifier
                .padding(innerPadding)
                .padding(8.dp)
                .verticalScroll(state = scrollState)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Key Configuration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            KeyConfigTextField(title = "Toggle Shooting Mode:", value = newConfig.shootingModeKeyCode.keyCodeToString()) {
                                newConfig=newConfig.copy(shootingModeKeyCode = it.nativeKeyEvent.keyCode)
                                return@KeyConfigTextField true
                            }
                        }
                        HorizontalDivider()
                        KeyConfigTextField(title = "Fire:", value = newConfig.fireKeyCode.keyCodeToString()) {
                            newConfig=newConfig.copy(fireKeyCode = it.nativeKeyEvent.keyCode)
                            return@KeyConfigTextField true
                        }
                        HorizontalDivider()
                        KeyConfigTextField(title = "Scope:", value = newConfig.scopeKeyCode.keyCodeToString()) {
                            newConfig=newConfig.copy(scopeKeyCode = it.nativeKeyEvent.keyCode)
                            return@KeyConfigTextField true
                        }
                    }
                }
                Card {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Button Size",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("${(newConfig.buttonScale * 100).roundToInt()} %")
                        }
                        Slider(value = newConfig.buttonScale,
                            valueRange = 0f..2f, steps = 0, onValueChange = {
                                newConfig = newConfig.copy(buttonScale = it)
                            })
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                "Delete data",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                modifier = Modifier.padding(top = 8.dp),
                                text = "Delete Keymapping for removed apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = newConfig.deleteDataOnRemove, onCheckedChange = {
                            newConfig = newConfig.copy(deleteDataOnRemove = it)
                        })
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    var cancellableDropDownExpanded by remember { mutableStateOf(false) }
                    var scopeDropDownExpanded by remember { mutableStateOf(false) }

                    ConstraintLayout(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val (title,
                            cancelTitle,
                            cancelBody,
                            cancelSelector,
                            divider1,
                            scopeTitle,
                            scopeBody,
                            scopeSelector) = createRefs()
                        Text(
                            "Touch Mode",
                            modifier = Modifier.constrainAs(title) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Cancelable Button",
                            modifier = Modifier
                                .constrainAs(cancelTitle) {
                                    top.linkTo(title.bottom)
                                }
                                .padding(top = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            modifier = Modifier
                                .constrainAs(cancelBody) {
                                    top.linkTo(cancelTitle.bottom)
                                }
                                .padding(top = 8.dp),
                            text = "Touch behaviour for cancelable buttons",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedCard(modifier = Modifier.constrainAs(cancelSelector) {
                            bottom.linkTo(cancelBody.bottom)
                            top.linkTo(cancelTitle.top)
                            end.linkTo(parent.end)
                        }, onClick = { cancellableDropDownExpanded = true }) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(min = 60.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    newConfig.cancellableTouchMode.name.capitalizeFirst(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = cancellableDropDownExpanded,
                                onDismissRequest = { cancellableDropDownExpanded = false }) {
                                TouchMode.entries.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.name.capitalizeFirst()) },
                                        onClick = {
                                            newConfig = newConfig.copy(cancellableTouchMode = it)
                                            cancellableDropDownExpanded = false
                                        })
                                }
                            }
                        }
                        HorizontalDivider(
                            Modifier
                                .constrainAs(divider1) {
                                    top.linkTo(cancelBody.bottom)
                                }
                                .padding(top = 8.dp))
                        Text(
                            "Scope",
                            modifier = Modifier
                                .constrainAs(scopeTitle) {
                                    top.linkTo(divider1.bottom)
                                }
                                .padding(top = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            modifier = Modifier
                                .constrainAs(scopeBody) {
                                    top.linkTo(scopeTitle.bottom)
                                }
                                .padding(top = 8.dp),
                            text = "Touch behaviour for Scope button",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedCard(modifier = Modifier.constrainAs(scopeSelector) {
                            bottom.linkTo(scopeBody.bottom)
                            top.linkTo(scopeTitle.top)
                            end.linkTo(parent.end)
                        }, onClick = { scopeDropDownExpanded = true }) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(min = 60.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    newConfig.scopeTouchMode.name.capitalizeFirst(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = scopeDropDownExpanded,
                                onDismissRequest = { scopeDropDownExpanded = false }) {
                                TouchMode.entries.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.name.capitalizeFirst()) },
                                        onClick = {
                                            newConfig = newConfig.copy(scopeTouchMode = it)
                                            scopeDropDownExpanded = false
                                        })
                                }
                            }
                        }
                    }


                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    var dropDownExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                "Theme",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                            Text(
                                modifier = Modifier.padding(top = 8.dp),
                                text = "Set theme preference for this app",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedCard(onClick = { dropDownExpanded = true }) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(min = 60.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    newConfig.themePreference.name.capitalizeFirst(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = dropDownExpanded,
                                onDismissRequest = { dropDownExpanded = false }) {
                                ThemePreference.entries.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.name.capitalizeFirst()) },
                                        onClick = {
                                            newConfig = newConfig.copy(themePreference = it)
                                            dropDownExpanded = false
                                        })
                                }
                            }
                        }

                    }
                }
                FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    newConfig = AppConfig.Default
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text(
                            modifier = Modifier.padding(start = 16.dp),
                            text = "Reset To Defaults",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = onNavigateAbout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                        Text(
                            modifier = Modifier.padding(start = 16.dp),
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyConfigTextField(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    onKeyEvent: (KeyEvent) -> Boolean
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    Row {
        Text(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f),
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedCard(border = BorderStroke(
            1.dp,
            color = if (hasFocus)   Color.Cyan else MaterialTheme.colorScheme.outline
        ),
            modifier = Modifier.clickable { focusRequester.requestFocus() }) {
            Text(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .focusRequester(focusRequester)
                    .onKeyEvent { onKeyEvent(it) }
                    .onFocusChanged { hasFocus = it.isFocused }
                    .focusable(),
                text = value,
                style = TextStyle(color = LocalContentColor.current),
            )
        }
    }
}