package com.epikaigle.bundletap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    private var displayFile by mutableStateOf<String?>(null)
    private var isBusy by mutableStateOf(false)
    private var update by mutableStateOf(InstallUpdates.current.value)
    private var deferredUri: Uri? = null

    private val picker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) begin(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InstallUpdates.restore(this)

        setContent {
            BundleTapScreen(
                update = update,
                fileName = displayFile,
                busy = isBusy,
                onSelectFile = { picker.launch(arrayOf("*/*")) },
                onEnableSource = { openUnknownSourcesSettings() },
                onConfirm = { launchPendingConfirmation() }
            )
        }
        lifecycleScope.launch {
            InstallUpdates.current.collectLatest { update = it }
        }
        handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    override fun onResume() {
        super.onResume()
        val waiting = deferredUri
        if (waiting != null && packageManager.canRequestPackageInstalls()) {
            deferredUri = null
            begin(waiting)
        }
    }

    private fun handleIncoming(incoming: Intent?) {
        val uri = when (incoming?.action) {
            Intent.ACTION_VIEW -> incoming.data
            Intent.ACTION_SEND -> incoming.getParcelableExtra(
                Intent.EXTRA_STREAM, Uri::class.java
            )
            else -> null
        }
        if (uri != null) begin(uri)
    }

    private fun begin(uri: Uri) {
        if (isBusy) return
        if (!packageManager.canRequestPackageInstalls()) {
            deferredUri = uri
            InstallUpdates.publish(
                this, InstallUpdate(
                    InstallPhase.AWAITING_CONFIRMATION,
                    "Autorisation nécessaire",
                    "Autorise BundleTap à installer des applications depuis cette source, puis reviens ici."
                )
            )
            openUnknownSourcesSettings()
            return
        }
        isBusy = true
        lifecycleScope.launch {
            try {
                displayFile = withContext(Dispatchers.IO) { ApkArchive(this@MainActivity).displayName(uri) }
                InstallUpdates.publish(
                    this@MainActivity, InstallUpdate(
                        InstallPhase.PREPARING, "Préparation du fichier",
                        "Vérification et préparation des APK compatibles avec le Pixel."
                    )
                )
                withContext(Dispatchers.IO) {
                    val prepared = ApkArchive(this@MainActivity).prepare(uri)
                    try {
                        PackageInstallEngine(this@MainActivity).commit(prepared)
                    } finally {
                        prepared.dispose()
                    }
                }
                if (InstallUpdates.current.value.phase == InstallPhase.PREPARING) {
                    InstallUpdates.publish(
                        this@MainActivity, InstallUpdate(
                            InstallPhase.AWAITING_CONFIRMATION,
                            "Installation envoyée",
                            "Android prépare la confirmation de sécurité."
                        )
                    )
                }
            } catch (e: Exception) {
                InstallUpdates.publish(
                    this@MainActivity, InstallUpdate(
                        InstallPhase.ERROR, "Impossible de continuer",
                        e.message ?: "Erreur lors de la lecture du fichier."
                    )
                )
            } finally {
                isBusy = false
            }
        }
    }

    private fun openUnknownSourcesSettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:" + packageName))
        )
    }

    private fun launchPendingConfirmation() {
        val confirmation = InstallUpdates.pendingConfirmation ?: return
        runCatching { startActivity(confirmation) }
    }
}
