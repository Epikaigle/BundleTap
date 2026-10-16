# BundleTap

## Interface 1.1 — optimisée pour Pixel 7 Pro

L'application utilise un écran unique et se réorganise selon la progression : accueil → permission Android (si nécessaire) → préparation → confirmation du système → succès ou erreur. Les erreurs disposent d'un texte compréhensible, d'une action pour recommencer et, si Android en fournit, de détails techniques repliables. Les boutons inutilisables sont masqués pendant l'installation.

L'interface est sombre, compatible avec le grossissement du texte, les zones tactiles Material d'au moins 48 dp et les annonces d'état TalkBack. Android conserve le contrôle de la confirmation de sécurité. Trois previews Compose (accueil, erreur, confirmation) sont disponibles dans Android Studio.

**Tests** : les tests unitaires sont exécutés par GitHub Actions ; les tests d'interaction Compose sont **compilés**, mais ne sont pas exécutés sans émulateur ou appareil Android. Un test sur le Pixel 7 Pro reste nécessaire avant de considérer l'expérience validée en conditions réelles.


Application native Kotlin + Jetpack Compose pour installer des APK et certains bundles sur **Pixel 7 Pro / Android 17 (API 37)**.

## Utilisation

1. Télécharge un fichier **.apk**, **.apkm**, **.apks** ou **.xapk** depuis une source fiable.
2. Ouvre BundleTap, touche **Choisir un fichier** et sélectionne ton téléchargement. Tu peux également choisir **Ouvrir avec BundleTap** quand ton gestionnaire de fichiers le propose.
3. Lors du premier usage, autorise BundleTap dans les paramètres **Installer des applications inconnues**.
4. Confirme l'installation dans l'interface de sécurité officielle d'Android.

Le fichier est traité localement sans connexion Internet ni root. Le moteur utilise PackageInstaller et gère les erreurs du système.

## Compatibilité et limites

- APK simple : pris en charge.
- APKM : pris en charge si l'archive contient un APK principal et des splits identifiables.
- APKS : variantes simples de bundletool seulement ; la lecture avancée du protocole toc.pb et les variantes conditionnelles ne sont pas implémentées.
- XAPK : archives sans données OBB seulement ; les archives avec fichier OBB sont rejetées.
- AAB : non pris en charge (nécessite une construction et une signature).
- Les sélections de split sont conservatrices : les archives complexes peuvent échouer sans installation. Un APK principal non ambigu est nécessaire.
- Les associations de fichiers dépendent du gestionnaire Android et des types MIME déclarés.
- Aucun installateur tiers ne doit contourner les confirmations imposées par Android.
- La sûreté du téléchargement n'est pas vérifiée : utilise uniquement des sources fiables.

## Télécharger l'application depuis GitHub

Dans **Actions → Android APK**, ouvre le dernier build réussi. Sous **Artifacts**, télécharge **BundleTap-debug-android17**, décompresse le ZIP puis installe **app-debug.apk** sur ton Pixel.

La signature debug créée sur le runner GitHub Actions peut varier d'un build à l'autre. Pour passer d'un build à l'autre, il pourra être nécessaire de désinstaller l'ancienne version (les données locales seront perdues). Un processus de signature release durable serait nécessaire pour obtenir des mises à jour transparentes.

## Compiler

Installe Android Studio compatible AGP 9.3, SDK Android 37, JDK 17, Gradle 9.5. Ouvre la racine de ce dépôt dans Android Studio.

Commande : gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest

Le workflow GitHub Actions lance automatiquement les tests unitaires + la compilation debug et celle des tests UI sur chaque push de main. Le Gradle Wrapper binaire n'est pas fourni : Android Studio ou Gradle 9.5 doivent être installés.

## Confidentialité

- Aucun accès réseau.
- Aucun accès global au stockage.
- Storage Access Framework pour choisir les fichiers.
- Permission REQUEST_INSTALL_PACKAGES uniquement pour demander l'installation Android.
- Les fichiers temporaires sont nettoyés après la session.

Licence MIT.
