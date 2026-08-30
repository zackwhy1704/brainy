package com.zackwhye.secondbrain.core.di

import com.zackwhye.secondbrain.core.network.AuthSessionManager
import com.zackwhye.secondbrain.core.network.AuthSessionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthSessionManager(impl: AuthSessionManagerImpl): AuthSessionManager
}
