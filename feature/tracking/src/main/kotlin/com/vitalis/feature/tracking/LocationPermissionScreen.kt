package com.vitalis.feature.tracking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.vitalis.core.designsystem.component.ButtonKind
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.TopoLines
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.SportType

/** Where the user stands on location, as far as recording a route is concerned. */
enum class LocationAccess { PRIME, APPROXIMATE, BLOCKED, GRANTED }

/**
 * Reads the outcome of a permission request. Precise location is the bar: an approximate fix
 * (~1 km) is rejected by every sport's accuracy gate, so a run would record nothing.
 *
 * @param canAskAgain `shouldShowRequestPermissionRationale(FINE)`: false after a request means the
 *   system will not show its dialog again and only Settings can grant it.
 */
fun locationAccessAfterRequest(fine: Boolean, coarse: Boolean, canAskAgain: Boolean): LocationAccess = when {
    fine -> LocationAccess.GRANTED
    !canAskAgain -> LocationAccess.BLOCKED
    coarse -> LocationAccess.APPROXIMATE
    else -> LocationAccess.PRIME
}

internal fun Context.hasFineLocation() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Permission priming (spec §11.3): says why before the system dialog asks, and routes a
 * permanent "no" to Settings instead of a dead button.
 */
@Composable
fun LocationPermissionRoute(sport: SportType, onGranted: () -> Unit, onBack: () -> Unit, vm: TrackingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var access by rememberSaveable { mutableStateOf(LocationAccess.PRIME) }
    var starting by remember { mutableStateOf(false) }

    fun proceed() {
        if (starting) return // the dialog result and ON_RESUME can both report the grant
        starting = true
        vm.selectSport(sport)
        vm.start(onStarted = onGranted)
    }

    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ?: true
        access = locationAccessAfterRequest(
            fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
            canAskAgain = canAskAgain,
        )
        if (access == LocationAccess.GRANTED) proceed()
    }

    // Coming back from Settings with precise location switched on: carry straight on.
    LifecycleResumeEffect(Unit) {
        if (context.hasFineLocation()) proceed()
        onPauseOrDispose {}
    }

    LocationPermissionScreen(
        access = access,
        onAllow = {
            if (access == LocationAccess.BLOCKED) {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            } else {
                request.launch(TrackingPermissions)
            }
        },
        onLater = onBack,
    )
}

@Composable
fun LocationPermissionScreen(access: LocationAccess, onAllow: () -> Unit, onLater: () -> Unit) {
    Column(Modifier.fillMaxSize().background(VitalisColors.NightDeep)) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            TopoLines(VitalisColors.Lime.copy(alpha = .22f), Modifier.fillMaxSize(), centerY = .55f)
            LocationPin()
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(VitalisColors.Ground)
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val (title, action) = when (access) {
                LocationAccess.APPROXIMATE -> "Rute butuh lokasi akurat" to "Izinkan lokasi akurat"
                LocationAccess.BLOCKED -> "Izin lokasi dimatikan" to "Buka pengaturan"
                else -> "Izinkan lokasi untuk merekam rute" to "Izinkan lokasi"
            }
            Text(title, style = VitalisType.DisplayM.copy(fontSize = VitalisType.DisplayM.fontSize * .93f), color = VitalisColors.Ink, modifier = Modifier.semantics { heading() })
            when (access) {
                LocationAccess.APPROXIMATE -> Explain("Kamu memilih lokasi perkiraan. Dengan akurasi sekitar 1 km, jarak dan pace tidak bisa dihitung. Pilih \"Akurat\" saat diminta.")
                LocationAccess.BLOCKED -> Explain("Buka Izin › Lokasi, pilih \"Hanya saat aplikasi digunakan\", lalu nyalakan \"Gunakan lokasi akurat\". Kamu akan kembali ke sini otomatis.")
                else -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Point(Icons.Rounded.PlayArrow, "Hanya saat merekam", "GPS menyala setelah kamu menekan Mulai, dan mati saat selesai.")
                    Point(Icons.Rounded.Shield, "Rute tetap di perangkat", "Tidak ada unggahan selama sesi berjalan.")
                    Point(Icons.Rounded.NotificationsActive, "Notifikasi selama merekam", "Menampilkan durasi dan jarak saat layar mati.")
                }
            }
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PillButton(action, onAllow, Modifier.fillMaxWidth(), kind = ButtonKind.Lime)
                PillButton("Nanti saja", onLater, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost, height = 48.dp, textColor = VitalisColors.InkMuted)
            }
        }
    }
}

/** Lime pin with two soft halos and one slow ring pulsing outward, like a fix being acquired. */
@Composable
private fun LocationPin() {
    val pulse by rememberInfiniteTransition(label = "pin").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing)), label = "pulse",
    )
    Box(
        Modifier
            .size(96.dp)
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(VitalisColors.Lime.copy(alpha = .14f), r + 14.dp.toPx())
                drawCircle(VitalisColors.Lime.copy(alpha = .06f), r + 34.dp.toPx())
                drawCircle(VitalisColors.Lime.copy(alpha = .35f * (1 - pulse)), r + 70.dp.toPx() * pulse, style = Stroke(1.5.dp.toPx()))
            }
            .clip(CircleShape)
            .background(VitalisColors.Lime),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.LocationOn, null, Modifier.size(44.dp), tint = VitalisColors.Ink) }
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(VitalisColors.Sunken), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = VitalisColors.Ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = VitalisType.BodyStrong, color = VitalisColors.Ink)
            Text(body, style = VitalisType.Small, color = VitalisColors.InkMuted)
        }
    }
}

@Composable
private fun Explain(text: String) = Text(text, style = VitalisType.Body, color = VitalisColors.InkMuted)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocationPrimePreview() = VitalisTheme { LocationPermissionScreen(LocationAccess.PRIME, {}, {}) }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocationBlockedPreview() = VitalisTheme { LocationPermissionScreen(LocationAccess.BLOCKED, {}, {}) }
