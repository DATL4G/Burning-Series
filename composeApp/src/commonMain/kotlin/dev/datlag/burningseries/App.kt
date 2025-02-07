package dev.datlag.burningseries

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import dev.chrisbanes.haze.HazeState
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_bold
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_bold_italic
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_extra_bold
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_extra_bold_italic
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_extra_light
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_extra_light_italic
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_light
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_light_italic
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_medium
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_medium_italic
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_regular
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_regular_italic
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_semi_bold
import dev.datlag.burningseries.composeapp.generated.resources.Manrope_semi_bold_italic
import dev.datlag.burningseries.composeapp.generated.resources.Res
import dev.datlag.burningseries.model.SeriesData
import dev.datlag.burningseries.ui.theme.Colors
import dev.datlag.burningseries.ui.theme.dynamicDark
import dev.datlag.burningseries.ui.theme.dynamicLight
import dev.datlag.tooling.Platform
import dev.datlag.tooling.compose.platform.CombinedPlatformMaterialTheme
import dev.datlag.tooling.compose.platform.PlatformSurface
import dev.datlag.tooling.compose.platform.colorScheme
import dev.datlag.tooling.compose.platform.rememberIsTv
import dev.datlag.tooling.compose.toTypography
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.Font
import org.kodein.di.DI

val LocalDarkMode = compositionLocalOf<Boolean> { error("No dark mode state provided") }
val LocalEdgeToEdge = staticCompositionLocalOf<Boolean> { false }
val LocalDI = compositionLocalOf<DI> { error("No dependency injection provided") }
val LocalHaze = compositionLocalOf<HazeState> { error("No Haze state provided") }

@Composable
internal fun App(
    di: DI,
    systemDarkTheme: Boolean = isSystemInDarkTheme() || Platform.rememberIsTv(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDarkMode provides systemDarkTheme,
        LocalDI provides di
    ) {
        CombinedPlatformMaterialTheme (
            colorScheme = if (systemDarkTheme) Colors.dynamicDark() else Colors.dynamicLight(),
            typography = ManropeFontFamily().toTypography()
        ) {
            PlatformSurface(
                modifier = Modifier.fillMaxSize(),
                containerColor = Platform.colorScheme().background,
                contentColor = Platform.colorScheme().onBackground
            ) {
                content()
            }
        }
    }
}

@Composable
fun ManropeFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.Manrope_extra_light, FontWeight.ExtraLight),
        Font(Res.font.Manrope_extra_light_italic, FontWeight.ExtraLight, FontStyle.Italic),

        Font(Res.font.Manrope_light, FontWeight.Light),
        Font(Res.font.Manrope_light_italic, FontWeight.Light, FontStyle.Italic),

        Font(Res.font.Manrope_regular, FontWeight.Normal),
        Font(Res.font.Manrope_regular_italic, FontWeight.Normal, FontStyle.Italic),

        Font(Res.font.Manrope_medium, FontWeight.Medium),
        Font(Res.font.Manrope_medium_italic, FontWeight.Medium, FontStyle.Italic),

        Font(Res.font.Manrope_semi_bold, FontWeight.SemiBold),
        Font(Res.font.Manrope_semi_bold_italic, FontWeight.SemiBold, FontStyle.Italic),

        Font(Res.font.Manrope_bold, FontWeight.Bold),
        Font(Res.font.Manrope_bold_italic, FontWeight.Bold, FontStyle.Italic),

        Font(Res.font.Manrope_extra_bold, FontWeight.ExtraBold),
        Font(Res.font.Manrope_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    )
}
