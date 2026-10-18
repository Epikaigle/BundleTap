# BundleTap · Android 12 à 17 (API 31 - 37)

[![Android APK](https://github.com/Epikaigle/BundleTap/actions/workflows/android.yml/badge.svg)](https://github.com/Epikaigle/BundleTap/actions/workflows/android.yml)

Installateur personnel universel d'APK, APKM, APKS, XAPK et APKX pour Android 12+ (compatible jusqu'à Android 17 / API 37). Application 100% Kotlin + Jetpack Compose Material 3, sans publicité, sans pistage, sans serveur, sans root et sans aucune connexion réseau.

## Nouveautés & Points forts

- **Icône réelle de l'application** : extraction et affichage automatique de l'icône originale de l'application avant et pendant l'installation.
- **Taille & métadonnées précises** : affichage formaté de la taille de l'archive (Ko, Mo, Go), du nom de package, du numéro de version et du nombre de splits configurés.
- **Glisser-Déposer (Drag & Drop)** : support complet du drag & drop depuis n'importe quel gestionnaire de fichiers en mode multi-fenêtres ou écran scindé (tablettes et pliables).
- **Compatibilité universelle** : compatible de **Android 12 (API 31)** jusqu'à **Android 17 (API 37)**.
- **Empreinte mémoire ultra-légère** : gestion optimisée de la RAM, mise à l'échelle automatique des icônes pour éviter les fuites de mémoire.
- **Clé de signature persistante** : toutes les releases officielles sont signées avec la même clé persistante afin de permettre les mises à jour sans désinstaller l'application.

## Formats pris en charge

| Format | Comportement |
| --- | --- |
| **APK** | Installation unique classique |
| **APKM** | Base APK et sélection intelligente des modules ABI (ARM64...), langue et densité d'écran |
| **XAPK / APKX** | Base déclarée dans manifest.json ou nommée d'après le paquet ; extraction optimisée |
| **APKS / BAPKS** | Variantes de split APK issues de bundletool |
| **ZIP** | Détection automatique des archives ZIP contenant des fichiers APK |
| **AAB** | Non pris en charge directement (doit être compilé en APK/APKS au préalable) |
| **XAPK avec OBB** | Refus explicite et sécurisé, pas d'installation tronquée sans assets OBB |

Les fichiers d'archive sont d'abord indexés, puis **seuls les APK pertinents** sont extraits et transmis au `PackageInstaller` natif d'Android via un streaming optimisé en flux direct.

## Installer BundleTap

1. Rends-toi sur la page des [Releases GitHub](https://github.com/Epikaigle/BundleTap/releases).
2. Télécharge l'APK officiel signé (`BundleTap-v1.3.0.apk`).
3. Ouvre l'APK sur ton appareil Android et autorise l'installation.
4. Lance BundleTap, choisis ou glisse ton fichier APK/APKM/APKS/XAPK et valide la confirmation système.

## Confidentialité & Sécurité

- **Zéro télémétrie, zéro réseau** : aucune permission INTERNET demandée dans le manifeste.
- **Cache privé sécurisé** : les fichiers temporaires sont traités dans l'espace sandboxé de l'application et immédiatement purgés après utilisation.
- **Intégrité Android native** : utilise l'API officielle `PackageInstaller` du système Android.

Licence MIT.
