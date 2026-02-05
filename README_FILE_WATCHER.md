# Surveillance de Fichiers - ChatbotNaSoft

## 🎯 Objectif

Ce module implémente une surveillance robuste des fichiers FEED*.txt avec un mécanisme de stabilisation pour éviter le traitement de fichiers incomplets.

## 📁 Structure des fichiers créés

```
src/main/java/com/example/chatbotnasoft/
├── config/
│   └── FileWatcherProperties.java     # Configuration des propriétés
├── service/
│   ├── FileWatcherService.java        # Service principal de surveillance
│   ├── FileStabilizationService.java  # Gestion du délai de stabilisation
│   └── FileProcessingService.java     # Gestion des fichiers prêts
└── controller/
    └── FileWatcherController.java     # API de monitoring

src/test/java/com/example/chatbotnasoft/
└── FileWatcherTest.java               # Tests unitaires
```

## ⚙️ Configuration (application.yml)

```yaml
file-watcher:
  input-directory: input/feeds           # Répertoire surveillé
  file-pattern: FEED*.txt               # Pattern des fichiers
  stabilization-delay-minutes: 5        # Délai de stabilisation
  check-interval-seconds: 30            # Intervalle de vérification
  max-file-size-mb: 100                 # Taille maximale
```

## 🔄 Processus de surveillance

1. **Détection** : WatchService détecte les nouveaux fichiers FEED*.txt
2. **Stabilisation** : Attente de 5 minutes après la dernière modification
3. **Vérification** : Contrôle régulier de la taille du fichier
4. **Validation** : Marquage comme "prêt" si stabilisé

## 📊 Endpoints API

### Status général
```bash
GET http://localhost:8080/api/file-watcher/status
```

### Fichiers prêts pour traitement
```bash
GET http://localhost:8080/api/file-watcher/ready-files
```

### Fichiers en cours de stabilisation
```bash
GET http://localhost:8080/api/file-watcher/stabilizing-files
```

### Nettoyage des fichiers prêts
```bash
POST http://localhost:8080/api/file-watcher/clear-ready-files
```

## 🧪 Test du système

1. **Démarrer l'application** :
   ```bash
   mvn spring-boot:run
   ```

2. **Créer un fichier de test** :
   ```bash
   echo "Test content" > input/feeds/FEED20260205.txt
   ```

3. **Observer les logs** :
   ```
   INFO  - Nouveau fichier détecté: FEED20260205.txt
   INFO  - Début de stabilisation pour: FEED20260205.txt (délai: 5 minutes)
   INFO  - Fichier stabilisé et prêt pour traitement: FEED20260205.txt
   ```

4. **Vérifier le statut** :
   ```bash
   curl http://localhost:8080/api/file-watcher/status
   ```

## 🔍 Logs générés

- `INFO` - Événements majeurs (détection, stabilisation)
- `DEBUG` - Modifications de fichiers, vérifications
- `WARN` - Fichiers disparus, erreurs temporaires
- `ERROR` - Erreurs critiques

## 🚀 Prochaines étapes

Une fois le fichier marqué comme "prêt", les prochaines étapes incluront :
1. Lecture du contenu ligne par ligne
2. Parsing des données
3. Stockage dans MongoDB
4. Traitement par LLM

## ⚠️ Notes importantes

- Le répertoire `input/feeds` est créé automatiquement
- Les fichiers sont surveillés en temps réel
- La stabilisation garantit l'intégrité des données
- Le système évite les traitements dupliqués
