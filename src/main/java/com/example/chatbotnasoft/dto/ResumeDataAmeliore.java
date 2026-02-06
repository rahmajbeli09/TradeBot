package com.example.chatbotnasoft.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO amélioré pour représenter les données résumées d'une ligne FEED
 */
public class ResumeDataAmeliore {
    
    /**
     * Le type de message extrait de la ligne (2ème champ)
     */
    private String msgType;
    
    /**
     * Mapping complet pour TOUS les champs (même ceux sans signification)
     */
    private List<ChampDetail> mappingComplet;
    
    /**
     * Valeurs brutes de la ligne
     */
    private Map<String, String> valeurs;
    
    /**
     * Valeurs anonymisées pour les champs sensibles
     */
    private Map<String, String> valeursAnonymisees;
    
    /**
     * La ligne originale brute pour référence
     */
    private String ligneOriginale;
    
    /**
     * Indique si un mapping a été trouvé pour ce msgType
     */
    private boolean mappingTrouve;
    
    /**
     * Message d'erreur si le mapping n'est pas trouvé
     */
    private String erreur;
    
    /**
     * Nombre total de champs dans cette ligne
     */
    private int totalChamps;
    
    /**
     * Nombre de champs avec signification connue
     */
    private int champsAvecSignification;
    
    public ResumeDataAmeliore() {}
    
    public ResumeDataAmeliore(String msgType, List<ChampDetail> mappingComplet, 
                           Map<String, String> valeurs, Map<String, String> valeursAnonymisees, 
                           String ligneOriginale) {
        this.msgType = msgType;
        this.mappingComplet = mappingComplet;
        this.valeurs = valeurs;
        this.valeursAnonymisees = valeursAnonymisees;
        this.ligneOriginale = ligneOriginale;
        this.mappingTrouve = true;
        this.totalChamps = valeurs != null ? valeurs.size() : 0;
        this.champsAvecSignification = (int) mappingComplet.stream()
                .mapToInt(champ -> "Inconnu".equals(champ.getSignification()) ? 0 : 1)
                .sum();
    }
    
    public ResumeDataAmeliore(String msgType, String ligneOriginale, String erreur) {
        this.msgType = msgType;
        this.ligneOriginale = ligneOriginale;
        this.erreur = erreur;
        this.mappingTrouve = false;
        this.totalChamps = 0;
        this.champsAvecSignification = 0;
    }
    
    // Getters et setters
    public String getMsgType() {
        return msgType;
    }
    
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
    
    public List<ChampDetail> getMappingComplet() {
        return mappingComplet;
    }
    
    public void setMappingComplet(List<ChampDetail> mappingComplet) {
        this.mappingComplet = mappingComplet;
    }
    
    public Map<String, String> getValeurs() {
        return valeurs;
    }
    
    public void setValeurs(Map<String, String> valeurs) {
        this.valeurs = valeurs;
    }
    
    public Map<String, String> getValeursAnonymisees() {
        return valeursAnonymisees;
    }
    
    public void setValeursAnonymisees(Map<String, String> valeursAnonymisees) {
        this.valeursAnonymisees = valeursAnonymisees;
    }
    
    public String getLigneOriginale() {
        return ligneOriginale;
    }
    
    public void setLigneOriginale(String ligneOriginale) {
        this.ligneOriginale = ligneOriginale;
    }
    
    public boolean isMappingTrouve() {
        return mappingTrouve;
    }
    
    public void setMappingTrouve(boolean mappingTrouve) {
        this.mappingTrouve = mappingTrouve;
    }
    
    public String getErreur() {
        return erreur;
    }
    
    public void setErreur(String erreur) {
        this.erreur = erreur;
    }
    
    public int getTotalChamps() {
        return totalChamps;
    }
    
    public void setTotalChamps(int totalChamps) {
        this.totalChamps = totalChamps;
    }
    
    public int getChampsAvecSignification() {
        return champsAvecSignification;
    }
    
    public void setChampsAvecSignification(int champsAvecSignification) {
        this.champsAvecSignification = champsAvecSignification;
    }
    
    /**
     * Calcule le pourcentage de champs avec signification
     */
    public double getTauxCompletion() {
        if (totalChamps == 0) return 0.0;
        return (champsAvecSignification * 100.0) / totalChamps;
    }
    
    @Override
    public String toString() {
        return "ResumeDataAmeliore{" +
                "msgType='" + msgType + '\'' +
                ", totalChamps=" + totalChamps +
                ", champsAvecSignification=" + champsAvecSignification +
                ", tauxCompletion=" + String.format("%.1f", getTauxCompletion()) + "%" +
                ", mappingTrouve=" + mappingTrouve +
                '}';
    }
}
