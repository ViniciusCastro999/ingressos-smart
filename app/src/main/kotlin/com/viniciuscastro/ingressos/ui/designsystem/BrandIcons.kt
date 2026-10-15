package com.viniciuscastro.ingressos.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The identity's stroke icons (24 grid, 1.8 stroke, round caps), ported from its SVG paths.
 * Circles and rounded rects are written as arcs so every icon is plain path data.
 */
object BrandIcons {
    val Back by lazy { strokeIcon("back", "M19 12H5M12 19l-7-7 7-7") }
    val Search by lazy { strokeIcon("search", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0", "M20 20l-3.5-3.5") }
    val Calendar by lazy {
        strokeIcon(
            "calendar",
            "M6 5h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
            "M16 3v4M8 3v4M3 10h18",
        )
    }
    val Pin by lazy {
        strokeIcon(
            "pin",
            "M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z",
            "M9.5 10a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
        )
    }
    val Ticket by lazy {
        strokeIcon(
            "ticket",
            "M3 8a2 2 0 0 0 0 4v5a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5a2 2 0 0 0 0-4V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v2Z",
            "M13 7h4M13 11h4M13 15h2M9 4v15",
        )
    }
    val Home by lazy { strokeIcon("home", "M3 11l9-8 9 8", "M5 10v10h14V10M9 20v-6h6v6") }
    val Card by lazy {
        strokeIcon(
            "card",
            "M5 5h14a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
            "M2 10h20M6 15h4",
        )
    }
    val Pix by lazy { strokeIcon("pix", "M12 3l9 9-9 9-9-9z", "M8.5 12h7") }
    val Terminal by lazy {
        strokeIcon(
            "terminal",
            "M8 2h8a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z",
            "M9 6h6v5H9z",
            "M9 15h.01M12 15h.01M15 15h.01M9 18h.01M12 18h.01M15 18h.01",
        )
    }
    val Shield by lazy {
        strokeIcon("shield", "M12 3L5 6v5c0 4.6 2.9 8.2 7 10 4.1-1.8 7-5.4 7-10V6l-7-3Z", "M9 12l2 2 4-4")
    }
    val Check by lazy { strokeIcon("check", "M5 12l4 4L19 6") }
    val Close by lazy { strokeIcon("close", "M6 6l12 12M18 6L6 18") }
    val Alert by lazy {
        strokeIcon(
            "alert",
            "M12 8v5M12 17h.01",
            "M10.3 3.7L2.8 17a2 2 0 0 0 1.7 3h15a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0Z",
        )
    }
    val Refresh by lazy {
        strokeIcon("refresh", "M20 7v5h-5M4 17v-5h5", "M6.1 9A7 7 0 0 1 18.8 7M17.9 15A7 7 0 0 1 5.2 17")
    }
    val Chevron by lazy { strokeIcon("chevron", "M9 18l6-6-6-6") }
    val Plus by lazy { strokeIcon("plus", "M12 6v12M6 12h12") }
    val Minus by lazy { strokeIcon("minus", "M6 12h12") }

    private fun strokeIcon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    stroke = SolidColor(Color.Black), // tinted by Icon()
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
