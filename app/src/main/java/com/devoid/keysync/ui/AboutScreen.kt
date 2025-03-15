package com.devoid.keysync.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devoid.keysync.R


@OptIn(ExperimentalMaterial3Api::class)

@Preview
@Composable
fun PreviewAboutScreen(){
    AboutScreen(onNavigateBack = {}) {  }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit ,
    onOpenGithub:()->Unit
) {
    Scaffold(modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "About")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                })
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(paddingValues)
                .padding(8.dp)
                .verticalScroll(state = scrollState)
        ) {
            Image(
                painterResource(R.drawable.logo_raw),
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(color = Color.Black),
                contentDescription = null
            )
            Text(
                stringResource(R.string.app_name),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Version: ${getVersionName(LocalContext.current)}",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(modifier = Modifier, onClick = onOpenGithub) {
                    Icon(painterResource(R.drawable.github), contentDescription = null)
                    Text(modifier = Modifier.padding(start = 16.dp), text = "Github")
                }
//                FilledTonalButton(modifier = Modifier, onClick = {}) {
//                    Icon(painterResource(R.drawable.apk_document), contentDescription = null)
//                    Text(modifier = Modifier.padding(start = 16.dp), text = "F-Droid")
//                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Reach Me",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "akamunan@yahoo.com",
                        modifier = Modifier
                            .clickable { }
                            .padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

fun getVersionName(context: Context):String {
    return context.packageManager.getPackageInfo(context.packageName, 0).versionName?:"0.0"
}
