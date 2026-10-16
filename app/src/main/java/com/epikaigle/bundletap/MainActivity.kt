package com.epikaigle.bundletap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.epikaigle.bundletap.install.ApkArchive
import com.epikaigle.bundletap.install.InstallPhase
import com.epikaigle.bundletap.install.InstallUpdate
import com.epikaigle.bundletap.install.InstallUpdates
import com.epikaigle.bundletap.install.PackageInstallEngine
import com.epikaigle.bundletap.ui.BundleTapScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var isBusy by mutableStateOf(false)
    private var update by mutableStateOf(InstallUpdates.current.value)
    private var sourceAllowed by mutableStateOf(false)
    private var selectedUri: Uri? = null
    private var deferredUri: Uri? = null

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) begin(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        sourceAllowed = packageManager.canRequestPackageInstalls()
        InstallUpdates.restore(this)
        deferredUri = savedInstanceState?.getString("deferred_uri")?.let(Uri::parse)
        selectedUri = savedInstanceState?.getString("selected_uri")?.let(Uri::parse)

        setContent {
            BundleTapScreen(
                update = update,
                busy = isBusy,
                sourceAllowed = sourceAllowed,
                canConfirm = InstallUpdates.pendingConfirmation != null,
                canRetry = selectedUri != null && update.phase == InstallPhase.ERROR,
                onSelectFile = { picker.launch(arrayOf("*/*")) },
                onEnableSource = { openUnknownSourcesSettings() },
                onConfirm = { launchPendingConfirmation() },
                onRetry = { selectedUri?.let(::begin) },
                onReset = { clearResult() }
            )
        }
        lifecycleScope.launch {
            InstallUpdates.current.collectLatest { update = it }
        }
        // Configuration changes must not start a second installation.
        if (savedInstanceState == null) handleIncoming(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("deferred_uri", deferredUri?.toString())
        outState.putString("selected_uri", selectedUri?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    override fun onResume() {
        super.onResume()
        sourceAllowed = packageManager.canRequestPackageInstalls()
        val pending = deferredUri
        if (pending != null && sourceAllowed) {
            deferredUri = null
            begin(pending)
        }
    }

    private fun handleIncoming(incoming: Intent?) {
        val uri = when (incoming?.action) {
            Intent.ACTION_VIEW -> incoming.data
            Intent.ACTION_SEND -> incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri != null) begin(uri)
    }

    private fun begin(uri: Uri) {
        if (isBusy || update.phase == InstallPhase.INSTALLING ||
            update.phase == InstallPhase.AWAITING_CONFIRMATION) return
        selectedUri = uri
        val name = runCatching { ApkArchive(this).displayName(uri) }
            .getOrNull()?.take(160) ?: "Fichier sélectionné"
        if (!packageManager.canRequestPackageInstalls()) {
            deferredUri = uri
            InstallUpdates.publish(this, InstallUpdate(
                InstallPhase.NEEDS_PERMISSION, "Autorisation nécessaire",
                "Autorise BundleTap dans les paramètres, puis reviens à l'application.",
                fileName = name
            ))
            openUnknownSourcesSettings()
            return
        }
        deferredUri = null
        isBusy = true
        InstallUpdates.publish(this, InstallUpdate(
            InstallPhase.PREPARING, "Préparation du fichier",
            "Analyse et sélection des modules compatibles avec ton Pixel.",
            fileName = name
        ))
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val prepared = ApkArchive(this@MainActivity).prepare(uri)
                    try {
                        PackageInstallEngine(this@MainActivity).commit(prepared)
                    } finally {
                        prepared.dispose()
                    }
                }
            } catch (e: Exception) {
                InstallUpdates.publish(this@MainActivity, InstallUpdate(
                    InstallPhase.ERROR, "Fichier non installé",
                    "Ce fichier n'a pas pu être préparé ou installé. Vérifie son format et réessaie.",
                    details = e.localizedMessage?.take(400)
                ))
            } finally {
                isBusy = false
            }
        }
    }

    private fun clearResult() {
        if (isBusy || update.phase == InstallPhase.INSTALLING ||
            update.phase == InstallPhase.AWAITING_CONFIRMATION) return
        selectedUri = null
        deferredUri = null
        InstallUpdates.reset(this)
    }

    private fun openUnknownSourcesSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:$packageName")))
        }.onFailure {
            InstallUpdates.publish(this, InstallUpdate(
                InstallPhase.ERROR, "Paramètres indisponibles",
                "Ouvre les paramètres Android et autorise les installations depuis BundleTap.",
                details = it.localizedMessage?.take(400)
            ))
        }
    }

    private fun launchPendingConfirmation() {
        val confirmation = InstallUpdates.pendingConfirmation ?: return
        runCatching { startActivity(confirmation) }.onFailure {
            InstallUpdates.publish(this, InstallUpdate(
                InstallPhase.ERROR, "Confirmation indisponible",
                "Android n'a pas pu ouvrir la fenêtre de confirmation. Réessaie.",
                details = it.localizedMessage?.take(400)
            ))
        }
    }
}
