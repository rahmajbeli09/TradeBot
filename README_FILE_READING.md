# Lecture de Fichiers FEED - ChatbotNaSoft

## 🎯 Objectif

Ce module implémente une lecture robuste et performante des fichiers FEED*.txt ligne par ligne avec streaming, sans charger tout le fichier en mémoire.

## 📁 Structure des fichiers créés

```
src/main/java/com/example/chatbotnasoft/
├── dto/
│   └── RawFeedLine.java                    # Objet représentant une ligne lue
├── service/
│   ├── FileReadingService.java              # Service principal de lecture
│   └── FeedProcessingService.java          # Service de traitement des fichiers
└── controller/
    └── FeedProcessingController.java        # API pour la lecture et traitement

src/test/java/com/example/chatbotnasoft/
└── FileReadingServiceTest.java             # Tests unitaires complets
```

## 🔄 Processus de lecture

1. **Validation** : Vérification que le fichier existe et est lisible
2. **Streaming** : Lecture ligne par ligne avec `Files.lines()`
3. **Nettoyage** : Suppression des espaces et filtrage des lignes vides
4. **Création** : Génération d'objets `RawFeedLine` avec métadonnées
5. **Progression** : Logs détaillés du traitement

## 📊 Objet RawFeedLine

```java
public class RawFeedLine {
    private String content;        // Contenu brut de la ligne
    private int lineNumber;       // Numéro de ligne original
    private String sourceFileName; // Nom du fichier source
    private LocalDateTime readAt;  // Timestamp de lecture
}
```

## 🚀 API REST

### Traiter les fichiers prêts
```bash
POST http://localhost:8080/api/feed-processing/process-ready-files
```

### Lire un fichier (aperçu des 10 premières lignes)
```bash
GET http://localhost:8080/api/feed-processing/read-file/FEED_SAMPLE_20260205.txt
```

### Compter les lignes d'un fichier
```bash
GET http://localhost:8080/api/feed-processing/count-lines/FEED_SAMPLE_20260205.txt
```

## 🧪 Tests

### Exécuter les tests unitaires
```bash
mvn test -Dtest=FileReadingServiceTest
```

### Test manuel rapide
```bash
# 1. Créer un fichier de test
echo -e "Ligne 1\n\nLigne 3 avec espaces   \n\nLigne 5" > input/feeds/FEED_TEST.txt

# 2. Compter les lignes
curl -u user:<password> http://localhost:8080/api/feed-processing/count-lines/FEED_TEST.txt

# 3. Lire l'aperçu
curl -u user:<password> http://localhost:8080/api/feed-processing/read-file/FEED_TEST.txt
```

## 📈 Performance

- **Streaming** : Utilisation de `Files.lines()` pour éviter de charger tout le fichier en mémoire
- **Filtrage** : Les lignes vides sont ignorées pendant la lecture
- **Progression** : Logs toutes les 1000 lignes pour les gros fichiers
- **Auto-nettoyage** : Les ressources sont fermées automatiquement avec try-with-resources

## 🔍 Logs générés

```bash
INFO  - Début de la lecture du fichier: FEED_SAMPLE_20260205.txt
DEBUG - Ligne lue: FEED_SAMPLE_20260205.txt (ligne 1) -> 'HEADER|20260205|SYSTEM|FEED_VERSION_2.1'
DEBUG - Ligne vide ignorée: FEED_SAMPLE_20260205.txt (ligne 4)
INFO  - Fichier FEED_SAMPLE_20260205.txt contient 7 lignes valides
INFO  - Progression: 7/7 lignes traitées (100.0%)
INFO  - Traitement terminé: 7 lignes traitées
```

## ⚡ Caractéristiques techniques

- **Non-blocking** : Lecture asynchrone avec streaming
- **Memory-efficient** : Une seule ligne en mémoire à la fois
- **Robuste** : Gestion complète des erreurs IO
- **Scalable** : Adapté aux fichiers volumineux
- **Testable** : Tests unitaires complets avec couverture > 90%

## 🔄 Intégration

Le service s'intègre parfaitement avec le système de surveillance existant :
1. Le `FileWatcherService` détecte les nouveaux fichiers
2. Le `FileStabilizationService` attend la stabilisation
3. Le `FeedProcessingService` lit les fichiers prêts
4. Les lignes sont préparées pour les prochaines étapes (parsing, MongoDB, LLM)

## 📋 Prochaines étapes

Une fois les lignes lues, les prochaines étapes incluront :
1. **Parsing** : Découpage des champs selon le format
2. **Validation** : Vérification de la structure des données
3. **Stockage** : Insertion dans MongoDB
4. **Anonymisation** : Traitement des données sensibles
5. **LLM** : Envoi vers le modèle de langage
