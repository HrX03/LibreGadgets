package dev.hrx.libregadgets.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

data class GlucoseColor(
    val background: Color,
    val foreground: Color = Color.White,
)

val GlucoseHighColor = GlucoseColor(Color(0xFFFF5722))
val GlucoseCautionColor = GlucoseColor(Color(0xFFFFA000), Color(0xFF212121))
val GlucoseNormalColor = GlucoseColor(Color(0xFF388E3C))
val GlucoseLowColor = GlucoseColor(Color(0xFFD32F2F))
val GlucoseErrorColor = GlucoseColor(Color(0xFF424242))