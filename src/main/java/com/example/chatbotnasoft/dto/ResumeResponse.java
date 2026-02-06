package com.example.chatbotnasoft.dto;

import java.util.List;

/**
 * DTO pour représenter la réponse complète du service de résumé
 */
public class ResumeResponse {
    
    /**
     * Liste des données résumées pour chaque ligne
     */
    private List<ResumeData> resumeData;
    
    /**
     * Résumé textuel global généré pour l'agent
     */
    private String resumeTexte;
    
    /**
     * Statistiques du traitement
     */
    private ResumeStatistiques statistiques;
    
    public ResumeResponse() {}
    
    public ResumeResponse(List<ResumeData> resumeData, String resumeTexte) {
        this.resumeData = resumeData;
        this.resumeTexte = resumeTexte;
        this.statistiques = new ResumeStatistiques(resumeData);
    }
    
    // Getters et setters
    public List<ResumeData> getResumeData() {
        return resumeData;
    }
    
    public void setResumeData(List<ResumeData> resumeData) {
        this.resumeData = resumeData;
    }
    
    public String getResumeTexte() {
        return resumeTexte;
    }
    
    public void setResumeTexte(String resumeTexte) {
        this.resumeTexte = resumeTexte;
    }
    
    public ResumeStatistiques getStatistiques() {
        return statistiques;
    }
    
    public void setStatistiques(ResumeStatistiques statistiques) {
        this.statistiques = statistiques;
    }
    
    @Override
    public String toString() {
        return "ResumeResponse{" +
                "resumeDataSize=" + (resumeData != null ? resumeData.size() : 0) +
                ", resumeTexteLength=" + (resumeTexte != null ? resumeTexte.length() : 0) +
                '}';
    }
}
