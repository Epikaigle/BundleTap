package com.epikaigle.bundletap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.epikaigle.bundletap.install.ApkArchive
import com.epikaigle.bundletap.install.FileFormats
import com.epikaigle.bundletap.install.InstallPhase
import com.epikaigle.bundletap.install.InstallUpdate
import com.epikaigle.bundletap.install.InstallUpdates
import com.epikaigle.bundletap.ui.BundleTapScreen

class MainActivity : ComponentActivity() {
    private val installer: InstallerViewModel by viewModels()
    private var sourceAllowed by mutableStateOf(false)

    private val picker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) openFile(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        sourceAllowed = packageManager.canRequestPackageInstalls()
        setContent {
            val update by InstallUpdates.current.collectAsState()
            BundleTapScreen(
                update = update,
                busy = installer.busy,
                sourceAllowed = sourceAllowed,
                canConfirm = InstallUpdates.pendingConfirmation != null,
                canRetry = installer.retry() != null && update.phase == InstallPhase.ERROR,
                onSelectFile = { picker.launch(arrayOf("*/*")) },
                onEnableSource = ::openUnknownSourcesSettings,
                onConfirm = ::launchPendingConfirmation,
                onRetry = { installer.retry()?.let(::openFile) },
                onReset = installer::clear
            )
        }
        // Do not process ACTION_VIEW twice after rotating the phone.
        if (savedInstanceState == null) handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    override fun onResume() {
        super.onResume()
        sourceAllowed = packageManager.canRequestPackageInstalls()
        installer.onPermissionReturned()
        InstallUpdates.expireUnansweredSession(this)
    }

    private fun handleIncoming(incoming: Intent?) {
        val uri = when (incoming?.action) {
            Intent.ACTION_VIEW -> incoming.data
            Intent.ACTION_SEND -> incoming.getParcelableExtra(
                Intent.EXTRA_STREAM, Uri::class.java
            )
            else -> null
        }
        if (uri != null) openFile(uri)
    }

    private fun openFile(uri: Uri) {
        val phase = InstallUpdates.current.value.phase
        if (installer.busy || phase == InstallPhase.INSTALLING ||
            phase == InstallPhase.AWAITING_CONFIRMATION) {
            Toast.makeText(this, "Termine l'installation en cours avant d'en ouvrir une autre.",
                Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = runCatching { ApkArchive(this).displayName(uri) }
            .getOrNull() ?: "Fichier inconnu"
        if (!FileFormats.isInstallableName(fileName)) {
            InstallUpdates.publish(this, InstallUpdate(
                InstallPhase.ERROR,
                "Format non pris en charge",
                "Choisis un fichier APK, APKM, APKS ou XAPK.",
                fileName = fileName
            ))
            return
        }
        if (installer.needsPermission(uri)) {
            openUnknownSourcesSettings()
        } else {
            installer.start(uri)
        }
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
