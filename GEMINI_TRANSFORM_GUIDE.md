# Guide d'Utilisation - GeminiJsonToFeedMappingService

## 🎯 Objectif

Transformer les JSON bruts de Gemini en documents FeedMapping structurés avec le format exact requis.

## 🏗️ Architecture

### 1. **GeminiJsonToFeedMappingService**
- **Transformation dynamique** : Adapte le nombre de champs automatiquement
- **Remplacement intelligent** : "Donnée anonymisée X" → vraie valeur
- **Gestion des métadonnées** : Conserve _id, msgType, createdAt
- **Validation** : Vérifie la structure du JSON Gemini

### 2. **GeminiJsonTransformController**
- **API REST** complète pour la transformation
- **Support** : transformation simple, avec métadonnées, batch
- **Validation** : Vérification des JSON avant transformation

## 🚀 Utilisation

### 1. **Transformation simple (sans métadonnées)**
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform-simple" \
  -H "Content-Type: application/json" \
  -d '{
    "geminiJson": "{\"fields\": [\"Type\", \"Code\"], \"values\": [\"A3\", \"123\"]}"
  }'
```

### 2. **Transformation complète (avec métadonnées)**
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform" \
  -H "Content-Type: application/json" \
  -d '{
    "geminiJson": "{\"fields\": [\"Type de message\", \"Code de traitement\", \"Référence de transaction\", \"Montant\", \"Date de la transaction\", \"Identifiant du commerçant\"], \"values\": [\"16\", \"002\", \"ABC123\", \"1500\", \"2026-02-06\", \"M12345\"]}",
    "existingId": "6985ff1adc6f0aab18eece55",
    "msgType": "16",
    "createdAt": "2026-02-06T14:47:54.192+00:00"
  }'
```

### 3. **Transformation batch (plusieurs lignes)**
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform-batch" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonLines": [
      "{\"fields\": [\"Type\", \"Code\"], \"values\": [\"A3\", \"123\"]}",
      "{\"fields\": [\"Message\", \"Statut\"], \"values\": [\"INFO\", \"OK\"]}"
    ]
  }'
```

### 4. **Validation d'un JSON**
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "geminiJson": "{\"fields\": [\"Type\"], \"values\": [\"A3\"]}"
  }'
```

### 5. **Exemple interactif**
```bash
curl -X GET "http://localhost:8080/api/gemini-transform/example"
```

## 📊 Résultats attendus

### Entrée :
```json
{
  "fields": ["Type de message", "Code de traitement", "Référence de transaction", "Montant", "Date de la transaction", "Identifiant du commerçant"],
  "values": ["16", "002", "ABC123", "1500", "2026-02-06", "M12345"]
}
```

### Sortie (document complet) :
```json
{
  "success": true,
  "feedMapping": {
    "id": "6985ff1adc6f0aab18eece55",
    "msgType": "16",
    "mapping": {
      "Champ 1": "Type de message",
      "Champ 2": "Code de traitement",
      "Champ 3": "Référence de transaction",
      "Champ 4": "Montant",
      "Champ 5": "Date de la transaction",
      "Champ 6": "Identifiant du commerçant"
    },
    "createdAt": "2026-02-06T14:47:54.192"
  },
  "document": {
    "_id": "6985ff1adc6f0aab18eece55",
    "msgType": "16",
    "mapping": {
      "Champ 1": "Type de message",
      "Champ 2": "Code de traitement",
      "Champ 3": "Référence de transaction",
      "Champ 4": "Montant",
      "Champ 5": "Date de la transaction",
      "Champ 6": "Identifiant du commerçant"
    },
    "createdAt": "2026-02-06T14:47:54.192",
    "_class": "com.example.chatbotnasoft.entity.FeedMapping"
  }
}
```

## 🔧 Logique de transformation

### 1. **Analyse du JSON Gemini**
- Extraction des tableaux `fields` et `values`
- Validation de la structure
- Gestion des tailles différentes

### 2. **Création du mapping dynamique**
- Numérotation automatique : Champ 1, Champ 2, ...
- Remplacement des "Donnée anonymisée X" par les vraies valeurs
- Gestion des valeurs manquantes → "Valeur inconnue"

### 3. **Gestion des métadonnées**
- Conservation de `_id`, `msgType`, `createdAt`
- Génération automatique si non fournis
- Format de date flexible

### 4. **Validation**
- Vérification de la présence de `fields` et `values`
- Contrôle que ce sont des tableaux
- Messages d'erreur clairs

## 🧪 Tests

### Lancer les tests unitaires
```bash
mvn test -Dtest=GeminiJsonToFeedMappingServiceTest
```

### Tests disponibles
- Transformation avec données complètes
- Gestion des champs anonymisés
- Tailles différentes entre fields et values
- Transformation sans métadonnées
- Validation JSON
- Generation document complet
- Transformation batch

## 🎯 Cas d'usage typique

### 1. **Depuis LLMService**
```java
String geminiJson = llmService.analyzeLine(line);
FeedMapping mapping = transformService.transformGeminiJsonWithMetadata(
    geminiJson, existingId, msgType, createdAt
);
```

### 2. **Depuis API REST**
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform" \
  -H "Content-Type: application/json" \
  -d '{"geminiJson": "...", "existingId": "...", "msgType": "..."}'
```

### 3. **Pour traitement batch**
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform-batch" \
  -H "Content-Type: application/json" \
  -d '{"jsonLines": ["{...}", "{...}"]}'
```

## ⚠️ Points importants

1. **Dynamique** : S'adapte à n'importe quel nombre de champs
2. **Flexible** : Gère les métadonnées optionnelles
3. **Robuste** : Validation et gestion d'erreurs
4. **Compatible** : Respecte le format exact demandé
5. **Intelligent** : Remplace automatiquement les données anonymisées

---

**Le service est prêt à transformer tous vos JSON bruts Gemini !** 🚀
