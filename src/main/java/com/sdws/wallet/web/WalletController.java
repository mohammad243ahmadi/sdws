package com.sdws.wallet.web;

import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.model.Enums.Role;
import com.sdws.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WalletController {

    private static final List<String> OPERATORS = List.of("Roshan", "AWCC", "Etisalat", "MTN", "Salaam");
    private static final List<String> BILLS = List.of("Electricity", "Water", "Internet", "TV", "Other");

    private final WalletService service;

    private AppUser me(Authentication auth) { return service.current(auth.getName()); }

    private String form(Model m, String title, String action, String hint, Field... fields) {
        m.addAttribute("title", title);
        m.addAttribute("action", action);
        m.addAttribute("hint", hint);
        m.addAttribute("fields", List.of(fields));
        return "form";
    }

    /** Runs an operation and redirects with a success or error message. */
    private String run(RedirectAttributes ra, String backTo, String okMsg, Runnable op) {
        try {
            op.run();
            ra.addFlashAttribute("msg", okMsg);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:" + backTo;
        }
    }

    @GetMapping("/")
    public String home() { return "redirect:/dashboard"; }

    // ---------- dashboard & wallet ----------
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model m) {
        AppUser u = me(auth);
        if (u.getRole() == Role.ADMIN) return "redirect:/admin";
        m.addAttribute("user", u);
        m.addAttribute("wallet", service.wallet(u));
        m.addAttribute("txs", service.history(u, null).stream().limit(5).toList());
        return "dashboard";
    }

    @GetMapping("/wallet")
    public String wallet(Authentication auth, Model m) {
        AppUser u = me(auth);
        m.addAttribute("user", u);
        m.addAttribute("wallet", service.wallet(u));
        return "wallet";
    }

    // ---------- send ----------
    @GetMapping("/send")
    public String sendForm(Model m) {
        return form(m, "Send Money", "/send", null,
                Field.text("phone", "Recipient phone number"), Field.amount("amount", "Amount (AFN)"),
                Field.text("note", "Note (optional)", null, false));
    }

    @PostMapping("/send")
    public String send(Authentication auth, @RequestParam String phone, @RequestParam BigDecimal amount,
                       @RequestParam(required = false) String note, RedirectAttributes ra) {
        AppUser u = me(auth);
        return run(ra, "/send", "Money sent successfully.", () -> service.send(u, phone, amount, note));
    }

    // ---------- receive ----------
    @GetMapping("/receive")
    public String receive(Authentication auth, Model m) {
        AppUser u = me(auth);
        m.addAttribute("user", u);
        m.addAttribute("wallet", service.wallet(u));
        m.addAttribute("txs", service.history(u, "RECEIVE").stream().limit(10).toList());
        return "receive";
    }

    // ---------- add / withdraw ----------
    @GetMapping("/add")
    public String addForm(Model m) {
        return form(m, "Add Money", "/add", "Simulation: the amount is added directly to your wallet.",
                Field.amount("amount", "Amount (AFN)"));
    }

    @PostMapping("/add")
    public String add(Authentication auth, @RequestParam BigDecimal amount, RedirectAttributes ra) {
        AppUser u = me(auth);
        return run(ra, "/add", "Money added to your wallet.", () -> service.add(u, amount));
    }

    @GetMapping("/withdraw")
    public String withdrawForm(Model m) {
        return form(m, "Withdraw", "/withdraw", null, Field.amount("amount", "Amount (AFN)"));
    }

    @PostMapping("/withdraw")
    public String withdraw(Authentication auth, @RequestParam BigDecimal amount, RedirectAttributes ra) {
        AppUser u = me(auth);
        return run(ra, "/withdraw", "Withdrawal completed.", () -> service.withdraw(u, amount));
    }

    // ---------- requests ----------
    @GetMapping("/request")
    public String requestForm(Model m) {
        return form(m, "Request Money", "/request", null,
                Field.text("phone", "Request from (phone number)"), Field.amount("amount", "Amount (AFN)"));
    }

    @PostMapping("/request")
    public String request(Authentication auth, @RequestParam String phone, @RequestParam BigDecimal amount,
                          RedirectAttributes ra) {
        AppUser u = me(auth);
        try {
            service.requestMoney(u, phone, amount);
            ra.addFlashAttribute("msg", "Request sent.");
            return "redirect:/requests";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/request";
        }
    }

    @GetMapping("/requests")
    public String requests(Authentication auth, Model m) {
        AppUser u = me(auth);
        m.addAttribute("sent", service.sentRequests(u));
        m.addAttribute("incoming", service.incomingRequests(u));
        return "requests";
    }

    @PostMapping("/requests/{id}/{action}")
    public String respond(Authentication auth, @PathVariable Long id, @PathVariable String action, RedirectAttributes ra) {
        AppUser u = me(auth);
        try {
            service.respond(u, id, "accept".equals(action));
            ra.addFlashAttribute("msg", "accept".equals(action) ? "Request paid." : "Request declined.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/requests";
    }

    // ---------- top-up & bills ----------
    @GetMapping("/topup")
    public String topupForm(Model m) {
        return form(m, "Mobile Top-up", "/topup", "Simulation: no real operator is contacted.",
                Field.select("operator", "Operator", OPERATORS), Field.text("number", "Mobile number"),
                Field.amount("amount", "Amount (AFN)"));
    }

    @PostMapping("/topup")
    public String topup(Authentication auth, @RequestParam String operator, @RequestParam String number,
                        @RequestParam BigDecimal amount, RedirectAttributes ra) {
        AppUser u = me(auth);
        return run(ra, "/topup", "Top-up successful.", () -> service.topup(u, operator, number, amount));
    }

    @GetMapping("/bills")
    public String billForm(Model m) {
        return form(m, "Bill Payment", "/bills", "Simulation: the payment is recorded, no biller is contacted.",
                Field.select("category", "Category", BILLS), Field.text("billNumber", "Bill / customer number"),
                Field.amount("amount", "Amount (AFN)"));
    }

    @PostMapping("/bills")
    public String bill(Authentication auth, @RequestParam String category, @RequestParam String billNumber,
                       @RequestParam BigDecimal amount, RedirectAttributes ra) {
        AppUser u = me(auth);
        return run(ra, "/bills", "Bill paid.", () -> service.payBill(u, category, billNumber, amount));
    }

    // ---------- history ----------
    @GetMapping("/history")
    public String history(Authentication auth, @RequestParam(required = false) String type, Model m) {
        m.addAttribute("txs", service.history(me(auth), type));
        m.addAttribute("type", type);
        return "history";
    }

    // ---------- profile ----------
    @GetMapping("/profile")
    public String profile(Authentication auth, Model m) {
        AppUser u = me(auth);
        return form(m, "Profile", "/profile", "Phone number: " + u.getPhone() + " (cannot be changed). Leave the password fields empty to keep your password.",
                Field.text("fullName", "Full name", u.getFullName(), true),
                Field.email("email", "Email", u.getEmail()),
                Field.password("currentPassword", "Current password", false),
                Field.password("newPassword", "New password (min. 8 characters)", false));
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth, @RequestParam String fullName,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword, RedirectAttributes ra) {
        AppUser u = me(auth);
        return run(ra, "/profile", "Profile updated.", () -> service.updateProfile(u, fullName, email, currentPassword, newPassword));
    }
}
