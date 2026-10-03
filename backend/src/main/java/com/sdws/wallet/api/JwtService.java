package com.sdws.wallet.api;

import com.sdws.wallet.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String COOKIE_NAME = "SDWS-AUTH";

    private final JwtEncoder encoder;

    @Value("${app.jwt.expiration:PT1H}")
    private Duration expiration;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean secureCookie;

    public String issue(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sdws")
                .subject(user.getPhone())
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String authenticationCookie(AppUser user) {
        return ResponseCookie.from(COOKIE_NAME, issue(user))
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api")
                .maxAge(expiration)
                .build()
                .toString();
    }

    public String clearedCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api")
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }
}