package com.zackwhye.secondbrain.core.di

import android.content.Context
import androidx.room.Room
import com.zackwhye.secondbrain.core.database.AppDatabase
import com.zackwhye.secondbrain.core.database.dao.BriefDao
import com.zackwhye.secondbrain.core.database.dao.ItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "second_brain.db").build()

    @Provides
    @Singleton
    fun provideItemDao(appDatabase: AppDatabase): ItemDao = appDatabase.itemDao()

    @Provides
    @Singleton
    fun provideBriefDao(appDatabase: AppDatabase): BriefDao = appDatabase.briefDao()
}
