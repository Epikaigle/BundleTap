package com.epikaigle.bundletap.install

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class PreparedApks(
    val name: String,
    val apks: List<File>,
    val workspace: File
) {
    fun dispose() { workspace.deleteRecursively() }
}

/**
 * Copies the compressed archive once, then reads its central directory with ZipFile.
 * Only device-compatible APKs are extracted. No ZIP entry pathname is used as a
 * filesystem pathname. Strict size/CRC checks prevent corrupt or oversized inputs.
 */
class ApkArchive(private val context: Context) {
    companion object {
        private const val MAX_ENTRIES = 400
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024 * 1024
        private const val MAX_APK_BYTES = 800L * 1024 * 1024
        private const val MAX_EXTRACT_BYTES = 1500L * 1024 * 1024
        private const val MAX_METADATA_BYTES = 256L * 1024
    }

    fun displayName(uri: Uri): String {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return (it.getString(index) ?: "Fichier Android")
                    .substringAfterLast('/').take(180)
            }
        }
        return (uri.lastPathSegment?.substringAfterLast('/') ?: "Fichier Android").take(180)
    }

    fun prepare(uri: Uri): PreparedApks {
        val name = displayName(uri)
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        require(ext in setOf("apk", "apks", "apkm", "xapk")) {
            "Format non pris en charge : APK, APKM, APKS ou XAPK uniquement."
        }
        cleanAbandonedCache()
        val workspace = File(context.cacheDir, "installation-" + UUID.randomUUID())
        check(workspace.mkdir()) { "Impossible de créer le cache temporaire." }
        try {
            val source = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Fichier illisible ou accès expiré.")
            val apks = source.use { input ->
                if (ext == "apk") {
                    val output = File(workspace, "base.apk")
                    FileOutputStream(output).use { boundedCopy(input, it, MAX_APK_BYTES) }
                    listOf(output)
                } else {
                    val staged = File(workspace, "archive.zip")
                    FileOutputStream(staged).use { boundedCopy(input, it, MAX_ARCHIVE_BYTES) }
                    val selected = ZipFile(staged).use { zip ->
                        extractSelected(zip, ext, workspace)
                    }
                    check(staged.delete()) { "Impossible de nettoyer le cache compressé." }
                    selected
                }
            }
            require(apks.isNotEmpty()) { "Aucun APK compatible avec cet appareil." }
            val info = context.packageManager.getPackageArchiveInfo(
                apks.first().absolutePath, 0
            ) ?: throw IllegalArgumentException("APK principal invalide ou endommagé.")
            require(info.packageName.isNotBlank()) { "Identifiant d'application invalide." }
            require(info.applicationInfo?.minSdkVersion?.let {
                it <= Build.VERSION.SDK_INT
            } != false) { "Ce fichier demande une version Android plus récente." }
            return PreparedApks(name, apks, workspace)
        } catch (error: Exception) {
            workspace.deleteRecursively()
            throw error
        }
    }

    private fun extractSelected(zip: ZipFile, extension: String, dir: File): List<File> {
        val names = mutableSetOf<String>()
        val entries = linkedMapOf<String, ZipEntry>()
        var uncompressedTotal = 0L
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            require(names.size < MAX_ENTRIES) { "L'archive contient trop de fichiers." }
            val name = entry.name.replace('\\', '/')
            val lower = name.lowercase(Locale.ROOT)
            require(names.add(lower)) { "Noms de fichiers dupliqués dans l'archive." }
            if (entry.isDirectory) continue
            require(!lower.endsWith(".obb")) {
                "Cette archive comporte des données OBB non prises en charge."
            }
            require(!entry.name.startsWith("/") && !entry.name.contains("\u0000")) {
                "Nom de fichier suspect dans l'archive."
            }
            val limit = if (lower.endsWith(".apk")) MAX_APK_BYTES else MAX_METADATA_BYTES
            require(entry.size <= limit || entry.size < 0) {
                "L'archive contient un fichier trop volumineux."
            }
            if (entry.size >= 0) {
                uncompressedTotal += entry.size
                require(uncompressedTotal <= MAX_EXTRACT_BYTES) {
                    "Taille décompressée trop importante."
                }
            }
            entries[name] = entry
        }
        val apkNames = entries.keys.filter { it.endsWith(".apk", ignoreCase = true) }
        require(apkNames.isNotEmpty()) { "Archive sans fichier APK." }
        val meta = if (extension == "xapk") {
            val entry = entries.entries.firstOrNull {
                it.key.equals("manifest.json", ignoreCase = true)
            }?.value
            entry?.let {
                val bytes = zip.getInputStream(it).use { input ->
                    readLimited(input, MAX_METADATA_BYTES)
                }
                JSONObject(bytes.toString(Charsets.UTF_8))
            }
        } else null

        val splitMeta = meta?.optJSONArray("split_apks")
        var declaredBase: String? = null
        if (splitMeta != null) {
            for (i in 0 until splitMeta.length()) {
                val item = splitMeta.optJSONObject(i) ?: continue
                if (item.optString("id").equals("base", ignoreCase = true)) {
                    declaredBase = item.optString("file").takeIf { it.isNotBlank() }
                    break
                }
            }
        }
        // Some XAPK manifests list the base only as <package_name>.apk.
        if (declaredBase == null && extension == "xapk") {
            val pkg = meta?.optString("package_name")?.takeIf { it.isNotBlank() }
            if (pkg != null) {
                val expected = "$pkg.apk"
                declaredBase = apkNames.singleOrNull {
                    it.substringAfterLast('/').equals(expected, ignoreCase = true)
                }
            }
        }

        val languages = context.resources.configuration.locales
        val tags = (0 until languages.size()).map { languages[it].toLanguageTag() }
        val selected = ApkSelector.select(
            paths = apkNames,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            densityDpi = context.resources.displayMetrics.densityDpi,
            locales = tags,
            baseHint = declaredBase
        )
        var copied = 0L
        return selected.mapIndexed { index, name ->
            val entry = checkNotNull(entries[name]) { "Fichier APK manquant dans l'archive." }
            val target = File(dir, "apk-$index.apk")
            val crc = CRC32()
            zip.getInputStream(entry).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        size += n
                        copied += n
                        require(size <= MAX_APK_BYTES && copied <= MAX_EXTRACT_BYTES) {
                            "L'archive dépasse la taille maximale autorisée."
                        }
                        crc.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                    }
                    require(entry.size < 0 || size == entry.size) {
                        "Fichier APK tronqué."
                    }
                }
            }
            require(entry.crc < 0 || entry.crc == crc.value) {
                "Vérification d'intégrité du fichier APK échouée."
            }
            target
        }
    }

    private fun boundedCopy(input: InputStream, output: FileOutputStream, limit: Long) {
        var total = 0L
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            require(total <= limit) { "Fichier trop volumineux." }
            output.write(buffer, 0, n)
        }
    }

    private fun readLimited(input: InputStream, max: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= max) { "Manifeste XAPK trop volumineux." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun cleanAbandonedCache() {
        val threshold = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name.startsWith("installation-") &&
                file.lastModified() < threshold) {
                file.deleteRecursively()
            }
        }
    }
}
