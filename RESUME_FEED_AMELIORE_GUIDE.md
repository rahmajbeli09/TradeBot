# 📊 Guide d'utilisation - Service de Résumé Amélioré FEED

## 🎯 Objectif

Le service `ResumeFeedAmelioreService` fournit une version améliorée et complète de l'analyse des fichiers FEED avec des DTO structurés détaillés et un résumé global exhaustif.

## 🏗️ Architecture Améliorée

### Composants principaux

1. **ChampDetail** : DTO détaillé pour chaque champ avec anonymisation
2. **ResumeDataAmeliore** : DTO amélioré avec statistiques de complétude
3. **ResumeResponseAmeliore** : Réponse complète avec données améliorées
4. **ResumeStatistiquesAmeliorees** : Statistiques détaillées et avancées
5. **ResumeFeedAmelioreService** : Service métier amélioré
6. **ResumeFeedAmelioreController** : API REST améliorée

### Flux de traitement amélioré

```
Fichier FEED → Extraction msgType → Recherche mapping → Création DTO détaillé → Anonymisation → Résumé complet
```

## 📋 Structure des DTO Améliorés

### ChampDetail

```json
{
  "champ": "Champ 1",
  "signification": "Type d'enregistrement",
  "valeur": "061",
  "valeurAnonymisee": "061",
  "sensible": false
}
```

### ResumeDataAmeliore

```json
{
  "msgType": "05",
  "mappingComplet": [
    {
      "champ": "Champ 1",
      "signification": "Type d'enregistrement",
      "valeur": "061",
      "valeurAnonymisee": "061"
    },
    {
      "champ": "Champ 5",
      "signification": "Référence opération",
      "valeur": "TN823JXM7T75",
      "valeurAnonymisee": "TN8***75"
    }
  ],
  "valeurs": {"Champ 1": "061", "Champ 2": "05", ...},
  "valeursAnonymisees": {"Champ 1": "061", "Champ 5": "TN8***75", ...},
  "ligneOriginale": "061;05;20250613;062116;TN823JXM7T75;A",
  "mappingTrouve": true,
  "totalChamps": 6,
  "champsAvecSignification": 6,
  "tauxCompletion": 100.0
}
```

### ResumeResponseAmeliore

```json
{
  "fileName": "FEED_TEST_04.txt",
  "resumeDataAmeliore": [ResumeDataAmeliore1, ResumeDataAmeliore2, ...],
  "resumeTexteAmeliore": "📊 RÉSUMÉ AMÉLIORÉ DU FICHIER FEED...",
  "statistiques": {
    "totalLignes": 15,
    "lignesAvecMappingComplet": 12,
    "lignesAvecMappingPartiel": 2,
    "lignesSansMapping": 1,
    "tauxSuccesGlobal": 93.3,
    "tauxMappingComplet": 80.0,
    "totalChampsDifferents": 12,
    "moyenneChampsParMsgType": {"05": 6.5, "A3": 8.2},
    "tauxCompletionParMsgType": {"05": 95.5, "A3": 87.3}
  },
  "success": true
}
```

## 🚀 API REST Améliorée

### 1. Générer un résumé amélioré (POST)

```bash
POST /api/resume-feed-ameliore/generate
Content-Type: application/json

{
  "filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"
}
```

**Réponse** :
```json
{
  "fileName": "FEED_V2-30-01-2026v4.txt",
  "resumeDataAmeliore": [...],
  "resumeTexteAmeliore": "📊 RÉSUMÉ AMÉLIORÉ...",
  "statistiques": {...},
  "success": true
}
```

### 2. Générer un résumé amélioré (GET - Test navigateur)

```bash
GET /api/resume-feed-ameliore/generate/FEED_V2-30-01-2026v4.txt
```

### 3. Lister les fichiers FEED disponibles

```bash
GET /api/resume-feed-ameliore/list-files
```

### 4. Vérifier le statut du service amélioré

```bash
GET /api/resume-feed-ameliore/status
```

## 📊 Anonymisation Intelligente

### Règles d'anonymisation

1. **Champ 1 (Type)** : Conservé tel quel
2. **Champ 2 (Code)** : Conservé tel quel
3. **Champ 3 (Date)** : `20250613` → `2025**/**`
4. **Champ 4 (Heure)** : `062116` → `06***`
5. **Champ 5 (Référence)** : `TN823JXM7T75` → `TN8***75`
6. **Références longues** : `ABC123DEF456` → `ABC***456`
7. **Numéros longs** : `1234567890` → `***7890`
8. **Codes alphabétiques** : `ABCDEF` → `A**`

### Détection automatique de sensibilité

- Références uniques (8+ caractères alphanumériques)
- Noms propres (majuscules + lettres)
- Numéros de téléphone/identifiants (8+ chiffres)
- Mots-clés sensibles (ID, PASS, SECRET, KEY)

## 📈 Résumé Textuel Amélioré

Le résumé généré comprend :

### 📊 Statistiques Détaillées
- Lignes totales, avec mapping complet/partiel/sans mapping
- Taux de succès global et taux de mapping complet
- Types de messages différents et total de champs différents

### 🏷️ Répartition Détaillée par MsgType
- Nombre de lignes par msgType
- Moyenne de champs par msgType
- Taux de complétude par msgType

### 🔢 Champs les Plus Fréquents
- Top 10 des champs les plus utilisés
- Pourcentage d'utilisation de chaque champ

### 🔍 Détails Complets par MsgType
- TOUS les champs affichés avec nom/signification/valeur
- Indicateur de sensibilité `[*]` pour les valeurs anonymisées
- Aperçu des 5 premières lignes avec tous les champs

## 🧪 Tests

### Tests unitaires

Les tests sont dans `ResumeFeedAmelioreServiceTest.java` :

```bash
mvn test -Dtest=ResumeFeedAmelioreServiceTest
```

### Tests manuels

1. **Créer des mappings de test** :
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform-complete" \
  -H "Content-Type: application/json" \
  -d '{
    "geminiJson": "{\"fields\": [\"Type\", \"Code\", \"ID\", \"Heure\", \"Reference\"], \"values\": [\"061\", \"05\", \"20250613\", \"062116\", \"TN823JXM7T75\"]}",
    "msgType": "05"
  }'
```

2. **Tester le résumé amélioré** :
```bash
curl -X GET "http://localhost:8080/api/resume-feed-ameliore/generate/FEED_TEST_04.txt"
```

## 📝 Exemples d'utilisation

### Script bash pour traiter tous les fichiers

```bash
#!/bin/bash

# Lister les fichiers
FILES=$(curl -s "http://localhost:8080/api/resume-feed-ameliore/list-files" | jq -r '.files[]')

for file in $FILES; do
    echo "📊 Génération résumé amélioré pour: $file"
    
    # Générer le résumé complet
    curl -X POST "http://localhost:8080/api/resume-feed-ameliore/generate" \
         -H "Content-Type: application/json" \
         -d "{\"filePath\": \"input/feeds/$file\"}" \
         -o "output/${file}_resume_ameliore.json"
    
    echo "✅ Résumé amélioré généré: output/${file}_resume_ameliore.json"
done
```

### Analyse avec curl

```bash
# Analyser un fichier spécifique
curl -X POST "http://localhost:8080/api/resume-feed-ameliore/generate" \
     -H "Content-Type: application/json" \
     -d '{"filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"}' \
     | jq '.resumeTexteAmeliore'
```

## 📈 Exemple de résumé textuel amélioré généré

```
📊 RÉSUMÉ AMÉLIORÉ DU FICHIER FEED
=====================================
Fichier: FEED_V2-30-01-2026v4.txt
Généré le: Fri Feb 06 18:30:00 CET 2026

📈 STATISTIQUES DÉTAILLÉES
============================
• Lignes totales traitées: 15
• Lignes avec mapping complet (≥90%): 12
• Lignes avec mapping partiel: 2
• Lignes sans mapping: 1
• Taux de succès global: 93.3%
• Taux de mapping complet: 80.0%
• Types de messages différents: 3
• Total de champs différents: 12

🏷️ RÉPARTITION DÉTAILLÉE PAR MSG-TYPE
=====================================
📋 MsgType: 05
   • Nombre de lignes: 8
   • Moyenne de champs: 6.5
   • Taux de complétude: 95.5%

📋 MsgType: A3
   • Nombre de lignes: 5
   • Moyenne de champs: 8.2
   • Taux de complétude: 87.3%

🔢 CHAMPS LES PLUS FRÉQUENTS
=============================
1. Champ 1: 15 occurrences (100.0%)
2. Champ 2: 15 occurrences (100.0%)
3. Champ 3: 15 occurrences (100.0%)
4. Champ 4: 15 occurrences (100.0%)
5. Champ 5: 15 occurrences (100.0%)

🔍 DÉTAILS COMPLETS PAR MSG-TYPE
=================================
📋 MsgType: 05 (8 lignes)
   1. [6 champs] Champ 1(Type d'enregistrement):061 | Champ 2(Code de statut):05 | Champ 3(Identifiant principal):2025**/** | Champ 4(Identifiant secondaire):06*** | Champ 5(Référence opération):TN8***75 | Champ 6(Valeur numérique 1):A
   2. [6 champs] Champ 1(Type d'enregistrement):145 | Champ 2(Code de statut):05 | Champ 3(Identifiant principal):2025**/** | Champ 4(Identifiant secondaire):06*** | Champ 5(Référence opération):TN8***75 | Champ 6(Valeur numérique 1):C
   3. [7 champs] Champ 1(Type d'enregistrement):023 | Champ 2(Code de statut):05 | Champ 3(Identifiant principal):2025**/** | Champ 4(Identifiant secondaire):*** | Champ 5(Référence opération):32 | Champ 6(Valeur numérique 1):C | Champ 7(Inconnu):000000
   ... et 5 autres lignes similaires

🎯 Résumé amélioré généré avec succès
```

## ⚠️ Points d'attention

1. **Structures variables** : Chaque msgType peut avoir un nombre différent de champs
2. **Anonymisation intelligente** : Les valeurs sensibles sont automatiquement masquées
3. **Complétude variable** : Les mappings peuvent être partiels (taux de complétude < 100%)
4. **Performance** : Le service traite les fichiers ligne par ligne (adapté aux gros fichiers)
5. **Encodage** : Les fichiers doivent être en UTF-8
6. **Gestion d'erreurs** : Les lignes invalides sont incluses dans le résumé avec messages d'erreur

## 🔧 Configuration

La configuration se trouve dans `application.yml` :

```yaml
gemini:
  api-key: AIzaSyDffpeEpLaHTsnPZlBiW5eXYrOw5DGyhxc
  model: gemini-3-flash-preview
  base-url: https://generativelanguage.googleapis.com/v1beta
```

## 🎉 Cas d'usage améliorés

1. **Audit complet** : Vue d'ensemble structurée avec anonymisation
2. **Validation de données** : Identification des problèmes de complétude
3. **Reporting avancé** : Statistiques détaillées pour les équipes
4. **Analyse comparative** : Comparaison entre msgTypes avec métriques avancées
5. **Débogage intelligent** : Identification précise avec valeurs anonymisées
6. **Conformité RGPD** : Anonymisation automatique des données sensibles

---

**Le service de résumé amélioré est prêt à transformer vos fichiers FEED en données exploitables et sécurisées !** 🚀
