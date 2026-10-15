package com.viniciuscastro.ingressos.di

import com.viniciuscastro.ingressos.domain.common.Clock
import com.viniciuscastro.ingressos.domain.common.IdGenerator
import com.viniciuscastro.ingressos.domain.common.Telemetry
import com.viniciuscastro.ingressos.domain.payment.PaymentTerminal
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import com.viniciuscastro.ingressos.domain.repository.TransactionRunner
import com.viniciuscastro.ingressos.domain.usecase.ObserveEventUseCase
import com.viniciuscastro.ingressos.domain.usecase.ObserveEventsUseCase
import com.viniciuscastro.ingressos.domain.usecase.ObservePendingPurchaseUseCase
import com.viniciuscastro.ingressos.domain.usecase.ObservePurchaseHistoryUseCase
import com.viniciuscastro.ingressos.domain.usecase.ObservePurchaseUseCase
import com.viniciuscastro.ingressos.domain.usecase.ProcessPaymentResultUseCase
import com.viniciuscastro.ingressos.domain.usecase.ReleasePendingPurchaseUseCase
import com.viniciuscastro.ingressos.domain.usecase.StartPurchaseUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Composition root for the domain. Use cases carry no DI annotations, so the domain module stays free
 * of any framework; the wiring lives here.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    /** Singleton on purpose: its single-flight lock only works if every caller shares the instance. */
    @Provides
    @Singleton
    fun provideStartPurchase(
        events: EventRepository,
        purchases: PurchaseRepository,
        terminal: PaymentTerminal,
        clock: Clock,
        telemetry: Telemetry,
    ) = StartPurchaseUseCase(events, purchases, terminal, clock, telemetry)

    @Provides
    fun provideProcessPaymentResult(
        purchases: PurchaseRepository,
        events: EventRepository,
        transaction: TransactionRunner,
        idGenerator: IdGenerator,
        clock: Clock,
        telemetry: Telemetry,
    ) = ProcessPaymentResultUseCase(purchases, events, transaction, idGenerator, clock, telemetry)

    @Provides
    fun provideReleasePendingPurchase(purchases: PurchaseRepository, clock: Clock, telemetry: Telemetry) =
        ReleasePendingPurchaseUseCase(purchases, clock, telemetry)

    @Provides
    fun provideObserveEvents(events: EventRepository) = ObserveEventsUseCase(events)

    @Provides
    fun provideObserveEvent(events: EventRepository) = ObserveEventUseCase(events)

    @Provides
    fun provideObservePurchase(purchases: PurchaseRepository) = ObservePurchaseUseCase(purchases)

    @Provides
    fun provideObservePurchaseHistory(purchases: PurchaseRepository) = ObservePurchaseHistoryUseCase(purchases)

    @Provides
    fun provideObservePendingPurchase(purchases: PurchaseRepository) = ObservePendingPurchaseUseCase(purchases)
}
