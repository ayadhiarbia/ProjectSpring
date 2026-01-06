package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.Ordonance;
import com.mycompany.platforme_telemedcine.Repository.OrdonanceRepository;
import com.mycompany.platforme_telemedcine.Services.OrdonanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrdonanceServiceImp implements OrdonanceService {

    @Autowired
    private OrdonanceRepository ordonanceRepository;

    @Override
    public Ordonance createOrdonance(Ordonance ord) {
        return ordonanceRepository.save(ord);
    }

    @Override
    public Ordonance findOrdonanceById(Long id) {
        Optional<Ordonance> ordonance = ordonanceRepository.findById(id);
        return ordonance.orElse(null);
    }

    @Override
    public Ordonance getOrdonanceById(Long id) {
        // This can just call findOrdonanceById since they do the same thing
        return findOrdonanceById(id);
    }

    @Override
    public Ordonance updateOrdonance(Ordonance ord) {
        if (ord.getId() != null && ordonanceRepository.existsById(ord.getId())) {
            return ordonanceRepository.save(ord);
        }
        return null;
    }

    @Override
    public void deleteOrdonance(Long id) {
        ordonanceRepository.deleteById(id);
    }

    @Override
    public List<Ordonance> getAllOrdonance() {
        return ordonanceRepository.findAll();
    }
}