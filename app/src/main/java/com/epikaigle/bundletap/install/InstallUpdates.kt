package com.epikaigle.bundletap.install

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

enum class InstallPhase {
    READY, NEEDS_PERMISSION, PREPARING, INSTALLING, AWAITING_CONFIRMATION, SUCCESS, ERROR
}

data class InstallUpdate(
    val phase: InstallPhase,
    val title: String,
    val message: String,
    val fileName: String? = null,
    val details: String? = null
)

/**
 * The Activity and PackageInstaller callback share one observable installation status.
 * We only persist small, non-sensitive status text; APK contents never leave private cache.
 */
object InstallUpdates {
    private const val PREFS = "installation"
    private const val ACTIVE_SESSION = "active_session"
    private const val LAST_UPDATE = "last_update_at"
    private const val STALE_AFTER_MS = 20L * 60 * 1000

    val current = MutableStateFlow(
        InstallUpdate(InstallPhase.READY, "Prêt à installer", "Choisis un fichier pour commencer.")
    )

    @Volatile var pendingConfirmation: Intent? = null

    fun publish(context: Context, next: InstallUpdate) {
        val update = if (next.fileName == null && next.phase != InstallPhase.READY)
            next.copy(fileName = current.value.fileName) else next
        current.value = update
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("phase", update.phase.name)
            .putString("title", update.title)
            .putString("message", update.message)
            .putString("file_name", update.fileName)
            .putString("details", update.details)
            .putLong(LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val phase = runCatching {
            InstallPhase.valueOf(prefs.getString("phase", "READY") ?: "READY")
        }.getOrDefault(InstallPhase.READY)
        if (phase == InstallPhase.READY) return
        val fileName = prefs.getString("file_name", null)
        val interrupted = phase == InstallPhase.PREPARING ||
            (phase == InstallPhase.AWAITING_CONFIRMATION && pendingConfirmation == null) ||
            phase == InstallPhase.NEEDS_PERMISSION
        if (interrupted) {
            current.value = InstallUpdate(
                InstallPhase.ERROR,
                "Installation interrompue",
                "Sélectionne à nouveau le fichier pour reprendre.",
                fileName
            )
        } else {
            current.value = InstallUpdate(
                phase,
                prefs.getString("title", "BundleTap") ?: "BundleTap",
                prefs.getString("message", "") ?: "",
                fileName,
                prefs.getString("details", null)
            )
        }
        expireUnansweredSession(context)
    }

    /** A result callback may be lost if Android kills the process mid-install. */
    fun expireUnansweredSession(context: Context) {
        if (current.value.phase != InstallPhase.INSTALLING) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(LAST_UPDATE, 0L)
        if (last <= 0 || System.currentTimeMillis() - last < STALE_AFTER_MS) return
        pendingConfirmation = null
        clearSession(context)
        publish(context, InstallUpdate(
            InstallPhase.ERROR,
            "Résultat non reçu",
            "Android n’a pas renvoyé de résultat. Vérifie si l’application est installée avant de réessayer."
        ))
    }

    fun activateSession(context: Context, id: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(ACTIVE_SESSION, id).commit()
    }

    fun isCurrentSession(context: Context, id: Int): Boolean =
        id >= 0 && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(ACTIVE_SESSION, -1) == id

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(ACTIVE_SESSION).apply()
    }

    fun reset(context: Context) {
        pendingConfirmation = null
        clearSession(context)
        publish(context, InstallUpdate(
            InstallPhase.READY, "Prêt à installer", "Choisis un fichier pour commencer."
        ))
    }
}
