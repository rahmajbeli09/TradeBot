package com.example.chatbotnasoft.dto;

/**
 * DTO pour représenter un champ FEED anonymisé
 */
public class ChampAnonymise {
    
    /**
     * Nom du champ (ex: "Champ 1", "Champ 2")
     */
    private String nomChamp;
    
    /**
     * Valeur originale du champ
     */
    private String valeurOriginale;
    
    /**
     * Valeur anonymisée du champ
     */
    private String valeurAnonymisee;
    
    /**
     * Type de champ détecté (DATE, HEURE, IDENTIFIANT, NOMBRE, CODE, VIDE)
     */
    private TypeChamp typeChamp;
    
    /**
     * Règle d'anonymisation appliquée
     */
    private String regleAppliquee;
    
    public ChampAnonymise() {}
    
    public ChampAnonymise(String nomChamp, String valeurOriginale, String valeurAnonymisee, 
                       TypeChamp typeChamp, String regleAppliquee) {
        this.nomChamp = nomChamp;
        this.valeurOriginale = valeurOriginale;
        this.valeurAnonymisee = valeurAnonymisee;
        this.typeChamp = typeChamp;
        this.regleAppliquee = regleAppliquee;
    }
    
    // Getters et setters
    public String getNomChamp() {
        return nomChamp;
    }
    
    public void setNomChamp(String nomChamp) {
        this.nomChamp = nomChamp;
    }
    
    public String getValeurOriginale() {
        return valeurOriginale;
    }
    
    public void setValeurOriginale(String valeurOriginale) {
        this.valeurOriginale = valeurOriginale;
    }
    
    public String getValeurAnonymisee() {
        return valeurAnonymisee;
    }
    
    public void setValeurAnonymisee(String valeurAnonymisee) {
        this.valeurAnonymisee = valeurAnonymisee;
    }
    
    public TypeChamp getTypeChamp() {
        return typeChamp;
    }
    
    public void setTypeChamp(TypeChamp typeChamp) {
        this.typeChamp = typeChamp;
    }
    
    public String getRegleAppliquee() {
        return regleAppliquee;
    }
    
    public void setRegleAppliquee(String regleAppliquee) {
        this.regleAppliquee = regleAppliquee;
    }
    
    @Override
    public String toString() {
        return "ChampAnonymise{" +
                "nomChamp='" + nomChamp + '\'' +
                ", valeurOriginale='" + valeurOriginale + '\'' +
                ", valeurAnonymisee='" + valeurAnonymisee + '\'' +
                ", typeChamp=" + typeChamp +
                ", regleAppliquee='" + regleAppliquee + '\'' +
                '}';
    }
}
