package com.epikaigle.bundletap.install

import java.util.Locale

/** Filename filter before asking for sensitive Android install permissions. */
object FileFormats {
    private val supported = setOf("apk", "apkm", "apks", "xapk")

    fun isInstallableName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase(Locale.ROOT) in supported
}
