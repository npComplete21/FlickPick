package com.flickpick.importservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final RSAPublicKey publicKey;

    public JwtService(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
