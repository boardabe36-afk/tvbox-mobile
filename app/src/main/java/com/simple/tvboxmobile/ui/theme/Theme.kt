package com.simple.tvboxmobile.ui.theme

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

// Material 3 配色：与 TVBox 配色一致，但适配手机/平板的对比度
val BrandBlue = Color(0xFF4F8EF7)
val BrandOrange = Color(0xFFFF8A65)
val DarkBg = Color(0xFF0F1014)
val DarkSurface = Color(0xFF1A1B22)
val DarkSurfaceVariant = Color(0xFF25272F)
val LightTextPrimary = Color(0xFFEAEAEA)
val LightTextSecondary = Color(0xFFA0A0A8)

private val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandOrange,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    secondary = BrandOrange,
)

@Composable
fun TVBoxMobileTheme(
    useDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 暂时强制深色——视频应用深色为主，跟 TVBox 体验一致
    val colors = DarkColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
