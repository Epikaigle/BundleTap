package com.epikaigle.bundletap.install

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

data class PreparedApks(
    val name: String,
    val apks: List<File>,
    val workspace: File
) {
    fun dispose() { workspace.deleteRecursively() }
}

class ApkArchive(private val context: Context) {
    companion object {
        private const val MAX_FILES = 350
        private const val MAX_APK_BYTES = 700L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 1500L * 1024 * 1024
        private const val MAX_OTHER_BYTES = 32L * 1024 * 1024
    }

    fun displayName(uri: Uri): String {
        val cursor = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index) ?: "Fichier Android"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Fichier Android"
    }

    fun prepare(uri: Uri): PreparedApks {
        val name = displayName(uri)
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        require(extension in setOf("apk", "apks", "apkm", "xapk")) {
            "Format non pris en charge. Utilise APK, APKM, APKS ou XAPK."
        }

        val workspace = File(context.cacheDir, "installation-" + UUID.randomUUID()).apply {
            check(mkdirs()) { "Impossible de créer le dossier temporaire." }
        }
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Impossible de lire ce fichier.")
            val apks = input.use { stream ->
                if (extension == "apk") {
                    val single = File(workspace, "base.apk")
                    FileOutputStream(single).use { out ->
                        copyBounded(stream, out, MAX_APK_BYTES)
                    }
                    listOf(single)
                } else {
                    val discovered = unpackArchive(stream, workspace)
                    if (discovered.isEmpty()) {
                        throw IllegalArgumentException("Archive sans APK installable.")
                    }
                    val locales = Build.VERSION_CODES::class.java // no device-specific constants needed
                    @Suppress("UNUSED_VARIABLE")
                    val unused = locales
                    val deviceLanguages = context.resources.configuration.locales
                    val tags = (0 until deviceLanguages.size()).map {
                        deviceLanguages[it].toLanguageTag()
                    }
                    val chosen = ApkSelector.select(
                        paths = discovered.map { it.first },
                        supportedAbis = Build.SUPPORTED_ABIS.toList(),
                        densityDpi = context.resources.displayMetrics.densityDpi,
                        locales = tags
                    ).toSet()
                    discovered.filter { it.first in chosen }.map { it.second }
                }
            }
            require(apks.isNotEmpty()) { "Aucun APK compatible pour le Pixel." }
            // Verify that the selected base is a real, parsable installable package.
            val baseApk = apks.first()
            val pkg = context.packageManager.getPackageArchiveInfo(baseApk.absolutePath, 0)
                ?: throw IllegalArgumentException("APK principal invalide ou illisible.")
            require(pkg.packageName.isNotBlank()) { "Nom du paquet APK manquant." }
            require(pkg.applicationInfo?.minSdkVersion?.let { it <= Build.VERSION.SDK_INT } != false) {
                "Cette application nécessite une version Android plus récente."
            }
            return PreparedApks(name, apks, workspace)
        } catch (error: Exception) {
            workspace.deleteRecursively()
            throw error
        }
    }

    private fun unpackArchive(stream: InputStream, workspace: File): List<Pair<String, File>> {
        val discovered = mutableListOf<Pair<String, File>>()
        val seen = mutableSetOf<String>()
        var total = 0L
        var entries = 0
        ZipInputStream(stream.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > MAX_FILES) {
                    throw IllegalArgumentException("Archive contenant trop de fichiers.")
                }
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                val entryName = entry.name.replace('\\', '/')
                val lowered = entryName.lowercase(Locale.ROOT)
                if (lowered.endsWith(".obb")) {
                    throw IllegalArgumentException(
                        "Les archives contenant des données OBB ne sont pas prises en charge."
                    )
                }
                if (!seen.add(lowered)) {
                    throw IllegalArgumentException("Entrée dupliquée dans l'archive.")
                }
                val isApk = lowered.endsWith(".apk")
                val limit = if (isApk) MAX_APK_BYTES else MAX_OTHER_BYTES
                if (entry.size > limit) throw IllegalArgumentException("Fichier d'archive trop volumineux.")

                val output = if (isApk) File.createTempFile("split-", ".apk", workspace) else null
                try {
                    val sink = if (output != null) FileOutputStream(output) else null
                    var size = 0L
                    sink.use { out ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read == -1) break
                            size += read
                            total += read
                            if (size > limit || total > MAX_TOTAL_BYTES) {
                                throw IllegalArgumentException("Archive trop volumineuse ou suspecte.")
                            }
                            out?.write(buffer, 0, read)
                        }
                    }
                    if (output != null) discovered += entryName to output
                } catch (error: Exception) {
                    output?.delete()
                    throw error
                } finally {
                    zip.closeEntry()
                }
            }
        }
        return discovered
    }

    private fun copyBounded(input: InputStream, out: FileOutputStream, max: Long) {
        var copied = 0L
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n == -1) break
            copied += n
            if (copied > max) throw IllegalArgumentException("APK trop volumineux.")
            out.write(buffer, 0, n)
        }
    }
}
