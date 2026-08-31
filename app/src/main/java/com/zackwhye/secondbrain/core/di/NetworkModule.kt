package com.zackwhye.secondbrain.core.di

import com.zackwhye.secondbrain.BuildConfig
import com.zackwhye.secondbrain.core.network.api.SupabaseAskApi
import com.zackwhye.secondbrain.core.network.api.SupabaseAuthApi
import com.zackwhye.secondbrain.core.network.api.SupabaseBriefsApi
import com.zackwhye.secondbrain.core.network.api.SupabaseItemsApi
import com.zackwhye.secondbrain.core.network.api.SupabaseStorageApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder().addInterceptor(logging).build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("${BuildConfig.SUPABASE_URL}/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideSupabaseAuthApi(retrofit: Retrofit): SupabaseAuthApi = retrofit.create(SupabaseAuthApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseItemsApi(retrofit: Retrofit): SupabaseItemsApi = retrofit.create(SupabaseItemsApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseStorageApi(retrofit: Retrofit): SupabaseStorageApi = retrofit.create(SupabaseStorageApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseBriefsApi(retrofit: Retrofit): SupabaseBriefsApi = retrofit.create(SupabaseBriefsApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseAskApi(retrofit: Retrofit): SupabaseAskApi = retrofit.create(SupabaseAskApi::class.java)
}
