# Guide de Test avec Authentification

## 🔐 Configuration de sécurité ajoutée

J'ai créé une configuration de sécurité qui permet de tester facilement :

### Utilisateurs de test :
- **Admin** : `admin` / `admin123`
- **Test** : `test` / `test123`

## 🧪 Méthodes de test

### 1. **Sans authentification (Recommandé pour les tests)**

Les endpoints suivants sont maintenant accessibles **sans authentification** :
- `/api/feed-summary/**`
- `/api/debug/**`
- `/api/anonymize/**`
- `/api/llm/**`
- `/api/feed-mappings/**`

#### Test direct :
```bash
# Lister les fichiers disponibles
curl -X GET "http://localhost:8080/api/feed-summary/available-files"

# Générer un résumé
curl -X POST "http://localhost:8080/api/feed-summary/generate-from-input" \
  -H "Content-Type: application/json" \
  -d '{"fileName": "FEED_TEST.txt"}'
```

### 2. **Avec authentification Basic**

Si vous voulez tester avec authentification :

#### Avec curl :
```bash
# Avec l'utilisateur admin
curl -X GET "http://localhost:8080/api/feed-summary/available-files" \
  -u "admin:admin123"

# Avec l'utilisateur test
curl -X POST "http://localhost:8080/api/feed-summary/generate-from-input" \
  -H "Content-Type: application/json" \
  -u "test:test123" \
  -d '{"fileName": "FEED_TEST.txt"}'
```

#### Avec Postman/Insomnia :
1. **Onglet Authorization**
2. **Type** : Basic Auth
3. **Username** : `admin` (ou `test`)
4. **Password** : `admin123` (ou `test123`)

### 3. **Avec navigateur web**

Si vous accédez via un navigateur :
- URL : `http://localhost:8080/api/feed-summary/available-files`
- Popup d'authentification apparaîtra
- Entrez : `admin` / `admin123`

## 🚀 Démarrage rapide

### 1. **Démarrez l'application**
```bash
# Dans votre IDE ou terminal
mvn spring-boot:run
```

### 2. **Testez immédiatement (sans auth)**
```bash
curl -X GET "http://localhost:8080/api/feed-summary/available-files"
```

### 3. **Créez un fichier de test**
```bash
mkdir -p input/feeds
echo "HEADER;05;12345;20250205123000;DATA1;DATA2" > input/feeds/FEED_TEST.txt
```

### 4. **Testez le résumé**
```bash
curl -X POST "http://localhost:8080/api/feed-summary/generate-from-input" \
  -H "Content-Type: application/json" \
  -d '{"fileName": "FEED_TEST.txt"}'
```

## 📊 Résultats attendus

### Réponse JSON :
```json
{
  "success": true,
  "fileName": "FEED_TEST.txt",
  "filePath": "input/feeds/FEED_TEST.txt",
  "totalLines": 1,
  "readableLines": [
    {
      "msgType": "05",
      "champsLisibles": {
        "Champ 1": "Type de Message : HEADER",
        "Champ 2": "Sous-type de Message : 05",
        "Champ 3": "Identifiant Principal : 12345",
        "Champ 4": "Timestamp : 20250205123000",
        "Champ 5": "Non défini : DATA1",
        "Champ 6": "Non défini : DATA2"
      }
    }
  ]
}
```

## 🛠️ Si vous voulez désactiver complètement la sécurité

Ajoutez dans `application.yml` :
```yaml
spring:
  security:
    enabled: false
```

Ou commentez la dépendance `spring-boot-starter-security` dans `pom.xml`.

## 🔧 Personnalisation

### Ajouter d'autres utilisateurs :
Modifiez `SecurityConfig.java` :
```java
UserDetails newUser = User.builder()
    .username("votre-user")
    .password(passwordEncoder.encode("votre-password"))
    .roles("USER")
    .build();
```

### Changer les endpoints publics :
Modifiez la ligne dans `SecurityConfig.java` :
```java
.requestMatchers("/votre-endpoint/**").permitAll()
```

---

**✅ Maintenant vous pouvez tester sans avoir à vous authentifier !** 🎉
