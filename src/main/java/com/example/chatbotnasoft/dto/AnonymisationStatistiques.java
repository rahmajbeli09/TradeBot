package com.example.chatbotnasoft.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO pour représenter les statistiques d'anonymisation
 */
public class AnonymisationStatistiques {
    
    /**
     * Nombre total de lignes traitées
     */
    private int totalLignes;
    
    /**
     * Nombre de lignes anonymisées avec succès
     */
    private int lignesAnonymisees;
    
    /**
     * Nombre de lignes avec erreur
     */
    private int lignesEnErreur;
    
    /**
     * Répartition des types de champs par type
     */
    private Map<TypeChamp, Integer> repartitionTypesChamps;
    
    /**
     * Nombre total de champs traités
     */
    private int totalChampsTraites;
    
    /**
     * Nombre de champs anonymisés (modifiés)
     */
    private int champsAnonymises;
    
    /**
     * Types de msgType rencontrés
     */
    private java.util.Set<String> msgTypesRencontres;
    
    public AnonymisationStatistiques() {}
    
    public AnonymisationStatistiques(List<LigneAnonymisee> lignes) {
        this.totalLignes = lignes.size();
        this.msgTypesRencontres = new java.util.HashSet<>();
        this.repartitionTypesChamps = new java.util.HashMap<>();
        
        int lignesOK = 0;
        int lignesErreur = 0;
        int totalChamps = 0;
        int champsAnonymises = 0;
        
        for (LigneAnonymisee ligne : lignes) {
            if (ligne.isAnonymisationReussie()) {
                lignesOK++;
                
                // Compter les msgType
                if (ligne.getMsgType() != null) {
                    msgTypesRencontres.add(ligne.getMsgType());
                }
                
                // Analyser les champs
                if (ligne.getChamps() != null) {
                    totalChamps += ligne.getChamps().size();
                    
                    for (ChampAnonymise champ : ligne.getChamps()) {
                        TypeChamp type = champ.getTypeChamp();
                        repartitionTypesChamps.put(type, 
                            repartitionTypesChamps.getOrDefault(type, 0) + 1);
                        
                        // Compter les champs anonymisés (ceux dont la valeur a été modifiée)
                        if (!champ.getValeurOriginale().equals(champ.getValeurAnonymisee())) {
                            champsAnonymises++;
                        }
                    }
                }
            } else {
                lignesErreur++;
            }
        }
        
        this.lignesAnonymisees = lignesOK;
        this.lignesEnErreur = lignesErreur;
        this.totalChampsTraites = totalChamps;
    }
    
    // Getters et setters
    public int getTotalLignes() {
        return totalLignes;
    }
    
    public void setTotalLignes(int totalLignes) {
        this.totalLignes = totalLignes;
    }
    
    public int getLignesAnonymisees() {
        return lignesAnonymisees;
    }
    
    public void setLignesAnonymisees(int lignesAnonymisees) {
        this.lignesAnonymisees = lignesAnonymisees;
    }
    
    public int getLignesEnErreur() {
        return lignesEnErreur;
    }
    
    public void setLignesEnErreur(int lignesEnErreur) {
        this.lignesEnErreur = lignesEnErreur;
    }
    
    public Map<TypeChamp, Integer> getRepartitionTypesChamps() {
        return repartitionTypesChamps;
    }
    
    public void setRepartitionTypesChamps(Map<TypeChamp, Integer> repartitionTypesChamps) {
        this.repartitionTypesChamps = repartitionTypesChamps;
    }
    
    public int getTotalChampsTraites() {
        return totalChampsTraites;
    }
    
    public void setTotalChampsTraites(int totalChampsTraites) {
        this.totalChampsTraites = totalChampsTraites;
    }
    
    public int getChampsAnonymises() {
        return champsAnonymises;
    }
    
    public void setChampsAnonymises(int champsAnonymises) {
        this.champsAnonymises = champsAnonymises;
    }
    
    public java.util.Set<String> getMsgTypesRencontres() {
        return msgTypesRencontres;
    }
    
    public void setMsgTypesRencontres(java.util.Set<String> msgTypesRencontres) {
        this.msgTypesRencontres = msgTypesRencontres;
    }
    
    /**
     * Calcule le pourcentage de lignes anonymisées avec succès
     */
    public double getTauxSuccesAnonymisation() {
        if (totalLignes == 0) return 0.0;
        return (lignesAnonymisees * 100.0) / totalLignes;
    }
    
    /**
     * Calcule le pourcentage de champs anonymisés
     */
    public double getTauxChampsAnonymises() {
        if (totalChampsTraites == 0) return 0.0;
        return (champsAnonymises * 100.0) / totalChampsTraites;
    }
    
    @Override
    public String toString() {
        return "AnonymisationStatistiques{" +
                "totalLignes=" + totalLignes +
                ", lignesAnonymisees=" + lignesAnonymisees +
                ", lignesEnErreur=" + lignesEnErreur +
                ", totalChampsTraites=" + totalChampsTraites +
                ", champsAnonymises=" + champsAnonymises +
                ", msgTypesRencontres=" + msgTypesRencontres.size() +
                '}';
    }
}
