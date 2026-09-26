package com.vitalis.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * VITALIS palette — warm paper ground, ink hero cards, one lime accent,
 * one calorie orange. Macro colours differ in lightness (ink / orange / olive),
 * not hue alone, so they survive colour-blindness; every bar still gets a label.
 */
object VitalisColors {
    val Ground = Color(0xFFF3F1EC)
    val Card = Color(0xFFFFFFFF)
    val Sunken = Color(0xFFECE9E2)
    val Hairline = Color(0xFFEEEBE4)
    val Border = Color(0xFFDEDAD1)
    val BorderControl = Color(0xFFCFCBC1)

    val Ink = Color(0xFF15161A)
    val InkMuted = Color(0xFF5E5F66)
    val InkFaint = Color(0xFF8A8B91)

    /** Dark surfaces: hero cards, tab bar, live tracking. */
    val Night = Color(0xFF15161A)
    val NightDeep = Color(0xFF0E0F12)
    val NightTrack = Color(0xFF2A2B31)
    val NightLine = Color(0xFF24252B)
    val OnNightMuted = Color(0xFFA3A4AA)

    val Lime = Color(0xFFD4F24A)
    val OnLimeMuted = Color(0xFF3A4210)
    val Olive = Color(0xFF9DBA1F)
    val Orange = Color(0xFFE8572A)
    /** Orange dark enough for text on white (≥4.5:1). */
    val OrangeText = Color(0xFFB8401A)
    val Water = Color(0xFF2F6FEB)
    val WaterEmpty = Color(0xFFE3E8F4)
    val StepsBlue = Color(0xFF9CC0FF)
    val HeartRate = Color(0xFFFF8A5C)

    val Success = Color(0xFF1F6B44)
    val SuccessBg = Color(0xFFDDF3E6)
    val Danger = Color(0xFFB42318)

    val TierBronze = Color(0xFFB8733A)
    val TierSilver = Color(0xFF9EA3AD)
    val TierGold = Color(0xFFD9A520)

    val Heat = listOf(Sunken, Color(0xFFE4F4A0), Color(0xFFC5E23A), Ink)
}

/**
 * ponytail: system sans for now. The design uses Bricolage Grotesque (display) +
 * Geist (body); drop their TTFs in core/designsystem/res/font and swap these two vals.
 */
private val DisplayFont = FontFamily.SansSerif
private val BodyFont = FontFamily.SansSerif

/** Tabular figures so live numbers don't jiggle while they update. */
private const val TNUM = "tnum"

object VitalisType {
    val HeroXl = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 100.sp, letterSpacing = (-0.04).em, lineHeight = 100.sp, fontFeatureSettings = TNUM)
    val Hero = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 60.sp, letterSpacing = (-0.04).em, lineHeight = 60.sp, fontFeatureSettings = TNUM)
    val DisplayL = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.03).em, lineHeight = 36.sp, fontFeatureSettings = TNUM)
    val DisplayM = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.025).em, lineHeight = 34.sp)
    val Metric = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.03).em, lineHeight = 34.sp, fontFeatureSettings = TNUM)
    val Title = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val Value = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, fontFeatureSettings = TNUM)
    val BodyStrong = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, fontFeatureSettings = TNUM)
    val Body = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val Small = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM)
    val Caption = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM)
    val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
}

@Composable
fun VitalisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = VitalisColors.Ink,
            onPrimary = Color.White,
            secondary = VitalisColors.Lime,
            onSecondary = VitalisColors.Ink,
            tertiary = VitalisColors.Orange,
            background = VitalisColors.Ground,
            onBackground = VitalisColors.Ink,
            surface = VitalisColors.Card,
            onSurface = VitalisColors.Ink,
            surfaceVariant = VitalisColors.Sunken,
            onSurfaceVariant = VitalisColors.InkMuted,
            outline = VitalisColors.BorderControl,
            outlineVariant = VitalisColors.Hairline,
            error = VitalisColors.Danger,
        ),
        content = content,
    )
}
