package com.example.chatbotnasoft.dto;

/**
 * Énumération des types de champs FEED pour l'anonymisation
 */
public enum TypeChamp {
    
    /**
     * Champ de type date (format YYYYMMDD)
     */
    DATE("Date au format YYYYMMDD"),
    
    /**
     * Champ de type heure (format HHMMSS)
     */
    HEURE("Heure au format HHMMSS"),
    
    /**
     * Champ de type identifiant alphanumérique
     */
    IDENTIFIANT("Identifiant alphanumérique"),
    
    /**
     * Champ de type nombre (compte, volume, valeur)
     */
    NOMBRE("Nombre numérique"),
    
    /**
     * Champ de type code ou texte libre
     */
    CODE("Code ou texte libre"),
    
    /**
     * Champ vide
     */
    VIDE("Champ vide"),
    
    /**
     * Champ de type inconnu/non classifié
     */
    INCONNU("Type non déterminé");
    
    private final String description;
    
    TypeChamp(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
