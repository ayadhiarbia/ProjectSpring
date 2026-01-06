package com.mycompany.platforme_telemedcine.dto;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class PatientQuickViewDTO {
    private Long patientId;
    private String patientName;
    private int age;
    private Date lastVisit;
    private LocalDate nextAppointment;
    private List<String> recentPrescriptions;
    private List<String> allergies;
    private String error;

    // Getters and setters
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public Date getLastVisit() { return lastVisit; }
    public void setLastVisit(Date lastVisit) { this.lastVisit = lastVisit; }
    public LocalDate getNextAppointment() { return nextAppointment; }
    public void setNextAppointment(LocalDate nextAppointment) { this.nextAppointment = nextAppointment; }
    public List<String> getRecentPrescriptions() { return recentPrescriptions; }
    public void setRecentPrescriptions(List<String> recentPrescriptions) { this.recentPrescriptions = recentPrescriptions; }
    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}