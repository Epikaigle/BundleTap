package com.epikaigle.bundletap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epikaigle.bundletap.install.InstallPhase
import com.epikaigle.bundletap.install.InstallUpdate

private val Navy = Color(0xFF07131E)
private val Panel = Color(0xFF122638)
private val Mint = Color(0xFF71E7D5)
private val Muted = Color(0xFFB0C0CC)

@Composable
fun BundleTapScreen(
    update: InstallUpdate,
    fileName: String?,
    busy: Boolean,
    sourceAllowed: Boolean,
    canConfirm: Boolean,
    onSelectFile: () -> Unit,
    onEnableSource: () -> Unit,
    onConfirm: () -> Unit
) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, background = Navy, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(color = Navy, modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(52.dp).background(
                            Mint.copy(alpha = .13f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("B", color = Mint, fontSize = 29.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("BundleTap", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("INSTALLATEUR ANDROID", color = Muted, fontSize = 10.sp,
                            letterSpacing = 1.8.sp)
                    }
                }

                Card(shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.size(104.dp).background(
                            Mint.copy(alpha = .11f), RoundedCornerShape(30.dp)),
                            contentAlignment = Alignment.Center) {
                            Text("↓", color = Mint, fontSize = 58.sp,
                                fontWeight = FontWeight.Light)
                        }
                        Spacer(Modifier.height(22.dp))
                        Text("Installer, sans complication.", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ouvre un fichier APK ou un bundle. BundleTap prépare les modules, Android confirme l'installation.",
                            fontSize = 14.sp, color = Muted, lineHeight = 21.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(26.dp))
                        Button(
                            onClick = onSelectFile,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Mint, contentColor = Navy)
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(10.dp))
                            Text("Choisir un fichier", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(".APK   ·   .APKM   ·   .APKS   ·   .XAPK",
                            color = Muted, fontSize = 12.sp)
                    }
                }

                Card(shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (update.phase) {
                                    InstallPhase.SUCCESS -> "✓"
                                    InstallPhase.ERROR -> "!"
                                    else -> "•"
                                },
                                color = if (update.phase == InstallPhase.ERROR)
                                    Color(0xFFFF9D99) else Mint,
                                fontSize = 26.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(update.title, fontWeight = FontWeight.Bold,
                                fontSize = 17.sp, modifier = Modifier.weight(1f))
                            if (busy) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Mint, strokeWidth = 2.dp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(update.message, color = Muted,
                            fontSize = 13.sp, lineHeight = 20.sp)
                        if (fileName != null) {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = .12f))
                            Spacer(Modifier.height(12.dp))
                            Text("FICHIER", color = Mint, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                            Text(fileName, fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                        if (update.phase == InstallPhase.AWAITING_CONFIRMATION &&
                            !sourceAllowed) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = onEnableSource,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("Autoriser l'installation")
                            }
                        } else if (update.phase == InstallPhase.AWAITING_CONFIRMATION &&
                            canConfirm) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = onConfirm,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("Afficher la confirmation Android")
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("▣", color = Mint, fontSize = 26.sp)
                    Column {
                        Text("Conçu pour ton Pixel 7 Pro",
                            fontWeight = FontWeight.SemiBold)
                        Text("Android 17 • ARM64", color = Muted, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = .10f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top) {
                    Text("◇", color = Mint, fontSize = 27.sp)
                    Column {
                        Text("Installation sécurisée", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Pas de root ni de publicité. La confirmation d'Android reste obligatoire. Utilise des fichiers de confiance.",
                            fontSize = 13.sp, lineHeight = 20.sp, color = Muted)
                    }
                }
                OutlinedButton(onClick = onEnableSource,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("⚙", color = Mint, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("Autorisation d'installation")
                }
                Text("Traitement local • aucune donnée envoyée",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, color = Muted, fontSize = 11.sp)
            }
        }
    }
}
