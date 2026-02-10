package com.example.chatbotnasoft.entity;

public enum MappingStatus {
    VALIDE("Validé"),
    A_VERIFIER("À vérifier"),
    INCOMPLET("Incomplet");

    private final String label;

    MappingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
