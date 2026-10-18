package com.epikaigle.bundletap.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

/** Converts asynchronous Android installer callbacks into user-friendly states. */
class InstallStatusReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_INSTALL_STATUS = "com.epikaigle.bundletap.INSTALL_STATUS"
        const val EXTRA_OWN_SESSION = "com.epikaigle.bundletap.SESSION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val session = intent.getIntExtra(EXTRA_OWN_SESSION, -1)
        if (!InstallUpdates.isCurrentSession(context, session)) return

        val code = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE
        )
        val diagnostic = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            ?.take(350)?.takeIf { it.isNotBlank() }

        when (code) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
                }
                if (confirmation == null) {
                    fail(context, "Confirmation indisponible",
                        "Android n'a pas fourni la fenêtre d'installation.", diagnostic)
                    return
                }
                InstallUpdates.pendingConfirmation = confirmation
                InstallUpdates.publish(context, InstallUpdate(
                    InstallPhase.AWAITING_CONFIRMATION,
                    "Confirmation requise",
                    "Valide l'installation dans la fenêtre de sécurité Android."
                ))
                // Android can block an activity launch from a background receiver.
                // The visible Activity also exposes an explicit, accessible confirm button.
                runCatching {
                    confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmation)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                val installedPkg = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
                    ?: InstallUpdates.current.value.packageName
                InstallUpdates.pendingConfirmation = null
                InstallUpdates.clearSession(context)
                InstallUpdates.publish(context, InstallUpdate(
                    InstallPhase.SUCCESS, "Installation terminée",
                    "L'application est installée sur ton appareil.",
                    packageName = installedPkg
                ))
            }
            else -> {
                val isAborted = code == PackageInstaller.STATUS_FAILURE_ABORTED
                val title = if (isAborted) "Installation annulée" else "Installation impossible"
                val explanation = when (code) {
                    PackageInstaller.STATUS_FAILURE_ABORTED ->
                        "L'installation a été annulée. Tu peux relancer ce fichier ou en choisir un autre."
                    PackageInstaller.STATUS_FAILURE_BLOCKED ->
                        "Android a bloqué l'installation. Vérifie les autorisations de source inconnue et la provenance du fichier."
                    PackageInstaller.STATUS_FAILURE_CONFLICT ->
                        "Conflit de signature : une version incompatible est déjà présente sur l'appareil. Désinstalle l'ancienne version, puis réessaie."
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                        "Ce fichier n'est pas compatible avec l'architecture de ton appareil."
                    PackageInstaller.STATUS_FAILURE_INVALID ->
                        "Le fichier semble incomplet, mal signé ou contient des modules incompatibles."
                    PackageInstaller.STATUS_FAILURE_STORAGE ->
                        "Stockage saturé : libère de l'espace sur l'appareil, puis réessaie."
                    else -> "L'installation n'a pas abouti. Vérifie le fichier et réessaie."
                }
                fail(context, title, explanation, diagnostic)
            }
        }
    }

    private fun fail(context: Context, title: String, message: String, detail: String?) {
        InstallUpdates.pendingConfirmation = null
        InstallUpdates.clearSession(context)
        InstallUpdates.publish(context, InstallUpdate(
            InstallPhase.ERROR, title, message, details = detail
        ))
    }
}
