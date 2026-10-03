package com.sdws.wallet.config;

import com.sdws.wallet.model.*;
import com.sdws.wallet.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first administrator on an empty database. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder encoder;
    private final String adminPhone;
    private final String adminPassword;

    public DataInitializer(UserRepository users, WalletRepository wallets, PasswordEncoder encoder,
                           @Value("${app.admin.phone}") String adminPhone,
                           @Value("${app.admin.password}") String adminPassword) {
        this.users = users; this.wallets = wallets; this.encoder = encoder;
        this.adminPhone = adminPhone; this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.existsByPhone(adminPhone)) return;
        AppUser a = new AppUser();
        a.setFullName("System Administrator");
        a.setPhone(adminPhone);
        a.setPasswordHash(encoder.encode(adminPassword));
        a.setRole(Enums.Role.ADMIN);
        users.save(a);

        Wallet w = new Wallet();
        w.setUser(a);
        w.setAccountNumber(String.format("SW%010d", a.getId()));
        wallets.save(w);
    }
}
