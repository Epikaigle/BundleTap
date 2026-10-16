package com.epikaigle.bundletap.install

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

enum class InstallPhase { READY, PREPARING, AWAITING_CONFIRMATION, SUCCESS, ERROR }

data class InstallUpdate(
    val phase: InstallPhase,
    val title: String,
    val message: String
)

/** Receiver and activity can communicate without exporting a receiver to other apps. */
object InstallUpdates {
    val current = MutableStateFlow(
        InstallUpdate(InstallPhase.READY, "Prêt à installer", "Sélectionne un fichier pour commencer.")
    )

    @Volatile
    var pendingConfirmation: Intent? = null

    fun publish(context: Context, update: InstallUpdate) {
        current.value = update
        context.getSharedPreferences("installation", Context.MODE_PRIVATE)
            .edit()
            .putString("phase", update.phase.name)
            .putString("title", update.title)
            .putString("message", update.message)
            .apply()
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences("installation", Context.MODE_PRIVATE)
        val phase = runCatching {
            InstallPhase.valueOf(prefs.getString("phase", "READY") ?: "READY")
        }.getOrDefault(InstallPhase.READY)
        if (phase != InstallPhase.READY && phase != InstallPhase.PREPARING) {
            current.value = InstallUpdate(
                phase,
                prefs.getString("title", "BundleTap") ?: "BundleTap",
                prefs.getString("message", "") ?: ""
            )
        }
    }
}
