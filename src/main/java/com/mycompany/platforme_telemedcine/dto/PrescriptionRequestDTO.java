package com.mycompany.platforme_telemedcine.dto;

import java.util.List;

public class PrescriptionRequestDTO {
    private List<String> medicaments;
    private String instructions;
    private boolean aiValidated;

    // Getters and setters
    public List<String> getMedicaments() {
        return medicaments;
    }

    public void setMedicaments(List<String> medicaments) {
        this.medicaments = medicaments;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public boolean isAiValidated() {
        return aiValidated;
    }

    public void setAiValidated(boolean aiValidated) {
        this.aiValidated = aiValidated;
    }
}