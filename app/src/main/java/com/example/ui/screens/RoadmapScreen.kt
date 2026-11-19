package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.OptionalFeatureOption
import com.example.domain.PhaseStatus
import com.example.domain.RoadmapPhaseInfo
import com.example.ui.components.PillTone
import com.example.ui.components.StatusPill
import com.example.ui.state.FerizyWorkspaceUiState

@Composable
fun RoadmapScreen(
  uiState: FerizyWorkspaceUiState,
  onToggleReviewedOptional: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StatusPill(text = "ATURAN KERJA BERTAHAP", tone = PillTone.INFO)
          Text(
            text = "Roadmap 4 Fase & Opsi Fitur Tambahan",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text =
              "Seluruh 4 Fase (Multi-Container Fase 1, Mesin Native Fase 2, Sniffer 0ms & Notif Terpusat Fase 3, serta Log Rotation & Uji Ketahanan 24 Jam Fase 4) sudah aktif dan siap diekspor ke paket Electron (.ZIP).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    items(uiState.roadmapPhases, key = { it.phaseNumber }) { phase ->
      PhaseCard(phase = phase)
    }

    item {
      Text(
        text = "Ceklist Fitur Tambahan Opsional (Evaluasi Opsi & Dampak)",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 6.dp),
      )
    }

    items(uiState.optionalModules, key = { it.id }) { option ->
      OptionalModuleCard(
        option = option,
        checked = option.id in uiState.reviewedOptionalIds,
        onToggle = { onToggleReviewedOptional(option.id) },
      )
    }
  }
}

@Composable
private fun PhaseCard(phase: RoadmapPhaseInfo) {
  val (tone, icon) =
    when (phase.status) {
      PhaseStatus.IMPLEMENTED -> PillTone.SUCCESS to Icons.Rounded.CheckCircle
      PhaseStatus.AWAITING_CONFIRMATION -> PillTone.WARNING to Icons.Rounded.HourglassTop
      PhaseStatus.PLANNED -> PillTone.NEUTRAL to Icons.Rounded.Schedule
    }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
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
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          StatusPill(text = phase.status.label, tone = tone)
        }
      }
      Text(
        text = phase.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = phase.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      phase.deliverables.forEach { item ->
        Text(
          text = "• $item",
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = phase.optionsAndImpact,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(10.dp),
        )
      }
    }
  }
}

@Composable
private fun OptionalModuleCard(
  option: OptionalFeatureOption,
  checked: Boolean,
  onToggle: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          StatusPill(text = option.targetPhase, tone = PillTone.INFO)
          Text(
            text = option.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
          )
        }
        Checkbox(
          checked = checked,
          onCheckedChange = { onToggle() },
        )
      }
      Text(
        text = option.description,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Opsi: ${option.optionsDetail}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = "Dampak RAM/CPU: ${option.impactRamCpu}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "Dampak Risiko/Kecepatan: ${option.impactRiskSpeed}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
