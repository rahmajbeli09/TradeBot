package com.example.chatbotnasoft.dto;

import java.util.*;

/**
 * DTO pour représenter les statistiques améliorées du résumé
 */
public class ResumeStatistiquesAmeliorees {
    
    /**
     * Nombre total de lignes traitées
     */
    private int totalLignes;
    
    /**
     * Nombre de lignes avec mapping complet
     */
    private int lignesAvecMappingComplet;
    
    /**
     * Nombre de lignes avec mapping partiel
     */
    private int lignesAvecMappingPartiel;
    
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
    private Set<String> msgTypesUniques;
    
    /**
     * Nombre moyen de champs par msgType
     */
    private Map<String, Double> moyenneChampsParMsgType;
    
    /**
     * Nombre total de champs différents rencontrés
     */
    private int totalChampsDifferents;
    
    /**
     * Statistiques de complétude des mappings
     */
    private Map<String, Double> tauxCompletionParMsgType;
    
    /**
     * Champs les plus fréquents dans tout le fichier
     */
    private Map<String, Integer> champsPlusFrequents;
    
    public ResumeStatistiquesAmeliorees() {}
    
    public ResumeStatistiquesAmeliorees(List<ResumeDataAmeliore> resumeDataList) {
        this.totalLignes = resumeDataList.size();
        this.msgTypeCount = new HashMap<>();
        this.msgTypesUniques = new HashSet<>();
        this.moyenneChampsParMsgType = new HashMap<>();
        this.tauxCompletionParMsgType = new HashMap<>();
        this.champsPlusFrequents = new HashMap<>();
        
        Map<String, Integer> totalChampsParMsgType = new HashMap<>();
        Map<String, Integer> totalChampsAvecSignificationParMsgType = new HashMap<>();
        
        int avecMappingComplet = 0;
        int avecMappingPartiel = 0;
        int sansMapping = 0;
        Set<String> tousLesChamps = new HashSet<>();
        
        for (ResumeDataAmeliore data : resumeDataList) {
            if (data.isMappingTrouve()) {
                // Calculer le taux de complétude
                double tauxCompletion = data.getTauxCompletion();
                if (tauxCompletion >= 90.0) {
                    avecMappingComplet++;
                } else {
                    avecMappingPartiel++;
                }
                
                // Statistiques par msgType
                String msgType = data.getMsgType();
                if (msgType != null && !msgType.trim().isEmpty()) {
                    msgTypeCount.put(msgType, msgTypeCount.getOrDefault(msgType, 0) + 1);
                    msgTypesUniques.add(msgType);
                    
                    totalChampsParMsgType.put(msgType, 
                        totalChampsParMsgType.getOrDefault(msgType, 0) + data.getTotalChamps());
                    totalChampsAvecSignificationParMsgType.put(msgType, 
                        totalChampsAvecSignificationParMsgType.getOrDefault(msgType, 0) + data.getChampsAvecSignification());
                    
                    // Compter les champs pour la fréquence
                    if (data.getMappingComplet() != null) {
                        for (ChampDetail champ : data.getMappingComplet()) {
                            tousLesChamps.add(champ.getChamp());
                            champsPlusFrequents.put(champ.getChamp(), 
                                champsPlusFrequents.getOrDefault(champ.getChamp(), 0) + 1);
                        }
                    }
                }
            } else {
                sansMapping++;
            }
        }
        
        this.lignesAvecMappingComplet = avecMappingComplet;
        this.lignesAvecMappingPartiel = avecMappingPartiel;
        this.lignesSansMapping = sansMapping;
        this.totalChampsDifferents = tousLesChamps.size();
        
        // Calculer les moyennes et taux de complétude
        for (String msgType : msgTypesUniques) {
            int totalChamps = totalChampsParMsgType.get(msgType);
            int totalAvecSignification = totalChampsAvecSignificationParMsgType.get(msgType);
            int count = msgTypeCount.get(msgType);
            
            moyenneChampsParMsgType.put(msgType, totalChamps / (double) count);
            tauxCompletionParMsgType.put(msgType, (totalAvecSignification * 100.0) / totalChamps);
        }
    }
    
    // Getters et setters
    public int getTotalLignes() {
        return totalLignes;
    }
    
    public void setTotalLignes(int totalLignes) {
        this.totalLignes = totalLignes;
    }
    
    public int getLignesAvecMappingComplet() {
        return lignesAvecMappingComplet;
    }
    
    public void setLignesAvecMappingComplet(int lignesAvecMappingComplet) {
        this.lignesAvecMappingComplet = lignesAvecMappingComplet;
    }
    
    public int getLignesAvecMappingPartiel() {
        return lignesAvecMappingPartiel;
    }
    
    public void setLignesAvecMappingPartiel(int lignesAvecMappingPartiel) {
        this.lignesAvecMappingPartiel = lignesAvecMappingPartiel;
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
    
    public Set<String> getMsgTypesUniques() {
        return msgTypesUniques;
    }
    
    public void setMsgTypesUniques(Set<String> msgTypesUniques) {
        this.msgTypesUniques = msgTypesUniques;
    }
    
    public Map<String, Double> getMoyenneChampsParMsgType() {
        return moyenneChampsParMsgType;
    }
    
    public void setMoyenneChampsParMsgType(Map<String, Double> moyenneChampsParMsgType) {
        this.moyenneChampsParMsgType = moyenneChampsParMsgType;
    }
    
    public int getTotalChampsDifferents() {
        return totalChampsDifferents;
    }
    
    public void setTotalChampsDifferents(int totalChampsDifferents) {
        this.totalChampsDifferents = totalChampsDifferents;
    }
    
    public Map<String, Double> getTauxCompletionParMsgType() {
        return tauxCompletionParMsgType;
    }
    
    public void setTauxCompletionParMsgType(Map<String, Double> tauxCompletionParMsgType) {
        this.tauxCompletionParMsgType = tauxCompletionParMsgType;
    }
    
    public Map<String, Integer> getChampsPlusFrequents() {
        return champsPlusFrequents;
    }
    
    public void setChampsPlusFrequents(Map<String, Integer> champsPlusFrequents) {
        this.champsPlusFrequents = champsPlusFrequents;
    }
    
    /**
     * Calcule le taux de succès global (lignes avec mapping)
     */
    public double getTauxSuccesGlobal() {
        if (totalLignes == 0) return 0.0;
        return ((lignesAvecMappingComplet + lignesAvecMappingPartiel) * 100.0) / totalLignes;
    }
    
    /**
     * Calcule le taux de mapping complet
     */
    public double getTauxMappingComplet() {
        if (totalLignes == 0) return 0.0;
        return (lignesAvecMappingComplet * 100.0) / totalLignes;
    }
    
    @Override
    public String toString() {
        return "ResumeStatistiquesAmeliorees{" +
                "totalLignes=" + totalLignes +
                ", lignesAvecMappingComplet=" + lignesAvecMappingComplet +
                ", lignesAvecMappingPartiel=" + lignesAvecMappingPartiel +
                ", lignesSansMapping=" + lignesSansMapping +
                ", msgTypesUniques=" + msgTypesUniques.size() +
                ", totalChampsDifferents=" + totalChampsDifferents +
                '}';
    }
}
