package com.sdws.wallet.web;

import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WalletService service;

    @GetMapping
    public String overview(Model m) {
        var users = service.allUsers();
        var txs = service.allTransactions(null, null);
        m.addAttribute("userCount", users.size());
        m.addAttribute("activeCount", users.stream().filter(AppUser::isActive).count());
        m.addAttribute("txCount", txs.size());
        m.addAttribute("totalBalance", service.totalBalance());
        m.addAttribute("txs", txs.stream().limit(10).toList());
        return "admin-dashboard";
    }

    @GetMapping("/users")
    public String users(Model m) {
        m.addAttribute("users", service.allUsers());
        return "admin-users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(Authentication auth, @PathVariable Long id, RedirectAttributes ra) {
        try {
            service.toggleActive(service.current(auth.getName()), id);
            ra.addFlashAttribute("msg", "User status updated.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(required = false) String type,
                               @RequestParam(required = false) String phone, Model m) {
        m.addAttribute("txs", service.allTransactions(type, phone));
        m.addAttribute("type", type);
        m.addAttribute("phone", phone);
        return "admin-transactions";
    }
}
