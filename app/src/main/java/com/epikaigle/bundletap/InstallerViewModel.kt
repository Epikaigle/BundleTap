package com.epikaigle.bundletap

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epikaigle.bundletap.install.ApkArchive
import com.epikaigle.bundletap.install.InstallPhase
import com.epikaigle.bundletap.install.InstallUpdate
import com.epikaigle.bundletap.install.InstallUpdates
import com.epikaigle.bundletap.install.PackageInstallEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Survives Activity recreation. Installation preparation is intentionally owned
 * by a ViewModel rather than an Activity lifecycle coroutine.
 */
class InstallerViewModel(application: Application) : AndroidViewModel(application) {
    var busy by mutableStateOf(false)
        private set
    var selectedUri: Uri? = null
        private set
    private var deferredUri: Uri? = null
    private var installJob: Job? = null

    init {
        InstallUpdates.restore(application)
    }

    fun needsPermission(uri: Uri): Boolean {
        val ctx = getApplication<Application>()
        selectedUri = uri
        if (ctx.packageManager.canRequestPackageInstalls()) return false
        deferredUri = uri
        val name = runCatching { ApkArchive(ctx).displayName(uri) }
            .getOrDefault("Fichier sélectionné").take(160)
        InstallUpdates.publish(ctx, InstallUpdate(
            InstallPhase.NEEDS_PERMISSION,
            "Autorisation nécessaire",
            "Autorise BundleTap dans les paramètres Android, puis reviens ici.",
            name
        ))
        return true
    }

    fun onPermissionReturned() {
        val uri = deferredUri ?: return
        val context = getApplication<Application>()
        if (context.packageManager.canRequestPackageInstalls()) {
            deferredUri = null
            start(uri)
        }
    }

    fun selectFile(uri: Uri) {
        val context = getApplication<Application>()
        selectedUri = uri
        val filename = runCatching { ApkArchive(context).displayName(uri) }
            .getOrDefault("Fichier sélectionné").take(160)
        InstallUpdates.publish(context, InstallUpdate(
            InstallPhase.FILE_SELECTED,
            "Fichier prêt",
            "Clique sur « Installer » pour lancer l'installation sur ton appareil.",
            fileName = filename
        ))
    }

    fun start(targetUri: Uri? = null) {
        val uri = targetUri ?: selectedUri ?: return
        val context = getApplication<Application>()
        val phase = InstallUpdates.current.value.phase
        if (busy && phase == InstallPhase.INSTALLING) return
        installJob?.cancel()
        selectedUri = uri
        deferredUri = null
        busy = true
        val filename = runCatching { ApkArchive(context).displayName(uri) }
            .getOrDefault("Fichier sélectionné").take(160)
        InstallUpdates.publish(context, InstallUpdate(
            InstallPhase.PREPARING,
            "Préparation du fichier",
            "Analyse et sélection des modules compatibles avec ton appareil.",
            fileName = filename
        ))
        installJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val prepared = ApkArchive(context).prepare(uri)
                    try {
                        PackageInstallEngine(context).commit(prepared)
                    } finally {
                        prepared.dispose()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                InstallUpdates.publish(context, InstallUpdate(
                    InstallPhase.ERROR,
                    "Fichier non installé",
                    "Le fichier n'a pas pu être préparé. Vérifie son format ou essaie une autre version.",
                    details = e.localizedMessage?.take(400)
                ))
            } finally {
                busy = false
            }
        }
    }

    fun retry(): Uri? = selectedUri

    fun cancel() {
        installJob?.cancel()
        installJob = null
        busy = false
        val app = getApplication<Application>()
        InstallUpdates.abandonCurrentSession(app)
        selectedUri = null
        deferredUri = null
        InstallUpdates.reset(app)
    }

    fun clear() {
        val phase = InstallUpdates.current.value.phase
        if (busy && phase == InstallPhase.INSTALLING) return
        installJob?.cancel()
        installJob = null
        busy = false
        val app = getApplication<Application>()
        if (phase == InstallPhase.AWAITING_CONFIRMATION || phase == InstallPhase.PREPARING) {
            InstallUpdates.abandonCurrentSession(app)
        }
        selectedUri = null
        deferredUri = null
        InstallUpdates.reset(app)
    }
}
