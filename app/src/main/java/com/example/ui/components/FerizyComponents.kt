package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AccountContainerConfig
import com.example.domain.GeoProviderMode
import com.example.domain.PortGeoPreset
import com.example.domain.ProxyProtocol
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed

enum class PillTone {
  SUCCESS,
  INFO,
  WARNING,
  ERROR,
  NEUTRAL,
}

@Composable
fun StatusPill(
  text: String,
  tone: PillTone,
  modifier: Modifier = Modifier,
) {
  val (bg, fg, border) =
    when (tone) {
      PillTone.SUCCESS ->
        Triple(
          StatusGreen.copy(alpha = 0.16f),
          StatusGreen,
          StatusGreen.copy(alpha = 0.45f),
        )
      PillTone.INFO ->
        Triple(
          MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          MaterialTheme.colorScheme.primary,
          MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        )
      PillTone.WARNING ->
        Triple(
          StatusAmber.copy(alpha = 0.18f),
          StatusAmber,
          StatusAmber.copy(alpha = 0.45f),
        )
      PillTone.ERROR ->
        Triple(
          StatusRed.copy(alpha = 0.18f),
          StatusRed,
          StatusRed.copy(alpha = 0.45f),
        )
      PillTone.NEUTRAL ->
        Triple(
          MaterialTheme.colorScheme.surfaceVariant,
          MaterialTheme.colorScheme.onSurfaceVariant,
          MaterialTheme.colorScheme.outline,
        )
    }

  Surface(
    color = bg,
    contentColor = fg,
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, border),
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun HeroWorkspaceBanner(
  totalTabs: Int,
  accountCount: Int = 5,
  maxTabs: Int = 15,
  estimatedRamGb: Double,
  onQuickExportZip: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.fillMaxWidth(),
  ) {
    Box {
      Image(
        painter = painterResource(id = R.drawable.img_ferizy_hero_1794849965607),
        contentDescription = stringResource(id = R.string.cd_hero_banner),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(168.dp).clip(MaterialTheme.shapes.large),
      )
      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              Brush.verticalGradient(
                colors =
                  listOf(
                    Color(0xD9070E1B),
                    Color(0xEB070E1B),
                  )
              )
            )
      )
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StatusPill(text = "FASE 1–4 AKTIF", tone = PillTone.INFO)
          StatusPill(text = "$accountCount Akun • $totalTabs/$maxTabs Tab", tone = PillTone.SUCCESS)
          StatusPill(
            text = String.format("~%.2f GB / 16 GB", estimatedRamGb),
            tone = PillTone.NEUTRAL,
          )
        }
        Text(
          text = "Browser Multi-Container Ferizy",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
        )
        Text(
          text =
            "Sniffer 0ms + Event Bus <1ms, Notif Terpusat, Watchdog 350MB, 5 Modul Opsional, Log Rotation & Uji 24 Jam.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFCBD5E1),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onQuickExportZip,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.testTag("hero_export_zip_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Archive,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ekspor .ZIP ${com.example.domain.FerizyBuildVersion.RELEASE_TAG}")
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Computer,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "i7-4790 • 256GB SSD",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountEditorDialog(
  account: AccountContainerConfig,
  onDismiss: () -> Unit,
  onSave: (
    protocol: ProxyProtocol,
    host: String,
    port: Int,
    username: String,
    password: String,
    geoMode: GeoProviderMode,
    geoPreset: PortGeoPreset,
    customLat: Double,
    customLng: Double,
  ) -> Unit,
) {
  var protocol by remember(account.accountId) { mutableStateOf(account.proxy.protocol) }
  var host by remember(account.accountId) { mutableStateOf(account.proxy.host) }
  var portText by remember(account.accountId) { mutableStateOf(account.proxy.port.toString()) }
  var username by remember(account.accountId) { mutableStateOf(account.proxy.username) }
  var password by remember(account.accountId) { mutableStateOf(account.proxy.password) }
  var geoMode by remember(account.accountId) { mutableStateOf(account.geoMode) }
  var geoPreset by remember(account.accountId) { mutableStateOf(account.geoPreset) }
  var customLatText by remember(account.accountId) { mutableStateOf(account.customLat.toString()) }
  var customLngText by remember(account.accountId) { mutableStateOf(account.customLng.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${account.name} (${account.partition})",
          style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Rounded.Close, contentDescription = "Tutup dialog")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Jalur Multi-IP & Proxy (WebRTC Anti-Leak aktif)",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ProxyProtocol.entries.forEach { item ->
            FilterChip(
              selected = protocol == item,
              onClick = { protocol = item },
              label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        if (protocol != ProxyProtocol.DIRECT) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = host,
              onValueChange = { host = it },
              label = { Text("Host / IP") },
              singleLine = true,
              modifier = Modifier.weight(2f),
            )
            OutlinedTextField(
              value = portText,
              onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
              label = { Text("Port") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = username,
              onValueChange = { username = it },
              label = { Text("Username") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Geolocation Provider 0ms",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          GeoProviderMode.entries.forEach { mode ->
            FilterChip(
              selected = geoMode == mode,
              onClick = { geoMode = mode },
              label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        if (geoMode == GeoProviderMode.PRESET_0MS) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            PortGeoPreset.entries.forEach { preset ->
              FilterChip(
                selected = geoPreset == preset,
                onClick = { geoPreset = preset },
                label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
          if (geoPreset == PortGeoPreset.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = customLatText,
                onValueChange = { customLatText = it },
                label = { Text("Latitude") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = customLngText,
                onValueChange = { customLngText = it },
                label = { Text("Longitude") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            protocol,
            host,
            portText.toIntOrNull() ?: 8080,
            username,
            password,
            geoMode,
            geoPreset,
            customLatText.toDoubleOrNull() ?: account.customLat,
            customLngText.toDoubleOrNull() ?: account.customLng,
          )
        },
        modifier = Modifier.testTag("save_account_editor_button"),
      ) {
        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Simpan ke Blueprint")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Batal")
      }
    },
  )
}
