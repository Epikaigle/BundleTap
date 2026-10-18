package com.epikaigle.bundletap.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FileFormatsTest {
    @Test
    fun acceptedExtensionsCaseInsensitive() {
        listOf("app.apk", "app.APKM", "bundle.ApKs", "name.XAPK", "app.apkx", "bundle.BAPKS")
            .forEach { assertTrue(FileFormats.isInstallableName(it)) }
    }

    @Test
    fun ignoresOtherFilesWithoutRequestingInstallPermission() {
        listOf("photo.png", "archive.zip", "app.apk.zip", "file", "")
            .forEach { assertFalse(FileFormats.isInstallableName(it)) }
    }

    @Test
    fun isSupportedExtensionIncludesZipAndAllBundles() {
        listOf("apk", "apks", "apkm", "xapk", "apkx", "bapks", "zip",
            "APK", "APKS", "APKM", "XAPK", "APKX", "BAPKS", "ZIP").forEach {
            assertTrue(FileFormats.isSupportedExtension(it))
        }
        listOf("png", "pdf", "exe", "tar", "gz", "aab", "").forEach {
            assertFalse(FileFormats.isSupportedExtension(it))
        }
    }

    @Test
    fun cleanFileNameSanitization() {
        assertEquals("Mon Application.apk", FileFormats.cleanFileName("Mon%20Application.apk"))
        assertEquals("Double Encodage.xapk", FileFormats.cleanFileName("Double%2520Encodage.xapk"))
        assertEquals("app.apk", FileFormats.cleanFileName("app.apk?download=true&token=xyz"))
        assertEquals("bundle.apks", FileFormats.cleanFileName("bundle.apks#heading-anchor"))
        assertEquals("direct.apk", FileFormats.cleanFileName("/storage/emulated/0/Download/direct.apk"))
        assertEquals("windows.apkm", FileFormats.cleanFileName("C:\\Downloads\\windows.apkm"))
        assertEquals("clean.apk", FileFormats.cleanFileName("clean\r\n\t\u0000.apk"))
        assertEquals("Fichier Android", FileFormats.cleanFileName("   \t  "))
        assertEquals("Fichier Android", FileFormats.cleanFileName(""))
    }

    @Test
    fun extractExtensionEdgeCases() {
        assertEquals("apk", FileFormats.extractExtension("my.app.apk?download=1"))
        assertEquals("xapk", FileFormats.extractExtension("GAME%20v1.0.XAPK#ref"))
        assertEquals("apks", FileFormats.extractExtension("bundle (v2).ApKs"))
        assertEquals("bapks", FileFormats.extractExtension("package.BAPKS"))
        assertEquals("zip", FileFormats.extractExtension("archive.zip"))
        assertEquals("", FileFormats.extractExtension("filename_without_extension"))
        assertEquals("", FileFormats.extractExtension("trailing_dot."))
    }

    @Test
    fun zipMagicHeaderVerification() {
        val validZipBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)
        val validStream = ByteArrayInputStream(validZipBytes)
        assertTrue(FileFormats.hasZipMagicHeader(validStream))

        val invalidBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG header
        val invalidStream = ByteArrayInputStream(invalidBytes)
        assertFalse(FileFormats.hasZipMagicHeader(invalidStream))

        val shortBytes = byteArrayOf(0x50, 0x4B)
        val shortStream = ByteArrayInputStream(shortBytes)
        assertFalse(FileFormats.hasZipMagicHeader(shortStream))
    }

    @Test
    fun formatOfBadge() {
        assertEquals("APK", FileFormats.formatOf("test.apk"))
        assertEquals("APKM", FileFormats.formatOf("test.apkm"))
        assertEquals("XAPK", FileFormats.formatOf("test.xapk"))
        assertEquals("ZIP", FileFormats.formatOf("test.zip"))
        assertEquals("APK", FileFormats.formatOf("no_extension"))
    }
}
