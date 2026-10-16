# BundleTap · Android 17

[![Android APK](https://github.com/Epikaigle/BundleTap/actions/workflows/android.yml/badge.svg)](https://github.com/Epikaigle/BundleTap/actions/workflows/android.yml)

Installateur personnel APK, APKM, APKS et XAPK pour **Pixel 7 Pro / Android 17 (API 37)**. Application Kotlin + Jetpack Compose, sans publicité, serveur, root ou connexion réseau.

## Installer sur le Pixel

1. Ouvre les [compilations GitHub](https://github.com/Epikaigle/BundleTap/actions/workflows/android.yml) et sélectionne une exécution réussie sur main.
2. Télécharge l'artefact **BundleTap-debug-Android17**, ou **BundleTap-signed-release-Android17** si tu as configuré ta signature personnelle.
3. Extrais le ZIP, ouvre l'APK sur ton Pixel, puis autorise son installation si nécessaire.
4. Ouvre BundleTap, choisis un APK/APKM/APKS/XAPK et valide la confirmation système.

**Signature :** les APK debug GitHub sont signés avec une clé temporaire susceptible de changer. Pour effectuer des mises à jour sans désinstaller l'application, conserve **une clé release privée et persistante** : [procédure de signature](docs/SIGNING.md). La première migration debug vers release pourra demander une désinstallation.

## Ce que BundleTap prend en charge

| Format | Comportement |
| --- | --- |
| APK | Installation unique |
| APKM | Base APK et modules ABI, langue et densité reconnus |
| XAPK | Base déclarée dans manifest.json ou nommée d'après le paquet ; pas de données OBB |
| APKS | Variantes simples de split APK ; certaines variantes bundletool avancées ne sont pas interprétées |
| AAB | Non ; le bundle doit être compilé et signé préalablement |
| XAPK avec OBB | Refus explicite, pas d'installation partielle |

La sélection privilégie le CPU ARM64 et les ressources compatibles avec le Pixel, sans mélanger les variantes de densité de modules distincts. Les fichiers ZIP sont d'abord indexés, puis **seuls les APK sélectionnés** sont extraits. Des limites de taille et des contrôles CRC empêchent certaines archives corrompues ou abusives. Android vérifie les APK au moment de l'installation.

**Sécurité :** ces contrôles ne prouvent pas qu'un téléchargement est fiable. Ne choisis que des applications provenant de sources de confiance.

## Interface

Un écran unique sombre et de grandes zones tactiles : accueil, autorisation, préparation, confirmation Android, succès ou erreur. Après un échec, tu peux sélectionner un autre fichier ou réessayer. Les détails techniques restent repliés. Un ViewModel conserve les opérations de préparation en cas de rotation de l'écran.

## Développer et tester

Le dépôt est développé directement sur main. Prérequis : Android Studio compatible API 37, JDK 17 et Gradle 9.5.0. Build local :

    gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest

GitHub Actions exécute les tests unitaires, Android Lint, la compilation de l'application et **la compilation des tests d'interaction Compose**. Les tests Compose ne sont **pas exécutés** en CI faute d'émulateur/appareil Android 17. Le plan de [tests manuels sur Pixel](docs/TESTING.md) doit être effectué pour confirmer l'expérience réelle.

## Confidentialité

Aucun réseau ni collecte de données. Les fichiers sont choisis via le sélecteur Android et traités dans le cache privé de BundleTap. Permission REQUEST_INSTALL_PACKAGES uniquement pour lancer l'installateur Android, dont la confirmation ne peut être contournée.

Licence MIT.
