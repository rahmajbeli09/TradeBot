package com.example.chatbotnasoft.dto;

import java.util.List;

/**
 * DTO pour représenter la réponse de l'anonymisation de fichier FEED
 */
public class AnonymisationResponse {
    
    /**
     * Nom du fichier traité
     */
    private String fileName;
    
    /**
     * Liste des lignes anonymisées
     */
    private List<LigneAnonymisee> lignesAnonymisees;
    
    /**
     * Contenu complet du fichier anonymisé (prêt pour Gemini)
     */
    private String contenuAnonymise;
    
    /**
     * Statistiques de l'anonymisation
     */
    private AnonymisationStatistiques statistiques;
    
    /**
     * Indique si l'anonymisation a réussi
     */
    private boolean success;
    
    /**
     * Message d'erreur si l'anonymisation échoue
     */
    private String erreur;
    
    public AnonymisationResponse() {}
    
    public AnonymisationResponse(String fileName, List<LigneAnonymisee> lignesAnonymisees, 
                            String contenuAnonymise) {
        this.fileName = fileName;
        this.lignesAnonymisees = lignesAnonymisees;
        this.contenuAnonymise = contenuAnonymise;
        this.statistiques = new AnonymisationStatistiques(lignesAnonymisees);
        this.success = true;
    }
    
    public AnonymisationResponse(String fileName, String erreur) {
        this.fileName = fileName;
        this.erreur = erreur;
        this.success = false;
    }
    
    // Getters et setters
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public List<LigneAnonymisee> getLignesAnonymisees() {
        return lignesAnonymisees;
    }
    
    public void setLignesAnonymisees(List<LigneAnonymisee> lignesAnonymisees) {
        this.lignesAnonymisees = lignesAnonymisees;
    }
    
    public String getContenuAnonymise() {
        return contenuAnonymise;
    }
    
    public void setContenuAnonymise(String contenuAnonymise) {
        this.contenuAnonymise = contenuAnonymise;
    }
    
    public AnonymisationStatistiques getStatistiques() {
        return statistiques;
    }
    
    public void setStatistiques(AnonymisationStatistiques statistiques) {
        this.statistiques = statistiques;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErreur() {
        return erreur;
    }
    
    public void setErreur(String erreur) {
        this.erreur = erreur;
    }
    
    @Override
    public String toString() {
        return "AnonymisationResponse{" +
                "fileName='" + fileName + '\'' +
                ", lignesCount=" + (lignesAnonymisees != null ? lignesAnonymisees.size() : 0) +
                ", success=" + success +
                '}';
    }
}
