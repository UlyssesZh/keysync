package com.devoid.keysync.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.devoid.keysync.MainActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext



@Composable
fun DualTopBar(
    modifier: Modifier = Modifier,
    isPrimaryVisible: Boolean = true,
    isPrimaryExpanded: Boolean = true,
    primaryContent: @Composable () -> Unit,
    primaryExpandedContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = CircleShape.copy(CornerSize(30.dp))
    )
    {
        AnimatedContent(
            modifier = Modifier
                .clickable(onClick = onClick)
                .heightIn(min = 55.dp)
                .wrapContentHeight(align = Alignment.CenterVertically)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            targetState = isPrimaryVisible
        ) {
            if (it) {
                Column {
                    primaryContent()
                    AnimatedVisibility(isPrimaryExpanded) {
                        primaryExpandedContent()
                    }
                }
            } else {
                secondaryContent()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableAppIcon(
    modifier: Modifier = Modifier,
    icon: Drawable?,
    selected: Boolean = false,
    borderColor: Color = Color.Red,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(CircleShape.copy(CornerSize(20)))
            .wrapContentSize()
            .border(
                width = if (selected) 3.dp else -1.dp,
                color = borderColor,
                CircleShape.copy(CornerSize(20))
            )
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        AsyncImage(
            icon,
            contentDescription = null,
            colorFilter = if (selected) ColorFilter.tint(
                borderColor.copy(alpha = 0.2f),
                blendMode = BlendMode.Overlay
            ) else null,
        )
    }
}

@Composable
fun PackagesLayout(
    modifier: Modifier = Modifier,
    viewModel: MainActivityViewModel,
    packages: List<String>,
    isSelected: (Int) -> Boolean = { false },
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit = {}
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.FixedSize(60.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        itemsIndexed(
            items = packages,
            key = { _, item -> item }
        ) { index, packageName ->
            val icon by produceState<Drawable?>(initialValue = null) {
                value = withContext(Dispatchers.IO) {
                    viewModel.getPackageIcon(packageName)
                }
            }
            val appName = viewModel.getPackageName(packageName)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SelectableAppIcon(
                    modifier = Modifier.size(60.dp),
                    icon = icon,
                    selected = isSelected(index),
                    onLongClick = { onLongClick(index) },
                    onClick = { onClick(index) }
                )
                Text(
                    appName,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                )
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    appName: String ,
    viewModel: MainActivityViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToSettings:()->Unit,
    onPackageIconClick:(String)->Unit
) {
        val selectedPackages = remember { mutableStateMapOf<Int, Boolean>() }
        val packages by viewModel.addedPackages.collectAsState()
        val sheetState = rememberModalBottomSheetState()
        var isBottomSheetVisible by remember { mutableStateOf(false) }
        val connectedDevices by viewModel.connectedDevices.collectAsState()
        var showBottomSheetContent by remember { mutableStateOf(false) }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                val isPrimaryContentVisible by remember { derivedStateOf { selectedPackages.size == 0 } }
                var isTopBarExpanded by remember { mutableStateOf(false) }
                val animatedRotation by animateFloatAsState(
                    targetValue = if (isTopBarExpanded) -90f else 0f,
                    label = "rotation"
                )
                DualTopBar(
                    modifier = Modifier
                        .padding(16.dp),
                    isPrimaryVisible = isPrimaryContentVisible,
                    isPrimaryExpanded = isTopBarExpanded,
                    primaryContent = {
                        Row {
                            Text(
                                "Connected Devices:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            Text(
                                modifier = Modifier.padding(end = 16.dp),
                                text = "${connectedDevices.size} Devices",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                modifier = Modifier.rotate(animatedRotation),
                                contentDescription = null
                            )
                        }
                    },
                    primaryExpandedContent = {
                        LazyColumn(Modifier.padding(top = 8.dp)) {
                            connectedDevices.forEach { (key, value) ->
                                item(key = key) {
                                    HorizontalDivider()
                                    Row(Modifier.padding(8.dp)) {
                                        Text("•", fontSize = 20.sp, color = Color.Green)
                                        Text(
                                            value,
                                            Modifier.padding(start = 16.dp),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2
                                        )
                                    }

                                }
                            }
                        }
                    },
                    secondaryContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Selected: ${selectedPackages.size}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            Icon(
                                Icons.Rounded.Delete,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.removePackages(selectedPackages.keys)
                                        selectedPackages.clear()
                                    }
                                    .padding(4.dp),
                                contentDescription = "Delete Selected Items",
                                tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }) {
                    if (isPrimaryContentVisible) {
                        isTopBarExpanded = !isTopBarExpanded
                    }
                }
            },
            bottomBar = {
                BottomAppBar(actions = {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Advanced Setting"
                        )
                    }

                    Spacer(Modifier.width(32.dp))
                }, floatingActionButton = {
                    FloatingActionButton(onClick = {
                        isBottomSheetVisible = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add app")
                    }
                })
            }
        ) { contentPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                PackagesLayout(packages = packages,
                    viewModel = viewModel,
                    isSelected = {
                        selectedPackages[it] ?: false
                    },
                    onLongClick = {
                        selectedPackages[it] = true
                    },
                    onClick = { position ->
                        if (selectedPackages[position] != null) {
                            selectedPackages.remove(position)
                            return@PackagesLayout
                        }
                        if (selectedPackages.size > 0) {
                            selectedPackages[position] = true
                            return@PackagesLayout
                        }
                        onPackageIconClick(packages[position])
                    })
                if (isBottomSheetVisible) {
                    ModalBottomSheet(
                        modifier = Modifier.fillMaxHeight(),
                        onDismissRequest = {
                            isBottomSheetVisible = false
                            showBottomSheetContent = false
                        }, sheetState = sheetState
                    ) {
                        var installedPackages by remember { mutableStateOf<List<String>?>(null) }

                        LaunchedEffect(sheetState.currentValue, sheetState.targetValue) {
                            if ((sheetState.currentValue == SheetValue.PartiallyExpanded ||
                                        sheetState.currentValue == SheetValue.Expanded) &&
                                !showBottomSheetContent
                            ) {
                                installedPackages =
                                    viewModel.getInstalledPackages()
                                        .map { it.packageName }
                                delay(500)
                                showBottomSheetContent = true
                            }
                        }
                        if (showBottomSheetContent) {
                            installedPackages?.let {
                                Column {
                                    Text(modifier=Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),text = "Add Games:", style = MaterialTheme.typography.titleLarge)
                                    PackagesLayout(
                                        packages = it,
                                        viewModel = viewModel,
                                        onLongClick = {},
                                        onClick = { position ->
                                            viewModel.addPackage(it[position])
                                            isBottomSheetVisible = false
                                            showBottomSheetContent = false
                                        })
                                }
                            }
                        } else {
                            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        }

                    }
                }
            }
        }
    }
