package com.devoid.keysync

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.size.Precision
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KeySyncApplication : Application(), SingletonImageLoader.Factory {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base);

    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .diskCachePolicy(CachePolicy.DISABLED)
            .crossfade(true)
            .precision(Precision.INEXACT)
            .build()
    }
}