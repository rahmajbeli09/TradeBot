package com.example.chatbotnasoft.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO pour représenter les statistiques du résumé
 */
public class ResumeStatistiques {
    
    /**
     * Nombre total de lignes traitées
     */
    private int totalLignes;
    
    /**
     * Nombre de lignes avec mapping trouvé
     */
    private int lignesAvecMapping;
    
    /**
     * Nombre de lignes sans mapping
     */
    private int lignesSansMapping;
    
    /**
     * Répartition des msgType avec leur nombre d'occurrences
     */
    private Map<String, Integer> msgTypeCount;
    
    /**
     * Liste des différents msgType présents
     */
    private java.util.Set<String> msgTypesUniques;
    
    public ResumeStatistiques() {}
    
    public ResumeStatistiques(List<ResumeData> resumeDataList) {
        this.totalLignes = resumeDataList.size();
        this.msgTypeCount = new HashMap<>();
        this.msgTypesUniques = new java.util.HashSet<>();
        
        int avecMapping = 0;
        int sansMapping = 0;
        
        for (ResumeData data : resumeDataList) {
            if (data.isMappingTrouve()) {
                avecMapping++;
            } else {
                sansMapping++;
            }
            
            String msgType = data.getMsgType();
            if (msgType != null && !msgType.trim().isEmpty()) {
                msgTypeCount.put(msgType, msgTypeCount.getOrDefault(msgType, 0) + 1);
                msgTypesUniques.add(msgType);
            }
        }
        
        this.lignesAvecMapping = avecMapping;
        this.lignesSansMapping = sansMapping;
    }
    
    // Getters et setters
    public int getTotalLignes() {
        return totalLignes;
    }
    
    public void setTotalLignes(int totalLignes) {
        this.totalLignes = totalLignes;
    }
    
    public int getLignesAvecMapping() {
        return lignesAvecMapping;
    }
    
    public void setLignesAvecMapping(int lignesAvecMapping) {
        this.lignesAvecMapping = lignesAvecMapping;
    }
    
    public int getLignesSansMapping() {
        return lignesSansMapping;
    }
    
    public void setLignesSansMapping(int lignesSansMapping) {
        this.lignesSansMapping = lignesSansMapping;
    }
    
    public Map<String, Integer> getMsgTypeCount() {
        return msgTypeCount;
    }
    
    public void setMsgTypeCount(Map<String, Integer> msgTypeCount) {
        this.msgTypeCount = msgTypeCount;
    }
    
    public java.util.Set<String> getMsgTypesUniques() {
        return msgTypesUniques;
    }
    
    public void setMsgTypesUniques(java.util.Set<String> msgTypesUniques) {
        this.msgTypesUniques = msgTypesUniques;
    }
    
    /**
     * Calcule le taux de succès des mappings
     */
    public double getTauxSucces() {
        if (totalLignes == 0) return 0.0;
        return (lignesAvecMapping * 100.0) / totalLignes;
    }
    
    @Override
    public String toString() {
        return "ResumeStatistiques{" +
                "totalLignes=" + totalLignes +
                ", lignesAvecMapping=" + lignesAvecMapping +
                ", lignesSansMapping=" + lignesSansMapping +
                ", msgTypesUniques=" + msgTypesUniques.size() +
                '}';
    }
}
