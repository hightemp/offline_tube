package com.hightemp.offline_tube.di

import com.hightemp.offline_tube.data.repository.DownloadRepositoryImpl
import com.hightemp.offline_tube.data.repository.SettingsRepositoryImpl
import com.hightemp.offline_tube.data.repository.VideoRepositoryImpl
import com.hightemp.offline_tube.domain.repository.DownloadRepository
import com.hightemp.offline_tube.domain.repository.SettingsRepository
import com.hightemp.offline_tube.domain.repository.VideoRepository
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
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
