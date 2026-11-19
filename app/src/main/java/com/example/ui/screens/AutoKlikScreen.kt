package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.AutoKlikExtensionSettings
import com.example.domain.AutoKlikPoint
import com.example.domain.AutoKlikPointType
import com.example.ui.components.PillTone
import com.example.ui.components.StatusPill
import com.example.ui.state.FerizyWorkspaceUiState
import com.example.ui.theme.CodeSurfaceDark
import com.example.ui.theme.CodeTextLight
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoKlikScreen(
  uiState: FerizyWorkspaceUiState,
  onSelectAccount: (Int) -> Unit,
  onUpdateExtensionSettings: (Int, AutoKlikExtensionSettings) -> Unit,
  onAddPoint: (
    accountId: Int,
    type: AutoKlikPointType,
    label: String,
    x: Int,
    y: Int,
    endX: Int?,
    endY: Int?,
    selector: String,
    inputValue: String,
    popupKey: String,
    delayAfterMs: Int,
  ) -> Unit,
  onUpdateInputText: (accountId: Int, pointId: String, newText: String) -> Unit,
  onToggleAntiRevert: (accountId: Int, pointId: String) -> Unit,
  onDeletePoint: (accountId: Int, pointId: String) -> Unit,
  onCopyAccountPoints: (Int) -> Unit,
  onPastePointsToAll: () -> Unit,
  onExportJson: () -> Unit,
  onOpenAccountJson: () -> Unit,
  onNotifyCopied: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clipboard = LocalClipboardManager.current
  val currentAccount = uiState.selectedPointsAccount
  var showAddDialog by remember { mutableStateOf(false) }

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
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            StatusPill(text = "AutoKlik Pro v3.2", tone = PillTone.INFO)
            StatusPill(text = "ntfy: ${uiState.globalConfig.ntfyTopic}", tone = PillTone.WARNING)
          }
          Text(
            text = "Profil Titik Per-Akun & Ekspor .JSON",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text =
              "Data penumpang/teks disimpan langsung di dalam konfigurasi Titik Input Ketik (tidak dipisah menjadi form eksternal), kompatibel dengan skema AutoKlik Pro v3.2.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            uiState.accounts.forEach { acc ->
              FilterChip(
                selected = acc.accountId == currentAccount.accountId,
                onClick = { onSelectAccount(acc.accountId) },
                label = { Text("${acc.name} (${acc.points.size})") },
                modifier = Modifier.testTag("points_account_chip_${acc.accountId}"),
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { onCopyAccountPoints(currentAccount.accountId) },
              modifier = Modifier.testTag("copy_points_button"),
            ) {
              Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Copy ${currentAccount.name}")
            }
            OutlinedButton(
              onClick = onPastePointsToAll,
              modifier = Modifier.testTag("paste_all_points_button"),
            ) {
              Icon(imageVector = Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Paste ke Semua Akun")
            }
            Button(
              onClick = { showAddDialog = true },
              modifier = Modifier.testTag("add_point_button"),
            ) {
              Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Tambah Titik")
            }
            Button(
              onClick = onExportJson,
              modifier = Modifier.testTag("export_autoklik_json_button"),
            ) {
              Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save .JSON")
            }
            OutlinedButton(
              onClick = onOpenAccountJson,
              modifier = Modifier.testTag("open_autoklik_json_button"),
            ) {
              Icon(imageVector = Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open .JSON")
            }
          }
        }
      }
    }

    item {
      val ext = currentAccount.extensionSettings
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "AutoKlik-v3.2-Mandiri-Extension (${currentAccount.name})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "Tersimpan terpisah di ${currentAccount.partition} & bisa diedit langsung dari browser desktop.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = ext.enabled,
              onCheckedChange = { checked ->
                onUpdateExtensionSettings(currentAccount.accountId, ext.copy(enabled = checked))
              },
              modifier = Modifier.testTag("ext_enabled_switch"),
            )
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = ext.loopEnabled,
              onClick = {
                onUpdateExtensionSettings(currentAccount.accountId, ext.copy(loopEnabled = !ext.loopEnabled))
              },
              label = { Text(if (ext.loopEnabled) "Loop Otomatis: ON" else "Loop Otomatis: OFF") },
              modifier = Modifier.testTag("ext_loop_chip"),
            )
            FilterChip(
              selected = ext.smartMatch,
              onClick = {
                onUpdateExtensionSettings(currentAccount.accountId, ext.copy(smartMatch = !ext.smartMatch))
              },
              label = { Text("Smart Match") },
              modifier = Modifier.testTag("ext_smartmatch_chip"),
            )
            FilterChip(
              selected = ext.antiRevert,
              onClick = {
                onUpdateExtensionSettings(currentAccount.accountId, ext.copy(antiRevert = !ext.antiRevert))
              },
              label = { Text("Lazy Anti-Revert") },
              modifier = Modifier.testTag("ext_antirevert_chip"),
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = ext.maxLoops.toString(),
              onValueChange = { raw ->
                val v = raw.filter(Char::isDigit).toIntOrNull() ?: 0
                onUpdateExtensionSettings(currentAccount.accountId, ext.copy(maxLoops = v))
              },
              label = { Text("Maks Loop (0=∞)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("ext_maxloops_input"),
            )
            OutlinedTextField(
              value = ext.loopDelayMs.toString(),
              onValueChange = { raw ->
                val v = raw.filter(Char::isDigit).toIntOrNull() ?: 350
                onUpdateExtensionSettings(currentAccount.accountId, ext.copy(loopDelayMs = v))
              },
              label = { Text("Jeda Loop (ms)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("ext_loopdelay_input"),
            )
            OutlinedTextField(
              value = ext.radiusPx.toString(),
              onValueChange = { raw ->
                val v = raw.filter(Char::isDigit).toIntOrNull() ?: 28
                onUpdateExtensionSettings(currentAccount.accountId, ext.copy(radiusPx = v))
              },
              label = { Text("Radius (px)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("ext_radius_input"),
            )
          }
        }
      }
    }

    items(currentAccount.points, key = { it.id }) { point ->
      AutoKlikPointCard(
        point = point,
        onUpdateInputText = { newText ->
          onUpdateInputText(currentAccount.accountId, point.id, newText)
        },
        onToggleAntiRevert = { onToggleAntiRevert(currentAccount.accountId, point.id) },
        onDelete = { onDeletePoint(currentAccount.accountId, point.id) },
      )
    }

    item {
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
            Text(
              text = "Pratinjau Skema .JSON v3.2",
              style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
              onClick = {
                clipboard.setText(AnnotatedString(uiState.autoklikPreviewJson))
                onNotifyCopied("JSON AutoKlik Pro v3.2 disalin ke clipboard.")
              },
            ) {
              Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Salin JSON")
            }
          }
          Surface(
            shape = MaterialTheme.shapes.small,
            color = CodeSurfaceDark,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = uiState.autoklikPreviewJson,
              fontFamily = JetBrainsMonoFamily,
              style = MaterialTheme.typography.labelSmall,
              color = CodeTextLight,
              modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddAutoKlikPointDialog(
      accountName = currentAccount.name,
      onDismiss = { showAddDialog = false },
      onConfirm = { type, label, x, y, endX, endY, selector, inputVal, popupKey, delayMs ->
        onAddPoint(
          currentAccount.accountId,
          type,
          label,
          x,
          y,
          endX,
          endY,
          selector,
          inputVal,
          popupKey,
          delayMs,
        )
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun AutoKlikPointCard(
  point: AutoKlikPoint,
  onUpdateInputText: (String) -> Unit,
  onToggleAntiRevert: () -> Unit,
  onDelete: () -> Unit,
) {
  val tone =
    when (point.type) {
      AutoKlikPointType.TAP -> PillTone.INFO
      AutoKlikPointType.SWIPE -> PillTone.WARNING
      AutoKlikPointType.INPUT -> PillTone.SUCCESS
      AutoKlikPointType.POPUP_KEY -> PillTone.NEUTRAL
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
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          StatusPill(text = "#${point.order} ${point.type.badge}", tone = tone)
          Text(
            text = point.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        IconButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Hapus titik ${point.label}",
          )
        }
      }

      val coordInfo =
        when (point.type) {
          AutoKlikPointType.SWIPE ->
            "Swipe (${point.x},${point.y}) -> (${point.endX ?: point.x},${point.endY ?: point.y}) • ${point.durationMs}ms • jeda ${point.delayAfterMs}ms • radius ${point.radiusPx}px"
          AutoKlikPointType.POPUP_KEY ->
            "Popup Key: ${point.popupKey} • (${point.x},${point.y}) • jeda ${point.delayAfterMs}ms"
          else ->
            "Koordinat (${point.x},${point.y}) • jeda ${point.delayAfterMs}ms • radius ${point.radiusPx}px"
        }

      Text(
        text = coordInfo,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (point.selector.isNotBlank()) {
        Text(
          text = "Cache Selector: ${point.selector}",
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (point.type == AutoKlikPointType.INPUT) {
        OutlinedTextField(
          value = point.inputValue,
          onValueChange = onUpdateInputText,
          label = { Text("Isi Teks / Data Penumpang (Di Dalam Titik Input)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_point_field_${point.id}"),
        )
        FilterChip(
          selected = point.antiRevert,
          onClick = onToggleAntiRevert,
          label = {
            Text(
              text = if (point.antiRevert) "Lazy Anti-Revert: Aktif" else "Lazy Anti-Revert: Mati",
              style = MaterialTheme.typography.labelSmall,
            )
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddAutoKlikPointDialog(
  accountName: String,
  onDismiss: () -> Unit,
  onConfirm: (
    type: AutoKlikPointType,
    label: String,
    x: Int,
    y: Int,
    endX: Int?,
    endY: Int?,
    selector: String,
    inputValue: String,
    popupKey: String,
    delayAfterMs: Int,
  ) -> Unit,
) {
  var type by remember { mutableStateOf(AutoKlikPointType.TAP) }
  var label by remember { mutableStateOf("") }
  var xText by remember { mutableStateOf("540") }
  var yText by remember { mutableStateOf("420") }
  var endXText by remember { mutableStateOf("540") }
  var endYText by remember { mutableStateOf("200") }
  var selector by remember { mutableStateOf("") }
  var inputValue by remember { mutableStateOf("") }
  var popupKey by remember { mutableStateOf("Enter") }
  var delayText by remember { mutableStateOf("500") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Tambah Titik AutoKlik • $accountName") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AutoKlikPointType.entries.forEach { ptType ->
            FilterChip(
              selected = type == ptType,
              onClick = { type = ptType },
              label = { Text(ptType.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        OutlinedTextField(
          value = label,
          onValueChange = { label = it },
          label = { Text("Nama Titik") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = xText,
            onValueChange = { xText = it.filter(Char::isDigit) },
            label = { Text("X") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = yText,
            onValueChange = { yText = it.filter(Char::isDigit) },
            label = { Text("Y") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = delayText,
            onValueChange = { delayText = it.filter(Char::isDigit) },
            label = { Text("Jeda ms (0.5s=500)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        if (type == AutoKlikPointType.SWIPE) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = endXText,
              onValueChange = { endXText = it.filter(Char::isDigit) },
              label = { Text("End X") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = endYText,
              onValueChange = { endYText = it.filter(Char::isDigit) },
              label = { Text("End Y") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }
        if (type == AutoKlikPointType.INPUT) {
          OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Teks / Data Penumpang") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        if (type == AutoKlikPointType.POPUP_KEY) {
          OutlinedTextField(
            value = popupKey,
            onValueChange = { popupKey = it },
            label = { Text("Key (Enter / Tab / Escape / Space)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        OutlinedTextField(
          value = selector,
          onValueChange = { selector = it },
          label = { Text("CSS Selector (Opsional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            type,
            label,
            xText.toIntOrNull() ?: 540,
            yText.toIntOrNull() ?: 420,
            if (type == AutoKlikPointType.SWIPE) endXText.toIntOrNull() ?: 540 else null,
            if (type == AutoKlikPointType.SWIPE) endYText.toIntOrNull() ?: 200 else null,
            selector,
            inputValue,
            popupKey,
            delayText.toIntOrNull() ?: 500,
          )
        },
      ) {
        Text("Tambahkan")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Batal")
      }
    },
  )
}
