package com.mycompany.platforme_telemedcine.dto;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class MedicalSummaryDTO {
    private String patientName;
    private int age;
    private String bloodType;
    private List<String> allergies;
    private List<String> chronicConditions;
    private List<String> currentMedications;
    private Date lastConsultation;
    private LocalDate nextAppointment;

    // Getters and setters
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }
    public List<String> getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(List<String> chronicConditions) { this.chronicConditions = chronicConditions; }
    public List<String> getCurrentMedications() { return currentMedications; }
    public void setCurrentMedications(List<String> currentMedications) { this.currentMedications = currentMedications; }
    public Date getLastConsultation() { return lastConsultation; }
    public void setLastConsultation(Date lastConsultation) { this.lastConsultation = lastConsultation; }
    public LocalDate getNextAppointment() { return nextAppointment; }
    public void setNextAppointment(LocalDate nextAppointment) { this.nextAppointment = nextAppointment; }
}