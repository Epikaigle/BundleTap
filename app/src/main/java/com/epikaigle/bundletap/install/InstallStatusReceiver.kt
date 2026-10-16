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
                val confirmation = intent.getParcelableExtra(
                    Intent.EXTRA_INTENT, Intent::class.java
                )
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
                InstallUpdates.pendingConfirmation = null
                InstallUpdates.clearSession(context)
                InstallUpdates.publish(context, InstallUpdate(
                    InstallPhase.SUCCESS, "Installation terminée",
                    "L'application est installée sur ton Pixel."
                ))
            }
            else -> {
                val explanation = when (code) {
                    PackageInstaller.STATUS_FAILURE_ABORTED ->
                        "L'installation a été annulée. Tu peux réessayer."
                    PackageInstaller.STATUS_FAILURE_BLOCKED ->
                        "Android a bloqué l'installation. Vérifie les autorisations et la provenance du fichier."
                    PackageInstaller.STATUS_FAILURE_CONFLICT ->
                        "La signature ou la version ne correspond pas à l'application déjà installée."
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                        "Ce fichier n'est pas compatible avec ton Pixel 7 Pro."
                    PackageInstaller.STATUS_FAILURE_INVALID ->
                        "Le fichier semble incomplet, mal signé ou contient des modules incompatibles."
                    PackageInstaller.STATUS_FAILURE_STORAGE ->
                        "Le stockage est insuffisant. Libère de l'espace, puis réessaie."
                    else -> "L'installation n'a pas abouti. Vérifie le fichier et réessaie."
                }
                fail(context, "Installation impossible", explanation, diagnostic)
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
