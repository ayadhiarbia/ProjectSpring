package com.mycompany.platforme_telemedcine.RestControllers;

import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Models.Patient;
import com.mycompany.platforme_telemedcine.Models.Medecin;
import com.mycompany.platforme_telemedcine.Services.UserService;
import com.mycompany.platforme_telemedcine.Services.PatientService;
import com.mycompany.platforme_telemedcine.Services.MedecinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private MedecinService medecinService;

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
                patient.setName(requestBody.get("name") != null ? requestBody.get("name").toString() : null);
                patient.setPrenom(requestBody.get("prenom") != null ? requestBody.get("prenom").toString() : null);
                patient.setEmail(requestBody.get("email") != null ? requestBody.get("email").toString() : null);
                patient.setPassword(requestBody.get("password") != null ? requestBody.get("password").toString() : null);
                patient.setRole(UserRole.PATIENT);
                savedUser = patientService.createPatient(patient);
            } else if (role == UserRole.MEDECIN) {
                Medecin medecin = new Medecin();
                medecin.setName(requestBody.get("name") != null ? requestBody.get("name").toString() : null);
                medecin.setPrenom(requestBody.get("prenom") != null ? requestBody.get("prenom").toString() : null);
                medecin.setEmail(requestBody.get("email") != null ? requestBody.get("email").toString() : null);
                medecin.setPassword(requestBody.get("password") != null ? requestBody.get("password").toString() : null);
                medecin.setRole(UserRole.MEDECIN);
                savedUser = medecinService.createMedecin(medecin);
            } else {
                return new ResponseEntity<>("Invalid role. Must be PATIENT or MEDECIN", HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User u = userService.getUserById(id);
        if (u != null) {
            user.setId(id);
            userService.updateUser(user);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
    public ResponseEntity<User> getUserByRole(@PathVariable String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            User user = userService.getUserByRole(userRole);
            if (user != null) {
                return new ResponseEntity<>(user, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
