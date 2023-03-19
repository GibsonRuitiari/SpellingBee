package me.gibsoncodes.spellingbee.ui.theme

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val YELLOW = Color(0xfff8cd05)
private val GRAY = Color(0xffe6e6e6)
private val YELLOW_NIGHT = Color(0xFFE4CD05)
private val GRAY_NIGHT = Color(0xFF6F6F6F)
private val ERROR = Color(0xFFCF6679)
val ANSWER_FOUND = Color(0xFF9FC587)

private val DarkColors = darkColors(
    primary = YELLOW_NIGHT,
    secondary = GRAY_NIGHT,
    error = ERROR
)
private val LightColors = lightColors(
    primary = YELLOW,
    secondary = GRAY,
    onSurface = Color.Black,
    onPrimary = Color.Black,
    error = ERROR
)

@Composable
fun SpellingBeeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }
    val localContext= LocalContext.current
    val window = (localContext as? Activity)?.window

    SideEffect {
        window?.let {_window ->
            _window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            _window.statusBarColor =android.graphics.Color.TRANSPARENT
        }
    }
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}