# Anonymisation des Msg-Types Inconnus - ChatbotNaSoft

## 🎯 Objectif

Ce module implémente la détection des msg-types dans MongoDB et l'anonymisation automatique des lignes avec msg-types inconnus pour préparer les données avant l'envoi au LLM.

## 📁 Structure des fichiers créés

```
src/main/java/com/example/chatbotnasoft/
├── entity/
│   └── Feed.java                          # Entité MongoDB pour les msg-types
├── repository/
│   └── FeedRepository.java                  # Repository MongoDB pour les feeds
├── dto/
│   ├── AnonymizedLine.java                # Ligne anonymisée avec métadonnées
│   └── AnonymizationResult.java           # Résultat complet de l'anonymisation
├── service/
│   ├── FeedDetectionService.java           # Détection des msg-types dans MongoDB
│   └── AnonymizationService.java         # Service d'anonymisation
├── config/
│   └── ServiceConfiguration.java          # Configuration des dépendances
└── controller/
    └── AnonymizationController.java        # API pour l'anonymisation

src/test/java/com/example/chatbotnasoft/
└── AnonymizationServiceTest.java           # Tests unitaires complets
```

## 🔄 Processus d'Anonymisation

1. **Détection** : Vérification si le msg-type existe dans la collection `feed`
2. **Classification** : Séparation des msg-types connus/inconnus
3. **Anonymisation** : Remplacement des champs ≥3 par `xxxxx` pour les inconnus
4. **Conservation** : Les 2 premiers champs restent intacts
5. **Regroupement** : Préparation pour l'envoi au LLM

## 📊 Schéma MongoDB

### Collection : feed
```json
{
  "_id": "64a1b2c3d4e5f6789012345",
  "msgType": "20",
  "description": "Type 20 connu",
  "isActive": true,
  "createdAt": "2026-02-05T10:30:00Z",
  "updatedAt": "2026-02-05T10:30:00Z"
}
```

## 🔧 Objets créés

### AnonymizedLine
```java
public class AnonymizedLine {
    private String originalLine;        // Ligne brute originale
    private String anonymizedLine;      // Ligne après anonymisation
    private String msgType;            // Type de message
    private int lineNumber;            // Numéro de ligne
    private String sourceFileName;      // Fichier source
    private boolean wasAnonymized;    // Si la ligne a été anonymisée
    private LocalDateTime anonymizedAt; // Timestamp d'anonymisation
}
```

### AnonymizationResult
```java
public class AnonymizationResult {
    private Map<String, List<AnonymizedLine>> resultsByMsgType;  // Résultats par msg-type
    private List<String> unknownMsgTypes;                        // Msg-types inconnus
    private List<String> knownMsgTypes;                           // Msg-types connus
    private int totalLinesProcessed;                                // Total lignes traitées
    private int anonymizedLinesCount;                               // Lignes anonymisées
    private double anonymizationRate;                                 // Taux d'anonymisation
}
```

## 🚀 API REST

### Vérifier un msg-type
```bash
POST http://localhost:8080/api/anonymization/check-msg-type/20
```

**Réponse :**
```json
{
  "success": true,
  "msgType": "20",
  "isKnown": true,
  "requiresAnonymization": false,
  "timestamp": "2026-02-05T10:30:00Z"
}
```

### Anonymiser un fichier complet
```bash
POST http://localhost:8080/api/anonymization/anonymize-file/FEED_TEST_ANONYMIZATION.txt
```

**Réponse :**
```json
{
  "success": true,
  "fileName": "FEED_TEST_ANONYMIZATION.txt",
  "totalLines": 8,
  "anonymizedLines": 4,
  "nonAnonymizedLines": 4,
  "anonymizationRate": 50.0,
  "unknownMsgTypes": ["99"],
  "knownMsgTypes": ["20", "21"],
  "resultsByMsgType": {
    "20": [
      {
        "originalLine": "077;20;23012025;XXXX;YYYY;ZZZZ",
        "anonymizedLine": "077;20;23012025;XXXX;YYYY;ZZZZ",
        "wasAnonymized": false
      }
    ],
    "99": [
      {
        "originalLine": "079;99;23012025;DDDD;EEEE;FFFF",
        "anonymizedLine": "079;99;23012025;xxxxx;xxxxx;xxxxx",
        "wasAnonymized": true
      }
    ]
  }
}
```

### Créer un msg-type
```bash
POST http://localhost:8080/api/anonymization/create-feed-type
?msgType=99&description=Type 99 pour tests
```

### Lister les msg-types
```bash
GET http://localhost:8080/api/anonymization/list-feed-types
```

## 🧪 Tests

### Exécuter les tests unitaires
```bash
mvn test -Dtest=AnonymizationServiceTest
```

### Test manuel rapide
```bash
# 1. Créer des msg-types de test
curl -u user:<password> -X POST "http://localhost:8080/api/anonymization/create-feed-type?msgType=20&description=Type%2020"
curl -u user:<password> -X POST "http://localhost:8080/api/anonymization/create-feed-type?msgType=21&description=Type%2021"

# 2. Vérifier les msg-types
curl -u user:<password> http://localhost:8080/api/anonymization/list-feed-types

# 3. Anonymiser un fichier
curl -u user:<password> -X POST http://localhost:8080/api/anonymization/anonymize-file/FEED_TEST_ANONYMIZATION.txt
```

## 📈 Logs générés

```bash
INFO  - 🔍 Début de l'anonymisation pour 2 groupes
DEBUG - ✅ Msg-type '20' connu dans la base de données
INFO  - ❌ Msg-type '99' inconnu - anonymisation requise
INFO  - ✅ Msg-type '20' connu - pas d'anonymisation
INFO  - ❌ Msg-type '99' inconnu - anonymisation des champs
DEBUG - 🔒 Anonymisation - Ligne 3: '079;99;23012025;DDDD;EEEE;FFFF' -> '079;99;23012025;xxxxx;xxxxx;xxxxx'
INFO  - 🔒 2 lignes anonymisées pour le msg-type '99'
INFO  - 📊 Résumé de l'anonymisation:
INFO  -    • Lignes totales traitées: 4
INFO  -    • Msg-types inconnus: 1 ([99])
INFO  -    • Msg-types connus: 1 ([20])
INFO  -    • Msg-type inconnu '99': 2 lignes anonymisées
INFO  -    • Lignes anonymisées: 2
INFO  -    • Lignes non anonymisées: 2
INFO  -    • Taux d'anonymisation: 50.0%
INFO  - 🤖 Prêt pour envoi au LLM: 1 msg-types inconnus
```

## 🔍 Règles d'Anonymisation

### Format d'entrée
```
077;99;23012025;XXXX;YYYY;ZZZZ
```

### Format de sortie (si msg-type inconnu)
```
077;99;23012025;xxxxx;xxxxx;xxxxx
```

### Format de sortie (si msg-type connu)
```
077;99;23012025;XXXX;YYYY;ZZZZ  # Inchangé
```

## ⚡ Performance

- **Détection efficace** : Requête MongoDB indexée sur msg_type
- **Anonymisation dynamique** : Gère les lignes avec nombre de champs variable
- **Conservation mémoire** : Traitement streaming par groupe
- **Gestion d'erreurs** : Validation robuste du format

## 🔄 Intégration Pipeline

Le système s'intègre parfaitement dans le pipeline existant :

1. **FileWatcherService** → Détection des fichiers
2. **FileStabilizationService** → Stabilisation (5 secondes)
3. **FileReadingService** → Lecture des lignes
4. **FeedParsingService** → Regroupement par msg-type
5. **FeedDetectionService** → **Vérification MongoDB** ⭐
6. **AnonymizationService** → **Anonymisation si inconnu** ⭐
7. **Prochaines étapes** → Envoi LLM, Stockage MongoDB

## 📋 Prochaines étapes

Une fois les lignes anonymisées, les prochaines étapes incluront :
1. **Envoi au LLM** : Traitement des lignes anonymisées
2. **Stockage MongoDB** : Persistance des résultats
3. **Mapping métier** : Transformation vers le format final
4. **Validation finale** : Vérification de la cohérence
