package com.epikaigle.bundletap.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkSelectorTest {
    private val abis = listOf("arm64-v8a", "armeabi-v7a")

    @Test fun apkMirrorSelectsArm64AndFrenchEnglish() {
        val actual = ApkSelector.select(
            listOf(
                "base.apk", "split_config.arm64_v8a.apk", "split_config.armeabi_v7a.apk",
                "split_config.x86.apk", "split_config.fr.apk", "split_config.en.apk",
                "split_config.ja.apk", "split_myfeature.apk"
            ), abis, 480, listOf("fr-FR")
        )
        assertEquals(
            listOf("base.apk", "split_config.arm64_v8a.apk",
                "split_config.fr.apk", "split_config.en.apk", "split_myfeature.apk"),
            actual
        )
    }

    @Test fun densitySelectionIsIndependentPerFeature() {
        val actual = ApkSelector.select(
            listOf(
                "splits/base-master.apk",
                "splits/base-mdpi.apk", "splits/base-xxhdpi.apk",
                "splits/feature-master.apk", "splits/feature-mdpi.apk",
                "splits/feature-xxxhdpi.apk"
            ), abis, 500, listOf("fr-FR")
        )
        assertTrue("splits/base-xxhdpi.apk" in actual)
        assertTrue("splits/feature-xxxhdpi.apk" in actual)
        assertTrue("splits/base-mdpi.apk" !in actual)
        assertTrue("splits/feature-mdpi.apk" !in actual)
    }

    @Test fun xapkSupportsPackageNamedBaseFromManifest() {
        val actual = ApkSelector.select(
            listOf(
                "com.example.app.apk",
                "config.arm64_v8a.apk",
                "config.x86.apk",
                "config.xxhdpi.apk"
            ), abis, 480, listOf("fr-FR"), "com.example.app.apk"
        )
        assertEquals(
            listOf("com.example.app.apk", "config.arm64_v8a.apk",
                "config.xxhdpi.apk"), actual
        )
    }

    @Test fun abiCanFallbackToSecondaryWhenPrimaryAbsent() {
        val actual = ApkSelector.select(
            listOf("base.apk", "split_config.armeabi_v7a.apk", "split_config.x86.apk"),
            abis, 480, listOf("fr-FR")
        )
        assertEquals(listOf("base.apk", "split_config.armeabi_v7a.apk"), actual)
    }

    @Test fun featureNameThatLooksLikeLanguageIsPreserved() {
        val actual = ApkSelector.select(
            listOf("base.apk", "split_pro.apk", "split_config.fr.apk"),
            abis, 480, listOf("fr")
        )
        assertTrue(actual.contains("split_pro.apk"))
    }

    @Test fun ambiguousOrMissingBaseRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ApkSelector.select(listOf("one.apk", "two.apk"), abis, 480, listOf("fr"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ApkSelector.select(
                listOf("base.apk", "config.fr.apk"), abis, 480, listOf("fr"),
                "com.example.missing.apk"
            )
        }
    }

    @Test fun universalVariantIsNotMixedWithSplits() {
        assertEquals(
            listOf("universal.apk"),
            ApkSelector.select(
                listOf("universal.apk", "base.apk", "config.fr.apk"),
                abis, 480, listOf("fr")
            )
        )
    }

    @Test fun oneApkArchiveIsAccepted() {
        assertEquals(
            listOf("app.apk"),
            ApkSelector.select(listOf("app.apk"), abis, 480, listOf("fr"))
        )
    }

    @Test fun multipleStandalonesSelectsDeviceAbi() {
        val actual = ApkSelector.select(
            listOf(
                "standalones/app-x86.apk",
                "standalones/app-arm64_v8a.apk",
                "standalones/app-armeabi_v7a.apk"
            ),
            abis, 480, listOf("fr")
        )
        assertEquals(listOf("standalones/app-arm64_v8a.apk"), actual)
    }

    @Test fun inferredBaseFromSingleNonSplitCandidate() {
        val actual = ApkSelector.select(
            listOf(
                "com.mycustom.app.apk",
                "config.arm64_v8a.apk",
                "config.xxhdpi.apk"
            ),
            abis, 480, listOf("fr")
        )
        assertEquals(
            listOf("com.mycustom.app.apk", "config.arm64_v8a.apk", "config.xxhdpi.apk"),
            actual
        )
    }

    @Test fun abiAliasRecognizesArm64AndAarch64() {
        val actual = ApkSelector.select(
            listOf(
                "base.apk",
                "split_config.arm64.apk",
                "split_config.x86.apk"
            ),
            abis, 480, listOf("fr")
        )
        assertEquals(listOf("base.apk", "split_config.arm64.apk"), actual)
    }

    @Test fun abiGroupingHandlesHyphensAndAliasesWithoutDuplicating() {
        // Hyphens instead of underscores (e.g. arm64-v8a vs armeabi-v7a)
        val actual = ApkSelector.select(
            listOf(
                "base.apk",
                "split_config.arm64-v8a.apk",
                "split_config.armeabi-v7a.apk",
                "split_config.x86.apk"
            ),
            abis, 480, listOf("fr")
        )
        // Must only select ONE ABI split (the highest priority ABI: arm64-v8a), NEVER both!
        assertEquals(listOf("base.apk", "split_config.arm64-v8a.apk"), actual)
    }

    @Test fun densityGroupingHandlesDpiAndParentheses() {
        val actual = ApkSelector.select(
            listOf(
                "base.apk",
                "base-480dpi.apk",
                "base-320dpi.apk",
                "feature (xxhdpi).apk",
                "feature (mdpi).apk"
            ),
            abis, 480, listOf("fr")
        )
        assertTrue("base-480dpi.apk" in actual)
        assertFalse("base-320dpi.apk" in actual)
        assertTrue("feature (xxhdpi).apk" in actual)
        assertFalse("feature (mdpi).apk" in actual)
    }

    @Test fun languageExtractionHandlesBcp47AndRegionalAndBrackets() {
        val actual = ApkSelector.select(
            listOf(
                "base.apk",
                "split_config.fr_rFR.apk",
                "split_config.b+es+419.apk",
                "split_config.ja.apk",
                "split_config [de].apk"
            ),
            abis, 480, listOf("fr-FR", "es-419")
        )
        assertTrue("split_config.fr_rFR.apk" in actual)
        assertTrue("split_config.b+es+419.apk" in actual)
        assertFalse("split_config.ja.apk" in actual)
        assertFalse("split_config [de].apk" in actual)
    }

    @Test fun caseInsensitiveFilenamesInSelector() {
        val actual = ApkSelector.select(
            listOf(
                "BASE.APK",
                "SPLIT_CONFIG.ARM64_V8A.APK",
                "SPLIT_CONFIG.X86.APK",
                "SPLIT_CONFIG.FR.APK"
            ),
            abis, 480, listOf("fr")
        )
        assertEquals(
            listOf("BASE.APK", "SPLIT_CONFIG.ARM64_V8A.APK", "SPLIT_CONFIG.FR.APK"),
            actual
        )
    }
}

