# 🔒 Guide d'utilisation - Service d'Anonymisation FEED

## 🎯 Objectif

Le service `AnonymisationFeedService` anonymise les lignes de fichiers FEED selon des règles précises pour préparer les données à envoyer à Gemini LLM.

## 🏗️ Architecture

### Composants principaux

1. **ChampAnonymise** : DTO pour chaque champ avec détails d'anonymisation
2. **LigneAnonymisee** : DTO pour chaque ligne anonymisée
3. **AnonymisationResponse** : Réponse complète avec contenu anonymisé
4. **AnonymisationStatistiques** : Statistiques détaillées de l'anonymisation
5. **AnonymisationFeedService** : Service métier d'anonymisation
6. **AnonymisationFeedController** : API REST pour exposer le service
7. **TypeChamp** : Énumération des types de champs pour classification

### Flux de traitement

```
Fichier FEED → Extraction msgType → Analyse type champ → Anonymisation → Construction ligne → Contenu final pour Gemini
```

## 📋 Règles d'Anonymisation

### 🎯 Règle Principale

1. **Conserver exactement les 3 premiers champs** (Champ 1, 2, 3) sans aucune modification
2. **Anonymiser tous les autres champs** (Champ 4 et suivants) selon leur type détecté

### 📊 Types et Règles Spécifiques

| Type de Champ | Détection | Règle d'Anonymisation | Exemples |
|---------------|-------------|------------------------|-----------|
| **DATE** | Format YYYYMMDD | `YYYYMMDD` (conservé) | `20250613` → `20250613` |
| **HEURE** | Format HHMMSS | `HHMMSS` (conservé) | `062116` → `062116` |
| **IDENTIFIANT** | [A-Z0-9]{8,} | `ID_XXXXX` | `TN823JXM7T75` → `ID_TN8X75` |
| **NOMBRE** | Chiffres purs | `NUM_XXXX...` | `123456` → `NUM_XXXXXX` |
| **CODE** | Court texte/libre | `CODE_XX` | `ABC` → `CODE_ABC` |
| **VIDE** | Champ vide | Vide (conservé) | `` → `` |

### 🔍 Algorithmes de Détection

#### Date
```regex
\d{8} ET (commence par 20 ou 19)
```

#### Heure
```regex
\d{6} ET HH ≤ 23 ET MM ≤ 59 ET SS ≤ 59
```

#### Identifiant
```regex
[A-Z0-9]{8,}
```

#### Nombre
```regex
\d+
```

#### Code
```regex
^[A-Z0-9]{1,3}$
```

## 🚀 API REST

### 1. Anonymiser un fichier (POST)

```bash
POST /api/anonymiser-feed/anonymiser
Content-Type: application/json

{
  "filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"
}
```

**Réponse** :
```json
{
  "fileName": "FEED_V2-30-01-2026v4.txt",
  "lignesAnonymisees": [...],
  "contenuAnonymise": "061;05;20250613;062116;ID_TN8X75;NUM_XXXXXX;CODE_XX;...",
  "statistiques": {...},
  "success": true
}
```

### 2. Anonymiser un fichier (GET - Test navigateur)

```bash
GET /api/anonymiser-feed/anonymiser/FEED_V2-30-01-2026v4.txt
```

### 3. Lister les fichiers FEED disponibles

```bash
GET /api/anonymiser-feed/list-files
```

### 4. Vérifier le statut du service

```bash
GET /api/anonymiser-feed/status
```

## 📊 Format de Sortie

### LigneAnonymisee

```json
{
  "ligneOriginale": "061;05;20250613;062116;TN823JXM7T75;123456;ABC;0000",
  "ligneAnonymisee": "061;05;20250613;062116;ID_TN8X75;NUM_XXXXXX;CODE_ABC;0000",
  "msgType": "05",
  "totalChamps": 8,
  "anonymisationReussie": true,
  "champs": [
    {
      "nomChamp": "Champ 1",
      "valeurOriginale": "061",
      "valeurAnonymisee": "061",
      "typeChamp": "INCONNU",
      "regleAppliquee": "Conservé (3 premiers champs)"
    },
    {
      "nomChamp": "Champ 5",
      "valeurOriginale": "TN823JXM7T75",
      "valeurAnonymisee": "ID_TN8X75",
      "typeChamp": "IDENTIFIANT",
      "regleAppliquee": "Identifiant anonymisé (ID_XXXXX)"
    }
  ]
}
```

### AnonymisationResponse

```json
{
  "fileName": "FEED_TEST_04.txt",
  "lignesAnonymisees": [LigneAnonymisee1, LigneAnonymisee2, ...],
  "contenuAnonymise": "061;05;20250613;062116;ID_TN8X75;NUM_XXXXXX;CODE_XX;...",
  "statistiques": {
    "totalLignes": 10,
    "lignesAnonymisees": 10,
    "lignesEnErreur": 0,
    "totalChampsTraites": 85,
    "champsAnonymises": 45,
    "tauxSuccesAnonymisation": 100.0,
    "tauxChampsAnonymises": 52.9
  },
  "success": true
}
```

## 🧪 Tests

### Tests unitaires

Les tests sont dans `AnonymisationFeedServiceTest.java` :

```bash
mvn test -Dtest=AnonymisationFeedServiceTest
```

### Tests manuels

1. **Créer un fichier de test** :
```bash
echo "061;05;20250613;062116;TN823JXM7T75;123456;ABC;0000" > input/feeds/FEED_ANONYM_TEST.txt
```

2. **Tester l'anonymisation** :
```bash
curl -X GET "http://localhost:8080/api/anonymiser-feed/anonymiser/FEED_ANONYM_TEST.txt"
```

## 📝 Exemples d'utilisation

### Script bash pour traiter tous les fichiers

```bash
#!/bin/bash

# Lister les fichiers
FILES=$(curl -s "http://localhost:8080/api/anonymiser-feed/list-files" | jq -r '.files[]')

for file in $FILES; do
    echo "🔒 Anonymisation de: $file"
    
    # Anonymiser le fichier
    curl -X POST "http://localhost:8080/api/anonymiser-feed/anonymiser" \
         -H "Content-Type: application/json" \
         -d "{\"filePath\": \"input/feeds/$file\"}" \
         -o "output/${file}_anonymise.txt"
    
    echo "✅ Fichier anonymisé: output/${file}_anonymise.txt"
done
```

### Envoi à Gemini LLM

```bash
# Anonymiser et envoyer directement à Gemini
curl -X POST "http://localhost:8080/api/anonymiser-feed/anonymiser" \
     -H "Content-Type: application/json" \
     -d '{"filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"}' \
     | jq -r '.contenuAnonymise' \
     | curl -X POST "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=AIzaSyDffpeEpLaHTsnPZlBiW5eXYrOw5DGyhxc" \
          -H "Content-Type: application/json" \
          -d "{\"contents\":[{\"parts\":[{\"text\":\"Voici des données FEED anonymisées à analyser :\\n\\n\"}]}]}"
```

## 📈 Exemples de Transformation

### Avant/Après Anonymisation

| Ligne Originale | Ligne Anonymisée | Champs Modifiés |
|----------------|-------------------|-----------------|
| `061;05;20250613;062116;TN823JXM7T75;123456;ABC;0000` | `061;05;20250613;062116;ID_TN8X75;NUM_XXXXXX;CODE_ABC;0000` | Champs 5, 6, 7 |
| `145;A3;20250613;062116;REF123456789;987654;XYZ;999999` | `145;A3;20250613;062116;ID_REF1X89;NUM_XXXXXX;CODE_XYZ;999999` | Champs 5, 6, 7 |
| `023;16;20250613;000000;32;C;000000000;0000000000;0000000000` | `023;16;20250613;000000;32;C;NUM_XXXXXXXXX;NUM_XXXXXXXXXX;NUM_XXXXXXXXXX` | Champs 5, 7, 8, 9 |

## ⚠️ Points d'attention

1. **Structures variables** : Le service s'adapte à n'importe quel nombre de champs
2. **Conservation absolue** : Les 3 premiers champs ne sont JAMAIS modifiés
3. **Détection automatique** : Les types sont détectés par patterns regex
4. **Structure préservée** : Le nombre de champs et les séparateurs sont conservés
5. **Gestion d'erreurs** : Les lignes invalides sont incluses avec messages d'erreur
6. **Performance** : Traitement ligne par ligne pour gros fichiers
7. **Encodage** : Les fichiers doivent être en UTF-8

## 🔧 Configuration

La configuration se trouve dans `application.yml` :

```yaml
gemini:
  api-key: AIzaSyDffpeEpLaHTsnPZlBiW5eXYrOw5DGyhxc
  model: gemini-3-flash-preview
  base-url: https://generativelanguage.googleapis.com/v1beta
```

## 🎉 Cas d'usage

1. **Préparation pour LLM** : Anonymiser avant envoi à Gemini pour mapping
2. **Conformité RGPD** : Masquage automatique des données sensibles
3. **Tests automatisés** : Intégration dans les pipelines CI/CD
4. **Validation de données** : Vérification des formats avant traitement LLM
5. **Archivage sécurisé** : Stockage des données anonymisées pour traitement

---

**Le service d'anonymisation FEED est prêt à préparer vos fichiers pour Gemini LLM !** 🔒🚀

Utilisez l'API pour transformer vos fichiers FEED en données sécurisées et exploitables !
