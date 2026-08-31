package com.zackwhye.secondbrain.core.di

import com.zackwhye.secondbrain.core.prefs.FirstRunStore
import com.zackwhye.secondbrain.core.prefs.SharedPrefsFirstRunStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirstRunModule {

    @Binds
    @Singleton
    abstract fun bindFirstRunStore(impl: SharedPrefsFirstRunStore): FirstRunStore
}
