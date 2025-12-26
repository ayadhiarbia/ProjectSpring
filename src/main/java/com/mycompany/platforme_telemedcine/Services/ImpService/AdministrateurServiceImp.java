package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.Administrateur;
import com.mycompany.platforme_telemedcine.Repository.AdministrateurRepository;
import com.mycompany.platforme_telemedcine.Services.AdministrateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdministrateurServiceImp implements AdministrateurService {
    @Autowired
    private AdministrateurRepository administrateurRepository;

    @Override
    public Administrateur createAdministrateur(Administrateur administrateur) {
        return administrateurRepository.save(administrateur);
    }

    @Override
    public Administrateur updateAdministrateur(Administrateur administrateur) {
        return administrateurRepository.save(administrateur);
    }

    @Override
    public Administrateur getAdministrateurById(Long id) {
        return administrateurRepository.findById(id).get();
    }

    @Override
    public void deleteAdministrateurById(Long id) {
        this.administrateurRepository.deleteById(id);
    }

    @Override
    public Administrateur getAdminByEmail(String email) {
        return administrateurRepository.findByEmail(email);

    }


}
