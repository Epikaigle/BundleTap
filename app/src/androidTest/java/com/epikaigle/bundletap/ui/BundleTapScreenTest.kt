package com.epikaigle.bundletap.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.epikaigle.bundletap.install.InstallPhase
import com.epikaigle.bundletap.install.InstallUpdate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BundleTapScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun welcomeActionSelectsFile() {
        var selected = false
        compose.setContent {
            BundleTapScreen(
                update = InstallUpdate(InstallPhase.READY, "Prêt", "Sélectionne un fichier."),
                busy = false, sourceAllowed = false, canConfirm = false, canRetry = false,
                onSelectFile = { selected = true },
                onEnableSource = {}, onConfirm = {}, onRetry = {}, onReset = {}
            )
        }
        compose.onNodeWithText("Sélectionner un fichier").performClick()
        compose.runOnIdle { assertTrue(selected) }
    }

    @Test fun permissionRequestIsContextual() {
        var settingsOpened = false
        compose.setContent {
            BundleTapScreen(
                update = InstallUpdate(
                    InstallPhase.NEEDS_PERMISSION, "Autorisation nécessaire",
                    "Autorise BundleTap.", "test.apkm"
                ),
                busy = false, sourceAllowed = false, canConfirm = false, canRetry = false,
                onSelectFile = {},
                onEnableSource = { settingsOpened = true },
                onConfirm = {}, onRetry = {}, onReset = {}
            )
        }
        compose.onNodeWithText("Autoriser l'installation").performClick()
        compose.runOnIdle { assertTrue(settingsOpened) }
        compose.onNodeWithText("Confirmer dans Android").assertDoesNotExist()
    }

    @Test fun technicalDetailsAreHiddenUntilRequested() {
        compose.setContent {
            BundleTapScreen(
                update = InstallUpdate(
                    InstallPhase.ERROR, "Installation impossible",
                    "Fichier invalide.", "invalid.apks", "INSTALL_FAILED_INVALID_APK"
                ),
                busy = false, sourceAllowed = true, canConfirm = false, canRetry = true,
                onSelectFile = {}, onEnableSource = {}, onConfirm = {},
                onRetry = {}, onReset = {}
            )
        }
        compose.onNodeWithText("INSTALL_FAILED_INVALID_APK").assertDoesNotExist()
        compose.onNodeWithText("Détails techniques").performClick()
        compose.onNodeWithText("INSTALL_FAILED_INVALID_APK").assertIsDisplayed()
    }

    @Test fun confirmationRequiresSystemIntent() {
        compose.setContent {
            BundleTapScreen(
                update = InstallUpdate(
                    InstallPhase.AWAITING_CONFIRMATION, "Confirmation requise",
                    "Valide l'installation.", "example.apkm"
                ),
                busy = false, sourceAllowed = true, canConfirm = false, canRetry = false,
                onSelectFile = {}, onEnableSource = {}, onConfirm = {},
                onRetry = {}, onReset = {}
            )
        }
        compose.onNodeWithText("Confirmer dans Android").assertDoesNotExist()
    }

    @Test fun successOffersOpenActionWhenAvailable() {
        var appOpened = false
        compose.setContent {
            BundleTapScreen(
                update = InstallUpdate(
                    InstallPhase.SUCCESS, "Installation terminée",
                    "L'application est installée.", "test.xapk",
                    packageName = "com.test.app"
                ),
                busy = false, sourceAllowed = true, canConfirm = false, canRetry = false,
                canOpenApp = true,
                onSelectFile = {}, onEnableSource = {}, onConfirm = {},
                onRetry = {}, onReset = {},
                onOpenApp = { if (it == "com.test.app") appOpened = true }
            )
        }
        compose.onNodeWithText("Ouvrir l'application").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(appOpened) }
    }

    @Test fun confirmationAllowsCancellation() {
        var cancelled = false
        compose.setContent {
            BundleTapScreen(
                update = InstallUpdate(
                    InstallPhase.AWAITING_CONFIRMATION, "Confirmation requise",
                    "Valide l'installation.", "example.apkm"
                ),
                busy = false, sourceAllowed = true, canConfirm = true, canRetry = false,
                onSelectFile = {}, onEnableSource = {}, onConfirm = {},
                onRetry = {}, onReset = { cancelled = true }
            )
        }
        compose.onNodeWithText("Annuler l'installation").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }
}
