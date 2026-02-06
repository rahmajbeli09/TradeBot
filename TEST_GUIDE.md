# Guide de Test - FeedSummaryService

## 🧪 Méthodes de test

### 1. **Test avec l'API REST (Recommandé)**

#### a) Lister les fichiers FEED disponibles
```bash
curl -X GET "http://localhost:8080/api/feed-summary/available-files"
```

#### b) Générer un résumé lisible (JSON)
```bash
curl -X POST "http://localhost:8080/api/feed-summary/generate-from-input" \
  -H "Content-Type: application/json" \
  -d '{"fileName": "FEED_20250205.txt"}'
```

#### c) Générer un résumé textuel formaté
```bash
curl -X POST "http://localhost:8080/api/feed-summary/generate-text" \
  -H "Content-Type: application/json" \
  -d '{"filePath": "input/feeds/FEED_20250205.txt"}'
```

#### d) Avec un chemin complet
```bash
curl -X POST "http://localhost:8080/api/feed-summary/generate" \
  -H "Content-Type: application/json" \
  -d '{"filePath": "d:/STAGE PFE NASOFT/ChatbotNaSoft/input/feeds/VOTRE_FICHIER.txt"}'
```

### 2. **Test avec Postman/Insomnia**

#### Configuration :
- **URL** : `http://localhost:8080/api/feed-summary/generate-from-input`
- **Méthode** : POST
- **Headers** : `Content-Type: application/json`
- **Body** (raw JSON) :
```json
{
  "fileName": "FEED_20250205.txt"
}
```

### 3. **Test avec un fichier FEED d'exemple**

#### Créer un fichier de test :
```bash
# Créer le répertoire si nécessaire
mkdir -p input/feeds

# Créer un fichier FEED de test
cat > input/feeds/FEED_TEST.txt << EOF
HEADER;05;12345;20250205123000;DONNEE1;DONNEE2
LINE1;05;67890;20250205124000;INFO_A;INFO_B;INFO_C
LINE2;10;11111;20250205125000;DATA_X
FOOTER;05;99999;20250205130000;FIN
EOF
```

### 4. **Test avec les mappings existants**

#### Vérifier les mappings en base :
```bash
# Si vous avez mongosh
mongosh mongodb://localhost:27017/chatbot
db.feed.find().pretty()

# Ou via l'API REST
curl -X GET "http://localhost:8080/api/feed-mappings"
```

### 5. **Test unitaire avec Maven**

#### Lancer les tests :
```bash
mvn test -Dtest=FeedSummaryServiceTest
```

#### Tests spécifiques :
```bash
mvn test -Dtest=FeedSummaryServiceTest#testGenerateTextSummary
```

## 📊 Résultats attendus

### Réponse JSON typique :
```json
{
  "success": true,
  "fileName": "FEED_TEST.txt",
  "filePath": "input/feeds/FEED_TEST.txt",
  "totalLines": 4,
  "readableLines": [
    {
      "msgType": "05",
      "champsLisibles": {
        "Champ 1": "Type de Message : HEADER",
        "Champ 2": "Sous-type de Message : 05",
        "Champ 3": "Identifiant Principal : 12345",
        "Champ 4": "Timestamp : 20250205123000",
        "Champ 5": "Données Additionnelles : DONNEE1",
        "Champ 6": "Non défini : DONNEE2"
      }
    }
  ]
}
```

### Réponse textuelle typique :
```
=== RÉSUMÉ LISIBLE DU FICHIER FEED ===

Ligne 1 (msgType: 05):
  Champ 1 → Type de Message : HEADER
  Champ 2 → Sous-type de Message : 05
  Champ 3 → Identifiant Principal : 12345
  Champ 4 → Timestamp : 20250205123000
  Champ 5 → Données Additionnelles : DONNEE1
  Champ 6 → Non défini : DONNEE2

=== TOTAL : 1 lignes traitées ===
```

## 🚨 Dépannage

### Erreurs communes :

#### 1. "Aucun mapping trouvé pour msgType"
- **Solution** : Assurez-vous que les mappings existent dans MongoDB
- **Vérification** : `curl -X GET "http://localhost:8080/api/feed-mappings"`

#### 2. "Le fichier n'existe pas"
- **Solution** : Vérifiez le chemin et que le fichier est dans `input/feeds/`
- **Vérification** : `curl -X GET "http://localhost:8080/api/feed-summary/available-files"`

#### 3. "Le fichier n'est pas lisible"
- **Solution** : Vérifiez les permissions du fichier
- **Solution** : Placez le fichier dans le bon répertoire

## 🔧 Tests avancés

### Test avec différents msgTypes :
```bash
# Test avec un msgType qui n'existe pas
curl -X POST "http://localhost:8080/api/feed-summary/generate-from-input" \
  -H "Content-Type: application/json" \
  -d '{"fileName": "FEED_UNKNOWN_MSGTYPE.txt"}'
```

### Test de performance :
```bash
# Créer un gros fichier (1000 lignes)
for i in {1..1000}; do
  echo "LINE_$i;05;ID_$i;$(date +%Y%m%d%H%M%S);DATA_$i" >> input/feeds/FEED_LARGE.txt
done

# Tester le traitement
time curl -X POST "http://localhost:8080/api/feed-summary/generate-from-input" \
  -H "Content-Type: application/json" \
  -d '{"fileName": "FEED_LARGE.txt"}'
```

## 📈 Monitoring

### Logs dans la console :
- Recherchez les messages : `✅ Traitement terminé`, `⚠️ Ligne ignorée`, `❌ Erreur`

### Métriques à surveiller :
- Nombre de lignes traitées
- Temps de traitement par ligne
- Taux de succès/échec

---

**Prêt à tester ! Lancez votre application Spring Boot et commencez avec l'API REST.** 🚀
