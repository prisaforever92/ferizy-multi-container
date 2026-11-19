package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.EnduranceAuditResult
import com.example.domain.GlobalRuntimeConfig
import com.example.domain.LogRotationPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Fase4EnduranceCard(
  globalConfig: GlobalRuntimeConfig,
  enduranceAudit: EnduranceAuditResult?,
  onUpdateFase4Config: (
    preset: LogRotationPreset,
    watchdogTargetMb: Int,
    healthReportEnabled: Boolean,
    snapshotIntervalMinutes: Int,
  ) -> Unit,
  onRunEnduranceAudit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.fillMaxWidth().testTag("fase4_endurance_card"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Fase 4: Log Rotation & Ketahanan 24 Jam",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        StatusPill(
          text = if (enduranceAudit?.passed != false) "SIAP 24 JAM" else "CEK RAM",
          tone = if (enduranceAudit?.passed != false) PillTone.SUCCESS else PillTone.WARNING,
        )
      }

      if (enduranceAudit != null) {
        Text(
          text = enduranceAudit.summary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Text(
        text = "Rotasi & Retensi Log (Aman SSD 256 GB):",
        style = MaterialTheme.typography.labelLarge,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        LogRotationPreset.entries.forEach { preset ->
          FilterChip(
            selected = globalConfig.logRotationPreset == preset,
            onClick = {
              onUpdateFase4Config(
                preset,
                globalConfig.watchdogCalibrationTargetMb,
                globalConfig.enduranceHealthReportEnabled,
                globalConfig.healthSnapshotIntervalMinutes,
              )
            },
            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("chip_log_preset_${preset.id}"),
          )
        }
      }

      Text(
        text = "Kalibrasi Watchdog 24 Jam (Reaktif Opsi A — Tab Aktif Aman):",
        style = MaterialTheme.typography.labelLarge,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(320, 350, 380).forEach { mb ->
          FilterChip(
            selected = globalConfig.watchdogCalibrationTargetMb == mb,
            onClick = {
              onUpdateFase4Config(
                globalConfig.logRotationPreset,
                mb,
                globalConfig.enduranceHealthReportEnabled,
                globalConfig.healthSnapshotIntervalMinutes,
              )
            },
            label = { Text("${mb} MB Standby", style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Health Report JSON (${globalConfig.healthSnapshotIntervalMinutes}m)",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = globalConfig.enduranceHealthReportEnabled,
          onCheckedChange = { enabled ->
            onUpdateFase4Config(
              globalConfig.logRotationPreset,
              globalConfig.watchdogCalibrationTargetMb,
              enabled,
              globalConfig.healthSnapshotIntervalMinutes,
            )
          },
          modifier = Modifier.testTag("switch_health_report"),
        )
      }

      if (globalConfig.enduranceHealthReportEnabled) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(30, 60, 120).forEach { mins ->
            FilterChip(
              selected = globalConfig.healthSnapshotIntervalMinutes == mins,
              onClick = {
                onUpdateFase4Config(
                  globalConfig.logRotationPreset,
                  globalConfig.watchdogCalibrationTargetMb,
                  globalConfig.enduranceHealthReportEnabled,
                  mins,
                )
              },
              label = { Text("Snapshot ${mins}m", style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }

      Text(
        text = "Taskbar Monitor Desktop: CPU & RAM PC + Browser tiap 3 detik (Main Process, nol lag sniffer) • Peringatan OVER >=85% + tombol ⚡ Bersih Cepat.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Button(
        onClick = onRunEnduranceAudit,
        modifier = Modifier.fillMaxWidth().testTag("run_endurance_audit_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.VerifiedUser,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Simulasi Audit 24 Jam (i7-4790 / 16 GB / 256 GB)")
      }
    }
  }
}
