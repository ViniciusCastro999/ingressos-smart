package com.viniciuscastro.ingressos.telemetry

import android.util.Log
import com.viniciuscastro.ingressos.domain.common.Telemetry
import com.viniciuscastro.ingressos.domain.common.TelemetryEvent

/** Development sink. In production the same events would feed an analytics/APM backend. */
class LogcatTelemetry : Telemetry {
    override fun track(event: TelemetryEvent) {
        Log.i(TAG, event.toString())
    }

    private companion object {
        const val TAG = "IngressosTelemetry"
    }
}
