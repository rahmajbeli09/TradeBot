package com.example.chatbotnasoft.dto;

import java.util.List;

/**
 * DTO pour représenter la réponse améliorée du service de résumé
 */
public class ResumeResponseAmeliore {
    
    /**
     * Liste des données résumées améliorées pour chaque ligne
     */
    private List<ResumeDataAmeliore> resumeDataAmeliore;
    
    /**
     * Résumé textuel amélioré et complet généré pour l'agent
     */
    private String resumeTexteAmeliore;
    
    /**
     * Nom du fichier traité
     */
    private String fileName;
    
    /**
     * Statistiques améliorées du traitement
     */
    private ResumeStatistiquesAmeliorees statistiques;
    
    /**
     * Indique si le traitement a réussi
     */
    private boolean success;
    
    public ResumeResponseAmeliore() {}
    
    public ResumeResponseAmeliore(String fileName, List<ResumeDataAmeliore> resumeDataAmeliore, 
                              String resumeTexteAmeliore) {
        this.fileName = fileName;
        this.resumeDataAmeliore = resumeDataAmeliore;
        this.resumeTexteAmeliore = resumeTexteAmeliore;
        this.statistiques = new ResumeStatistiquesAmeliorees(resumeDataAmeliore);
        this.success = true;
    }
    
    // Getters et setters
    public List<ResumeDataAmeliore> getResumeDataAmeliore() {
        return resumeDataAmeliore;
    }
    
    public void setResumeDataAmeliore(List<ResumeDataAmeliore> resumeDataAmeliore) {
        this.resumeDataAmeliore = resumeDataAmeliore;
    }
    
    public String getResumeTexteAmeliore() {
        return resumeTexteAmeliore;
    }
    
    public void setResumeTexteAmeliore(String resumeTexteAmeliore) {
        this.resumeTexteAmeliore = resumeTexteAmeliore;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public ResumeStatistiquesAmeliorees getStatistiques() {
        return statistiques;
    }
    
    public void setStatistiques(ResumeStatistiquesAmeliorees statistiques) {
        this.statistiques = statistiques;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    @Override
    public String toString() {
        return "ResumeResponseAmeliore{" +
                "fileName='" + fileName + '\'' +
                ", resumeDataSize=" + (resumeDataAmeliore != null ? resumeDataAmeliore.size() : 0) +
                ", success=" + success +
                '}';
    }
}
