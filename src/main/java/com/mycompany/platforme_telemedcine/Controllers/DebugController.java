package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Home page with all debug options
    @GetMapping("")
    public String debugHome() {
        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    ul { list-style-type: none; padding: 0; }
                    li { margin: 10px 0; }
                    a { 
                        display: block; 
                        padding: 10px; 
                        background: #007bff; 
                        color: white; 
                        text-decoration: none; 
                        border-radius: 5px;
                        width: 300px;
                    }
                    a:hover { background: #0056b3; }
                    .success { background: #28a745; }
                    .warning { background: #ffc107; color: #212529; }
                    .danger { background: #dc3545; }
                </style>
            </head>
            <body>
                <h2>🔧 Debug Tools</h2>
                <ul>
                    <li><a href="/debug/auth-test" class="success">🔐 Test Authentication Form</a></li>
                    <li><a href="/debug/users-db" class="">👥 View Database Users</a></li>
                    <li><a href="/debug/fix-roles" class="warning">⚙️ Fix Null Roles</a></li>
                    <li><a href="/debug/encode-all-passwords" class="danger">🔒 Encode Plain Passwords</a></li>
                    <li><a href="/debug/create-test-users" class="success">➕ Create Test Users</a></li>
                </ul>
            </body>
            </html>
            """;
    }

    @GetMapping("/auth-test")
    public String testAuthForm() {
        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; max-width: 500px; }
                    form { padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
                    input { width: 100%; padding: 8px; margin: 8px 0; box-sizing: border-box; }
                    input[type="submit"] { background: #007bff; color: white; border: none; cursor: pointer; }
                    input[type="submit"]:hover { background: #0056b3; }
                </style>
            </head>
            <body>
                <h2>🔐 Test Authentication</h2>
                <form method="post" action="/debug/auth-test">
                    <label>Email:</label><br>
                    <input type="text" name="email" value="admin@gmail.com"><br>
                    <label>Password:</label><br>
                    <input type="password" name="password" value="admin01"><br>
                    <input type="submit" value="Test Login">
                </form>
                <hr>
                <p><strong>Test Accounts:</strong></p>
                <ul>
                    <li>admin@gmail.com / admin01</li>
                    <li>patient@gmail.com / patient01</li>
                    <li>medecin@gmail.com / medecin01</li>
                </ul>
                <a href="/debug">← Back to Debug Menu</a>
            </body>
            </html>
            """;
    }

    @PostMapping("/auth-test")
    public Map<String, Object> testAuthentication(
            @RequestParam String email,
            @RequestParam String password) {

        Map<String, Object> result = new HashMap<>();
        result.put("email", email);
        result.put("password", password);

        try {
            // Check if user exists
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                result.put("error", "❌ User not found in database");
                return result;
            }

            result.put("userInDb", user.getEmail());
            result.put("dbRole", user.getRole() != null ? user.getRole().name() : "NULL");
            result.put("dbStatus", user.getStatus().name());
            result.put("dbPasswordHash", user.getPassword());
            result.put("passwordStartsWith", user.getPassword() != null ?
                    user.getPassword().substring(0, Math.min(10, user.getPassword().length())) : "N/A");

            // Check if password needs encoding
            String dbPassword = user.getPassword();
            boolean isEncoded = dbPassword != null &&
                    (dbPassword.startsWith("$2a$") || dbPassword.startsWith("$2b$"));
            result.put("passwordEncoded", isEncoded);

            if (!isEncoded) {
                result.put("warning", "⚠️ Password is NOT encoded! Use /debug/encode-all-passwords");
            }

            // Try to authenticate
            Authentication authRequest = new UsernamePasswordAuthenticationToken(email, password);
            Authentication authResult = authenticationProvider.authenticate(authRequest);

            result.put("success", true);
            result.put("authenticated", authResult.isAuthenticated());
            result.put("principal", authResult.getPrincipal().toString());
            result.put("authorities", authResult.getAuthorities().toString());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "❌ " + e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return result;
    }

    @GetMapping("/users-db")
    public Map<String, Object> getUsers() {
        List<User> users = userRepository.findAll();
        Map<String, Object> result = new HashMap<>();

        result.put("totalUsers", users.size());

        for (User user : users) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("name", user.getName());
            userInfo.put("role", user.getRole() != null ? user.getRole().name() : "NULL");
            userInfo.put("status", user.getStatus().name());
            userInfo.put("passwordLength", user.getPassword() != null ? user.getPassword().length() : 0);

            String password = user.getPassword();
            boolean isEncoded = password != null &&
                    (password.startsWith("$2a$") || password.startsWith("$2b$"));
            userInfo.put("passwordEncoded", isEncoded);
            userInfo.put("passwordPreview", password != null ?
                    password.substring(0, Math.min(30, password.length())) + (password.length() > 30 ? "..." : "") : "null");

            result.put(user.getEmail(), userInfo);
        }

        return result;
    }

    @GetMapping("/fix-roles")
    public String fixRoles() {
        List<User> users = userRepository.findAll();
        int fixed = 0;
        StringBuilder result = new StringBuilder();

        result.append("<html><body>");
        result.append("<h2>🔧 Fixing Null Roles</h2>");
        result.append("<ul>");

        for (User user : users) {
            result.append("<li>").append(user.getEmail()).append(": ");

            if (user.getRole() == null) {
                UserRole newRole;
                String emailLower = user.getEmail().toLowerCase();

                if (emailLower.contains("admin")) {
                    newRole = UserRole.ADMIN;
                } else if (emailLower.contains("medecin") || emailLower.contains("doctor")) {
                    newRole = UserRole.MEDECIN;
                } else if (emailLower.contains("patient")) {
                    newRole = UserRole.PATIENT;
                } else {
                    newRole = UserRole.PATIENT; // Default
                }

                user.setRole(newRole);
                userRepository.save(user);
                fixed++;

                result.append("Set role to ").append(newRole.name());
            } else {
                result.append("Already has role: ").append(user.getRole().name());
            }
            result.append("</li>");
        }

        result.append("</ul>");
        result.append("<p><strong>Fixed ").append(fixed).append(" users</strong></p>");
        result.append("<a href='/debug/users-db'>View Updated Users</a><br>");
        result.append("<a href='/debug'>← Back to Debug Menu</a>");
        result.append("</body></html>");

        return result.toString();
    }

    @GetMapping("/encode-all-passwords")
    public String encodeAllPasswords() {
        List<User> users = userRepository.findAll();
        int encoded = 0;
        StringBuilder result = new StringBuilder();

        result.append("<html><body>");
        result.append("<h2>🔒 Encoding Passwords</h2>");
        result.append("<ul>");

        for (User user : users) {
            String currentPassword = user.getPassword();
            result.append("<li><strong>").append(user.getEmail()).append("</strong>: ");

            // Check if already encoded
            if (currentPassword != null &&
                    (currentPassword.startsWith("$2a$") || currentPassword.startsWith("$2b$"))) {
                result.append("<span style='color: green;'>Already encoded (BCrypt)</span>");
            }
            // Check if it's a common plain password
            else if (currentPassword != null &&
                    (currentPassword.equals("admin01") ||
                            currentPassword.equals("patient01") ||
                            currentPassword.equals("medecin01"))) {

                // Determine which plain password it is
                String plainPassword = "";
                if (currentPassword.equals("admin01")) plainPassword = "admin01";
                else if (currentPassword.equals("patient01")) plainPassword = "patient01";
                else if (currentPassword.equals("medecin01")) plainPassword = "medecin01";

                String encodedPassword = passwordEncoder.encode(plainPassword);
                user.setPassword(encodedPassword);
                userRepository.save(user);
                encoded++;

                result.append("<span style='color: orange;'>Encoded '").append(plainPassword).append("' to:</span><br>");
                result.append("<code style='font-size: 0.8em;'>").append(encodedPassword.substring(0, 30)).append("...</code>");
            }
            else if (currentPassword != null && currentPassword.length() < 60) {
                // If password is short (likely plain text), encode it
                String encodedPassword = passwordEncoder.encode(currentPassword);
                user.setPassword(encodedPassword);
                userRepository.save(user);
                encoded++;

                result.append("<span style='color: orange;'>Encoded plain text to:</span><br>");
                result.append("<code style='font-size: 0.8em;'>").append(encodedPassword.substring(0, 30)).append("...</code>");
            }
            else {
                result.append("<span style='color: gray;'>Unknown format: ").append(
                        currentPassword != null ? currentPassword.substring(0, Math.min(20, currentPassword.length())) : "null"
                ).append("</span>");
            }
            result.append("</li>");
        }

        result.append("</ul>");
        result.append("<p><strong>Total encoded: ").append(encoded).append(" passwords</strong></p>");

        if (encoded > 0) {
            result.append("<p style='color: green;'>✅ Passwords have been encoded. You can now log in!</p>");
        }

        result.append("<a href='/debug/users-db'>View Updated Users</a><br>");
        result.append("<a href='/debug/auth-test'>Test Authentication</a><br>");
        result.append("<a href='/debug'>← Back to Debug Menu</a>");
        result.append("</body></html>");

        return result.toString();
    }

    @GetMapping("/create-test-users")
    public String createTestUsers() {
        StringBuilder result = new StringBuilder();
        result.append("<html><body>");
        result.append("<h2>➕ Create Test Users</h2>");

        // Common passwords to try
        String[] commonPasswords = {"admin01", "admin123", "password", "123456"};

        for (String pass : commonPasswords) {
            result.append("<h3>Trying password: ").append(pass).append("</h3>");

            // Try to create/update users with this password
            try {
                createOrUpdateUser("admin@gmail.com", "Admin User", pass, UserRole.ADMIN);
                createOrUpdateUser("patient@gmail.com", "Patient User", pass, UserRole.PATIENT);
                createOrUpdateUser("medecin@gmail.com", "Medecin User", pass, UserRole.MEDECIN);

                result.append("<p style='color: green;'>✅ Created users with password: ").append(pass).append("</p>");
                result.append("<a href='/debug/auth-test?email=admin@gmail.com&password=").append(pass).append("'>Test login</a><br>");
                break; // Stop after first successful creation

            } catch (Exception e) {
                result.append("<p style='color: orange;'>⚠️ Failed with password: ").append(pass).append(" - ").append(e.getMessage()).append("</p>");
            }
        }

        result.append("<hr>");
        result.append("<a href='/debug/users-db'>View All Users</a><br>");
        result.append("<a href='/debug/encode-all-passwords'>Encode Passwords</a><br>");
        result.append("<a href='/debug'>← Back to Debug Menu</a>");
        result.append("</body></html>");

        return result.toString();
    }

    private void createOrUpdateUser(String email, String name, String plainPassword, UserRole role) {
        var existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Update existing
            User user = existingUser.get();
            user.setName(name);
            user.setPassword(passwordEncoder.encode(plainPassword));
            user.setRole(role);
            userRepository.save(user);
        } else {
            // Create new
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPassword(passwordEncoder.encode(plainPassword));
            user.setRole(role);
            userRepository.save(user);
        }
    }
}