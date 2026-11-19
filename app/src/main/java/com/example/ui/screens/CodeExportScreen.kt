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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.PillTone
import com.example.ui.components.StatusPill
import com.example.ui.state.FerizyWorkspaceUiState
import com.example.ui.theme.CodeSurfaceDark
import com.example.ui.theme.CodeTextLight
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeExportScreen(
  uiState: FerizyWorkspaceUiState,
  onSelectFile: (String) -> Unit,
  onExportZip: () -> Unit,
  onNotifyCopied: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clipboard = LocalClipboardManager.current
  val selectedFile = uiState.selectedFile

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
            StatusPill(text = "${uiState.generatedFiles.size} File Siap", tone = PillTone.SUCCESS)
            StatusPill(text = "Electron 33 • Win x64 • ${com.example.domain.FerizyBuildVersion.RELEASE_TAG}", tone = PillTone.INFO)
          }
          Text(
            text = "Generator Proyek Electron Fase 1–4 (${com.example.domain.FerizyBuildVersion.RELEASE_TAG})",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text =
              "Seluruh kode Fase 1–4 (0ms API Sniffer, Event Bus <1ms, Clock Sync, Notif Terpusat Opsi A, 5 Modul Opsional, Log Rotation & Uji Ketahanan 24 Jam) siap diunduh dan di-build (.exe) di Windows.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = onExportZip,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.fillMaxWidth().testTag("export_zip_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Archive,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unduh Proyek Electron ${com.example.domain.FerizyBuildVersion.RELEASE_TAG} (.ZIP)")
          }
        }
      }
    }

    item {
      val winCmd = "npm install && npm run test:endurance && npm start\n# Build installer & portable .exe:\nnpm run build:win"
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
                imageVector = Icons.Rounded.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Perintah Build Windows (i7-4790 / 16 GB)",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            OutlinedButton(
              onClick = {
                clipboard.setText(AnnotatedString("npm install && npm run build:win"))
                onNotifyCopied("Perintah build Windows disalin.")
              },
            ) {
              Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Salin")
            }
          }
          Surface(
            shape = MaterialTheme.shapes.small,
            color = CodeSurfaceDark,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = winCmd,
              fontFamily = JetBrainsMonoFamily,
              style = MaterialTheme.typography.labelMedium,
              color = CodeTextLight,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }
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
          Text(
            text = "Penjelajah Berkas Sumber Electron",
            style = MaterialTheme.typography.titleMedium,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            uiState.generatedFiles.forEach { file ->
              FilterChip(
                selected = file.relativePath == uiState.selectedFilePath,
                onClick = { onSelectFile(file.relativePath) },
                label = {
                  Text(
                    text = file.relativePath,
                    style = MaterialTheme.typography.labelSmall,
                  )
                },
                modifier = Modifier.testTag("file_chip_${file.relativePath}"),
              )
            }
          }
        }
      }
    }

    if (selectedFile != null) {
      item {
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
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = selectedFile.relativePath,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Text(
                  text = "${selectedFile.category} • ${selectedFile.summary}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              OutlinedButton(
                onClick = {
                  clipboard.setText(AnnotatedString(selectedFile.content))
                  onNotifyCopied("${selectedFile.relativePath} disalin ke clipboard.")
                },
                modifier = Modifier.testTag("copy_code_button"),
              ) {
                Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salin Kode")
              }
            }

            Surface(
              shape = MaterialTheme.shapes.small,
              color = CodeSurfaceDark,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth(),
            ) {
              val numberedCode =
                selectedFile.content.lines().mapIndexed { index, line ->
                  "${(index + 1).toString().padStart(3, ' ')} | $line"
                }.joinToString("\n")
              Text(
                text = numberedCode,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = CodeTextLight,
                modifier =
                  Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
              )
            }
          }
        }
      }
    }
  }
}
