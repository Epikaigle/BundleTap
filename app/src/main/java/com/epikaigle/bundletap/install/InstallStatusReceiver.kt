package com.epikaigle.bundletap.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

/** Receives system results even when the installation UI is temporarily behind Settings. */
class InstallStatusReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_INSTALL_STATUS = "com.epikaigle.bundletap.INSTALL_STATUS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val code = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE
        )
        val diagnostic = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            ?.take(300).orEmpty()
        when (code) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = intent.getParcelableExtra(
                    Intent.EXTRA_INTENT, Intent::class.java
                )
                if (confirmation == null) {
                    InstallUpdates.publish(
                        context, InstallUpdate(
                            InstallPhase.ERROR, "Confirmation impossible",
                            "Android n'a pas transmis d'écran de confirmation."
                        )
                    )
                    return
                }
                InstallUpdates.pendingConfirmation = confirmation
                InstallUpdates.publish(
                    context, InstallUpdate(
                        InstallPhase.AWAITING_CONFIRMATION,
                        "Confirmation Android",
                        "Confirme l'installation dans la fenêtre système."
                    )
                )
                // This is a user-initiated install. Android may restrict a background launch;
                // the activity can also show the saved confirmation when brought to foreground.
                runCatching {
                    confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmation)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                InstallUpdates.pendingConfirmation = null
                InstallUpdates.publish(
                    context, InstallUpdate(
                        InstallPhase.SUCCESS,
                        "Application installée",
                        "L'installation s'est terminée avec succès."
                    )
                )
            }
            else -> {
                InstallUpdates.pendingConfirmation = null
                val reason = when (code) {
                    PackageInstaller.STATUS_FAILURE_ABORTED -> "Installation annulée."
                    PackageInstaller.STATUS_FAILURE_BLOCKED -> "Installation bloquée par Android."
                    PackageInstaller.STATUS_FAILURE_CONFLICT -> "Conflit avec une application existante ou sa signature."
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "APK incompatible avec cet appareil."
                    PackageInstaller.STATUS_FAILURE_INVALID -> "APK ou ensemble de splits invalide."
                    PackageInstaller.STATUS_FAILURE_STORAGE -> "Espace de stockage insuffisant."
                    else -> "Échec de l'installation."
                }
                InstallUpdates.publish(
                    context, InstallUpdate(
                        InstallPhase.ERROR, "Installation impossible",
                        if (diagnostic.isBlank()) reason else reason + "\n" + diagnostic
                    )
                )
            }
        }
    }
}
