package com.epikaigle.bundletap.install

import java.io.InputStream
import java.net.URLDecoder
import java.util.Locale

/** Filename filter before asking for sensitive Android install permissions. */
object FileFormats {
    val supported = setOf("apk", "apkm", "apks", "xapk", "apkx", "bapks")

    /**
     * Sanitizes file names from external file pickers, browsers, and intent extras.
     * Decodes URL percent encodings, strips query parameters, fragments, path components,
     * and control characters.
     */
    fun cleanFileName(raw: String): String {
        var name = raw
        repeat(2) {
            if (name.contains('%')) {
                val decoded = runCatching { URLDecoder.decode(name, "UTF-8") }.getOrNull()
                if (decoded != null) name = decoded
            }
        }
        name = name.substringBefore('?').substringBefore('#')
        name = name.substringAfterLast('/').substringAfterLast('\\')
        name = name.replace(Regex("[\\r\\n\\t\\u0000]"), "").trim()
        if (name.isBlank()) name = "Fichier Android"
        return name.take(180)
    }

    fun extractExtension(name: String): String {
        val sanitized = cleanFileName(name)
        val dotIndex = sanitized.lastIndexOf('.')
        if (dotIndex < 0 || dotIndex == sanitized.lastIndex) return ""
        return sanitized.substring(dotIndex + 1).lowercase(Locale.ROOT)
    }

    fun isInstallableName(name: String): Boolean =
        extractExtension(name) in supported

    fun isSupportedExtension(ext: String): Boolean =
        ext.lowercase(Locale.ROOT) in supported || ext.equals("zip", ignoreCase = true)

    fun hasZipMagicHeader(input: InputStream): Boolean {
        val header = ByteArray(4)
        val read = input.read(header)
        return read == 4 &&
            header[0] == 0x50.toByte() && // 'P'
            header[1] == 0x4B.toByte() && // 'K'
            header[2] == 0x03.toByte() &&
            header[3] == 0x04.toByte()
    }

    fun formatOf(name: String): String =
        extractExtension(name).uppercase(Locale.ROOT).ifBlank { "APK" }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.FRENCH, "%.1f Go", gb)
            mb >= 1.0 -> String.format(Locale.FRENCH, "%.1f Mo", mb)
            kb >= 1.0 -> String.format(Locale.FRENCH, "%.0f Ko", kb)
            else -> "$bytes octets"
        }
    }
}

