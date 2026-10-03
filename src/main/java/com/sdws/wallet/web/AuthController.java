package com.sdws.wallet.web;

import com.sdws.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final WalletService service;

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String phone,
                           @RequestParam(required = false) String email, @RequestParam String password,
                           RedirectAttributes ra) {
        try {
            service.register(fullName, phone, email, password);
            ra.addFlashAttribute("msg", "Account created. Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}
