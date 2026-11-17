package com.epikaigle.bundletap.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epikaigle.bundletap.BuildConfig
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
    isDragging: Boolean = false,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit = onReset,
    onOpenApp: (String) -> Unit = {},
    onInstall: () -> Unit = {}
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0E2235), Background, Background)
                        )
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                val horizontalPadding = if (maxWidth < 360.dp) 14.dp else 20.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BrandHeader(
                        sourceAllowed = sourceAllowed,
                        onEnableSource = onEnableSource
                    )

                    AnimatedVisibility(visible = update.phase != InstallPhase.READY) {
                        InstallStepTracker(phase = update.phase)
                    }

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
                                isDragging = isDragging,
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
                                isDragging = isDragging,
                                onSelectFile = onSelectFile,
                                onEnableSource = onEnableSource,
                                onConfirm = onConfirm,
                                onRetry = onRetry,
                                onReset = onReset,
                                onCancel = onCancel,
                                onOpenApp = onOpenApp,
                                onInstall = onInstall
                            )
                        }
                    }

                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = Muted.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(
    sourceAllowed: Boolean,
    onEnableSource: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Accent.copy(alpha = 0.14f))
                .border(BorderStroke(1.dp, Accent.copy(alpha = 0.35f)), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bundle),
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "BundleTap",
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (sourceAllowed) {
            Surface(
                onClick = onEnableSource,
                shape = RoundedCornerShape(14.dp),
                color = Elevated,
                border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = "Sources inconnues autorisées",
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Surface(
                onClick = onEnableSource,
                shape = RoundedCornerShape(14.dp),
                color = WarningContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, Warning.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        tint = Warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Autoriser",
                        color = Warning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallStepTracker(phase: InstallPhase) {
    val currentStep = when (phase) {
        InstallPhase.READY, InstallPhase.FILE_SELECTED, InstallPhase.NEEDS_PERMISSION -> 0
        InstallPhase.PREPARING -> 1
        InstallPhase.INSTALLING, InstallPhase.AWAITING_CONFIRMATION -> 2
        InstallPhase.SUCCESS, InstallPhase.ERROR -> 3
    }
    val stepIcons = listOf(
        R.drawable.ic_folder to "Sélection",
        R.drawable.ic_bundle to "Préparation",
        R.drawable.ic_shield to "Validation",
        (if (phase == InstallPhase.ERROR) R.drawable.ic_alert else R.drawable.ic_done) to "Résultat"
    )

    Surface(
        color = Panel.copy(alpha = 0.75f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepIcons.forEachIndexed { index, (iconRes, label) ->
                val isActive = index == currentStep
                val isDone = index < currentStep || (index == 3 && phase == InstallPhase.SUCCESS)
                val isErrorStep = index == 3 && phase == InstallPhase.ERROR

                val containerColor by animateColorAsState(
                    targetValue = when {
                        isErrorStep -> ErrorContainer
                        isActive && !isDone -> Accent
                        isDone -> Accent.copy(alpha = 0.2f)
                        else -> Elevated
                    },
                    label = "StepContainerColor"
                )
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isErrorStep -> Error
                        isActive || isDone -> Accent
                        else -> Border
                    },
                    label = "StepBorderColor"
                )
                val iconTint = when {
                    isErrorStep -> Error
                    isActive && !isDone -> AccentDark
                    isDone -> Accent
                    else -> Muted
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(containerColor)
                        .border(BorderStroke(1.dp, borderColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isDone) R.drawable.ic_done else iconRes),
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (index < stepIcons.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < currentStep) Accent.copy(alpha = 0.55f)
                                else BorderSubtle
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeCard(
    sourceAllowed: Boolean,
    isDragging: Boolean,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit
) {
    val formats = listOf("APK", "APKS", "APKM", "XAPK", "APKX", "ZIP")

    Surface(
        color = Panel,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            width = if (isDragging) 2.dp else 1.dp,
            color = if (isDragging) Accent else Border
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Accent.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, Accent.copy(alpha = 0.3f)), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bundle),
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(36.dp)
                )
            }

            if (!sourceAllowed) {
                Surface(
                    onClick = onEnableSource,
                    color = WarningContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Warning.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shield),
                            contentDescription = null,
                            tint = Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sources inconnues désactivées",
                            color = White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onEnableSource,
                            colors = ButtonDefaults.textButtonColors(contentColor = Warning)
                        ) {
                            Text("Activer", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            PrimaryAction(
                text = "Sélectionner un fichier",
                onClick = onSelectFile,
                enabled = true,
                icon = R.drawable.ic_folder
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                formats.forEach { fmt ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Elevated,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = fmt,
                            color = Subtle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallingAnimation(phase: InstallPhase, appIcon: Bitmap? = null) {
    val infiniteTransition = rememberInfiniteTransition(label = "InstallingInfinite")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(104.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = glowAlpha * 0.35f))
            )

            Canvas(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer(rotationZ = rotation)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Accent.copy(alpha = 0.25f),
                            Accent
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 280f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Elevated)
                    .border(BorderStroke(1.5.dp, Accent.copy(alpha = 0.6f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .scale(pulseScale)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_bundle),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(pulseScale)
                    )
                }
            }
        }

        Text(
            text = if (phase == InstallPhase.PREPARING) "Préparation…" else "Installation…",
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FileSummaryPill(update: InstallUpdate) {
    val name = update.fileName ?: return
    Surface(
        color = Elevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
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
                    text = formatTag,
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (update.appLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = update.appLabel,
                            color = White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        update.appVersion?.let { ver ->
                            Surface(
                                color = Panel,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Border)
                            ) {
                                Text(
                                    text = "v$ver",
                                    color = Subtle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = name,
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = name,
                        color = White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val sizeText = update.fileSizeBytes?.let { FileFormats.formatFileSize(it) }
                    ?.takeIf { it.isNotBlank() }
                val splitText = if (update.splitCount != null && update.splitCount > 1) {
                    "${update.splitCount} modules"
                } else null
                val metaText = listOfNotNull(sizeText, splitText).joinToString(" • ")
                if (metaText.isNotEmpty()) {
                    Text(
                        text = metaText,
                        color = Accent,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    isDragging: Boolean,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onOpenApp: (String) -> Unit,
    onInstall: () -> Unit
) {
    val failed = update.phase == InstallPhase.ERROR
    val complete = update.phase == InstallPhase.SUCCESS
    val isCancelled = failed && update.title == "Installation annulée"
    val tint = when {
        isCancelled -> Warning
        failed -> Error
        else -> Accent
    }
    val canDismiss = update.phase in setOf(
        InstallPhase.FILE_SELECTED,
        InstallPhase.SUCCESS,
        InstallPhase.ERROR
    )

    var expanded by rememberSaveable(update.details) { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        color = Panel,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            width = if (isDragging) 2.dp else 1.dp,
            color = when {
                isDragging -> Accent
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
                .padding(20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            if (working) {
                InstallingAnimation(phase = update.phase, appIcon = update.appIcon)
                if (update.fileName != null) {
                    Spacer(Modifier.height(12.dp))
                    FileSummaryPill(update)
                }
                Spacer(Modifier.height(16.dp))
                SecondaryAction(
                    text = "Annuler",
                    onClick = onCancel,
                    icon = R.drawable.ic_close
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (update.appIcon != null && !failed && !isCancelled) {
                        Box(modifier = Modifier.size(52.dp)) {
                            Image(
                                bitmap = update.appIcon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
                                        RoundedCornerShape(16.dp)
                                    )
                            )
                            if (complete) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Accent)
                                        .border(BorderStroke(1.5.dp, Panel), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_done),
                                        contentDescription = null,
                                        tint = AccentDark,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(tint.copy(alpha = 0.12f))
                                .border(
                                    BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    when {
                                        isCancelled || failed -> R.drawable.ic_alert
                                        complete -> R.drawable.ic_done
                                        update.phase == InstallPhase.NEEDS_PERMISSION -> R.drawable.ic_shield
                                        else -> R.drawable.ic_bundle
                                    }
                                ),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Text(
                        text = update.title,
                        color = White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (canDismiss) {
                        IconButton(onClick = onReset) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Fermer",
                                tint = Subtle,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (update.message.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = update.message,
                        color = Subtle,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }

                if (update.fileName != null) {
                    Spacer(Modifier.height(16.dp))
                    FileSummaryPill(update)
                }

                when (update.phase) {
                    InstallPhase.FILE_SELECTED -> {
                        Spacer(Modifier.height(18.dp))
                        PrimaryAction(
                            text = "Installer",
                            onClick = onInstall,
                            icon = R.drawable.ic_install
                        )
                        Spacer(Modifier.height(10.dp))
                        SecondaryAction(
                            text = "Autre fichier",
                            onClick = onSelectFile,
                            icon = R.drawable.ic_folder
                        )
                    }

                    InstallPhase.NEEDS_PERMISSION -> {
                        Spacer(Modifier.height(18.dp))
                        PrimaryAction(
                            text = "Autoriser l'installation",
                            onClick = onEnableSource,
                            icon = R.drawable.ic_shield
                        )
                        Spacer(Modifier.height(10.dp))
                        SecondaryAction(
                            text = "Annuler",
                            onClick = onReset,
                            icon = R.drawable.ic_close
                        )
                    }

                    InstallPhase.AWAITING_CONFIRMATION -> {
                        Spacer(Modifier.height(18.dp))
                        if (canConfirm) {
                            PrimaryAction(
                                text = "Confirmer dans Android",
                                onClick = onConfirm,
                                icon = R.drawable.ic_shield
                            )
                            Spacer(Modifier.height(10.dp))
                            SecondaryAction(
                                text = "Annuler l'installation",
                                onClick = onReset,
                                icon = R.drawable.ic_close
                            )
                        } else {
                            Text(
                                text = "Aucune fenêtre système détectée.",
                                color = Subtle,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(12.dp))
                            SecondaryAction(
                                text = "Annuler l'installation",
                                onClick = onReset,
                                icon = R.drawable.ic_close
                            )
                        }
                    }

                    InstallPhase.ERROR -> {
                        Spacer(Modifier.height(18.dp))
                        if (canRetry) {
                            PrimaryAction(
                                text = "Réessayer",
                                onClick = onRetry,
                                icon = R.drawable.ic_refresh
                            )
                            Spacer(Modifier.height(10.dp))
                            SecondaryAction(
                                text = "Autre fichier",
                                onClick = onSelectFile,
                                icon = R.drawable.ic_folder
                            )
                        } else {
                            PrimaryAction(
                                text = "Sélectionner un fichier",
                                onClick = onSelectFile,
                                icon = R.drawable.ic_folder
                            )
                        }

                        if (!update.details.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { expanded = !expanded },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text(if (expanded) "Masquer" else "Détails techniques")
                                }

                                if (expanded) {
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, update.details)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                Toast.makeText(
                                                    context,
                                                    "Copié",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_copy),
                                            contentDescription = "Copier les détails",
                                            tint = Accent,
                                            modifier = Modifier.size(20.dp)
                                        )
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
                                        .padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = update.details,
                                        color = Subtle,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    InstallPhase.SUCCESS -> {
                        Spacer(Modifier.height(18.dp))
                        if (canOpenApp && !update.packageName.isNullOrBlank()) {
                            PrimaryAction(
                                text = "Ouvrir l'application",
                                onClick = { onOpenApp(update.packageName) },
                                icon = R.drawable.ic_launch
                            )
                            Spacer(Modifier.height(10.dp))
                            SecondaryAction(
                                text = "Autre fichier",
                                onClick = onSelectFile,
                                icon = R.drawable.ic_folder
                            )
                        } else {
                            PrimaryAction(
                                text = "Autre fichier",
                                onClick = onSelectFile,
                                icon = R.drawable.ic_folder
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("BundleTap", text))
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
            .defaultMinSize(minHeight = 54.dp),
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
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    icon: Int? = null
) {
    val haptic = LocalHapticFeedback.current

    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Accent,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "Accueil", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun WelcomePreview() {
    BundleTapScreen(
        update = InstallUpdate(InstallPhase.READY, "BundleTap", ""),
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

@Preview(name = "Split-screen étroit (320dp)", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun NarrowSplitPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.FILE_SELECTED,
            "Prêt",
            "",
            fileName = "Mon.Application.Ultra.Longue_v8.9.10-arm64.xapk",
            appLabel = "Application avec un nom très long",
            appVersion = "8.9.10-release",
            fileSizeBytes = 78_500_000L,
            splitCount = 4
        ),
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

@Preview(name = "Préparation", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PreparingPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.PREPARING,
            "Préparation…",
            "",
            fileName = "spotify-arm64-v8a.apkm",
            fileSizeBytes = 45_200_000L,
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

@Preview(name = "Succès", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SuccessPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.SUCCESS,
            "Installée",
            "",
            fileName = "app-release.xapk",
            packageName = "com.example.app",
            appLabel = "Exemple App",
            appVersion = "1.0.0",
            fileSizeBytes = 12_300_000L
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

@Preview(name = "Erreur", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ErrorPreview() {
    BundleTapScreen(
        update = InstallUpdate(
            InstallPhase.ERROR,
            "Échec de l'installation",
            "Fichier corrompu, mal signé ou incomplet.",
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
