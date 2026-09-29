package lamz.netblocker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ShieldTealPrimary,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF70F7D7),
    secondary = ShieldCyanSecondary,
    onSecondary = Color(0xFF003549),
    tertiary = ShieldRedTertiary,
    onTertiary = Color(0xFF4C0014),
    background = SlateDarkBackground,
    onBackground = SlateTextPrimary,
    surface = SlateDarkSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldTealPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5F4E4),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF006686),
    onSecondary = Color.White,
    tertiary = Color(0xFFB91C1C),
    onTertiary = Color.White,
    background = SlateLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = SlateLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = SlateLightBorder
)

@Composable
fun NetBlockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent cyber branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
