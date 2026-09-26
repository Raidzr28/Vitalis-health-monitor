package com.vitalis.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisType

enum class MainTab(val label: String, val icon: ImageVector) {
    Today("Hari ini", Icons.Rounded.Home),
    Diary("Diary", Icons.AutoMirrored.Rounded.MenuBook),
    Track("Mulai aktivitas", Icons.Rounded.PlayArrow),
    Progress("Progress", Icons.Rounded.BarChart),
    Profile("Profil", Icons.Rounded.Person),
}

/** Floating ink pill with the lime Track button in the middle (spec §11.1). */
@Composable
fun VitalisTabBar(selected: MainTab?, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(18.dp, RoundedCornerShape(34.dp), ambientColor = VitalisColors.Ink, spotColor = VitalisColors.Ink)
                .clip(RoundedCornerShape(34.dp))
                .background(VitalisColors.Night)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { tab ->
                if (tab == MainTab.Track) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(VitalisColors.Lime)
                            .clickable(role = Role.Tab) { onSelect(tab) }
                            .semantics { contentDescription = tab.label; this.selected = selected == tab },
                        contentAlignment = Alignment.Center,
                    ) { Icon(tab.icon, null, Modifier.size(28.dp), tint = VitalisColors.Ink) }
                } else {
                    val color = if (selected == tab) VitalisColors.Lime else VitalisColors.OnNightMuted
                    Column(
                        Modifier
                            .width(62.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.Tab) { onSelect(tab) }
                            .semantics { this.selected = selected == tab },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Icon(tab.icon, null, Modifier.size(22.dp), tint = color)
                        Text(tab.label, style = VitalisType.Caption.copy(fontSize = VitalisType.Caption.fontSize * 0.92f), color = color)
                    }
                }
            }
        }
    }
}

/** Space to leave under scrolling content so the floating bar never covers it. */
val TabBarClearance = 120.dp
