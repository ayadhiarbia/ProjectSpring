package com.mycompany.platforme_telemedcine.RestControllers;

import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Models.Patient;
import com.mycompany.platforme_telemedcine.Models.Medecin;
import com.mycompany.platforme_telemedcine.Services.UserService;
import com.mycompany.platforme_telemedcine.Services.PatientService;
import com.mycompany.platforme_telemedcine.Services.MedecinService;
import com.mycompany.platforme_telemedcine.Services.AdministrateurService;
import com.mycompany.platforme_telemedcine.Models.Administrateur;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private MedecinService medecinService;

    @Autowired
    private AdministrateurService administrateurService;

    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody Map<String, Object> requestBody) {
        try {
            if (requestBody == null || !requestBody.containsKey("role")) {
                return new ResponseEntity<>("Role is required", HttpStatus.BAD_REQUEST);
            }

            String roleStr = requestBody.get("role").toString();
            UserRole role;
            try {
                role = UserRole.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>("Invalid role. Must be PATIENT or MEDECIN", HttpStatus.BAD_REQUEST);
            }

            User savedUser = null;

            if (role == UserRole.PATIENT) {
                Patient patient = new Patient();
                patient.setName((String) requestBody.get("name"));
                patient.setPrenom((String) requestBody.get("prenom"));
                patient.setEmail((String) requestBody.get("email"));
                patient.setPassword((String) requestBody.get("password"));
                patient.setRole(UserRole.PATIENT);

                if (requestBody.get("dateNaissance") != null) {
                    try {
                        patient.setDataNaissance(java.sql.Date.valueOf(requestBody.get("dateNaissance").toString()));
                    } catch (Exception ignored) {}
                }

                patient.setAdresse((String) requestBody.get("adresse"));
                patient.setAntecedentsMedicaux((String) requestBody.get("antecedentsMedicaux"));

                savedUser = patientService.createPatient(patient);

            } else if (role == UserRole.MEDECIN) {
                Medecin medecin = new Medecin();
                medecin.setName((String) requestBody.get("name"));
                medecin.setPrenom((String) requestBody.get("prenom"));
                medecin.setEmail((String) requestBody.get("email"));
                medecin.setPassword((String) requestBody.get("password"));
                medecin.setRole(UserRole.MEDECIN);

                medecin.setSpecialte((String) requestBody.get("specialte"));
                medecin.setDisponibilite((String) requestBody.get("disponibilite"));

                savedUser = medecinService.createMedecin(medecin);
            }

            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error creating user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        System.out.println("Users found: " + users.size());
        if (users.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        try {
            User existingUser = userService.getUserById(id);
            if (existingUser == null) {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }

            UserRole newRole = existingUser.getRole();
            if (requestBody.containsKey("role")) {
                try {
                    newRole = UserRole.valueOf(requestBody.get("role").toString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return new ResponseEntity<>("Invalid role", HttpStatus.BAD_REQUEST);
                }
            }

            if (newRole != existingUser.getRole()) {
                if (existingUser.getRole() == UserRole.PATIENT) {
                    patientService.deletePatientById(id);
                } else if (existingUser.getRole() == UserRole.MEDECIN) {
                    medecinService.deleteMedecinById(id);
                } else if (existingUser.getRole() == UserRole.ADMIN) {
                    administrateurService.deleteAdministrateurById(id);
                }

                User savedUser = null;
                String name = requestBody.containsKey("name") ? requestBody.get("name").toString() : existingUser.getName();
                String prenom = requestBody.containsKey("prenom") ? requestBody.get("prenom").toString() : existingUser.getPrenom();
                String email = requestBody.containsKey("email") ? requestBody.get("email").toString() : existingUser.getEmail();
                String password = requestBody.containsKey("password") && !requestBody.get("password").toString().isEmpty()
                        ? requestBody.get("password").toString()
                        : existingUser.getPassword();

                if (newRole == UserRole.PATIENT) {
                    Patient patient = new Patient();
                    patient.setName(name);
                    patient.setPrenom(prenom);
                    patient.setEmail(email);
                    patient.setPassword(password);
                    patient.setRole(UserRole.PATIENT);
                    savedUser = patientService.createPatient(patient);
                } else if (newRole == UserRole.MEDECIN) {
                    Medecin medecin = new Medecin();
                    medecin.setName(name);
                    medecin.setPrenom(prenom);
                    medecin.setEmail(email);
                    medecin.setPassword(password);
                    medecin.setRole(UserRole.MEDECIN);
                    savedUser = medecinService.createMedecin(medecin);
                } else if (newRole == UserRole.ADMIN) {
                    Administrateur admin = new Administrateur();
                    admin.setName(name);
                    admin.setPrenom(prenom);
                    admin.setEmail(email);
                    admin.setPassword(password);
                    admin.setRole(UserRole.ADMIN);
                    savedUser = administrateurService.createAdministrateur(admin);
                }

                return new ResponseEntity<>(savedUser, HttpStatus.OK);
            }

            User updatedUser = null;
            if (existingUser.getRole() == UserRole.PATIENT) {
                Patient patient = patientService.getPatientById(id);
                if (patient != null) {
                    if (requestBody.containsKey("name")) patient.setName(requestBody.get("name").toString());
                    if (requestBody.containsKey("prenom")) patient.setPrenom(requestBody.get("prenom").toString());
                    if (requestBody.containsKey("email")) patient.setEmail(requestBody.get("email").toString());
                    if (requestBody.containsKey("password") && !requestBody.get("password").toString().isEmpty()) {
                        patient.setPassword(requestBody.get("password").toString());
                    }
                    updatedUser = patientService.updatePatient(patient);
                }
            } else if (existingUser.getRole() == UserRole.MEDECIN) {
                Medecin medecin = medecinService.getMedecinById(id);
                if (medecin != null) {
                    if (requestBody.containsKey("name")) medecin.setName(requestBody.get("name").toString());
                    if (requestBody.containsKey("prenom")) medecin.setPrenom(requestBody.get("prenom").toString());
                    if (requestBody.containsKey("email")) medecin.setEmail(requestBody.get("email").toString());
                    if (requestBody.containsKey("password") && !requestBody.get("password").toString().isEmpty()) {
                        medecin.setPassword(requestBody.get("password").toString());
                    }
                    updatedUser = medecinService.updateMedecin(medecin);
                }
            } else if (existingUser.getRole() == UserRole.ADMIN) {
                Administrateur admin = administrateurService.getAdministrateurById(id);
                if (admin != null) {
                    if (requestBody.containsKey("name")) admin.setName(requestBody.get("name").toString());
                    if (requestBody.containsKey("prenom")) admin.setPrenom(requestBody.get("prenom").toString());
                    if (requestBody.containsKey("email")) admin.setEmail(requestBody.get("email").toString());
                    if (requestBody.containsKey("password") && !requestBody.get("password").toString().isEmpty()) {
                        admin.setPassword(requestBody.get("password").toString());
                    }
                    updatedUser = administrateurService.updateAdministrateur(admin);
                }
            }

            if (updatedUser != null) {
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Error updating user", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error updating user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            List<User> users = userService.getUsersByRole(userRole);  // Changed to getUsersByRole
            if (users != null && !users.isEmpty()) {
                return new ResponseEntity<>(users, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);  // Changed to NO_CONTENT instead of NOT_FOUND
            }
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}