package com.viniciuscastro.ingressos.di

import com.viniciuscastro.ingressos.domain.common.Clock
import com.viniciuscastro.ingressos.domain.common.IdGenerator
import com.viniciuscastro.ingressos.domain.common.Telemetry
import com.viniciuscastro.ingressos.telemetry.LogcatTelemetry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock { Instant.now() }

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = IdGenerator { UUID.randomUUID().toString() }

    @Provides
    @Singleton
    fun provideTelemetry(): Telemetry = LogcatTelemetry()
}
