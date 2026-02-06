package com.example.chatbotnasoft.dto;

import java.util.Map;

/**
 * DTO pour représenter les données résumées d'une ligne FEED
 */
public class ResumeData {
    
    /**
     * Le type de message extrait de la ligne (2ème champ)
     */
    private String msgType;
    
    /**
     * Le mapping des significations pour ce msgType
     * Clé: "Champ i", Valeur: "Signification du champ"
     */
    private Map<String, String> mapping;
    
    /**
     * Les valeurs brutes de la ligne
     * Clé: "Champ i", Valeur: "Valeur brute du champ"
     */
    private Map<String, String> valeurs;
    
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
    
    public ResumeData() {}
    
    public ResumeData(String msgType, Map<String, String> mapping, Map<String, String> valeurs, String ligneOriginale) {
        this.msgType = msgType;
        this.mapping = mapping;
        this.valeurs = valeurs;
        this.ligneOriginale = ligneOriginale;
        this.mappingTrouve = true;
    }
    
    public ResumeData(String msgType, String ligneOriginale, String erreur) {
        this.msgType = msgType;
        this.ligneOriginale = ligneOriginale;
        this.erreur = erreur;
        this.mappingTrouve = false;
    }
    
    // Getters et setters
    public String getMsgType() {
        return msgType;
    }
    
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
    
    public Map<String, String> getMapping() {
        return mapping;
    }
    
    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }
    
    public Map<String, String> getValeurs() {
        return valeurs;
    }
    
    public void setValeurs(Map<String, String> valeurs) {
        this.valeurs = valeurs;
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
    
    @Override
    public String toString() {
        return "ResumeData{" +
                "msgType='" + msgType + '\'' +
                ", mapping=" + mapping +
                ", valeurs=" + valeurs +
                ", mappingTrouve=" + mappingTrouve +
                '}';
    }
}
