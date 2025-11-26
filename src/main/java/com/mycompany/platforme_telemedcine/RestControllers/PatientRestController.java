package com.mycompany.platforme_telemedcine.RestControllers;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.mycompany.platforme_telemedcine.Models.Patient;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Services.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientRestController {

    @Autowired
    PatientService patientService;

    @PostMapping
    public ResponseEntity<?> addPatient(@RequestBody Map<String, Object> requestBody) {
        try {
            if (requestBody == null) {
                return new ResponseEntity<>("Request body is required", HttpStatus.BAD_REQUEST);
            }

            Patient patient = new Patient();


            patient.setName(requestBody.get("name") != null ? requestBody.get("name").toString() : null);
            patient.setPrenom(requestBody.get("prenom") != null ? requestBody.get("prenom").toString() : null);
            patient.setEmail(requestBody.get("email") != null ? requestBody.get("email").toString() : null);
            patient.setPassword(requestBody.get("password") != null ? requestBody.get("password").toString() : null);
            patient.setRole(UserRole.PATIENT);


            if (requestBody.get("dateNaissance") != null) {
                try {
                    String dateStr = requestBody.get("dateNaissance").toString();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date dateNaissance = sdf.parse(dateStr);
                    patient.setDataNaissance(dateNaissance);
                } catch (ParseException ignored) {}
            }

            if (requestBody.get("adresse") != null) {
                patient.setAdresse(requestBody.get("adresse").toString());
            }

            if (requestBody.get("antecedentsMedicaux") != null) {
                patient.setAntecedentsMedicaux(requestBody.get("antecedentsMedicaux").toString());
            }

            Patient savedPatient = patientService.createPatient(patient);

            return new ResponseEntity<>(savedPatient, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error creating patient: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        List<Patient> patients = patientService.getAllPatients();
        if(patients.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(patients, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        Patient p1 = patientService.getPatientById(id);
        if(p1 != null) {
            return new ResponseEntity<>(p1, HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable Long id, @RequestBody Patient patient) {
        Patient p1 = patientService.getPatientById(id);
        if(p1 != null) {
            patient.setId(id);
            patientService.updatePatient(patient);
            return new ResponseEntity<>(patient, HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Patient> deletePatient(@PathVariable Long id) {
        try{
            patientService.deletePatientById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
