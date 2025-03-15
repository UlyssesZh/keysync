package com.devoid.keysync.di

import android.content.Context
import com.devoid.keysync.data.local.DataStoreManager
import com.devoid.keysync.service.FloatingWindowStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DiModule{

    @Provides
    @Singleton
    fun provideDatastoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }
    @Provides
    fun provideFloatingWindowStateManager(@ApplicationContext context: Context,dataStoreManager: DataStoreManager):FloatingWindowStateManager{
        return FloatingWindowStateManager(context,dataStoreManager)
    }
}

