package com.epikaigle.bundletap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epikaigle.bundletap.R
import com.epikaigle.bundletap.install.InstallPhase
import com.epikaigle.bundletap.install.InstallUpdate

private val Background = Color(0xFF091421)
private val Panel = Color(0xFF152738)
private val Elevated = Color(0xFF203647)
private val Accent = Color(0xFF87E8D9)
private val White = Color(0xFFF4F9FD)
private val Subtle = Color(0xFFB4C8D4)
private val Error = Color(0xFFFFB7AD)
private val Border = Color(0xFF345062)

@Composable
fun BundleTapScreen(
    update: InstallUpdate,
    busy: Boolean,
    sourceAllowed: Boolean,
    canConfirm: Boolean,
    canRetry: Boolean,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit
) {
    val palette = darkColorScheme(
        primary = Accent, onPrimary = Background,
        background = Background, onBackground = White,
        surface = Panel, onSurface = White,
        surfaceVariant = Elevated, onSurfaceVariant = Subtle,
        outline = Border, error = Error
    )
    MaterialTheme(colorScheme = palette) {
        val isWorking = busy || update.phase == InstallPhase.PREPARING ||
            update.phase == InstallPhase.INSTALLING
        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(
                        listOf(Color(0xFF102738), Background, Background)))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                BrandHeader()
                if (update.phase == InstallPhase.READY) {
                    WelcomeCard(onSelectFile)
                } else {
                    InstallationCard(
                        update = update,
                        working = isWorking,
                        canConfirm = canConfirm,
                        canRetry = canRetry,
                        sourceAllowed = sourceAllowed,
                        onSelectFile = onSelectFile,
                        onEnableSource = onEnableSource,
                        onConfirm = onConfirm,
                        onRetry = onRetry,
                        onReset = onReset
                    )
                }
                PrivacyAndSupport(
                    sourceAllowed = sourceAllowed,
                    showSettings = update.phase == InstallPhase.READY ||
                        update.phase == InstallPhase.ERROR ||
                        update.phase == InstallPhase.SUCCESS,
                    onEnableSource = onEnableSource
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
                .background(Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bundle),
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text("BundleTap", color = White, fontSize = 24.sp,
                fontWeight = FontWeight.Bold)
            Text("Installateur de bundles", color = Subtle,
                style = MaterialTheme.typography.bodySmall)
        }
        Surface(shape = RoundedCornerShape(10.dp),
            color = Elevated, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Text("PIXEL 7 PRO", modifier = Modifier.padding(
                horizontal = 10.dp, vertical = 7.dp),
                color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WelcomeCard(onSelectFile: () -> Unit) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                    .background(Accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_bundle), null,
                    tint = Accent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Un fichier.\nUne installation.", color = White,
                fontSize = 29.sp, lineHeight = 34.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Choisis un APK ou un bundle téléchargé. BundleTap prépare les fichiers compatibles avec ton Pixel.",
                color = Subtle, style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(26.dp))
            PrimaryAction(
                text = "Sélectionner un fichier",
                onClick = onSelectFile,
                enabled = true
            )
            Spacer(Modifier.height(14.dp))
            Text("APK  ·  APKM  ·  APKS  ·  XAPK",
                color = Subtle, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InstallationCard(
    update: InstallUpdate,
    working: Boolean,
    canConfirm: Boolean,
    canRetry: Boolean,
    sourceAllowed: Boolean,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit
) {
    val failed = update.phase == InstallPhase.ERROR
    val complete = update.phase == InstallPhase.SUCCESS
    val tint = if (failed) Error else Accent
    var expanded by rememberSaveable(update.details) { mutableStateOf(false) }

    Surface(
        color = Panel,
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (failed) Error.copy(alpha = .40f) else Border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(tint.copy(alpha = .10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(when {
                            failed -> R.drawable.ic_alert
                            complete -> R.drawable.ic_done
                            update.phase == InstallPhase.NEEDS_PERMISSION ->
                                R.drawable.ic_shield
                            else -> R.drawable.ic_bundle
                        }),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(update.title, color = White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    if (working) {
                        Text(if (update.phase == InstallPhase.PREPARING) "Analyse du fichier" else "Traitement Android",
                            color = Subtle, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                update.message, color = Subtle,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )

            update.fileName?.let { name ->
                Spacer(Modifier.height(20.dp))
                Surface(color = Elevated, shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_bundle), null,
                            tint = Accent, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Fichier sélectionné", color = Subtle,
                                style = MaterialTheme.typography.labelSmall)
                            Text(name, color = White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (working) {
                Spacer(Modifier.height(22.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Accent, trackColor = Elevated
                )
                Spacer(Modifier.height(8.dp))
                Text("Traitement en cours…", color = Subtle,
                    style = MaterialTheme.typography.labelSmall)
            }

            when (update.phase) {
                InstallPhase.NEEDS_PERMISSION -> {
                    Spacer(Modifier.height(24.dp))
                    PrimaryAction("Autoriser l'installation", onEnableSource)
                    Spacer(Modifier.height(8.dp))
                    SecondaryAction("Annuler", onReset)
                }
                InstallPhase.AWAITING_CONFIRMATION -> {
                    Spacer(Modifier.height(24.dp))
                    if (canConfirm) {
                        PrimaryAction("Confirmer dans Android", onConfirm)
                    } else {
                        Text("Si aucune fenêtre ne s'affiche, ouvre à nouveau ce fichier.",
                            color = Subtle, style = MaterialTheme.typography.bodySmall)
                    }
                }
                InstallPhase.ERROR -> {
                    Spacer(Modifier.height(24.dp))
                    PrimaryAction("Choisir un autre fichier", onSelectFile)
                    if (canRetry) SecondaryAction("Réessayer ce fichier", onRetry)
                    if (!update.details.isNullOrBlank()) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(if (expanded) "Masquer les détails" else "Détails techniques")
                        }
                        if (expanded) {
                            Surface(
                                color = Elevated,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    update.details, color = Subtle,
                                    fontSize = 12.sp, lineHeight = 18.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                    SecondaryAction("Effacer le résultat", onReset)
                }
                InstallPhase.SUCCESS -> {
                    Spacer(Modifier.height(24.dp))
                    PrimaryAction("Installer un autre fichier", onSelectFile)
                    SecondaryAction("Retour à l'accueil", onReset)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent, contentColor = Background)
    ) {
        Text(text, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun SecondaryAction(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
    ) {
        Text(text, color = Accent,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PrivacyAndSupport(
    sourceAllowed: Boolean,
    showSettings: Boolean,
    onEnableSource: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(painterResource(R.drawable.ic_shield), null,
                tint = Accent, modifier = Modifier.size(22.dp))
            Column {
                Text("Une installation contrôlée", color = White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Traitement 100 % local. Aucun compte, aucune publicité. Android demande ton accord avant l'installation.",
                    color = Subtle, style = MaterialTheme.typography.bodySmall,
                    lineHeight = 19.sp
                )
            }
        }
        if (showSettings && sourceAllowed) {
            TextButton(
                onClick = onEnableSource,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Gérer l'autorisation Android",
                    style = MaterialTheme.typography.labelMedium,
                    color = Subtle)
            }
        }
        Text(
            "Android 17  ·  ARM64  ·  Version 1.1",
            color = Subtle.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}


@Preview(name = "Accueil Pixel 7 Pro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun WelcomePreview() {
    BundleTapScreen(
        update = InstallUpdate(InstallPhase.READY, "Prêt à installer", "Choisis un fichier."),
        busy = false, sourceAllowed = false, canConfirm = false, canRetry = false,
        onSelectFile = {}, onEnableSource = {}, onConfirm = {}, onRetry = {}, onReset = {}
    )
}

@Preview(name = "Erreur et reprise", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ErrorPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.ERROR, "Installation impossible",
            "Le fichier est incomplet ou ses modules ne sont pas compatibles.",
            fileName = "example-release-arm64.apkm", details = "INSTALL_FAILED_INVALID_APK"
        ),
        busy = false, sourceAllowed = true, canConfirm = false, canRetry = true,
        onSelectFile = {}, onEnableSource = {}, onConfirm = {}, onRetry = {}, onReset = {}
    )
}

@Preview(name = "Confirmation Android", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ConfirmationPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.AWAITING_CONFIRMATION, "Confirmation requise",
            "Valide l'installation dans la fenêtre de sécurité Android.",
            fileName = "example.apks"
        ),
        busy = false, sourceAllowed = true, canConfirm = true, canRetry = false,
        onSelectFile = {}, onEnableSource = {}, onConfirm = {}, onRetry = {}, onReset = {}
    )
}
