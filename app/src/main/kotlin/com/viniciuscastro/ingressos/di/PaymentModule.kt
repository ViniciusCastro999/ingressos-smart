package com.viniciuscastro.ingressos.di

import android.content.Context
import com.viniciuscastro.ingressos.BuildConfig
import com.viniciuscastro.ingressos.domain.payment.PaymentResultParser
import com.viniciuscastro.ingressos.domain.payment.PaymentTerminal
import com.viniciuscastro.ingressos.payment.cielo.CieloCallbackParser
import com.viniciuscastro.ingressos.payment.cielo.CieloConfig
import com.viniciuscastro.ingressos.payment.cielo.CieloDeepLinkFactory
import com.viniciuscastro.ingressos.payment.cielo.CieloDeepLinkTerminal
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The only place that knows the payment terminal is Cielo Smart. */
@Module
@InstallIn(SingletonComponent::class)
object PaymentModule {

    @Provides
    @Singleton
    fun provideCieloConfig(): CieloConfig = CieloConfig(
        clientId = BuildConfig.CIELO_CLIENT_ID,
        accessToken = BuildConfig.CIELO_ACCESS_TOKEN,
        callbackUri = "${BuildConfig.PAYMENT_CALLBACK_SCHEME}://${BuildConfig.PAYMENT_CALLBACK_HOST}",
        terminalScheme = BuildConfig.TERMINAL_SCHEME,
    )

    @Provides
    @Singleton
    fun providePaymentTerminal(@ApplicationContext context: Context, config: CieloConfig): PaymentTerminal =
        CieloDeepLinkTerminal(context, CieloDeepLinkFactory(config))

    @Provides
    @Singleton
    fun providePaymentResultParser(): PaymentResultParser = CieloCallbackParser()
}
