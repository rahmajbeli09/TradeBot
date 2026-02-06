# 📖 Guide d'utilisation - Service de Lecture Lisible FEED

## 🎯 Objectif

Le service `FeedReadableService` permet de générer des représentations lisibles des fichiers FEED en utilisant les mappings existants dans MongoDB.

## 🏗️ Architecture

### Composants principaux

1. **FeedReadableService** : Service métier pour la lecture lisible
2. **FeedReadableController** : API REST pour exposer le service
3. **ReadableFeedLine** : DTO pour représenter une ligne lisible

### Flux de traitement

```
Fichier FEED → Extraction msgType → Recherche mapping → Génération champs lisibles
```

## 📋 Prérequis

1. **Mappings existants** dans la collection `feedMapping` de MongoDB
2. **Fichiers FEED** dans le répertoire `input/feeds/`
3. **Structure des mappings** :
   ```json
   {
     "msgType": "05",
     "mapping": {
       "Champ 1": "Type d'enregistrement",
       "Champ 2": "Code de statut",
       "Champ 3": "Identifiant principal",
       ...
     }
   }
   ```

## 🚀 API REST

### 1. Lister les fichiers FEED disponibles

```bash
GET /api/feed-readable/list-files
```

**Réponse** :
```json
{
  "success": true,
  "totalFiles": 3,
  "files": ["FEED_V2-30-01-2026v4.txt", "FEED_V2-30-01-2026v6.txt", "FEED_TEST_04.txt"]
}
```

### 2. Générer une représentation lisible (POST)

```bash
POST /api/feed-readable/generate
Content-Type: application/json

{
  "filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"
}
```

**Réponse** :
```json
{
  "success": true,
  "filePath": "input/feeds/FEED_V2-30-01-2026v4.txt",
  "totalLines": 1,
  "linesWithMapping": 1,
  "linesWithoutMapping": 0,
  "readableLines": [
    {
      "msgType": "04",
      "ligneOriginale": "085;04;20250613;062116;TNZQNUDKX2Y3;0;0000;000000000;0000000000;0000000000;000000000;0000",
      "mappingTrouve": true,
      "champsLisibles": {
        "Champ 1": "Type d'enregistrement : 085",
        "Champ 2": "Code de statut : 04",
        "Champ 3": "Identifiant principal : 20250613",
        "Champ 4": "Identifiant secondaire : 062116",
        "Champ 5": "Référence opération : TNZQNUDKX2Y3",
        "Champ 6": "Valeur numérique 1 : 0",
        "Champ 7": "Inconnu : 0000",
        "Champ 8": "Inconnu : 000000000",
        "Champ 9": "Inconnu : 0000000000",
        "Champ 10": "Inconnu : 0000000000",
        "Champ 11": "Inconnu : 000000000",
        "Champ 12": "Inconnu : 0000"
      }
    }
  ]
}
```

### 3. Générer une représentation lisible (GET - Test navigateur)

```bash
GET /api/feed-readable/generate/FEED_V2-30-01-2026v4.txt
```

### 4. Générer un résumé textuel

```bash
POST /api/feed-readable/generate-text-summary
Content-Type: application/json

{
  "filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"
}
```

**Réponse** :
```json
{
  "success": true,
  "filePath": "input/feeds/FEED_V2-30-01-2026v4.txt",
  "totalLines": 1,
  "linesWithMapping": 1,
  "linesWithoutMapping": 0,
  "textSummary": "📊 RÉSUMÉ LISIBLE DU FICHIER FEED\n=====================================\n\n🔹 Ligne 1 (MsgType: 04)\n   Champ 1: Type d'enregistrement : 085\n   Champ 2: Code de statut : 04\n   ...\n\n📈 STATISTIQUES\n==============\n• Lignes totales: 1\n• Lignes avec mapping: 1\n• Lignes sans mapping: 0\n• Taux de succès: 100.0%\n\n🏷️ MSG-TYPES TRAITÉS\n=====================\n• 04: 1 ligne(s)\n"
}
```

### 5. Vérifier le statut du service

```bash
GET /api/feed-readable/status
```

## 🧪 Tests

### Tests unitaires

Les tests sont dans `FeedReadableServiceTest.java` :

```bash
mvn test -Dtest=FeedReadableServiceTest
```

### Tests manuels

1. **Créer un mapping de test** :
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform-complete" \
  -H "Content-Type: application/json" \
  -d '{
    "geminiJson": "{\"fields\": [\"Type\", \"Code\", \"ID\", \"Heure\"], \"values\": [\"085\", \"04\", \"20250613\", \"062116\"]}",
    "msgType": "04"
  }'
```

2. **Tester la lecture lisible** :
```bash
curl -X GET "http://localhost:8080/api/feed-readable/generate/FEED_TEST_04.txt"
```

## 📊 Format de sortie

### ReadableFeedLine

```json
{
  "msgType": "04",
  "ligneOriginale": "085;04;20250613;062116;TNZQNUDKX2Y3;0;0000;000000000;0000000000;0000000000;000000000;0000",
  "mappingTrouve": true,
  "champsLisibles": {
    "Champ 1": "Type d'enregistrement : 085",
    "Champ 2": "Code de statut : 04",
    "Champ 3": "Identifiant principal : 20250613"
  },
  "erreur": null
}
```

### Cas d'erreur

```json
{
  "msgType": "A3",
  "ligneOriginale": "145;A3;20250613;062116;TN823JXM7T75;C",
  "mappingTrouve": false,
  "champsLisibles": null,
  "erreur": "Aucun mapping trouvé pour le msgType: A3"
}
```

## ⚠️ Points d'attention

1. **Structures variables** : Chaque msgType peut avoir un nombre différent de champs
2. **Mappings incomplets** : Les champs sans mapping sont marqués "Inconnu"
3. **Performance** : Le service lit les fichiers ligne par ligne (adapté aux gros fichiers)
4. **Encodage** : Les fichiers doivent être en UTF-8

## 🔧 Configuration

La configuration se trouve dans `application.yml` :

```yaml
gemini:
  api-key: AIzaSyDffpeEpLaHTsnPZlBiW5eXYrOw5DGyhxc
  model: gemini-3-flash-preview
  base-url: https://generativelanguage.googleapis.com/v1beta
```

## 🎉 Cas d'usage

1. **Audit de fichiers FEED** : Comprendre le contenu des fichiers bruts
2. **Validation de mappings** : Vérifier que tous les msgTypes ont des mappings
3. **Documentation** : Générer de la documentation lisible pour les équipes métier
4. **Débogage** : Identifier rapidement les problèmes de structure des données

## 📝 Exemples d'utilisation

### Script bash pour traiter tous les fichiers

```bash
#!/bin/bash

# Lister les fichiers
FILES=$(curl -s "http://localhost:8080/api/feed-readable/list-files" | jq -r '.files[]')

for file in $FILES; do
    echo "📖 Traitement de: $file"
    
    # Générer le résumé lisible
    curl -X POST "http://localhost:8080/api/feed-readable/generate-text-summary" \
         -H "Content-Type: application/json" \
         -d "{\"filePath\": \"input/feeds/$file\"}" \
         | jq -r '.textSummary' > "output/${file}_readable.txt"
    
    echo "✅ Fichier généré: output/${file}_readable.txt"
done
```

### Analyse avec curl

```bash
# Analyser un fichier spécifique
curl -X POST "http://localhost:8080/api/feed-readable/generate" \
     -H "Content-Type: application/json" \
     -d '{"filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"}' \
     | jq '.readableLines[] | select(.mappingTrouve) | .champsLisibles'
```

---

**Le service est prêt à transformer vos fichiers FEED en représentations lisibles !** 🚀
