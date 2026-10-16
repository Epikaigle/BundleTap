package com.epikaigle.bundletap.install

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileFormatsTest {
    @Test fun acceptedExtensionsCaseInsensitive() {
        listOf("app.apk", "app.APKM", "bundle.ApKs", "name.XAPK")
            .forEach { assertTrue(FileFormats.isInstallableName(it)) }
    }

    @Test fun ignoresOtherFilesWithoutRequestingInstallPermission() {
        listOf("photo.png", "archive.zip", "app.apk.zip", "file", "")
            .forEach { assertFalse(FileFormats.isInstallableName(it)) }
    }
}
