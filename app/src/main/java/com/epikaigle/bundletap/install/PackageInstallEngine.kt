package com.epikaigle.bundletap.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.FileInputStream

/** Commit a complete set of split APKs atomically using the Android system installer. */
class PackageInstallEngine(private val context: Context) {
    fun commit(prepared: PreparedApks) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        val sessionId = installer.createSession(params)
        var committed = false
        try {
            installer.openSession(sessionId).use { session ->
                prepared.apks.forEachIndexed { index, apk ->
                    val entryName = "apk-" + index + ".apk"
                    FileInputStream(apk).use { input ->
                        session.openWrite(entryName, 0L, apk.length()).use { output ->
                            input.copyTo(output, 64 * 1024)
                            session.fsync(output)
                        }
                    }
                }
                val statusIntent = Intent(context, InstallStatusReceiver::class.java).apply {
                    action = InstallStatusReceiver.ACTION_INSTALL_STATUS
                    setPackage(context.packageName)
                }
                val callback = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    statusIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                session.commit(callback.intentSender)
                committed = true
            }
        } finally {
            if (!committed) runCatching { installer.abandonSession(sessionId) }
        }
    }
}
