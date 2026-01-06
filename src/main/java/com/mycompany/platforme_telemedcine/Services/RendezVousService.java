package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.RendezVous;
import java.util.Collection;
import java.util.List;

public interface RendezVousService {
    RendezVous createRendezvous(RendezVous rendezVous);
    List<RendezVous> getRendezVous();
    RendezVous getRendezVousById(Long id);
    void deleteRendezVous(Long id);
    RendezVous updateRendezVous(RendezVous rendezVous);
    List<RendezVous> getAllRendezVous();
    List<RendezVous> getByPatient(Long patientId);
    List<RendezVous> getRendezVousByMedecinId(Long medecinId);

    // Add this method to match what the controller is calling
    Collection<Object> getRendezVousByMedecin(Long doctorId);
}