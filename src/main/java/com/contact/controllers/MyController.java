package com.contact.controllers;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.contact.dao.UserRepository;
import com.contact.entities.User;
import com.contact.helper.MessageHelper;
import com.contact.services.EmailService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class MyController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;

    @RequestMapping("/")
    public String handle(Model m) {
        m.addAttribute("title", "Home - Smart Contact Manager");
        m.addAttribute("currentPage", "home");
        return "home";
    }

    @RequestMapping("/about")
    public String about(Model m) {
        m.addAttribute("title", "About - Smart Contact Manager");
        m.addAttribute("currentPage", "about");
        return "about";
    }

    @RequestMapping("/signup")
    public String signup(Model m, HttpSession session) {
        m.addAttribute("title", "Register - Smart Contact Manager");
        m.addAttribute("currentPage", "signup");
        m.addAttribute("user", new User());

        MessageHelper message = (MessageHelper) session.getAttribute("message");
        if (message != null) {
            m.addAttribute("message", message);
            session.removeAttribute("message");
        }

        return "signup";
    }

    @PostMapping("/do_register")
    public String register(@Valid @ModelAttribute("user") User user,
                           BindingResult result,
                           @RequestParam(value = "agreement", defaultValue = "false") boolean agreement,
                           Model m,
                           HttpSession session) {
        try {
            if (!agreement) {
                throw new Exception("You haven't agreed to the terms and conditions.");
            }

            if (result.hasErrors()) {
                m.addAttribute("user", user);
                m.addAttribute("currentPage", "signup");
                return "signup";
            }

            user.setRole("ROLE_USER");
            user.setEnabled(true);
            user.setImageUrl("default.png");
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            this.userRepository.save(user);

            m.addAttribute("user", new User());
            session.setAttribute("message", new MessageHelper("Successfully registered user", "alert-success"));
            m.addAttribute("currentPage", "signup");
            return "signup";

        } catch (DataIntegrityViolationException e) {
            session.setAttribute("message", new MessageHelper("User already exists with this email!", "alert-danger"));
            m.addAttribute("currentPage", "signup");
            return "signup";
        } catch (Exception e) {
            m.addAttribute("user", user);
            session.setAttribute("message", new MessageHelper("Something went wrong: " + e.getMessage(), "alert-danger"));
            m.addAttribute("currentPage", "signup");
            return "signup";
        }
    }

    @GetMapping("/signin")
    public String login(Model m) {
        m.addAttribute("title", "Sign In - Smart Contact Manager");
        m.addAttribute("currentPage", "signin");
        return "login";
    }
    
    @GetMapping("/forgotpassword")
    public String forgotpassword() {
    	return "forgotpassword";
    }
    @PostMapping("/send-otp")
    public String sendOTP(@RequestParam("email") String email,HttpSession session) {
    	
    	//generating otp of 4 digit
    	Random random = new Random();
    	int otp = random.nextInt(9999);
    	System.out.println("OTP: "+otp);
    	String subject = "OTP Veification";
    	String message = " OTP: "+otp;
    	String to = email;
    	boolean sendEmail = this.emailService.sendEmail(subject, message, to);
    	if (sendEmail) {
    		session.setAttribute("myotp", otp);
    		session.setAttribute("email", email);
    		return "verify_otp";
		}else {
			return "forgotpassword";
		}
    	
    	
    }
    
    @PostMapping("/verify-otp")
    public String verifyOTP(@RequestParam("otp") Integer otp,
                            HttpSession session,
                            Model model) {
        Integer myOTP = (int) session.getAttribute("myotp");
        String email = (String) session.getAttribute("email");

        if (myOTP == null || !myOTP.equals(otp)) {
            model.addAttribute("error", "Invalid OTP. Please try again.");
            return "verify_otp"; // reload same page with error
        }

        User user = this.userRepository.getUserByName(email);
        if (user == null) {
            model.addAttribute("error", "No user found with this email.");
            return "verify_otp"; // go back to forgot page with error
        }

        // OTP is correct & user exists
        return "reset_pass";
    }

    
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                HttpSession session,
                                Model model) {

        String email = (String) session.getAttribute("email");

        if (email == null) {
            model.addAttribute("error", "Session expired. Please try again.");
            return "forgotpassword";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "reset_pass"; // show reset form again with error
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long!");
            return "reset_pass";
        }

        User user = this.userRepository.getUserByName(email);
        if (user == null) {
            model.addAttribute("error", "User not found!");
            return "forgotpassword";
        }

        // update password
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // clear session attributes (optional but safe)
        session.removeAttribute("myotp");
        session.removeAttribute("email");

        model.addAttribute("message", "Password reset successfully! Please login.");
        return "login"; // redirect to login page
    }

}
