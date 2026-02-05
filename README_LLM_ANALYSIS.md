# Analyse LLM avec Gemini - ChatbotNaSoft

## 🎯 Objectif

Ce module implémente l'analyse dynamique des lignes anonymisées avec le LLM Gemini-1.5-Flash pour extraire la signification des champs et préparer les mappings pour le stockage MongoDB.

## 📁 Structure des fichiers créés

```
src/main/java/com/example/chatbotnasoft/
├── dto/
│   ├── FieldMapping.java                 # Mapping des champs par msg-type
│   └── LLMAnalysisResult.java          # Résultat complet de l'analyse LLM
├── config/
│   ├── GeminiProperties.java            # Configuration Gemini
│   └── LLMConfiguration.java          # Configuration RestTemplate et ObjectMapper
├── service/
│   └── LLMService.java               # Service principal d'analyse LLM
└── controller/
    └── LLMController.java            # API pour l'analyse LLM

src/test/java/com/example/chatbotnasoft/
└── LLMServiceTest.java               # Tests unitaires complets
```

## 🔧 Configuration Gemini

### application.yml
```yaml
gemini:
  api-key: AIzaSyDffpeEpLaHTsnPZlBiW5eXYrOw5DGyhxc
  model: gemini-1.5-flash
  base-url: https://generativelanguage.googleapis.com/v1beta
  timeout-seconds: 30
  max-retries: 3
  temperature: 0.1
  max-tokens: 1024
```

## 🤖 Processus d'Analyse LLM

1. **Sélection** : Uniquement les msg-types inconnus sont envoyés au LLM
2. **Analyse** : Gemini analyse dynamiquement chaque champ selon le msg-type
3. **Extraction** : Parsing du JSON réponse pour obtenir les significations
4. **Mapping** : Création des structures prêtes pour MongoDB
5. **Validation** : Vérification de la cohérence des résultats

## 📊 Objets créés

### FieldMapping
```java
public class FieldMapping {
    private String msgType;                    // Type de message
    private Map<String, String> mapping;       // Champ -> Signification
    private LocalDateTime analyzedAt;           // Timestamp d'analyse
    private String originalLine;                // Ligne originale
    private String anonymizedLine;              // Ligne anonymisée
    private int fieldCount;                    // Nombre de champs
}
```

### LLMAnalysisResult
```java
public class LLMAnalysisResult {
    private Map<String, List<FieldMapping>> resultsByMsgType;  // Résultats par msg-type
    private List<String> analysisErrors;                        // Erreurs d'analyse
    private int totalLinesAnalyzed;                            // Total lignes analysées
    private int successfulAnalyses;                               // Analyses réussies
    private int failedAnalyses;                                   // Analyses échouées
    private double successRate;                                    // Taux de succès
}
```

## 🚀 API REST

### Analyser un fichier complet
```bash
POST http://localhost:8080/api/llm/analyze-file/FEED_TEST_LLM.txt
```

**Réponse exemple :**
```json
{
  "success": true,
  "fileName": "FEED_TEST_LLM.txt",
  "totalLines": 5,
  "unknownMsgTypes": ["99", "88"],
  "knownMsgTypes": ["20"],
  "analyzedLines": 4,
  "successfulAnalyses": 4,
  "failedAnalyses": 0,
  "successRate": 100.0,
  "fieldMappings": {
    "99": [
      {
        "msgType": "99",
        "mapping": {
          "Champ 1": "Identifiant de transaction",
          "Champ 2": "Code de message",
          "Champ 3": "Date de traitement",
          "Champ 4": "Données sensibles 1",
          "Champ 5": "Données sensibles 2",
          "Champ 6": "Données sensibles 3"
        },
        "fieldCount": 6
      }
    ],
    "88": [
      {
        "msgType": "88", 
        "mapping": {
          "Champ 1": "Référence client",
          "Champ 2": "Type d'opération",
          "Champ 3": "Montant",
          "Champ 4": "Devise",
          "Champ 5": "Statut",
          "Champ 6": "Timestamp",
          "Champ 7": "Code validation"
        },
        "fieldCount": 7
      }
    ]
  }
}
```

### Analyser une ligne individuelle
```bash
POST http://localhost:8080/api/llm/analyze-line
?anonymizedLine=077;99;23012025;xxxxx;xxxxx;xxxxx&msgType=99
```

### Tester la connexion Gemini
```bash
GET http://localhost:8080/api/llm/test-connection
```

## 🧪 Tests

### Exécuter les tests unitaires
```bash
mvn test -Dtest=LLMServiceTest
```

### Test manuel rapide
```bash
# 1. Créer un fichier de test avec msg-types inconnus
echo -e "077;99;23012025;XXXX;YYYY;ZZZZ\n078;88;23012025;field3;field4" > input/feeds/FEED_TEST_LLM.txt

# 2. Analyser le fichier
curl -u user:<password> -X POST http://localhost:8080/api/llm/analyze-file/FEED_TEST_LLM.txt

# 3. Tester la connexion
curl -u user:<password] http://localhost:8080/api/llm/test-connection
```

## 📈 Logs générés

```bash
INFO  - 🤖 Début de l'analyse LLM pour 2 msg-types
INFO  - 🔍 Analyse du msg-type '99' avec 2 lignes
DEBUG - 🔍 Réponse Gemini brute: {"Champ 1": "Identifiant", "Champ 2": "Type", ...}
DEBUG - 📋 Mapping extrait: {Champ 1=Identifiant, Champ 2=Type, ...}
INFO  - ✅ Analyse terminée pour msg-type '99': 2 mappings créés
INFO  - 📊 Résumé de l'analyse LLM:
INFO  -    • Lignes totales analysées: 4
INFO  -    • Analyses réussies: 4
INFO  -    • Analyses échouées: 0
INFO  -    • Msg-types traités: 2
INFO  -    • Taux de succès: 100.0%
INFO  -    • Msg-type '99': 2 mappings
INFO  -    • Msg-type '88': 2 mappings
INFO  - 🧠 Analyse LLM terminée: 4 lignes analysées avec 100.0% de succès
INFO  - 💾 Prêt pour stockage MongoDB: 4 mappings créés
```

## 🔍 Prompt Gemini

Le prompt envoyé à Gemini pour chaque ligne :

```
Analyse cette ligne de feed anonymisée :
Ligne : 077;99;23012025;xxxxx;xxxxx;xxxxx

- Détecte dynamiquement la signification de chaque champ en fonction du msg-type
- Fournis le résultat sous **format JSON**, avec :
  {
    "Champ 1": "Signification",
    "Champ 2": "Signification", 
    "Champ 3": "Signification",
    ...
  }
- **Important** : Chaque ligne peut avoir un nombre différent de champs. Ne pas utiliser l'exemple comme modèle pour toutes les lignes.
```

## ⚡ Performance

- **Parallélisation** : Analyse simultanée des msg-types avec CompletableFuture
- **Mise en cache** : Même mapping appliqué à toutes les lignes du même msg-type
- **Gestion d'erreurs** : Retry automatique et gestion des timeouts
- **Validation JSON** : Parsing robuste avec extraction du JSON dans la réponse

## 🔄 Intégration Pipeline

Le système s'intègre parfaitement dans le pipeline existant :

1. **FileWatcherService** → Détection des fichiers
2. **FileStabilizationService** → Stabilisation (5 secondes)
3. **FileReadingService** → Lecture des lignes
4. **FeedParsingService** → Regroupement par msg-type
5. **FeedDetectionService** → Vérification MongoDB
6. **AnonymizationService** → Anonymisation si inconnu
7. **LLMService** → **Analyse dynamique des champs** ⭐
8. **Prochaines étapes** → Stockage MongoDB, Mapping final

## 📋 Prochaines étapes

Une fois les mappings LLM créés, les prochaines étapes incluront :
1. **Stockage MongoDB** : Persistance des FieldMapping dans la collection appropriée
2. **Validation métier** : Vérification des règles par msg-type
3. **Mapping final** : Transformation vers le format de production
4. **Monitoring** : Tableaux de bord et alertes
5. **Optimisation** : Cache des mappings pour les msg-types récurrents
