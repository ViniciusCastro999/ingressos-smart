package com.viniciuscastro.ingressos.payment.simulator

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class TerminalSimulatorActivity : ComponentActivity() {

    private val simulator = TerminalSimulator()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val request = simulator.parseRequest(intent.data?.toString().orEmpty())
        setContent {
            MaterialTheme(colorScheme = SimulatorColors) {
                Surface(Modifier.fillMaxSize(), color = Canvas) {
                    SimulatorScreen(
                        request = request,
                        onOutcome = { outcome -> request?.let { respond(simulator.respond(it, outcome)) } },
                        onCloseWithoutAnswer = ::finish,
                    )
                }
            }
        }
    }

    private fun respond(callbackUri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, callbackUri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            // Nobody listens to the callback: same as a real terminal answering a misconfigured app.
        }
        finish()
    }
}

@Composable
private fun SimulatorScreen(
    request: SimulatorRequest?,
    onOutcome: (SimulatedOutcome) -> Unit,
    onCloseWithoutAnswer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Simulador Cielo Smart", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Somente para desenvolvimento. Responde no mesmo formato do Deep Link da Cielo.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (request == null) {
            Text("Requisição inválida.", color = MaterialTheme.colorScheme.error)
        } else {
            TerminalDevice(request)
            SimulatedOutcome.entries.forEach { outcome ->
                if (outcome == SimulatedOutcome.APPROVE) {
                    Button(onClick = { onOutcome(outcome) }, Modifier.fillMaxWidth()) { Text(outcome.label) }
                } else {
                    OutlinedButton(onClick = { onOutcome(outcome) }, Modifier.fillMaxWidth()) { Text(outcome.label) }
                }
            }
        }
        TextButton(onClick = onCloseWithoutAnswer, Modifier.fillMaxWidth()) {
            Text("Fechar sem responder (simula terminal desligado)")
        }
    }
}

/** Dark terminal body with a light screen, echoing the LIO illustration of the app's visual identity. */
@Composable
private fun TerminalDevice(request: SimulatorRequest) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBody, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Screen, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("LIO", color = Cielo, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(formatCents(request.amountCents), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            request.payment.items.forEach { Text("${it.quantity}x ${it.name}", color = Ink) }
            Text("Forma: ${request.payment.paymentCode ?: "escolher no terminal"}", color = Muted)
            Text("Referência: ${request.payment.reference}", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Text(
            "•  •  •  •  •",
            color = Muted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private val Cielo = Color(0xFF0066A8)
private val Ink = Color(0xFF14232D)
private val Muted = Color(0xFF667781)
private val Canvas = Color(0xFFF4F8FA)
private val Screen = Color(0xFFF8FBFC)
private val TerminalBody = Color(0xFF21333C)

private val SimulatorColors = lightColorScheme(
    primary = Cielo,
    onPrimary = Color.White,
    background = Canvas,
    surface = Canvas,
    onSurface = Ink,
    outline = Color(0xFFA8CADB),
)

private fun formatCents(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(BigDecimal.valueOf(cents, 2))
