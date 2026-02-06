package com.example.chatbotnasoft.dto;

import java.util.List;

/**
 * DTO pour représenter une ligne FEED anonymisée
 */
public class LigneAnonymisee {
    
    /**
     * La ligne originale brute pour référence
     */
    private String ligneOriginale;
    
    /**
     * La ligne anonymisée prête pour Gemini
     */
    private String ligneAnonymisee;
    
    /**
     * Le msgType extrait de la ligne (2ème champ)
     */
    private String msgType;
    
    /**
     * Nombre total de champs dans la ligne
     */
    private int totalChamps;
    
    /**
     * Liste des détails de chaque champ
     */
    private List<ChampAnonymise> champs;
    
    /**
     * Indique si l'anonymisation a réussi
     */
    private boolean anonymisationReussie;
    
    /**
     * Message d'erreur si l'anonymisation échoue
     */
    private String erreur;
    
    public LigneAnonymisee() {}
    
    public LigneAnonymisee(String ligneOriginale, String ligneAnonymisee, String msgType, 
                           int totalChamps, List<ChampAnonymise> champs) {
        this.ligneOriginale = ligneOriginale;
        this.ligneAnonymisee = ligneAnonymisee;
        this.msgType = msgType;
        this.totalChamps = totalChamps;
        this.champs = champs;
        this.anonymisationReussie = true;
    }
    
    public LigneAnonymisee(String ligneOriginale, String erreur) {
        this.ligneOriginale = ligneOriginale;
        this.erreur = erreur;
        this.anonymisationReussie = false;
        this.totalChamps = 0;
    }
    
    // Getters et setters
    public String getLigneOriginale() {
        return ligneOriginale;
    }
    
    public void setLigneOriginale(String ligneOriginale) {
        this.ligneOriginale = ligneOriginale;
    }
    
    public String getLigneAnonymisee() {
        return ligneAnonymisee;
    }
    
    public void setLigneAnonymisee(String ligneAnonymisee) {
        this.ligneAnonymisee = ligneAnonymisee;
    }
    
    public String getMsgType() {
        return msgType;
    }
    
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
    
    public int getTotalChamps() {
        return totalChamps;
    }
    
    public void setTotalChamps(int totalChamps) {
        this.totalChamps = totalChamps;
    }
    
    public List<ChampAnonymise> getChamps() {
        return champs;
    }
    
    public void setChamps(List<ChampAnonymise> champs) {
        this.champs = champs;
    }
    
    public boolean isAnonymisationReussie() {
        return anonymisationReussie;
    }
    
    public void setAnonymisationReussie(boolean anonymisationReussie) {
        this.anonymisationReussie = anonymisationReussie;
    }
    
    public String getErreur() {
        return erreur;
    }
    
    public void setErreur(String erreur) {
        this.erreur = erreur;
    }
    
    @Override
    public String toString() {
        return "LigneAnonymisee{" +
                "msgType='" + msgType + '\'' +
                ", totalChamps=" + totalChamps +
                ", anonymisationReussie=" + anonymisationReussie +
                ", ligneAnonymisee='" + ligneAnonymisee + '\'' +
                '}';
    }
}
