package com.epikaigle.bundletap.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkSelectorTest {
    private val abis = listOf("arm64-v8a", "armeabi-v7a")

    @Test fun selectsArm64NotX86() {
        val input = listOf(
            "base.apk",
            "split_config.arm64_v8a.apk",
            "split_config.x86.apk",
            "split_config.fr.apk"
        )
        val actual = ApkSelector.select(input, abis, 480, listOf("fr-FR"))
        assertEquals(3, actual.size)
        assertTrue("split_config.arm64_v8a.apk" in actual)
        assertTrue("split_config.fr.apk" in actual)
    }

    @Test fun picksClosestScreenDensityAndLocale() {
        val input = listOf(
            "splits/base-master.apk",
            "splits/base-mdpi.apk",
            "splits/base-xxhdpi.apk",
            "splits/base-xxxhdpi.apk",
            "splits/base-en.apk",
            "splits/base-ja.apk"
        )
        val result = ApkSelector.select(input, abis, 500, listOf("fr-FR"))
        assertTrue("splits/base-xxhdpi.apk" in result)
        assertTrue("splits/base-mdpi.apk" !in result)
        assertTrue("splits/base-en.apk" in result)
        assertTrue("splits/base-ja.apk" !in result)
    }

    @Test fun rejectsUnknownBase() {
        assertThrows(IllegalArgumentException::class.java) {
            ApkSelector.select(
                listOf("one.apk", "two.apk"), abis, 480, listOf("fr")
            )
        }
    }

    @Test fun universalOnly() {
        val result = ApkSelector.select(
            listOf("universal.apk", "other.apk"), abis, 480, listOf("fr")
        )
        assertEquals(listOf("universal.apk"), result)
    }
}
