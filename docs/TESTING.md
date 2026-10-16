---
layout: default
title: Tests sur Pixel 7 Pro
---

# Validation réelle sur Pixel 7 Pro / Android 17

Ces tests sont à effectuer sur le téléphone ; une compilation réussie ne prouve pas l'installation de toutes les archives.

1. Premier lancement : pas de flash blanc, prise en compte des barres système, bouton de sélection visible.
2. Permissions : accès à Install unknown apps, retour sans permission, puis autorisation et reprise du fichier.
3. APK simple : ouverture depuis BundleTap et confirmation Android.
4. APKM multi-splits : archives avec ARM64, arm32, plusieurs langues et densités.
5. XAPK : base APK nommée d'après le package et désignée par manifest.json.
6. APKS : archive simple avec base + splits, et rejet explicite des variantes ambiguës.
7. ZIP invalide ou corrompu : message d'erreur lisible et possibilité de recommencer.
8. XAPK contenant des données OBB : refus sans installation partielle.
9. Rotation de l'écran pendant une longue préparation : progression conservée.
10. Refus de la confirmation Android : résultat exploitable et nouvelle tentative possible.
11. Agrandissement des polices et TalkBack : boutons et statuts lisibles et annoncés.
12. Ouvrir avec / Partager depuis le gestionnaire de fichiers, selon les MIME disponibles.
13. Reprise après arrêt forcé : comportement clair, sans écran bloqué indéfiniment.
14. Mise à jour release signée : installation d'un versionCode supérieur sans suppression de l'application.

Une application ordinaire sans root ne peut pas contourner la confirmation système d'installation.
