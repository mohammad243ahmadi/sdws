package com.sdws.wallet.api;

import com.sdws.wallet.api.ApiDtos.AuthResponse;
import com.sdws.wallet.api.ApiDtos.CsrfResponse;
import com.sdws.wallet.api.ApiDtos.LoginRequest;
import com.sdws.wallet.api.ApiDtos.RegisterRequest;
import com.sdws.wallet.api.ApiDtos.UserResponse;
import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final WalletService walletService;
    private final CsrfTokenRepository csrfTokenRepository;

    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = csrfTokenRepository.loadToken(request);
        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        walletService.register(request.fullName(), request.phone(), request.email(), request.password());
        AppUser user = walletService.current(request.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(UserResponse.from(user)));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.phone(), request.password()));
        } catch (AuthenticationException exception) {
            throw new AuthenticationServiceException("Invalid phone number or password.");
        }
        AppUser user = walletService.current(request.phone());
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.authenticationCookie(user));
        return new AuthResponse(UserResponse.from(user));
    }

    @GetMapping("/me")
    public AuthResponse current(Authentication authentication) {
        return new AuthResponse(UserResponse.from(walletService.current(authentication.getName())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.clearedCookie());
        return ResponseEntity.noContent().build();
    }
}