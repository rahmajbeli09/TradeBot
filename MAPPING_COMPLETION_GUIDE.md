# Guide d'Utilisation - MappingCompletionService

## 🎯 Objectif

Transformer les mappings anonymisés avec "Donnée anonymisée X" en mappings complets avec les vraies significations des champs.

## 🏗️ Architecture

### 1. **MappingCompletionService**
- **Base de données interne** : Contient les significations réelles par msgType
- **Logique de transformation** : Remplace "Donnée anonymisée X" par la signification correspondante
- **Déduction automatique** : Si pas de mapping, déduit selon le contexte

### 2. **MappingCompletionController**
- **API REST** pour transformer les mappings
- **Support** : transformation individuelle, par msgType, ou globale
- **Gestion** : Ajout de nouveaux mappings réels

## 🚀 Utilisation

### 1. **Transformer un mapping spécifique**
```bash
curl -X POST "http://localhost:8080/api/mapping-completion/complete/6985fac872518e868ac39e43"
```

### 2. **Transformer tous les mappings d'un msgType**
```bash
curl -X POST "http://localhost:8080/api/mapping-completion/complete-by-msgtype/A3"
```

### 3. **Transformer depuis JSON (votre cas d'usage)**
```bash
curl -X POST "http://localhost:8080/api/mapping-completion/complete-from-json" \
  -H "Content-Type: application/json" \
  -d '{
    "_id": "6985fac872518e868ac39e43",
    "msgType": "A3",
    "mapping": {
      "Champ 1": "Numéro de séquence",
      "Champ 2": "Type de message (A3)",
      "Champ 3": "Donnée anonymisée 1",
      "Champ 4": "Donnée anonymisée 2"
    },
    "createdAt": "2026-02-06T14:29:28.490+00:00",
    "_class": "com.example.chatbotnasoft.entity.FeedMapping"
  }'
```

### 4. **Transformer tous les mappings anonymisés**
```bash
curl -X POST "http://localhost:8080/api/mapping-completion/complete-all"
```

### 5. **Voir les mappings réels disponibles**
```bash
curl -X GET "http://localhost:8080/api/mapping-completion/real-mappings/A3"
```

### 6. **Ajouter un nouveau mapping réel**
```bash
curl -X POST "http://localhost:8080/api/mapping-completion/add-real-mapping" \
  -H "Content-Type: application/json" \
  -d '{
    "msgType": "A3",
    "champKey": "Champ 5",
    "signification": "Montant de la transaction"
  }'
```

## 📊 Résultats attendus

### Entrée :
```json
{
  "_id": "6985fac872518e868ac39e43",
  "msgType": "A3",
  "mapping": {
    "Champ 1": "Numéro de séquence",
    "Champ 2": "Type de message (A3)",
    "Champ 3": "Donnée anonymisée 1",
    "Champ 4": "Donnée anonymisée 2"
  },
  "createdAt": "2026-02-06T14:29:28.490+00:00",
  "_class": "com.example.chatbotnasoft.entity.FeedMapping"
}
```

### Sortie :
```json
{
  "success": true,
  "originalMapping": { ... },
  "completedMapping": {
    "_id": "6985fac872518e868ac39e43",
    "msgType": "A3",
    "mapping": {
      "Champ 1": "Numéro de séquence",
      "Champ 2": "Type de message (A3)",
      "Champ 3": "Identifiant unique de l'opération",
      "Champ 4": "Identifiant de l'entité source (ex: compte, utilisateur)"
    },
    "createdAt": "2026-02-06T14:29:28.490+00:00",
    "_class": "com.example.chatbotnasoft.entity.FeedMapping"
  }
}
```

## 🗄️ Base de données interne

### Mappings pré-configurés :

#### **msgType A3**
- Champ 3 → "Identifiant unique de l'opération"
- Champ 4 → "Identifiant de l'entité source (ex: compte, utilisateur)"
- Champ 5 → "Montant de la transaction"
- Champ 6 → "Devise de la transaction"
- Champ 7 → "Date et heure de l'opération"

#### **msgType 05**
- Champ 3 → "Code d'identification du message"
- Champ 4 → "Référence de la transaction"
- Champ 5 → "Statut du traitement"
- Champ 6 → "Timestamp de création"

#### **msgType 10**
- Champ 3 → "Identifiant du client"
- Champ 4 → "Type d'opération effectuée"
- Champ 5 → "Résultat de l'opération"

## 🔧 Logique de transformation

### 1. **Détection**
- Recherche du pattern "Donnée anonymisée X"
- Extraction du numéro X

### 2. **Recherche**
- Dans la base interne pour le msgType donné
- Correspondance exacte champKey → signification

### 3. **Déduction**
- Si pas trouvé, utilise la logique de déduction
- Basée sur le msgType et le numéro d'anonymisation

### 4. **Fallback**
- Si tout échoue → "Signification manquante"

## 🧪 Tests

### Lancer les tests unitaires
```bash
mvn test -Dtest=MappingCompletionServiceTest
```

### Tests disponibles
- Transformation avec données anonymisées
- Conservation des données explicites
- Gestion des msgType inconnus
- Ajout de nouveaux mappings

## 🎯 Cas d'usage typique

1. **Récupération** d'un mapping depuis MongoDB
2. **Détection** des "Donnée anonymisée X"
3. **Transformation** via l'API
4. **Stockage** ou affichage du mapping complété

---

**Le service est prêt à transformer tous vos mappings anonymisés !** 🚀
