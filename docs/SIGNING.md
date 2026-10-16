---
layout: default
title: Signature durable
---

# Signer BundleTap durablement

Le build debug de GitHub Actions change potentiellement de clé à chaque exécution. Pour mettre à jour BundleTap sans devoir le désinstaller, configure une **clé privée release persistante**. Aucun secret n'est stocké dans ce dépôt public.

## Préparation sur un ordinateur de confiance

Génère un keystore avec JDK 17 :

    keytool -genkeypair -keystore bundletap-release.jks -alias bundletap -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12

Garde le keystore et ses mots de passe en lieu sûr **hors de GitHub et des dossiers synchronisés publics**. Ne partage pas son contenu.

Encode le keystore sans saut de ligne, par exemple sous Linux :

    base64 -w 0 bundletap-release.jks

Sur macOS :

    base64 < bundletap-release.jks | tr -d '\n'

Dans GitHub → **Settings → Secrets and variables → Actions**, configure quatre secrets :

| Secret | Contenu |
| --- | --- |
| BUNDLETAP_KEYSTORE_B64 | Chaîne Base64 du keystore |
| BUNDLETAP_STORE_PASSWORD | Mot de passe du keystore |
| BUNDLETAP_KEY_ALIAS | Alias (bundletap dans l'exemple) |
| BUNDLETAP_KEY_PASSWORD | Mot de passe de la clé |

Relance **Actions → Android APK** sur main. L'artefact **BundleTap-signed-release-Android17** est ajouté lorsqu'une signature complète est configurée.

Utilise désormais toujours cette version release pour les mises à jour et incrémente le versionCode à chaque publication. Si BundleTap debug est installé, une désinstallation initiale avant le passage au release signé pourra être nécessaire ; les clés sont différentes.

Ne publie jamais le keystore, les mots de passe ou leur représentation Base64 dans le dépôt, ses logs, des tickets ou des discussions publiques. Perdre la clé privée empêche les mises à jour de l'application signée avec cette clé.
