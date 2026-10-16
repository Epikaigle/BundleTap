package com.epikaigle.bundletap.install

import kotlin.math.abs
import kotlin.math.ln

/**
 * Conservative filename selection for split sets. No hidden installs or architecture guessing.
 * APKM and XAPK generally contain device-targeted splits. Bundletool .apks may contain variants.
 * If we cannot recognize an unambiguous base APK, refuse rather than guessing.
 */
object ApkSelector {
    private val abiTokens = linkedMapOf(
        "arm64_v8a" to "arm64-v8a",
        "armeabi_v7a" to "armeabi-v7a",
        "x86_64" to "x86_64",
        "x86" to "x86"
    )

    private val densities = linkedMapOf(
        "ldpi" to 120, "mdpi" to 160, "tvdpi" to 213,
        "hdpi" to 240, "xhdpi" to 320, "xxhdpi" to 480, "xxxhdpi" to 640
    )

    fun select(
        paths: List<String>,
        supportedAbis: List<String>,
        densityDpi: Int,
        locales: List<String>
    ): List<String> {
        require(paths.isNotEmpty()) { "Aucun APK trouvé dans ce fichier." }
        if (paths.size == 1) return paths

        // A bundletool set may include mutually exclusive standalone, instant and split variants.
        val splitMode = paths.any { it.startsWith("splits/", ignoreCase = true) }
        val candidates = paths.filter { path ->
            val normalized = path.lowercase()
            if (splitMode) normalized.startsWith("splits/")
            else !normalized.startsWith("standalones/") &&
                !normalized.startsWith("instant/") &&
                !normalized.startsWith("system/")
        }
        if (candidates.isEmpty()) throw IllegalArgumentException("Aucun groupe d'APK installable.")

        val universal = candidates.filter {
            val name = it.substringAfterLast('/').lowercase()
            name == "universal.apk" || name == "standalone.apk"
        }
        if (universal.size == 1) return universal

        val bases = candidates.filter {
            val name = it.substringAfterLast('/').lowercase()
            name == "base.apk" || name == "base-master.apk" ||
                name == "base_master.apk"
        }
        val base = when {
            bases.size == 1 -> bases.single()
            bases.isEmpty() -> throw IllegalArgumentException(
                "APK principal introuvable : ensemble de fichiers non reconnu."
            )
            else -> throw IllegalArgumentException("Plusieurs APK principaux : archive ambiguë.")
        }

        val normalisedAbis = supportedAbis.map { it.lowercase().replace('-', '_') }
        val availableAbis = candidates.flatMap { path ->
            abiTokens.keys.filter { hasToken(path.substringAfterLast('/').lowercase(), it) }
        }.toSet()
        val preferredAbi = normalisedAbis.firstOrNull { it in availableAbis }.orEmpty()
        val languages = locales.map { it.substringBefore('-').lowercase() }.toSet() + "en"
        val densityEntries = candidates.filter { name -> densityFor(name) != null }
        val closestDensity = densityEntries.minByOrNull { name ->
            val candidateDensity = densityFor(name)!!.toDouble()
            abs(ln(candidateDensity / densityDpi.coerceAtLeast(1).toDouble()))
        }?.let(::densityFor)

        val selected = candidates.filter { path ->
            if (path == base) return@filter true
            val lowered = path.substringAfterLast('/').lowercase()
            val abi = abiTokens.keys.firstOrNull { token -> hasToken(lowered, token) }
            if (abi != null) {
                // Prefer the first ABI that the device supports.
                return@filter normalisedAbis.contains(abi) && abi == preferredAbi
            }
            val density = densityFor(path)
            if (density != null) return@filter density == closestDensity
            val language = languageFor(path)
            if (language != null) return@filter language in languages
            true // feature-master and otherwise unqualified splits
        }
        if (base !in selected) throw IllegalStateException("APK principal manquant.")
        return listOf(base) + selected.filterNot { it == base }
    }

    private fun hasToken(name: String, token: String): Boolean {
        val normalized = name.replace('-', '_').replace('.', '_')
        return Regex("(^|_)" + Regex.escape(token) + "(_|$)").containsMatchIn(normalized)
    }

    private fun densityFor(path: String): Int? {
        val name = path.substringAfterLast('/').lowercase()
        val symbolic = densities.entries.firstOrNull { hasToken(name, it.key) }?.value
        if (symbolic != null) return symbolic
        val match = Regex("(?:^|[-_.])(\\d{3,4})dpi(?:[-_.]|$)")
            .find(name) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun languageFor(path: String): String? {
        val name = path.substringAfterLast('/').lowercase().removeSuffix(".apk")
        val token = name.split('.', '_', '-').lastOrNull() ?: return null
        return token.takeIf {
            it.length in 2..3 && it.all(Char::isLetter) &&
                it !in setOf("apk", "base", "dpi")
        }
    }
}
