package org.example.casjwtdemo.controller;

import org.example.casjwtdemo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class WebController {

    @Autowired
    private JwtService jwtService;

    @Value("${cas.login.url}")
    private String casLoginUrl;

    @Value("${cas.server.url}")
    private String casServerUrl;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        // Check if user is already authenticated
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");

        if (authenticated != null && authenticated) {
            System.out.println("User already authenticated, redirecting to dashboard");
            return "redirect:/dashboard";
        }

        // User not authenticated, redirect to CAS login
        System.out.println("User not authenticated, redirecting to CAS login");
        return "redirect:" + casLoginUrl;
    }

    @GetMapping("/login/cas")
    public String casCallback(@RequestParam(value = "ticket", required = false) String ticket,
                             HttpSession session, Model model) {
        if (ticket == null) {
            model.addAttribute("error", "No ticket received from CAS");
            return "error";
        }

        try {
            // Step 1: Validate JWT ticket (one time only)
            Map<String, Object> jwtData = jwtService.decodeJwt(ticket);
            System.out.println("JWT validation successful for session: " + session.getId());

            // Step 2: Set authentication in session (this handles all future requests)
            session.setAttribute("authenticated", true);
            session.setAttribute("loginTime", java.time.LocalDateTime.now());

            // Step 3: Store JWT data ONLY for display purposes
            session.setAttribute("jwtData", jwtData);
            session.setAttribute("originalJwtTicket", ticket); // Keep original intact

            System.out.println("User authenticated and session established");

            // Redirect to dashboard
            return "redirect:/dashboard";
        } catch (Exception e) {
            System.out.println("JWT validation failed: " + e.getMessage());
            model.addAttribute("error", "Failed to process CAS ticket: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // Check ONLY session authentication (no JWT re-validation)
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");

        if (authenticated == null || !authenticated) {
            System.out.println("User not authenticated, redirecting to login");
            return "redirect:/login";
        }

        // User is authenticated via session, get JWT data for display only
        Map<String, Object> jwtData = (Map<String, Object>) session.getAttribute("jwtData");
        String originalJwt = (String) session.getAttribute("originalJwtTicket");

        model.addAttribute("jwtData", jwtData);
        model.addAttribute("originalJwt", originalJwt);

        // Add session info for debugging
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("sessionCreationTime", new java.util.Date(session.getCreationTime()));
        model.addAttribute("loginTime", session.getAttribute("loginTime"));

        System.out.println("Dashboard accessed via session authentication");
        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request) {
        System.out.println("Starting logout for session: " + session.getId());

        // Invalidate session first
        try {
            session.invalidate();
            System.out.println("Session invalidated successfully");
        } catch (IllegalStateException e) {
            System.out.println("Session was already invalidated");
        }

        // Redirect directly to CAS logout URL
        String casLogoutUrl = casServerUrl + "/logout";
        System.out.println("Redirecting to CAS logout: " + casLogoutUrl);
        return "redirect:" + casLogoutUrl;
    }
}
