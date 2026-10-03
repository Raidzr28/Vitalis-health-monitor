package com.vitalis.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.ButtonKind
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.TopoLines
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.domain.gamification.BadgeCatalog
import com.vitalis.core.domain.gamification.LevelCurve
import com.vitalis.core.domain.gamification.StreakCalculator
import com.vitalis.core.model.LevelTier
import com.vitalis.core.model.XpAction
import com.vitalis.core.model.XpAward
import com.vitalis.data.Celebration
import com.vitalis.data.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Decel = CubicBezierEasing(.05f, .7f, .1f, 1f)
private val Overshoot = CubicBezierEasing(.34f, 1.56f, .64f, 1f)
private val Id = Locale.forLanguageTag("id")

@HiltViewModel
class CelebrationViewModel @Inject constructor(private val gamification: GamificationRepository) : ViewModel() {
    val current: StateFlow<Celebration?> = gamification.celebrations.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun dismiss() = gamification.dismissCelebration()
}

/**
 * Sits above the whole app and shows queued celebrations one at a time — a streak milestone
 * and a level-up earned by the same meal play back to back.
 */
@Composable
fun CelebrationOverlay(vm: CelebrationViewModel = hiltViewModel()) {
    val current by vm.current.collectAsStateWithLifecycle()
    BackHandler(enabled = current != null) { vm.dismiss() }
    AnimatedContent(
        targetState = current,
        transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(350, easing = Decel), initialScale = .96f)) togetherWith fadeOut(tween(200)) },
        label = "celebration",
    ) { c ->
        when (c) {
            null -> Unit
            is Celebration.LevelUp -> LevelUpScreen(c, vm::dismiss)
            is Celebration.StreakMilestone -> StreakScreen(c, vm::dismiss)
            is Celebration.BadgeUnlocked -> BadgeUnlockedScreen(c, vm::dismiss)
        }
    }
}

// ── Level up ────────────────────────────────────────────────────────────────────

@Composable
fun LevelUpScreen(c: Celebration.LevelUp, onContinue: () -> Unit) {
    val before = LevelCurve.progressFor(c.totalXp - c.xpGained)
    val after = LevelCurve.progressFor(c.totalXp)
    val arc = remember(c) { Animatable(if (before.level == c.fromLevel) before.fraction else 0f) }
    var flipped by remember(c) { mutableStateOf(false) }
    var started by remember(c) { mutableStateOf(false) }
    LaunchedEffect(c) {
        started = true
        arc.animateTo(1f, tween(800, easing = Decel)) // finish the old level
        flipped = true
        arc.snapTo(0f)
        arc.animateTo(after.fraction, tween(600, easing = Decel)) // start the new one
    }
    val xp by animateIntAsState(if (started) c.xpGained else 0, tween(900, easing = Decel), label = "xp")
    val tier = LevelTier.forLevel(c.toLevel)

    Column(
        Modifier.fillMaxSize().blockTouches().background(VitalisColors.NightDeep).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            TopoLines(VitalisColors.Lime.copy(alpha = .10f), Modifier.fillMaxSize(), centerY = .3f)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Text("NAIK LEVEL", style = VitalisType.Mono.copy(fontSize = 13.sp, letterSpacing = 2.6.sp), color = VitalisColors.Lime)
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                    LevelRing(arc.value, Modifier.size(220.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Level", style = VitalisType.Small.copy(fontSize = 14.sp), color = VitalisColors.OnNightMuted)
                        AnimatedContent(
                            targetState = if (flipped) c.toLevel else c.fromLevel,
                            transitionSpec = {
                                (slideInVertically(spring(Spring.DampingRatioMediumBouncy)) { it } + fadeIn()) togetherWith
                                    (slideOutVertically(tween(250)) { -it } + fadeOut(tween(200)))
                            },
                            label = "level",
                        ) { level ->
                            Text("$level", style = VitalisType.HeroXl.copy(fontSize = 88.sp, lineHeight = 92.sp), color = Color.White,
                                modifier = Modifier.semantics { heading(); contentDescription = "Naik ke level ${c.toLevel}" })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(9.dp).rotate(45f).clip(RoundedCornerShape(2.dp)).background(tier.color()))
                            Text(tier.label(), style = VitalisType.Small.copy(fontWeight = FontWeight.Bold), color = tier.color())
                        }
                    }
                }
                Text("+${Formatters.kcal(xp)} XP", style = VitalisType.DisplayL.copy(fontSize = 40.sp, lineHeight = 44.sp), color = Color.White)
                Text(
                    "${Formatters.kcal(after.xpIntoLevel.toInt())} / ${Formatters.kcal(after.xpNeededForLevel.toInt())} XP ke level ${after.level + 1}",
                    style = VitalisType.Small.copy(fontSize = 14.sp), color = VitalisColors.OnNightMuted,
                )
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(VitalisColors.Night).padding(horizontal = 18.dp, vertical = 6.dp)) {
                    c.awards.forEachIndexed { i, award ->
                        val alpha by animateFloatAsState(if (started) 1f else 0f, tween(300, delayMillis = 900 + i * 80), label = "row")
                        if (i > 0) HorizontalDivider(color = VitalisColors.NightLine)
                        Row(Modifier.fillMaxWidth().padding(vertical = 11.dp).alpha(alpha), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(award.label() + if (award.multiplier > 1) " · ${award.multiplier}×" else "", style = VitalisType.Body.copy(fontSize = 14.sp), color = Color.White)
                            Text("+${award.amount}", style = VitalisType.Mono.copy(fontSize = 14.sp), color = VitalisColors.Lime)
                        }
                    }
                }
            }
        }
        PillButton("Lanjut", onContinue, Modifier.fillMaxWidth(), kind = ButtonKind.Lime)
    }
}

@Composable
private fun LevelRing(fraction: Float, modifier: Modifier) = Canvas(modifier) {
    val stroke = 14.dp.toPx()
    val inset = stroke / 2
    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
    // Dotted orbit just outside the ring — the set's signature accent.
    drawCircle(
        VitalisColors.Lime.copy(alpha = .45f), radius = size.minDimension / 2 + 18.dp.toPx(),
        style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(.5f, 5.dp.toPx()))),
    )
    drawArc(VitalisColors.NightTrack, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
    drawArc(VitalisColors.Lime, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
}

// ── Streak milestone ──────────────────────────────────────────────────────────

@Composable
fun StreakScreen(c: Celebration.StreakMilestone, onContinue: () -> Unit) {
    val flame = remember(c) { Animatable(.5f) }
    LaunchedEffect(c) { flame.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    val flicker by rememberInfiniteTransition(label = "flicker").animateFloat(-2f, 2f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "rot")
    var started by remember(c) { mutableStateOf(false) }
    LaunchedEffect(c) { started = true }
    val week = (6 downTo 0).map { c.lastActiveDate.minusDays(it.toLong()) }
    val nextFreeze = (c.days / StreakCalculator.DAYS_PER_FREEZE + 1) * StreakCalculator.DAYS_PER_FREEZE

    Column(Modifier.fillMaxSize().blockTouches().background(VitalisColors.Ground).navigationBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .background(Brush.radialGradient(listOf(Color(0xFFF8D9C8), VitalisColors.Ground), center = Offset.Unspecified, radius = 900f)),
            contentAlignment = Alignment.Center,
        ) {
            TopoLines(VitalisColors.Orange.copy(alpha = .25f), Modifier.fillMaxSize(), centerY = .55f)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(
                    Modifier.size(200.dp).scale(flame.value).clip(CircleShape).background(VitalisColors.Orange),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(104.dp).rotate(flicker), tint = Color.White)
                }
                if (c.bonusXp > 0) {
                    Text(
                        "+${Formatters.kcal(c.bonusXp)} XP", style = VitalisType.Mono.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = VitalisColors.Lime,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(VitalisColors.Ink).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${c.days} hari berturut-turut", style = VitalisType.DisplayL, color = VitalisColors.Ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text("Satu makanan atau satu aktivitas per hari sudah cukup menjaganya.", style = VitalisType.Body, color = VitalisColors.InkMuted, textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEachIndexed { i, day ->
                // Days fill left to right, 120 ms apart, each popping slightly past full size.
                val fill by animateFloatAsState(if (started) 1f else 0f, tween(320, delayMillis = 400 + i * 120, easing = Overshoot), label = "day$i")
                val on = fill > .3f
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(38.dp).scale(.8f + .2f * fill).clip(CircleShape)
                            .background(if (on) VitalisColors.Orange else VitalisColors.Sunken),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Color.White) }
                    Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Id).take(3), style = VitalisType.Caption, color = VitalisColors.InkMuted)
                }
            }
        }
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.AcUnit, null, Modifier.size(18.dp), tint = VitalisColors.Water)
            Text(
                if (c.freezes > 0) "Kamu punya ${c.freezes} streak freeze. Berikutnya di hari ke-$nextFreeze."
                else "Streak freeze berikutnya didapat di hari ke-$nextFreeze.",
                style = VitalisType.Small, color = VitalisColors.InkMuted,
            )
        }
        PillButton("Mantap", onContinue, Modifier.fillMaxWidth().padding(20.dp))
    }
}

// ── Badge unlocked ──────────────────────────────────────────────────────────────

@Composable
fun BadgeUnlockedScreen(c: Celebration.BadgeUnlocked, onContinue: () -> Unit) {
    val badge = c.badge
    val pop = remember(c) { Animatable(.4f) }
    LaunchedEffect(c) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    val orbit by rememberInfiniteTransition(label = "orbit").animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")

    Column(
        Modifier.fillMaxSize().blockTouches().background(VitalisColors.NightDeep).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            TopoLines(badge.tier.color().copy(alpha = .18f), Modifier.fillMaxSize(), centerY = .38f)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (badge.hidden) "BADGE RAHASIA" else "BADGE BARU", style = VitalisType.Mono.copy(fontSize = 13.sp, letterSpacing = 2.6.sp), color = VitalisColors.Lime)
                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(220.dp).rotate(orbit)) {
                        drawCircle(
                            badge.tier.color().copy(alpha = .6f),
                            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(.5f, 7.dp.toPx()))),
                        )
                    }
                    BadgeMedallion(badge, unlocked = true, size = 168.dp, modifier = Modifier.scale(pop.value))
                }
                Text(badge.title, style = VitalisType.DisplayL, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                Text(badge.description(), style = VitalisType.Body, color = VitalisColors.OnNightMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(badge.tier.color().copy(alpha = .18f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(9.dp).rotate(45f).clip(RoundedCornerShape(2.dp)).background(badge.tier.color()))
                    Text(badge.tier.label(), style = VitalisType.Small.copy(fontWeight = FontWeight.Bold), color = badge.tier.color())
                }
            }
        }
        PillButton("Mantap", onContinue, Modifier.fillMaxWidth(), kind = ButtonKind.Lime)
    }
}

// ── Shared pieces ─────────────────────────────────────────────────────────────

/** A celebration covers the app; taps must not fall through to the screen underneath. */
@Composable
private fun Modifier.blockTouches() = clickable(remember { MutableInteractionSource() }, indication = null) {}

private fun LevelTier.label() = name.lowercase().replaceFirstChar { it.uppercase() }

private fun LevelTier.color() = when (this) {
    LevelTier.BRONZE -> VitalisColors.TierBronze
    LevelTier.SILVER -> VitalisColors.TierSilver
    LevelTier.GOLD -> VitalisColors.TierGold
    else -> VitalisColors.Lime
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LevelUpPreview() = VitalisTheme {
    LevelUpScreen(
        Celebration.LevelUp(
            fromLevel = 7, toLevel = 8, xpGained = 250, totalXp = 6_228,
            awards = listOf(
                XpAward(XpAction.COMPLETE_GPS_ACTIVITY, 50),
                XpAward(XpAction.PER_KM_DISTANCE, 50, multiplier = 5),
                XpAward(XpAction.BREAK_PERSONAL_RECORD, 150),
            ),
        ),
    ) {}
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun StreakPreview() = VitalisTheme {
    StreakScreen(Celebration.StreakMilestone(days = 7, bonusXp = 100, lastActiveDate = LocalDate.of(2026, 9, 26), freezes = 0)) {}
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BadgeUnlockedPreview() = VitalisTheme {
    BadgeUnlockedScreen(Celebration.BadgeUnlocked(BadgeCatalog.ALL.first { it.id == "hill_starter" })) {}
}
