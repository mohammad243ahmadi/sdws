package com.sdws.wallet.config;

import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        AppUser u = users.findByPhone(phone).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return User.withUsername(u.getPhone())
                .password(u.getPasswordHash())
                .roles(u.getRole().name())
                .disabled(!u.isActive())
                .build();
    }
}
