package com.epikaigle.bundletap.install

import org.junit.Assert.assertEquals
import org.junit.Test

class AdditionalApkSelectorTest {
    @Test fun bundletoolKeepsLanguagesForBaseAndFeatures() {
        val selected = ApkSelector.select(
            listOf(
                "splits/base-master.apk", "splits/base-en.apk",
                "splits/base-fr.apk", "splits/base-ja.apk",
                "splits/feature-master.apk", "splits/feature-fr.apk"
            ), listOf("arm64-v8a"), 480, listOf("fr-FR")
        )
        assertEquals(
            listOf("splits/base-master.apk", "splits/base-en.apk",
                "splits/base-fr.apk", "splits/feature-master.apk", "splits/feature-fr.apk"),
            selected
        )
    }
}
