package com.epikaigle.bundletap.install

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln

/**
 * Resolves only one base APK and compatible config splits. Never mixes unrelated
 * standalone/split variants or installs APKs for another device architecture.
 * Metadata from an XAPK may explicitly identify an arbitrarily named base.
 */
object ApkSelector {
    private val abiTokenMap = mapOf(
        "arm64_v8a" to "arm64_v8a",
        "arm64" to "arm64_v8a",
        "aarch64" to "arm64_v8a",
        "armeabi_v7a" to "armeabi_v7a",
        "armeabi" to "armeabi_v7a",
        "armv7" to "armeabi_v7a",
        "x86_64" to "x86_64",
        "x64" to "x86_64",
        "x86" to "x86"
    )
    private val densityTokens = linkedMapOf(
        "ldpi" to 120, "mdpi" to 160, "tvdpi" to 213,
        "hdpi" to 240, "xhdpi" to 320, "xxhdpi" to 480, "xxxhdpi" to 640
    )
    private val languages = Locale.getISOLanguages().toSet() + setOf("fil", "in", "iw", "ji")

    fun select(
        paths: List<String>,
        supportedAbis: List<String>,
        densityDpi: Int,
        locales: List<String>,
        baseHint: String? = null
    ): List<String> {
        require(paths.isNotEmpty()) { "Aucun fichier APK dans cette archive." }
        require(paths.distinct().size == paths.size) { "Noms d'APK dupliqués." }
        if (paths.size == 1) return paths

        val deviceAbis = supportedAbis.map {
            it.lowercase(Locale.ROOT).replace('-', '_')
        }

        val hasSplits = paths.any {
            val l = it.lowercase(Locale.ROOT)
            l.startsWith("splits/") || l.contains("/splits/")
        }
        val candidates = paths.filter { path ->
            val lower = path.lowercase(Locale.ROOT)
            if (hasSplits) lower.startsWith("splits/") || lower.contains("/splits/")
            else !lower.startsWith("standalones/") && !lower.contains("/standalones/") &&
                !lower.startsWith("instant/") && !lower.contains("/instant/") &&
                !lower.startsWith("system/") && !lower.contains("/system/")
        }
        if (candidates.isEmpty()) {
            val standalones = paths.filter {
                it.startsWith("standalones/", ignoreCase = true) ||
                    it.contains("/standalones/", ignoreCase = true)
            }
            if (standalones.size == 1) return standalones
            if (standalones.isNotEmpty()) {
                for (abi in deviceAbis) {
                    val match = standalones.firstOrNull { path ->
                        val s = stem(path)
                        abiTokenMap.entries.any { (token, canonical) ->
                            canonical == abi && hasToken(s, token)
                        }
                    }
                    if (match != null) return listOf(match)
                }
                throw IllegalArgumentException(
                    "Cette archive contient des variantes standalone incompatibles avec ton appareil."
                )
            }
            throw IllegalArgumentException(
                "Cette archive ne contient aucun composant d'installation exploitable."
            )
        }

        val hinted = baseHint?.let { hint ->
            val matches = candidates.filter {
                it.equals(hint, ignoreCase = true) ||
                    it.substringAfterLast('/').equals(hint.substringAfterLast('/'), ignoreCase = true)
            }
            require(matches.size <= 1) { "Plusieurs APK principaux correspondent au manifeste." }
            matches.singleOrNull()
        }
        if (baseHint != null && hinted == null) {
            throw IllegalArgumentException("L'APK principal déclaré dans le manifeste est absent.")
        }
        val universal = candidates.filter {
            it.substringAfterLast('/').lowercase(Locale.ROOT) in
                setOf("universal.apk", "standalone.apk")
        }
        if (hinted == null && universal.size == 1) return universal
        val bases = candidates.filter {
            it.substringAfterLast('/').lowercase(Locale.ROOT) in
                setOf("base.apk", "base-master.apk", "base_master.apk", "main.apk", "master.apk")
        }
        val inferredBase = if (hinted == null && bases.isEmpty()) {
            val nonSplits = candidates.filter { path ->
                val s = stem(path)
                val isAbi = abiTokenMap.keys.any { hasToken(s, it) }
                val isDensity = densityFor(path) != null
                val isLang = languageFor(path, emptySet()) != null
                !isAbi && !isDensity && !isLang
            }
            if (nonSplits.size == 1) {
                nonSplits.single()
            } else if (nonSplits.isNotEmpty()) {
                nonSplits.firstOrNull {
                    val s = stem(it)
                    s.contains("base") || s.contains("master") || s.contains("main")
                }
            } else null
        } else null

        val base = hinted ?: bases.singleOrNull() ?: inferredBase
            ?: throw IllegalArgumentException(
                "APK principal introuvable ou ambigu. Vérifie l'archive."
            )

        val wantedLanguages = locales.map { it.substringBefore('-').lowercase(Locale.ROOT) }
            .toSet() + "en"

        val masterPrefixes = candidates.mapNotNull { candidate ->
            val s = stem(candidate)
            val suffix = Regex("[._-]master$").find(s)
            suffix?.let { s.substring(0, it.range.first) }
        }.toSet()
        val others = candidates.filterNot { it == base }
        val abiCandidates = others.mapNotNull { path ->
            val s = stem(path)
            val matched = abiTokenMap.entries.firstOrNull { hasToken(s, it.key) }
            matched?.let { path to it.value }
        }
        val densityCandidates = others.filterNot { p ->
            abiCandidates.any { it.first == p }
        }.mapNotNull { p -> densityFor(p)?.let { p to it } }

        val abiSelected = abiCandidates.groupBy { groupOf(it.first, it.second) }.values
            .mapNotNull { group ->
                val best = deviceAbis.firstOrNull { abi ->
                    group.any { it.second == abi }
                } ?: return@mapNotNull null
                group.firstOrNull { it.second == best }?.first
            }.toSet()

        val densitySelected = densityCandidates.groupBy {
            groupOf(it.first, densityLabel(it.first))
        }.values.mapNotNull { group ->
            group.minWithOrNull(compareBy<Pair<String, Int>> {
                abs(ln(it.second.toDouble() / densityDpi.coerceAtLeast(1)))
            }.thenBy { it.first })?.first
        }.toSet()

        val abiPaths = abiCandidates.map { it.first }.toSet()
        val densityPaths = densityCandidates.map { it.first }.toSet()
        val picked = others.filter { path ->
            when {
                path in abiPaths -> path in abiSelected
                path in densityPaths -> path in densitySelected
                else -> {
                    val language = languageFor(path, masterPrefixes)
                    language == null || language in wantedLanguages
                }
            }
        }
        return listOf(base) + picked
    }

    private fun stem(path: String) =
        path.substringAfterLast('/').removeSuffix(".apk").lowercase(Locale.ROOT)

    private fun hasToken(stem: String, token: String): Boolean {
        val normal = stem.replace('-', '_').replace('.', '_')
        return Regex("(^|_)" + Regex.escape(token) + "(_|$)").containsMatchIn(normal)
    }

    private fun densityFor(path: String): Int? {
        val s = stem(path)
        val symbolic = densityTokens.entries.firstOrNull { hasToken(s, it.key) }
        if (symbolic != null) return symbolic.value
        return Regex("(?:^|[._-])(\\d{3,4})dpi(?:$|[._-])")
            .find(s)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun densityLabel(path: String): String {
        val s = stem(path)
        return densityTokens.keys.firstOrNull { hasToken(s, it) }
            ?: Regex("(?:^|[._-])\\d{3,4}dpi$").find(s)?.value
                ?.trimStart('.', '-', '_')
            ?: ""
    }

    private fun groupOf(path: String, qualifier: String): String {
        val s = stem(path)
        val escaped = Regex.escape(qualifier)
        return s.replace(Regex("(?i)[._-]$escaped$"), "")
    }

    private fun languageFor(path: String, masterPrefixes: Set<String>): String? {
        val s = stem(path)
        // Restrict to configuration suffixes, not dynamic feature module names.
        val match = Regex("(?:^|[._-])([a-z]{2,3})$").find(s) ?: return null
        val lang = match.groupValues[1]
        if (lang !in languages) return null
        val prefix = s.substring(0, match.range.first)
        return if (prefix.endsWith("config") || prefix == "base" || prefix in masterPrefixes ||
            prefix.startsWith("base_") || prefix.startsWith("base.")) lang else null
    }
}
