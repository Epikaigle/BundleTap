package com.epikaigle.bundletap.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epikaigle.bundletap.R
import com.epikaigle.bundletap.install.FileFormats
import com.epikaigle.bundletap.install.InstallPhase
import com.epikaigle.bundletap.install.InstallUpdate

private val Background = Color(0xFF08121E)
private val Panel = Color(0xFF122232)
private val Elevated = Color(0xFF1B3145)
private val Accent = Color(0xFF7CE6D6)
private val AccentDark = Color(0xFF003830)
private val White = Color(0xFFF0F6FC)
private val Subtle = Color(0xFFA5BED0)
private val Muted = Color(0xFF688599)
private val Border = Color(0xFF2A4356)
private val BorderSubtle = Color(0xFF1C3243)
private val Warning = Color(0xFFFFD180)
private val WarningContainer = Color(0xFF382907)
private val Error = Color(0xFFFFB4AB)
private val ErrorContainer = Color(0xFF451113)

@Composable
fun BundleTapScreen(
    update: InstallUpdate,
    busy: Boolean,
    sourceAllowed: Boolean,
    canConfirm: Boolean,
    canRetry: Boolean,
    canOpenApp: Boolean = false,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit = onReset,
    onOpenApp: (String) -> Unit = {}
) {
    val palette = darkColorScheme(
        primary = Accent,
        onPrimary = AccentDark,
        primaryContainer = Color(0xFF005047),
        onPrimaryContainer = Color(0xFFA2F5E5),
        secondary = Color(0xFF8AD4C7),
        onSecondary = AccentDark,
        surface = Panel,
        onSurface = White,
        surfaceVariant = Elevated,
        onSurfaceVariant = Subtle,
        outline = Border,
        outlineVariant = BorderSubtle,
        error = Error,
        onError = Color(0xFF690005),
        errorContainer = ErrorContainer,
        onErrorContainer = Color(0xFFFFDAD6),
        background = Background,
        onBackground = White
    )

    MaterialTheme(colorScheme = palette) {
        val isWorking = busy || update.phase == InstallPhase.PREPARING ||
            update.phase == InstallPhase.INSTALLING

        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0E2235), Background, Background)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                BrandHeader()

                InstallStepTracker(phase = update.phase)

                AnimatedContent(
                    targetState = update.phase == InstallPhase.READY,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(180))
                    },
                    label = "MainCardTransition"
                ) { isReady ->
                    if (isReady) {
                        WelcomeCard(
                            sourceAllowed = sourceAllowed,
                            onSelectFile = onSelectFile,
                            onEnableSource = onEnableSource
                        )
                    } else {
                        InstallationCard(
                            update = update,
                            working = isWorking,
                            canConfirm = canConfirm,
                            canRetry = canRetry,
                            canOpenApp = canOpenApp,
                            sourceAllowed = sourceAllowed,
                            onSelectFile = onSelectFile,
                            onEnableSource = onEnableSource,
                            onConfirm = onConfirm,
                            onRetry = onRetry,
                            onReset = onReset,
                            onCancel = onCancel,
                            onOpenApp = onOpenApp
                        )
                    }
                }

                PrivacyAndSupport(
                    sourceAllowed = sourceAllowed,
                    showSettings = update.phase == InstallPhase.READY ||
                        update.phase == InstallPhase.ERROR ||
                        update.phase == InstallPhase.SUCCESS,
                    onEnableSource = onEnableSource
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    val deviceModel = remember {
        val model = Build.MODEL.trim()
        if (model.isNotBlank()) model.uppercase() else "PIXEL 7 PRO"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Accent.copy(alpha = 0.14f))
                .border(BorderStroke(1.dp, Accent.copy(alpha = 0.35f)), RoundedCornerShape(16.dp)),
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
            Text(
                "BundleTap",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                "Installateur de bundles Android",
                color = Subtle,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Elevated,
            border = BorderStroke(1.dp, Border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Accent)
                )
                Text(
                    deviceModel,
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun InstallStepTracker(phase: InstallPhase) {
    val currentStep = when (phase) {
        InstallPhase.READY -> 0
        InstallPhase.NEEDS_PERMISSION -> 0
        InstallPhase.PREPARING -> 1
        InstallPhase.INSTALLING, InstallPhase.AWAITING_CONFIRMATION -> 2
        InstallPhase.SUCCESS, InstallPhase.ERROR -> 3
    }
    val steps = listOf("Fichier", "Analyse", "Validation", "Résultat")

    Surface(
        color = Panel.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, title ->
                val isActive = index == currentStep
                val isDone = index < currentStep
                val stepColor by animateColorAsState(
                    targetValue = when {
                        isActive -> Accent
                        isDone -> Accent.copy(alpha = 0.8f)
                        else -> Muted.copy(alpha = 0.6f)
                    },
                    label = "StepColor"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Accent else if (isDone) Accent.copy(alpha = 0.2f) else Elevated
                            )
                            .border(
                                BorderStroke(1.dp, if (isActive || isDone) Accent else Border),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                painter = painterResource(R.drawable.ic_done),
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(11.dp)
                            )
                        } else {
                            Text(
                                "${index + 1}",
                                color = if (isActive) AccentDark else Subtle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        title,
                        color = stepColor,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }

                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(1.dp)
                            .background(if (index < currentStep) Accent.copy(alpha = 0.5f) else BorderSubtle)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(
    sourceAllowed: Boolean,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit
) {
    var selectedFormatInfo by remember { mutableStateOf<String?>(null) }
    val formats = listOf(
        "APK" to "Application standard unique",
        "APKM" to "Bundle APKMirror avec splits (ABI, langue, densité)",
        "APKS" to "Archive split APKs générée par bundletool",
        "XAPK" to "Package complet APKPure avec base et splits"
    )

    Surface(
        color = Panel,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Accent.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, Accent.copy(alpha = 0.3f)), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bundle),
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Un fichier.\nUne installation.",
                color = White,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Sélectionne un APK ou un bundle (APKM, APKS, XAPK). BundleTap extrait et installe uniquement les composants adaptés à ton appareil.",
                color = Subtle,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )

            if (!sourceAllowed) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = WarningContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Warning.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shield),
                            contentDescription = null,
                            tint = Warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Autorisation requise",
                                color = White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Active l'installation de sources inconnues pour continuer.",
                                color = Subtle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(
                            onClick = onEnableSource,
                            colors = ButtonDefaults.textButtonColors(contentColor = Warning)
                        ) {
                            Text("Activer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Elevated.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_done),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Source inconnue autorisée · Prêt à installer",
                        color = Subtle,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            PrimaryAction(
                text = "Sélectionner un fichier",
                onClick = onSelectFile,
                enabled = true,
                icon = R.drawable.ic_folder
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "Formats compatibles :",
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                formats.forEach { (fmt, desc) ->
                    val isSelected = selectedFormatInfo == fmt
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Accent.copy(alpha = 0.2f) else Elevated,
                        border = BorderStroke(1.dp, if (isSelected) Accent else BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedFormatInfo = if (isSelected) null else fmt
                            }
                    ) {
                        Text(
                            fmt,
                            color = if (isSelected) Accent else Subtle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = selectedFormatInfo != null) {
                selectedFormatInfo?.let { fmt ->
                    val desc = formats.firstOrNull { it.first == fmt }?.second ?: ""
                    Surface(
                        color = Elevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            desc,
                            color = Subtle,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallationCard(
    update: InstallUpdate,
    working: Boolean,
    canConfirm: Boolean,
    canRetry: Boolean,
    canOpenApp: Boolean,
    sourceAllowed: Boolean,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val failed = update.phase == InstallPhase.ERROR
    val complete = update.phase == InstallPhase.SUCCESS
    val isCancelled = failed && update.title == "Installation annulée"
    val tint = when {
        isCancelled -> Warning
        failed -> Error
        complete -> Accent
        else -> Accent
    }

    var expanded by rememberSaveable(update.details) { mutableStateOf(false) }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        color = Panel,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            1.dp,
            when {
                isCancelled -> Warning.copy(alpha = 0.45f)
                failed -> Error.copy(alpha = 0.45f)
                complete -> Accent.copy(alpha = 0.45f)
                else -> Border
            }
        ),
        modifier = Modifier.animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(tint.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, tint.copy(alpha = 0.35f)), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            when {
                                isCancelled -> R.drawable.ic_alert
                                failed -> R.drawable.ic_alert
                                complete -> R.drawable.ic_done
                                update.phase == InstallPhase.NEEDS_PERMISSION -> R.drawable.ic_shield
                                else -> R.drawable.ic_bundle
                            }
                        ),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        update.title,
                        color = White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (working) {
                        Text(
                            if (update.phase == InstallPhase.PREPARING)
                                "Analyse et extraction des modules"
                            else
                                "Transmission à Android PackageInstaller",
                            color = Accent,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                update.message,
                color = Subtle,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )

            update.fileName?.let { name ->
                Spacer(Modifier.height(18.dp))
                Surface(
                    color = Elevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val formatTag = FileFormats.formatOf(name)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Accent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f))
                        ) {
                            Text(
                                formatTag,
                                color = Accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }

                        Column(Modifier.weight(1f)) {
                            if (update.appLabel != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        update.appLabel,
                                        color = White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    update.appVersion?.let { ver ->
                                        Surface(
                                            color = Elevated,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Border)
                                        ) {
                                            Text(
                                                "v$ver",
                                                color = Subtle,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    name,
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    "Fichier sélectionné",
                                    color = Muted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    name,
                                    color = White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (update.splitCount != null && update.splitCount > 1) {
                                Text(
                                    "Base + ${update.splitCount - 1} splits configurés",
                                    color = Accent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (working) {
                Spacer(Modifier.height(22.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Accent,
                    trackColor = Elevated
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Traitement en cours…",
                        color = Subtle,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        if (update.phase == InstallPhase.PREPARING) "Étape 2/4" else "Étape 3/4",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        "Annuler la préparation",
                        color = Subtle,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            when (update.phase) {
                InstallPhase.NEEDS_PERMISSION -> {
                    Spacer(Modifier.height(24.dp))
                    PrimaryAction("Autoriser l'installation", onEnableSource, icon = R.drawable.ic_shield)
                    Spacer(Modifier.height(10.dp))
                    SecondaryAction("Annuler", onReset)
                }

                InstallPhase.AWAITING_CONFIRMATION -> {
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        color = Elevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_shield),
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "La boîte de dialogue système Android nécessite ta validation pour installer l'application.",
                                color = Subtle,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    if (canConfirm) {
                        PrimaryAction("Confirmer dans Android", onConfirm)
                        Spacer(Modifier.height(10.dp))
                        SecondaryAction("Annuler l'installation", onReset)
                    } else {
                        Text(
                            "Si aucune fenêtre ne s'affiche, ouvre à nouveau ce fichier.",
                            color = Subtle,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        SecondaryAction("Annuler l'installation", onReset)
                    }
                }

                InstallPhase.ERROR -> {
                    Spacer(Modifier.height(24.dp))
                    PrimaryAction("Choisir un autre fichier", onSelectFile, icon = R.drawable.ic_folder)
                    if (canRetry) {
                        Spacer(Modifier.height(10.dp))
                        SecondaryAction("Réessayer ce fichier", onRetry)
                    }

                    if (!update.details.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(if (expanded) "Masquer les détails" else "Détails techniques")
                            }

                            if (expanded) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(update.details))
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        Toast.makeText(
                                            context,
                                            "Détails copiés dans le presse-papiers",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_copy),
                                        contentDescription = null,
                                        tint = Accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copier", color = Accent, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        AnimatedVisibility(visible = expanded) {
                            Surface(
                                color = Elevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    update.details,
                                    color = Subtle,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    TertiaryAction("Effacer le résultat", onReset)
                }

                InstallPhase.SUCCESS -> {
                    Spacer(Modifier.height(24.dp))
                    if (canOpenApp && !update.packageName.isNullOrBlank()) {
                        PrimaryAction(
                            text = "Ouvrir l'application",
                            onClick = { onOpenApp(update.packageName) },
                            icon = R.drawable.ic_launch
                        )
                        Spacer(Modifier.height(10.dp))
                        SecondaryAction("Installer un autre fichier", onSelectFile)
                        Spacer(Modifier.height(4.dp))
                        TertiaryAction("Retour à l'accueil", onReset)
                    } else {
                        PrimaryAction("Installer un autre fichier", onSelectFile, icon = R.drawable.ic_folder)
                        Spacer(Modifier.height(10.dp))
                        SecondaryAction("Retour à l'accueil", onReset)
                    }
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
    enabled: Boolean = true,
    icon: Int? = null
) {
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = AccentDark,
            disabledContainerColor = Elevated,
            disabledContentColor = Muted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SecondaryAction(text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Text(
            text,
            color = Accent,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun TertiaryAction(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
    ) {
        Text(
            text,
            color = Subtle,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun PrivacyAndSupport(
    sourceAllowed: Boolean,
    showSettings: Boolean,
    onEnableSource: () -> Unit
) {
    val versionName = remember {
        com.epikaigle.bundletap.BuildConfig.VERSION_NAME
    }
    val abi = remember { Build.SUPPORTED_ABIS.firstOrNull() ?: "ARM64" }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = Panel.copy(alpha = 0.6f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Une installation contrôlée",
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Traitement 100 % local. Aucun compte, aucune connexion réseau. Android demande ton accord avant chaque installation.",
                        color = Subtle,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (showSettings) {
            OutlinedButton(
                onClick = onEnableSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield),
                    contentDescription = null,
                    tint = if (sourceAllowed) Accent else Warning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (sourceAllowed) "Gérer l'autorisation Android" else "Autoriser BundleTap dans les paramètres",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sourceAllowed) Subtle else Warning
                )
            }
        }

        Text(
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})  ·  $abi  ·  v$versionName",
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Accueil avec autorisation", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun WelcomePreview() {
    BundleTapScreen(
        update = InstallUpdate(InstallPhase.READY, "Prêt à installer", "Choisis un fichier pour commencer."),
        busy = false,
        sourceAllowed = true,
        canConfirm = false,
        canRetry = false,
        onSelectFile = {},
        onEnableSource = {},
        onConfirm = {},
        onRetry = {},
        onReset = {}
    )
}

@Preview(name = "Accueil sans autorisation", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun WelcomeNoPermissionPreview() {
    BundleTapScreen(
        update = InstallUpdate(InstallPhase.READY, "Prêt à installer", "Choisis un fichier pour commencer."),
        busy = false,
        sourceAllowed = false,
        canConfirm = false,
        canRetry = false,
        onSelectFile = {},
        onEnableSource = {},
        onConfirm = {},
        onRetry = {},
        onReset = {}
    )
}

@Preview(name = "Préparation et analyse", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PreparingPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.PREPARING,
            "Préparation du fichier",
            "Analyse et sélection des modules compatibles avec ton appareil.",
            fileName = "spotify-arm64-v8a.apkm",
            splitCount = 4,
            appLabel = "Spotify",
            appVersion = "8.9.10"
        ),
        busy = true,
        sourceAllowed = true,
        canConfirm = false,
        canRetry = false,
        onSelectFile = {},
        onEnableSource = {},
        onConfirm = {},
        onRetry = {},
        onReset = {}
    )
}

@Preview(name = "Confirmation requise", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ConfirmationPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.AWAITING_CONFIRMATION,
            "Confirmation requise",
            "Valide l'installation dans la fenêtre de sécurité Android.",
            fileName = "example.apks",
            splitCount = 3
        ),
        busy = false,
        sourceAllowed = true,
        canConfirm = true,
        canRetry = false,
        onSelectFile = {},
        onEnableSource = {},
        onConfirm = {},
        onRetry = {},
        onReset = {}
    )
}

@Preview(name = "Succès avec ouverture", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SuccessPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.SUCCESS,
            "Installation terminée",
            "L'application est installée sur ton appareil.",
            fileName = "app-release.xapk",
            packageName = "com.example.app",
            appLabel = "Exemple App",
            appVersion = "1.0.0"
        ),
        busy = false,
        sourceAllowed = true,
        canConfirm = false,
        canRetry = false,
        canOpenApp = true,
        onSelectFile = {},
        onEnableSource = {},
        onConfirm = {},
        onRetry = {},
        onReset = {}
    )
}

@Preview(name = "Erreur avec détails techniques", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ErrorPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.ERROR,
            "Installation impossible",
            "Le fichier est incomplet ou ses modules ne sont pas compatibles.",
            fileName = "example-release-arm64.apkm",
            details = "INSTALL_FAILED_INVALID_APK: Failed to parse /data/app/vmdl12345.tmp/base.apk"
        ),
        busy = false,
        sourceAllowed = true,
        canConfirm = false,
        canRetry = true,
        onSelectFile = {},
        onEnableSource = {},
        onConfirm = {},
        onRetry = {},
        onReset = {}
    )
}

@Preview(name = "Installation annulée", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun CancelledPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.ERROR,
            "Installation annulée",
            "L'installation a été interrompue. Tu peux relancer ce fichier ou en choisir un autre.",
            fileName = "example-release-arm64.apkm"
        ),
        busy = false,
        sourceAllowed = true,
        canConfirm = false,
        canRetry = true,
        onSelectFile = {},
        onEnableSource = {},
        onConfirm = {},
        onRetry = {},
        onReset = {}
    )
}
