package com.example.chatbotnasoft.dto;

/**
 * DTO pour représenter les détails d'un champ FEED
 */
public class ChampDetail {
    
    /**
     * Nom du champ (ex: "Champ 1", "Champ 2")
     */
    private String champ;
    
    /**
     * Signification du champ (ou "Inconnu" si non trouvée)
     */
    private String signification;
    
    /**
     * Valeur brute du champ
     */
    private String valeur;
    
    /**
     * Valeur anonymisée/transformation du champ
     */
    private String valeurAnonymisee;
    
    public ChampDetail() {}
    
    public ChampDetail(String champ, String signification, String valeur, String valeurAnonymisee) {
        this.champ = champ;
        this.signification = signification;
        this.valeur = valeur;
        this.valeurAnonymisee = valeurAnonymisee;
    }
    
    // Getters et setters
    public String getChamp() {
        return champ;
    }
    
    public void setChamp(String champ) {
        this.champ = champ;
    }
    
    public String getSignification() {
        return signification;
    }
    
    public void setSignification(String signification) {
        this.signification = signification;
    }
    
    public String getValeur() {
        return valeur;
    }
    
    public void setValeur(String valeur) {
        this.valeur = valeur;
    }
    
    public String getValeurAnonymisee() {
        return valeurAnonymisee;
    }
    
    public void setValeurAnonymisee(String valeurAnonymisee) {
        this.valeurAnonymisee = valeurAnonymisee;
    }
    
    /**
     * Vérifie si le champ a une signification connue
     */
    public boolean hasSignificationConnue() {
        return signification != null && !"Inconnu".equals(signification);
    }
    
    /**
     * Vérifie si la valeur est sensible (doit être anonymisée)
     */
    public boolean isValeurSensible() {
        if (valeur == null || valeur.trim().isEmpty()) {
            return false;
        }
        
        // Critères de sensibilité
        String upperValeur = valeur.toUpperCase().trim();
        
        // Références uniques (longues chaînes alphanumériques)
        if (upperValeur.matches("[A-Z0-9]{8,}")) {
            return true;
        }
        
        // Noms propres (commencent par majuscule suivi de lettres)
        if (upperValeur.matches("^[A-Z][A-Z]{2,}$")) {
            return true;
        }
        
        // Numéros de téléphone ou identifiants personnels
        if (upperValeur.matches("\\d{8,}")) {
            return true;
        }
        
        // Codes spécifiques sensibles
        if (upperValeur.contains("ID") || upperValeur.contains("PASS") || 
            upperValeur.contains("SECRET") || upperValeur.contains("KEY")) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "ChampDetail{" +
                "champ='" + champ + '\'' +
                ", signification='" + signification + '\'' +
                ", valeur='" + valeur + '\'' +
                ", valeurAnonymisee='" + valeurAnonymisee + '\'' +
                ", sensible=" + isValeurSensible() +
                '}';
    }
}
