package com.mycompany.platforme_telemedcine.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "pending", required = false) String pending,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password!");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully!");
        }
        if (pending != null) {
            model.addAttribute("infoMessage", "Your account is pending or rejected. Please contact an administrator.");
        }
        return "login";
    }


}

