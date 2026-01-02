package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.Patient;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/patient")
public class PatientViewController {

    // Helper method to check if user is logged in and add patient to model
    private boolean setupPatientModel(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return false;
        }
        model.addAttribute("patient", patient);
        return true;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/dashboard";
    }

   // @GetMapping("/appointments")
  //  public String appointments(HttpSession session, Model model) {
       // if (!setupPatientModel(session, model)) {
          //  return "redirect:/login";
       // }
       // return "patient/appointments";
    //}

  //  @GetMapping("/appointments/details")
   // public String appointmentDetails(HttpSession session, Model model) {
      //  if (!setupPatientModel(session, model)) {
        //    return "redirect:/login";
     //   }
      //  return "patient/appointments-details";  // Fixed to match your actual template name
   // }

    //@GetMapping("/chat")  // Changed from "/chat" to avoid conflict with ChatController
   // public String chat(HttpSession session, Model model) {
       // if (!setupPatientModel(session, model)) {
        //    return "redirect:/login";
       // }
      //  return "patient/chat";
   // }

    @GetMapping("/chat-room")
    public String chatRoom(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/chat-room";
    }

    @GetMapping("/consultation-history")
    public String consultationHistory(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/consultaion-history";  // Fixed to match your actual template name
    }

    @GetMapping("/consultation-details")
    public String consultationDetails(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/consultation-details";
    }

    @GetMapping("/consultation-room")
    public String consultationRoom(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/consultation-room";
    }

    @GetMapping("/document-details")
    public String documentDetails(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/document-details";
    }

    @GetMapping("/lab-results")
    public String labResults(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/lab-results";
    }

    @GetMapping("/medical-records")
    public String medicalRecords(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/medical-records";
    }

    @GetMapping("/medical-summary")
    public String medicalSummary(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/medical-summary";
    }

    @GetMapping("/prescriptions")
    public String prescriptions(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/prescriptions";
    }

    @GetMapping("/prescriptions-details")
    public String prescriptionsDetails(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/prescriptions-details";
    }

    @GetMapping("/vaccinations")
    public String vaccinations(HttpSession session, Model model) {
        if (!setupPatientModel(session, model)) {
            return "redirect:/login";
        }
        return "patient/vaccinations";
    }
}