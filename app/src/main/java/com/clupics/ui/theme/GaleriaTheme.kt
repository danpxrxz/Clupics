package com.clupics.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = Color(0xFF3B6952),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFBCEDD2),
    onPrimaryContainer   = Color(0xFF002115),
    secondary            = Color(0xFF4D6357),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFCFE9D9),
    onSecondaryContainer = Color(0xFF0A1F16),
    tertiary             = Color(0xFF3B6470),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFBEEAF7),
    onTertiaryContainer  = Color(0xFF001F27),
    error                = Color(0xFFBA1A1A),
    errorContainer       = Color(0xFFFFDAD6),
    onError              = Color(0xFFFFFFFF),
    onErrorContainer     = Color(0xFF410002),
    background           = Color(0xFFF5FBF5),
    onBackground         = Color(0xFF171D1A),
    surface              = Color(0xFFF5FBF5),
    onSurface            = Color(0xFF171D1A),
    surfaceVariant       = Color(0xFFDBE5DC),
    onSurfaceVariant     = Color(0xFF404943),
    outline              = Color(0xFF707973),
    outlineVariant       = Color(0xFFBFC9C1),
    inverseSurface       = Color(0xFF2C322E),
    inverseOnSurface     = Color(0xFFECF2ED),
    inversePrimary       = Color(0xFFA1D1B7),
    surfaceTint          = Color(0xFF3B6952),
    scrim                = Color(0xFF000000),
    surfaceBright        = Color(0xFFF5FBF5),
    surfaceDim           = Color(0xFFD5DBD6),
    surfaceContainer          = Color(0xFFE9EFE9),
    surfaceContainerLow       = Color(0xFFEFF5EF),
    surfaceContainerHigh      = Color(0xFFE3E9E4),
    surfaceContainerHighest   = Color(0xFFDDE4DE),
)

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF80CBAA),
    onPrimary            = Color(0xFF003826),
    primaryContainer     = Color(0xFF22503C),
    onPrimaryContainer   = Color(0xFF9DE8C5),
    secondary            = Color(0xFFB3CCBE),
    onSecondary          = Color(0xFF1F352A),
    secondaryContainer   = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE9D9),
    tertiary             = Color(0xFFA2CDD9),
    onTertiary           = Color(0xFF02363F),
    tertiaryContainer    = Color(0xFF1F4D57),
    onTertiaryContainer  = Color(0xFFBEEAF7),
    error                = Color(0xFFFFB4AB),
    errorContainer       = Color(0xFF93000A),
    onError              = Color(0xFF690005),
    onErrorContainer     = Color(0xFFFFDAD6),
    background           = Color(0xFF0C110F),
    onBackground         = Color(0xFFDCE4DE),
    surface              = Color(0xFF0C110F),
    onSurface            = Color(0xFFDCE4DE),
    surfaceVariant       = Color(0xFF404943),
    onSurfaceVariant     = Color(0xFFBFC9C1),
    outline              = Color(0xFF8A938C),
    outlineVariant       = Color(0xFF404943),
    inverseSurface       = Color(0xFFDCE4DE),
    inverseOnSurface     = Color(0xFF2C322E),
    inversePrimary       = Color(0xFF3B6952),
    surfaceTint          = Color(0xFF80CBAA),
    scrim                = Color(0xFF000000),
    surfaceBright        = Color(0xFF323836),
    surfaceDim           = Color(0xFF0C110F),
    surfaceContainer          = Color(0xFF1A201D),
    surfaceContainerLow       = Color(0xFF171D1A),
    surfaceContainerHigh      = Color(0xFF242B27),
    surfaceContainerHighest   = Color(0xFF2E3530),
)

private fun ColorScheme.asAmoled(): ColorScheme = copy(
    background              = Color(0xFF000000),
    surface                 = Color(0xFF000000),
    surfaceVariant          = Color(0xFF070B09),
    surfaceContainer        = Color(0xFF0A0E0C),
    surfaceContainerLow     = Color(0xFF050807),
    surfaceContainerHigh    = Color(0xFF101512),
    surfaceContainerHighest = Color(0xFF161C19),
    surfaceDim              = Color(0xFF000000),
    surfaceBright           = Color(0xFF111714),
    onBackground            = Color(0xFFE8F0EB),
    onSurface               = Color(0xFFE8F0EB),
    outline                 = Color(0xFF6B7570),
    outlineVariant          = Color(0xFF252B28),
)

val GaleriaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GaleriaTheme(
    darkTheme    : Boolean = isSystemInDarkTheme(),
    amoledTheme  : Boolean = false,
    dynamicColor : Boolean = true,
    content      : @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val colorScheme = if (amoledTheme && darkTheme) baseScheme.asAmoled() else baseScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Color de los iconos de la barra según tema.
            // El visor sobreescribe esto al entrar y lo restaura al salir.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            // windowBackground = surface del tema.
            // Android usa este valor para la animación de tarjeta predictiva nativa:
            // dibuja este color detrás de la pantalla que se está cerrando.
            window.setBackgroundDrawable(ColorDrawable(colorScheme.surface.toArgb()))
        }
    }

    MaterialTheme(
        colorScheme  = colorScheme,
        typography   = GaleriaTypography,
        shapes       = GaleriaShapes,
        motionScheme = MotionScheme.expressive(),
        content      = content
    )
}
