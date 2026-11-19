package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.AutoKlikJsonSerializer
import com.example.domain.ZipExporter
import com.example.ui.components.AccountEditorDialog
import com.example.ui.screens.AutoKlikScreen
import com.example.ui.screens.CodeExportScreen
import com.example.ui.screens.PreFlightScreen
import com.example.ui.screens.RoadmapScreen
import com.example.ui.state.FerizyWorkspaceUiState
import com.example.ui.state.FerizyWorkspaceViewModel
import com.example.ui.state.WorkspaceTab
import com.example.ui.theme.FerizyForgeTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FerizyForgeTheme {
        val viewModel: FerizyWorkspaceViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        FerizyForgeAppShell(
          uiState = uiState,
          viewModel = viewModel,
        )
      }
    }
  }
}

private data class NavTabSpec(
  val tab: WorkspaceTab,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val workspaceNavItems =
  listOf(
    NavTabSpec(
      tab = WorkspaceTab.PREFLIGHT,
      labelRes = R.string.nav_preflight,
      selectedIcon = Icons.Rounded.Dashboard,
      unselectedIcon = Icons.Outlined.Dashboard,
      testTag = "nav_preflight",
    ),
    NavTabSpec(
      tab = WorkspaceTab.CODE_ZIP,
      labelRes = R.string.nav_export_zip,
      selectedIcon = Icons.Rounded.Code,
      unselectedIcon = Icons.Outlined.Code,
      testTag = "nav_export_zip",
    ),
    NavTabSpec(
      tab = WorkspaceTab.AUTOKLIK,
      labelRes = R.string.nav_autoklik,
      selectedIcon = Icons.Rounded.TouchApp,
      unselectedIcon = Icons.Outlined.TouchApp,
      testTag = "nav_autoklik",
    ),
    NavTabSpec(
      tab = WorkspaceTab.ROADMAP,
      labelRes = R.string.nav_roadmap,
      selectedIcon = Icons.Rounded.Map,
      unselectedIcon = Icons.Outlined.Map,
      testTag = "nav_roadmap",
    ),
  )

@Composable
private fun FerizyForgeAppShell(
  uiState: FerizyWorkspaceUiState,
  viewModel: FerizyWorkspaceViewModel,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  val zipLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
      if (uri != null) {
        val result = ZipExporter.writeElectronZipToUri(context, uri, uiState.generatedFiles)
        result.fold(
          onSuccess = { count ->
            viewModel.showStatusMessage(
              "Berhasil ekspor $count file proyek Electron (${com.example.domain.FerizyBuildVersion.RELEASE_TAG}) ke .ZIP."
            )
          },
          onFailure = { err ->
            viewModel.showStatusMessage("Gagal ekspor ZIP: ${err.message}")
          },
        )
      }
    }

  val jsonLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
      if (uri != null) {
        val selectedAcc = uiState.selectedPointsAccount
        val singleJson = AutoKlikJsonSerializer.buildSingleAccountExportJson(selectedAcc, uiState.globalConfig)
        val result = ZipExporter.writeTextFileToUri(context, uri, singleJson)
        result.fold(
          onSuccess = {
            viewModel.showStatusMessage("Konfigurasi titik ${selectedAcc.name} disimpan ke .JSON.")
          },
          onFailure = { err ->
            viewModel.showStatusMessage("Gagal simpan JSON: ${err.message}")
          },
        )
      }
    }

  val openJsonLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("File tidak dapat dibaca")
        }.fold(
          onSuccess = { raw -> viewModel.importAccountJson(uiState.selectedAccountIdForPoints, raw) },
          onFailure = { err -> viewModel.showStatusMessage("Gagal membuka file JSON: ${err.message}") },
        )
      }
    }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  BackHandler(enabled = uiState.currentTab != WorkspaceTab.PREFLIGHT) {
    viewModel.selectTab(WorkspaceTab.PREFLIGHT)
  }

  val triggerZipExport = { zipLauncher.launch("${com.example.domain.FerizyBuildVersion.ZIP_BASENAME}.zip") }
  val triggerJsonExport = {
    jsonLauncher.launch("autoklik-akun-${uiState.selectedAccountIdForPoints}.json")
  }
  val triggerJsonOpen = { openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    if (useRail) {
      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      ) { innerPadding ->
        Row(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            workspaceNavItems.forEach { item ->
              val selected = uiState.currentTab == item.tab
              val label = stringResource(id = item.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(item.tab) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
          Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
          ) {
            FerizyScreenContent(
              uiState = uiState,
              viewModel = viewModel,
              onExportZip = triggerZipExport,
              onExportJson = triggerJsonExport,
              onOpenJson = triggerJsonOpen,
              modifier = Modifier.fillMaxHeight().widthIn(max = 900.dp),
            )
          }
        }
      }
    } else {
      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            workspaceNavItems.forEach { item ->
              val selected = uiState.currentTab == item.tab
              val label = stringResource(id = item.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(item.tab) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        },
      ) { innerPadding ->
        FerizyScreenContent(
          uiState = uiState,
          viewModel = viewModel,
          onExportZip = triggerZipExport,
          onExportJson = triggerJsonExport,
          onOpenJson = triggerJsonOpen,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
      }
    }
  }

  val editingAccount = uiState.accounts.find { it.accountId == uiState.editingAccountId }
  if (editingAccount != null) {
    AccountEditorDialog(
      account = editingAccount,
      onDismiss = viewModel::closeAccountEditor,
      onSave = { protocol, host, port, user, pass, geoMode, geoPreset, lat, lng ->
        viewModel.updateAccountProxyAndGeo(
          accountId = editingAccount.accountId,
          protocol = protocol,
          host = host,
          port = port,
          username = user,
          password = pass,
          geoMode = geoMode,
          geoPreset = geoPreset,
          customLat = lat,
          customLng = lng,
        )
      },
    )
  }
}

@Composable
private fun FerizyScreenContent(
  uiState: FerizyWorkspaceUiState,
  viewModel: FerizyWorkspaceViewModel,
  onExportZip: () -> Unit,
  onExportJson: () -> Unit,
  onOpenJson: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (uiState.currentTab) {
    WorkspaceTab.PREFLIGHT -> {
      PreFlightScreen(
        uiState = uiState,
        onAddAccount = viewModel::addAccountContainer,
        onRemoveAccount = viewModel::removeAccountContainer,
        onAddTab = viewModel::addTabToAccount,
        onRemoveTab = viewModel::removeTabFromAccount,
        onToggleTabLogin = viewModel::toggleTabLoginSimulated,
        onToggleTabRunner = viewModel::toggleTabRunner,
        onSimulateTabWin = viewModel::simulateTabWin,
        onSimulateTabCaptcha = viewModel::simulateTabCaptcha,
        onSimulateTabLogout = viewModel::simulateTabLogout,
        onEditAccount = viewModel::openAccountEditor,
        onUpdateGlobal = viewModel::updateGlobalSettings,
        onUpdateDisplayAndZoom = viewModel::updateDisplayAndZoomSettings,
        onUpdateFase3Config = viewModel::updateFase3Config,
        onResumeAllCaptcha = viewModel::resumeAllPausedCaptchaTabs,
        onUpdateFase4Config = viewModel::updateFase4Config,
        onRunEnduranceAudit = viewModel::runEnduranceAuditSimulation,
        onQuickExportZip = onExportZip,
        onSaveAccountJson = { accId ->
          viewModel.selectPointsAccount(accId)
          onExportJson()
        },
        onImportAccountJson = viewModel::importAccountJson,
        onNotifyMessage = viewModel::showStatusMessage,
        modifier = modifier,
      )
    }
    WorkspaceTab.CODE_ZIP -> {
      CodeExportScreen(
        uiState = uiState,
        onSelectFile = viewModel::selectGeneratedFile,
        onExportZip = onExportZip,
        onNotifyCopied = viewModel::showStatusMessage,
        modifier = modifier,
      )
    }
    WorkspaceTab.AUTOKLIK -> {
      AutoKlikScreen(
        uiState = uiState,
        onSelectAccount = viewModel::selectPointsAccount,
        onUpdateExtensionSettings = viewModel::updateAccountExtensionSettings,
        onAddPoint = viewModel::addAutoKlikPoint,
        onUpdateInputText = viewModel::updateInputPointText,
        onToggleAntiRevert = viewModel::togglePointAntiRevert,
        onDeletePoint = viewModel::deleteAutoKlikPoint,
        onCopyAccountPoints = viewModel::copyPointsFromAccount,
        onPastePointsToAll = viewModel::pastePointsToAllAccounts,
        onExportJson = onExportJson,
        onOpenAccountJson = onOpenJson,
        onNotifyCopied = viewModel::showStatusMessage,
        modifier = modifier,
      )
    }
    WorkspaceTab.ROADMAP -> {
      RoadmapScreen(
        uiState = uiState,
        onToggleReviewedOptional = viewModel::toggleReviewedOptionalModule,
        modifier = modifier,
      )
    }
  }
}
