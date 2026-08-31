package com.zackwhye.secondbrain.core.di

import com.zackwhye.secondbrain.core.data.AskRepository
import com.zackwhye.secondbrain.core.data.AskRepositoryImpl
import com.zackwhye.secondbrain.core.data.BriefRepository
import com.zackwhye.secondbrain.core.data.BriefRepositoryImpl
import com.zackwhye.secondbrain.core.data.ItemRepository
import com.zackwhye.secondbrain.core.data.ItemRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository

    @Binds
    @Singleton
    abstract fun bindBriefRepository(impl: BriefRepositoryImpl): BriefRepository

    @Binds
    @Singleton
    abstract fun bindAskRepository(impl: AskRepositoryImpl): AskRepository
}
