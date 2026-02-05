# Parsing et Regroupement par Msg-Type - ChatbotNaSoft

## 🎯 Objectif

Ce module implémente l'extraction du msg-type (2ème champ) et le regroupement des lignes par msg-type pour préparer les données aux traitements suivants.

## 📁 Structure des fichiers créés

```
src/main/java/com/example/chatbotnasoft/
├── dto/
│   ├── ParsedFeedGroup.java           # Représente un groupe de lignes par msg-type
│   └── ParsingResult.java            # Résultat complet du parsing
├── service/
│   └── FeedParsingService.java        # Service principal de parsing
└── controller/
    └── ParsingController.java         # API pour le parsing

src/test/java/com/example/chatbotnasoft/
└── FeedParsingServiceTest.java        # Tests unitaires complets
```

## 🔄 Processus de Parsing

1. **Extraction** : Découpage de chaque ligne sur le séparateur `;`
2. **Validation** : Vérification qu'il y a au moins 2 champs
3. **Extraction msg-type** : Récupération du 2ème champ (index 1)
4. **Regroupement** : Création de groupes par msg-type
5. **Gestion des erreurs** : Collecte des lignes mal formées

## 📊 Objets créés

### ParsedFeedGroup
```java
public class ParsedFeedGroup {
    private String msgType;           // Type de message
    private List<RawFeedLine> lines;  // Lignes du groupe
    private LocalDateTime parsedAt;   // Timestamp de parsing
    private int totalLines;           // Nombre de lignes
    private String sourceFileName;    // Fichier source
}
```

### ParsingResult
```java
public class ParsingResult {
    private Map<String, ParsedFeedGroup> groupsByMsgType;  // Groupes par msg-type
    private List<String> parsingErrors;                    // Erreurs de parsing
    private int totalLinesProcessed;                        // Total lignes traitées
    private int validLinesProcessed;                        // Lignes valides
    private int errorLinesCount;                            // Nombre d'erreurs
    private double successRate;                             // Taux de succès
}
```

## 🚀 API REST

### Parser un fichier complet
```bash
POST http://localhost:8080/api/parsing/parse-file/FEED_TEST_PARSING.txt
```

**Réponse exemple :**
```json
{
  "success": true,
  "fileName": "FEED_TEST_PARSING.txt",
  "totalLines": 14,
  "validLines": 14,
  "errorLines": 0,
  "groupCount": 5,
  "successRate": 100.0,
  "groups": {
    "20": {
      "msgType": "20",
      "totalLines": 6,
      "sourceFileName": "FEED_TEST_PARSING.txt"
    },
    "21": {
      "msgType": "21", 
      "totalLines": 3,
      "sourceFileName": "FEED_TEST_PARSING.txt"
    }
  }
}
```

### Valider une ligne individuelle
```bash
GET http://localhost:8080/api/parsing/validate-line?content=077;20;23012025;XXXX;YYYY;ZZZZ
```

## 🧪 Tests

### Exécuter les tests unitaires
```bash
mvn test -Dtest=FeedParsingServiceTest
```

### Test manuel rapide
```bash
# 1. Créer un fichier de test
echo -e "077;20;23012025;XXXX;YYYY;ZZZZ\n078;21;23012025;AAAA;BBBB;CCCC\n079;20;23012025;DDDD;EEEE;FFFF" > input/feeds/FEED_TEST.txt

# 2. Parser le fichier
curl -u user:<password> -X POST http://localhost:8080/api/parsing/parse-file/FEED_TEST.txt

# 3. Valider une ligne
curl -u user:<password> "http://localhost:8080/api/parsing/validate-line?content=077;20;23012025;XXXX;YYYY;ZZZZ"
```

## 📈 Logs générés

```bash
INFO  - 🔍 Début du parsing et regroupement de 14 lignes
DEBUG - 🔧 Extraction msg-type - Ligne 1: '077;20;23012025;XXXX;YYYY;ZZZZ' -> '20'
INFO  - 📋 Création d'un nouveau groupe pour msg-type: '20'
DEBUG - ✅ Ligne 1 ajoutée au groupe '20'
INFO  - 📊 Résumé du parsing:
INFO  -    • Lignes totales traitées: 14
INFO  -    • Lignes valides: 14
INFO  -    • Erreurs de parsing: 0
INFO  -    • Groupes créés: 5
INFO  -    • Groupe '20': 6 lignes
INFO  -    • Groupe '21': 3 lignes
INFO  -    • Groupe '22': 1 lignes
```

## 🔍 Gestion des Erreurs

### Types d'erreurs gérées
- **Nombre de champs insuffisant** : Moins de 2 champs séparés par `;`
- **Msg-type vide** : 2ème champ vide ou ne contenant que des espaces
- **Ligne mal formée** : Format incorrect ou parsing impossible

### Exemples de lignes avec erreurs
```bash
# Erreur: nombre de champs insuffisant
"un seul champ"

# Erreur: msg-type vide  
"077;;23012025;XXXX;YYYY;ZZZZ"

# Erreur: pas de séparateur
"ligne sans point virgule"
```

## ⚡ Performance

- **Streaming** : Traitement ligne par ligne sans chargement complet
- **Regroupement efficace** : Utilisation de HashMap pour O(1) lookup
- **Gestion mémoire** : Libération des ressources après traitement
- **Scalable** : Adapté aux fichiers volumineux

## 🔄 Intégration

Le service s'intègre parfaitement dans le pipeline existant :
1. **FileWatcherService** → Détection des fichiers
2. **FileStabilizationService** → Stabilisation (5 secondes)
3. **FileReadingService** → Lecture des lignes
4. **FeedParsingService** → **Extraction et regroupement** ⭐
5. **Prochaines étapes** → MongoDB, LLM, Anonymisation

## 📋 Prochaines étapes

Une fois les lignes regroupées par msg-type, les prochaines étapes incluront :
1. **Validation métier** : Vérification des règles par msg-type
2. **Stockage MongoDB** : Insertion des groupes dans la base
3. **Anonymisation** : Traitement des données sensibles
4. **Appel LLM** : Envoi des données au modèle de langage
5. **Mapping** : Transformation vers le format final
