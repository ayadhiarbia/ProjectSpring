package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.Consultation;
import com.mycompany.platforme_telemedcine.Repository.ConsultationRepository;
import com.mycompany.platforme_telemedcine.Services.ConsultationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsultationServiceImp implements ConsultationService {

    @Autowired
    private ConsultationRepository consultationRepository;

    @Override
    public Consultation createConsultation(Consultation consultation) {
        return consultationRepository.save(consultation);
    }

    @Override
    public Consultation getConsultationById(Long id) {
        return consultationRepository.findConsultationById(id);
    }

    @Override
    public Consultation updateConsultation(Consultation consultation) {
        return consultationRepository.save(consultation);
    }

    @Override
    public void deleteConsultation(Long id) {
        consultationRepository.deleteById(id);
    }

    @Override
    public List<Consultation> getAllConsultations() {
        return consultationRepository.findAll();
    }

    // Add these additional methods for consultation controller
    public List<Consultation> getConsultationsByPatientId(Long patientId) {
        return consultationRepository.findByRendezVousPatientId(patientId);
    }

    public List<Consultation> getActiveConsultations() {
        return consultationRepository.findByIsActiveTrue();
    }

    public List<Consultation> getActiveConsultationsByPatientId(Long patientId) {
        return consultationRepository.findByRendezVousPatientIdAndIsActiveTrue(patientId);
    }
}