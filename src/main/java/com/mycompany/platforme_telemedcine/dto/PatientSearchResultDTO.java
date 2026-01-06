package com.mycompany.platforme_telemedcine.dto;

import java.time.LocalDate;
import java.util.Date;

public class PatientSearchResultDTO {
    private Long id;
    private String name;
    private String email;
    private Date lastAppointment;
    private LocalDate nextAppointment;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Date getLastAppointment() { return lastAppointment; }
    public void setLastAppointment(Date lastAppointment) { this.lastAppointment = lastAppointment; }
    public LocalDate getNextAppointment() { return nextAppointment; }
    public void setNextAppointment(LocalDate nextAppointment) { this.nextAppointment = nextAppointment; }
}