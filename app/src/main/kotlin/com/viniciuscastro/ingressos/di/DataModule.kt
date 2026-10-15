package com.viniciuscastro.ingressos.di

import android.content.Context
import com.viniciuscastro.ingressos.data.DataLayer
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import com.viniciuscastro.ingressos.domain.repository.TransactionRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataLayer(@ApplicationContext context: Context): DataLayer = DataLayer(context)

    @Provides
    fun provideEventRepository(dataLayer: DataLayer): EventRepository = dataLayer.eventRepository

    @Provides
    fun providePurchaseRepository(dataLayer: DataLayer): PurchaseRepository = dataLayer.purchaseRepository

    @Provides
    fun provideTransactionRunner(dataLayer: DataLayer): TransactionRunner = dataLayer.transactionRunner
}
