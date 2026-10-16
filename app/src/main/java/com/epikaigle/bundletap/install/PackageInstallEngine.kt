package com.epikaigle.bundletap.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.FileInputStream

/** Commit an APK (or a compatible split set) using the standard system confirmation. */
class PackageInstallEngine(private val context: Context) {
    fun commit(prepared: PreparedApks) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        val sessionId = installer.createSession(params)
        InstallUpdates.activateSession(context, sessionId)
        var committed = false
        try {
            installer.openSession(sessionId).use { session ->
                prepared.apks.forEachIndexed { index, apk ->
                    FileInputStream(apk).use { input ->
                        session.openWrite("apk-$index.apk", 0L, apk.length()).use { output ->
                            input.copyTo(output, 64 * 1024)
                            session.fsync(output)
                        }
                    }
                }
                val status = Intent(context, InstallStatusReceiver::class.java).apply {
                    action = InstallStatusReceiver.ACTION_INSTALL_STATUS
                    setPackage(context.packageName)
                    putExtra(InstallStatusReceiver.EXTRA_OWN_SESSION, sessionId)
                }
                val callback = PendingIntent.getBroadcast(
                    context, sessionId, status,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                InstallUpdates.publish(context, InstallUpdate(
                    InstallPhase.INSTALLING, "Installation en cours",
                    "Les fichiers ont été transmis à Android."
                ))
                session.commit(callback.intentSender)
                committed = true
            }
        } finally {
            if (!committed) {
                runCatching { installer.abandonSession(sessionId) }
                InstallUpdates.clearSession(context)
            }
        }
    }
}
