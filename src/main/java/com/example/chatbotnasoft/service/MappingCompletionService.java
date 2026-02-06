package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.FeedMapping;
import com.example.chatbotnasoft.repository.FeedMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service pour transformer les mappings anonymisés en mappings complets
 * avec les vraies significations des champs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MappingCompletionService {
    
    private final FeedMappingRepository feedMappingRepository;
    
    // Base de données interne des significations par msgType et champ
    private static final Map<String, Map<String, String>> REAL_MAPPINGS_DATABASE = new HashMap<>();
    
    static {
        // Initialisation de la base de données interne des significations
        initializeRealMappingsDatabase();
    }
    
    /**
     * Transforme un mapping anonymisé en mapping complet
     * @param anonymizedMapping Mapping avec des "Donnée anonymisée X"
     * @return Mapping complet avec vraies significations
     */
    public FeedMapping completeMapping(FeedMapping anonymizedMapping) {
        if (anonymizedMapping == null || anonymizedMapping.getMapping() == null) {
            return anonymizedMapping;
        }
        
        String msgType = anonymizedMapping.getMsgType();
        Map<String, String> originalMapping = anonymizedMapping.getMapping();
        Map<String, String> completedMapping = new HashMap<>();
        
        // Traiter chaque champ du mapping
        for (Map.Entry<String, String> entry : originalMapping.entrySet()) {
            String champKey = entry.getKey();
            String value = entry.getValue();
            
            if (isAnonymizedData(value)) {
                // Remplacer par la signification réelle
                String realSignification = getRealSignification(msgType, champKey, value);
                completedMapping.put(champKey, realSignification);
            } else {
                // Garder la valeur existante si elle n'est pas anonymisée
                completedMapping.put(champKey, value);
            }
        }
        
        // Créer le nouveau mapping complété
        FeedMapping completedFeedMapping = new FeedMapping();
        completedFeedMapping.setId(anonymizedMapping.getId());
        completedFeedMapping.setMsgType(msgType);
        completedFeedMapping.setMapping(completedMapping);
        completedFeedMapping.setCreatedAt(anonymizedMapping.getCreatedAt());
        
        log.info("✅ Mapping complété pour msgType '{}': {} champs traités", msgType, completedMapping.size());
        
        return completedFeedMapping;
    }
    
    /**
     * Vérifie si une valeur est une donnée anonymisée
     */
    private boolean isAnonymizedData(String value) {
        return value != null && value.matches("Donnée anonymisée \\d+");
    }
    
    /**
     * Récupère la signification réelle pour un champ donné
     */
    private String getRealSignification(String msgType, String champKey, String anonymizedValue) {
        // Extraire le numéro de la donnée anonymisée
        Pattern pattern = Pattern.compile("Donnée anonymisée (\\d+)");
        Matcher matcher = pattern.matcher(anonymizedValue);
        
        if (!matcher.matches()) {
            return "Signification manquante";
        }
        
        int anonymizedNumber = Integer.parseInt(matcher.group(1));
        
        // Chercher dans la base de données interne
        Map<String, String> msgTypeMappings = REAL_MAPPINGS_DATABASE.get(msgType);
        if (msgTypeMappings != null) {
            String realSignification = msgTypeMappings.get(champKey);
            if (realSignification != null) {
                return realSignification;
            }
        }
        
        // Si pas trouvé, essayer de déduire selon le numéro et le type
        return getDeducedSignification(msgType, champKey, anonymizedNumber);
    }
    
    /**
     * Déduit la signification selon le contexte
     */
    private String getDeducedSignification(String msgType, String champKey, int anonymizedNumber) {
        // Logique de déduction selon le msgType
        switch (msgType) {
            case "A3":
                return getA3Signification(champKey, anonymizedNumber);
            case "05":
                return get05Signification(champKey, anonymizedNumber);
            case "10":
                return get10Signification(champKey, anonymizedNumber);
            default:
                return "Signification manquante pour " + msgType;
        }
    }
    
    private String getA3Signification(String champKey, int anonymizedNumber) {
        switch (anonymizedNumber) {
            case 1: return "Identifiant unique de l'opération";
            case 2: return "Identifiant de l'entité source (ex: compte, utilisateur)";
            case 3: return "Montant de la transaction";
            case 4: return "Devise de la transaction";
            case 5: return "Date et heure de l'opération";
            default: return "Signification manquante";
        }
    }
    
    private String get05Signification(String champKey, int anonymizedNumber) {
        switch (anonymizedNumber) {
            case 1: return "Code d'identification du message";
            case 2: return "Référence de la transaction";
            case 3: return "Statut du traitement";
            case 4: return "Timestamp de création";
            default: return "Signification manquante";
        }
    }
    
    private String get10Signification(String champKey, int anonymizedNumber) {
        switch (anonymizedNumber) {
            case 1: return "Identifiant du client";
            case 2: return "Type d'opération effectuée";
            case 3: return "Résultat de l'opération";
            default: return "Signification manquante";
        }
    }
    
    /**
     * Initialise la base de données interne des mappings réels
     */
    private static void initializeRealMappingsDatabase() {
        // Mappings pour msgType A3
        Map<String, String> a3Mappings = new HashMap<>();
        a3Mappings.put("Champ 3", "Identifiant unique de l'opération");
        a3Mappings.put("Champ 4", "Identifiant de l'entité source (ex: compte, utilisateur)");
        a3Mappings.put("Champ 5", "Montant de la transaction");
        a3Mappings.put("Champ 6", "Devise de la transaction");
        a3Mappings.put("Champ 7", "Date et heure de l'opération");
        REAL_MAPPINGS_DATABASE.put("A3", a3Mappings);
        
        // Mappings pour msgType 05
        Map<String, String> o5Mappings = new HashMap<>();
        o5Mappings.put("Champ 3", "Code d'identification du message");
        o5Mappings.put("Champ 4", "Référence de la transaction");
        o5Mappings.put("Champ 5", "Statut du traitement");
        o5Mappings.put("Champ 6", "Timestamp de création");
        REAL_MAPPINGS_DATABASE.put("05", o5Mappings);
        
        // Mappings pour msgType 10
        Map<String, String> t10Mappings = new HashMap<>();
        t10Mappings.put("Champ 3", "Identifiant du client");
        t10Mappings.put("Champ 4", "Type d'opération effectuée");
        t10Mappings.put("Champ 5", "Résultat de l'opération");
        REAL_MAPPINGS_DATABASE.put("10", t10Mappings);
        
        // Ajouter d'autres msgTypes au besoin...
    }
    
    /**
     * Ajoute ou met à jour un mapping dans la base interne
     */
    public void addRealMapping(String msgType, String champKey, String signification) {
        REAL_MAPPINGS_DATABASE.computeIfAbsent(msgType, k -> new HashMap<>())
                           .put(champKey, signification);
        log.info("📝 Ajout mapping réel: {} -> {} = {}", msgType, champKey, signification);
    }
    
    /**
     * Affiche tous les mappings réels disponibles pour un msgType
     */
    public Map<String, String> getRealMappingsForMsgType(String msgType) {
        return REAL_MAPPINGS_DATABASE.getOrDefault(msgType, new HashMap<>());
    }
}
