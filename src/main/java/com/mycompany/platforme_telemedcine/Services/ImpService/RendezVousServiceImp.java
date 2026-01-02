package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Repository.*;
import com.mycompany.platforme_telemedcine.Services.RendezVousService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RendezVousServiceImp implements RendezVousService {

    @Autowired
    private RendezVousRepository rendezVousRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedecinRepository medecinRepository;

    @Override
    public RendezVous createRendezvous(RendezVous rendezVous) {
        return rendezVousRepository.save(rendezVous);
    }

    @Override
    public List<RendezVous> getRendezVous() {
        return rendezVousRepository.findAll();
    }

    @Override
    public RendezVous getRendezVousById(Long id) {
        return rendezVousRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteRendezVous(Long id) {
        rendezVousRepository.deleteById(id);
    }

    @Override
    public RendezVous updateRendezVous(RendezVous rendezVous) {
        return rendezVousRepository.save(rendezVous);
    }

    @Override
    public List<RendezVous> getAllRendezVous() {
        return rendezVousRepository.findAll();
    }

    @Override
    public List<RendezVous> getByPatient(Long patientId) {
        List<Object[]> rawResults = rendezVousRepository.findRawAppointmentsByPatientId(patientId);
        List<RendezVous> appointments = new ArrayList<>();

        for (Object[] row : rawResults) {
            RendezVous rdv = new RendezVous();

            rdv.setId(((Number) row[0]).longValue());
            if (row[1] instanceof java.sql.Date sqlDate) {
                rdv.setDate(sqlDate.toLocalDate());
            }
            rdv.setTime((String) row[2]);

            try {
                rdv.setStatus(StatusRendezVous.valueOf((String) row[3]));
            } catch (IllegalArgumentException e) {
                rdv.setStatus(StatusRendezVous.PENDING);
            }

            rdv.setDescription((String) row[4]);

            Object patientObj = row[6];
            if (patientObj instanceof Number pidNum) {
                patientRepository.findById(pidNum.longValue()).ifPresent(rdv::setPatient);
            } else {
                System.out.println("Expected patient id at row[6], got: " + patientObj);
            }

            Object medecinObj = row[7];
            if (medecinObj instanceof Number didNum) {
                medecinRepository.findById(didNum.longValue()).ifPresent(rdv::setMedecin);
            } else {
                System.out.println("Expected medecin id at row[7], got: " + medecinObj);
            }


            appointments.add(rdv);
        }

        return appointments;
    }

    @Override
    public List<RendezVous> getRendezVousByMedecinId(Long medecinId) {
        return rendezVousRepository.findByMedecinId(medecinId);
    }
}
