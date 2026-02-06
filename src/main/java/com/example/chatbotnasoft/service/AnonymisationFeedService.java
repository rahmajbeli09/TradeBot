package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Service pour anonymiser les lignes de fichiers FEED
 * Prépare les données pour l'envoi à Gemini LLM
 */
@Service
@Slf4j
public class AnonymisationFeedService {

    /**
     * Anonymise un fichier FEED complet selon les règles spécifiées
     * @param filePath Chemin du fichier FEED à anonymiser
     * @return AnonymisationResponse avec les lignes anonymisées prêtes pour Gemini
     */
    public AnonymisationResponse anonymiserFichier(String filePath) {
        log.info("🔒 Début de l'anonymisation du fichier: {}", filePath);
        
        List<LigneAnonymisee> lignesAnonymisees = new ArrayList<>();
        String fileName = extractFileName(filePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String ligne;
            int numeroLigne = 0;
            
            while ((ligne = reader.readLine()) != null) {
                numeroLigne++;
                ligne = ligne.trim();
                
                // Ignorer les lignes vides
                if (ligne.isEmpty()) {
                    continue;
                }
                
                log.debug("📝 Anonymisation ligne {}: {}", numeroLigne, ligne);
                
                LigneAnonymisee ligneAnonymisee = anonymiserLigne(ligne, numeroLigne);
                lignesAnonymisees.add(ligneAnonymisee);
            }
            
            log.info("✅ Anonymisation terminée: {} lignes traitées", lignesAnonymisees.size());
            
        } catch (IOException e) {
            log.error("❌ Erreur lors de la lecture du fichier {}: {}", filePath, e.getMessage());
            return new AnonymisationResponse(fileName, "Erreur de lecture du fichier: " + e.getMessage());
        }
        
        // Générer le contenu anonymisé complet
        String contenuAnonymise = genererContenuAnonymise(lignesAnonymisees);
        
        return new AnonymisationResponse(fileName, lignesAnonymisees, contenuAnonymise);
    }

    /**
     * Anonymise une ligne individuelle selon les règles spécifiées
     * @param ligne Ligne FEED à anonymiser
     * @param numeroLigne Numéro de la ligne pour le logging
     * @return LigneAnonymisee avec les détails de l'anonymisation
     */
    private LigneAnonymisee anonymiserLigne(String ligne, int numeroLigne) {
        try {
            // Extraire les champs séparés par ";"
            String[] champs = ligne.split(";");
            
            // Vérifier qu'on a au moins 2 champs pour extraire le msgType
            if (champs.length < 2) {
                String erreur = "Ligne " + numeroLigne + ": Moins de 2 champs trouvés";
                log.warn("⚠️ {}", erreur);
                return new LigneAnonymisee(ligne, erreur);
            }
            
            // Extraire le msgType (2ème champ)
            String msgType = champs[1].trim();
            log.debug("🔍 MsgType extrait: '{}' pour ligne {}", msgType, numeroLigne);
            
            // Anonymiser chaque champ selon les règles
            List<ChampAnonymise> champsAnonymises = new ArrayList<>();
            for (int i = 0; i < champs.length; i++) {
                String nomChamp = "Champ " + (i + 1);
                String valeurOriginale = champs[i].trim();
                
                ChampAnonymise champAnonymise = anonymiserChamp(nomChamp, valeurOriginale, i + 1);
                champsAnonymises.add(champAnonymise);
            }
            
            // Construire la ligne anonymisée
            String ligneAnonymisee = construireLigneAnonymisee(champsAnonymises);
            
            log.debug("✅ Ligne {} anonymisée avec succès - {} champs", numeroLigne, champsAnonymises.size());
            return new LigneAnonymisee(ligne, ligneAnonymisee, msgType, champsAnonymises.size(), champsAnonymises);
            
        } catch (Exception e) {
            String erreur = "Erreur lors de l'anonymisation de la ligne " + numeroLigne + ": " + e.getMessage();
            log.error("❌ {}", erreur, e);
            return new LigneAnonymisee(ligne, erreur);
        }
    }

    /**
     * Anonymise un champ individuel selon les règles spécifiées
     * @param nomChamp Nom du champ (ex: "Champ 1")
     * @param valeurOriginale Valeur originale du champ
     * @param numeroChamp Numéro du champ (1-based)
     * @return ChampAnonymise avec les détails de l'anonymisation
     */
    private ChampAnonymise anonymiserChamp(String nomChamp, String valeurOriginale, int numeroChamp) {
        
        // Règle 1: Conserver exactement les 3 premiers champs sans modification
        if (numeroChamp <= 3) {
            return new ChampAnonymise(nomChamp, valeurOriginale, valeurOriginale, 
                                    TypeChamp.INCONNU, "Conservé (3 premiers champs)");
        }
        
        // Règle 2: Traiter les autres champs selon leur type
        if (valeurOriginale == null || valeurOriginale.trim().isEmpty()) {
            return new ChampAnonymise(nomChamp, valeurOriginale, valeurOriginale, 
                                    TypeChamp.VIDE, "Champ vide - laissé vide");
        }
        
        String valeurTrim = valeurOriginale.trim();
        TypeChamp typeChamp = detecterTypeChamp(valeurTrim);
        String valeurAnonymisee = appliquerRegleAnonymisation(valeurTrim, typeChamp, numeroChamp);
        String regleAppliquee = getRegleAppliquee(typeChamp, valeurTrim, valeurAnonymisee);
        
        return new ChampAnonymise(nomChamp, valeurOriginale, valeurAnonymisee, 
                                typeChamp, regleAppliquee);
    }

    /**
     * Détecte le type d'un champ selon son contenu
     * @param valeur Valeur à analyser
     * @return TypeChamp détecté
     */
    private TypeChamp detecterTypeChamp(String valeur) {
        if (valeur == null || valeur.trim().isEmpty()) {
            return TypeChamp.VIDE;
        }
        
        String valeurTrim = valeur.trim().toUpperCase();
        
        // Détection de date (format YYYYMMDD)
        if (valeurTrim.matches("\\d{8}") && 
            (valeurTrim.startsWith("20") || valeurTrim.startsWith("19"))) {
            return TypeChamp.DATE;
        }
        
        // Détection d'heure (format HHMMSS)
        if (valeurTrim.matches("\\d{6}") && 
            Integer.parseInt(valeurTrim.substring(0, 2)) <= 23 && 
            Integer.parseInt(valeurTrim.substring(2, 4)) <= 59 && 
            Integer.parseInt(valeurTrim.substring(4, 6)) <= 59) {
            return TypeChamp.HEURE;
        }
        
        // Détection d'identifiant alphanumérique (longue chaîne avec lettres et chiffres)
        if (valeurTrim.matches("[A-Z0-9]{8,}")) {
            return TypeChamp.IDENTIFIANT;
        }
        
        // Détection de nombre pur
        if (valeurTrim.matches("\\d+")) {
            return TypeChamp.NOMBRE;
        }
        
        // Détection de code court (ex: "A", "05", "C")
        if (valeurTrim.matches("^[A-Z0-9]{1,3}$")) {
            return TypeChamp.CODE;
        }
        
        return TypeChamp.CODE; // Par défaut, considérer comme code/texte libre
    }

    /**
     * Applique la règle d'anonymisation appropriée selon le type de champ
     * @param valeur Valeur originale
     * @param typeChamp Type détecté du champ
     * @param numeroChamp Numéro du champ
     * @return Valeur anonymisée
     */
    private String appliquerRegleAnonymisation(String valeur, TypeChamp typeChamp, int numeroChamp) {
        switch (typeChamp) {
            case DATE:
                // Règle: Si le champ est une date au format YYYYMMDD → remplace par YYYYMMDD
                if (valeur.matches("\\d{8}")) {
                    return valeur; // Conserver le format exact
                }
                break;
                
            case HEURE:
                // Règle: Si le champ est une heure au format HHMMSS → remplace par HHMMSS
                if (valeur.matches("\\d{6}")) {
                    return valeur; // Conserver le format exact
                }
                break;
                
            case IDENTIFIANT:
                // Règle: Si le champ est un identifiant alphanumérique → remplace par ID_XXXXX
                String valeurUpper = valeur.toUpperCase();
                if (valeurUpper.matches("[A-Z0-9]{8,}")) {
                    int longueur = valeurUpper.length();
                    String debut = valeurUpper.substring(0, Math.min(4, longueur));
                    String fin = "X".repeat(Math.max(1, longueur - 4));
                    return "ID_" + debut + fin;
                }
                break;
                
            case NOMBRE:
                // Règle: Si le champ est un nombre → remplace par NUM_XXXX... (même nombre de caractères)
                if (valeur.matches("\\d+")) {
                    int longueur = valeur.length();
                    return "NUM_" + "X".repeat(longueur);
                }
                break;
                
            case CODE:
                // Règle: Si le champ est un code ou texte libre → remplace par CODE_XX
                String valeurTrim = valeur.trim();
                if (valeurTrim.length() <= 3) {
                    return "CODE_" + valeurTrim;
                } else {
                    return "CODE_XX";
                }

                
            case VIDE:
                // Règle: Si le champ est vide → laisse vide
                return valeur;
                
            default:
                return valeur;
        }
        
        return valeur; // Valeur par défaut si aucune règle ne s'applique
    }

    /**
     * Génère la description de la règle appliquée
     * @param typeChamp Type du champ
     * @param valeurOriginale Valeur originale
     * @param valeurAnonymisee Valeur anonymisée
     * @return Description de la règle
     */
    private String getRegleAppliquee(TypeChamp typeChamp, String valeurOriginale, String valeurAnonymisee) {
        if (valeurOriginale.equals(valeurAnonymisee)) {
            return "Aucune modification (valeur conservée)";
        }
        
        switch (typeChamp) {
            case DATE:
                return "Date conservée (format YYYYMMDD)";
            case HEURE:
                return "Heure conservée (format HHMMSS)";
            case IDENTIFIANT:
                return "Identifiant anonymisé (ID_XXXXX)";
            case NOMBRE:
                return "Nombre anonymisé (NUM_XXXX...)";
            case CODE:
                return "Code anonymisé (CODE_XX)";
            case VIDE:
                return "Champ vide - laissé vide";
            default:
                return "Type inconnu - valeur conservée";
        }
    }

    /**
     * Construit la ligne anonymisée à partir des champs anonymisés
     * @param champsAnonymises Liste des champs anonymisés
     * @return Ligne anonymisée complète avec séparateurs ";"
     */
    private String construireLigneAnonymisee(List<ChampAnonymise> champsAnonymises) {
        StringBuilder ligneBuilder = new StringBuilder();
        
        for (int i = 0; i < champsAnonymises.size(); i++) {
            ChampAnonymise champ = champsAnonymises.get(i);
            ligneBuilder.append(champ.getValeurAnonymisee());
            
            if (i < champsAnonymises.size() - 1) {
                ligneBuilder.append(";");
            }
        }
        
        return ligneBuilder.toString();
    }

    /**
     * Génère le contenu complet du fichier anonymisé
     * @param lignesAnonymisees Liste des lignes anonymisées
     * @return Contenu complet du fichier prêt pour Gemini
     */
    private String genererContenuAnonymise(List<LigneAnonymisee> lignesAnonymisees) {
        StringBuilder contenuBuilder = new StringBuilder();
        
        for (LigneAnonymisee ligne : lignesAnonymisees) {
            if (ligne.isAnonymisationReussie()) {
                contenuBuilder.append(ligne.getLigneAnonymisee());
            } else {
                // En cas d'erreur, conserver la ligne originale avec commentaire
                contenuBuilder.append("# ERREUR: ").append(ligne.getErreur())
                          .append(" | LIGNE ORIGINALE: ").append(ligne.getLigneOriginale());
            }
            contenuBuilder.append("\n");
        }
        
        return contenuBuilder.toString();
    }

    /**
     * Extrait le nom du fichier à partir du chemin complet
     * @param filePath Chemin complet du fichier
     * @return Nom du fichier seul
     */
    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "Fichier inconnu";
        }
        
        // Gérer les séparateurs Windows et Unix
        String separator = filePath.contains("\\") ? "\\\\" : "/";
        String[] parts = filePath.split(separator);
        return parts[parts.length - 1];
    }

    /**
     * Liste les fichiers FEED disponibles dans le répertoire input/feeds
     * @return Liste des noms de fichiers FEED
     */
    public List<String> listAvailableFeedFiles() {
        List<String> feedFiles = new ArrayList<>();
        java.io.File feedDir = new java.io.File("input/feeds");
        
        if (feedDir.exists() && feedDir.isDirectory()) {
            java.io.File[] files = feedDir.listFiles((dir, name) -> name.startsWith("FEED") && name.endsWith(".txt"));
            
            if (files != null) {
                for (java.io.File file : files) {
                    feedFiles.add(file.getName());
                }
            }
        }
        
        Collections.sort(feedFiles);
        log.info("📂 {} fichiers FEED trouvés: {}", feedFiles.size(), feedFiles);
        return feedFiles;
    }
}
