# 📊 Guide d'utilisation - Service de Résumé Structuré FEED

## 🎯 Objectif

Le service `ResumeFeedService` transforme les lignes de fichiers FEED en DTO structurés et génère un résumé global lisible pour l'agent.

## 🏗️ Architecture

### Composants principaux

1. **ResumeData** : DTO pour chaque ligne avec mapping et valeurs
2. **ResumeResponse** : Réponse complète avec données structurées et résumé textuel
3. **ResumeStatistiques** : Statistiques détaillées du traitement
4. **ResumeFeedService** : Service métier pour la génération de résumés
5. **ResumeFeedController** : API REST pour exposer le service

### Flux de traitement

```
Fichier FEED → Extraction msgType → Recherche mapping → Création ResumeData → Génération résumé global
```

## 📋 Structure des DTO

### ResumeData

```json
{
  "msgType": "05",
  "mapping": {
    "Champ 1": "Type d'enregistrement",
    "Champ 2": "Code de statut",
    "Champ 3": "Identifiant principal"
  },
  "valeurs": {
    "Champ 1": "061",
    "Champ 2": "05",
    "Champ 3": "20250613"
  },
  "ligneOriginale": "061;05;20250613;062116;TN823JXM7T75;A",
  "mappingTrouve": true,
  "erreur": null
}
```

### ResumeResponse

```json
{
  "resumeData": [ResumeData1, ResumeData2, ...],
  "resumeTexte": "Résumé global généré lisible par l'agent...",
  "statistiques": {
    "totalLignes": 10,
    "lignesAvecMapping": 8,
    "lignesSansMapping": 2,
    "tauxSucces": 80.0,
    "msgTypesUniques": ["05", "A3", "16"],
    "msgTypeCount": {"05": 5, "A3": 3, "16": 2}
  }
}
```

## 🚀 API REST

### 1. Générer un résumé complet (POST)

```bash
POST /api/resume-feed/generate
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
  "resumeData": [...],
  "resumeTexte": "📊 RÉSUMÉ GLOBAL DU FICHIER FEED...",
  "statistiques": {...}
}
```

### 2. Générer un résumé (GET - Test navigateur)

```bash
GET /api/resume-feed/generate/FEED_V2-30-01-2026v4.txt
```

### 3. Lister les fichiers FEED disponibles

```bash
GET /api/resume-feed/list-files
```

### 4. Vérifier le statut du service

```bash
GET /api/resume-feed/status
```

## 📊 Format du résumé textuel

Le résumé généré comprend :

### 📈 Statistiques générales
- Nombre total de lignes traitées
- Lignes avec/sans mapping
- Taux de succès
- Types de messages différents

### 🏷️ Répartition par msgType
- Liste des msgType avec leur nombre d'occurrences
- Tri alphabétique pour lisibilité

### 🔍 Détails par msgType
- Aperçu des 3 premières lignes par msgType
- Affichage des 5 premiers champs avec significations
- Indication du nombre total de champs

### ❌ Lignes avec erreurs
- Liste des erreurs rencontrées
- Lignes originales problématiques

## 🧪 Tests

### Tests unitaires

Les tests sont dans `ResumeFeedServiceTest.java` :

```bash
mvn test -Dtest=ResumeFeedServiceTest
```

### Tests manuels

1. **Créer des mappings de test** :
```bash
curl -X POST "http://localhost:8080/api/gemini-transform/transform-complete" \
  -H "Content-Type: application/json" \
  -d '{
    "geminiJson": "{\"fields\": [\"Type\", \"Code\", \"ID\", \"Heure\"], \"values\": [\"061\", \"05\", \"20250613\", \"062116\"]}",
    "msgType": "05"
  }'
```

2. **Tester le résumé** :
```bash
curl -X GET "http://localhost:8080/api/resume-feed/generate/FEED_TEST_04.txt"
```

## 📝 Exemples d'utilisation

### Script bash pour traiter tous les fichiers

```bash
#!/bin/bash

# Lister les fichiers
FILES=$(curl -s "http://localhost:8080/api/resume-feed/list-files" | jq -r '.files[]')

for file in $FILES; do
    echo "📊 Génération résumé pour: $file"
    
    # Générer le résumé complet
    curl -X POST "http://localhost:8080/api/resume-feed/generate" \
         -H "Content-Type: application/json" \
         -d "{\"filePath\": \"input/feeds/$file\"}" \
         -o "output/${file}_resume.json"
    
    echo "✅ Résumé généré: output/${file}_resume.json"
done
```

### Analyse avec curl

```bash
# Analyser un fichier spécifique
curl -X POST "http://localhost:8080/api/resume-feed/generate" \
     -H "Content-Type: application/json" \
     -d '{"filePath": "input/feeds/FEED_V2-30-01-2026v4.txt"}' \
     | jq '.resumeTexte'
```

## ⚠️ Points d'attention

1. **Structures variables** : Chaque msgType peut avoir un nombre différent de champs
2. **Mappings incomplets** : Les champs sans mapping sont inclus dans les valeurs mais pas dans le mapping
3. **Performance** : Le service traite les fichiers ligne par ligne (adapté aux gros fichiers)
4. **Encodage** : Les fichiers doivent être en UTF-8
5. **Gestion d'erreurs** : Les lignes invalides sont incluses dans le résumé avec messages d'erreur

## 🔧 Configuration

La configuration se trouve dans `application.yml` :

```yaml
gemini:
  api-key: AIzaSyDffpeEpLaHTsnPZlBiW5eXYrOw5DGyhxc
  model: gemini-3-flash-preview
  base-url: https://generativelanguage.googleapis.com/v1beta
```

## 🎉 Cas d'usage

1. **Audit complet** : Vue d'ensemble structurée de tous les fichiers FEED
2. **Validation de données** : Identification rapide des problèmes de mapping
3. **Reporting** : Génération automatique de rapports pour les équipes
4. **Analyse comparative** : Comparaison entre différents msgTypes et volumes
5. **Débogage** : Identification précise des lignes problématiques

## 📈 Exemple de résumé textuel généré

```
📊 RÉSUMÉ GLOBAL DU FICHIER FEED
=====================================

📈 STATISTIQUES GÉNÉRALES
=============================
• Lignes totales traitées: 15
• Lignes avec mapping: 12
• Lignes sans mapping: 3
• Taux de succès: 80.0%
• Types de messages différents: 3

🏷️ RÉPARTITION PAR MSG-TYPE
============================
• 05: 8 ligne(s)
• A3: 5 ligne(s)
• 16: 2 ligne(s)

🔍 DÉTAILS PAR MSG-TYPE
=========================
📋 MsgType: 05 (8 lignes)
   1. Type d'enregistrement: 061 | Code de statut: 05 | Identifiant principal: 20250613 | Identifiant secondaire: 062116 | Référence opération: TN823JXM7T75
   2. Type d'enregistrement: 145 | Code de statut: 05 | Identifiant principal: 20250613 | Identifiant secondaire: 062116 | Référence opération: TN823JXM7T75
   3. Type d'enregistrement: 023 | Code de statut: 05 | Identifiant principal: 20250613 | Identifiant secondaire: 000000 | Référence opération: 32
   ... et 5 autres lignes similaires

📋 MsgType: A3 (5 lignes)
   1. Type de transaction: 145 | Code de sous-transaction: A3 | Date de transaction: 20250613 | Heure de transaction: 062116 | Référence unique: TN823JXM7T75
   ... et 4 autres lignes similaires

❌ LIGNES AVEC ERREURS
========================
1. Aucun mapping trouvé pour le msgType: XX
   Ligne: 145;XX;20250613;062116;TN823JXM7T75;C

🎯 Résumé généré le Fri Feb 06 18:00:00 CET 2026
```

---

**Le service de résumé structuré est prêt à transformer vos fichiers FEED !** 🚀
