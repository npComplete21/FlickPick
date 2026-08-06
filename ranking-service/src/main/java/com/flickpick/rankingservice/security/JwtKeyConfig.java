package com.flickpick.rankingservice.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class JwtKeyConfig {

    // Only the public key — this service can verify a token came from User
    // Service, but has no way to mint one itself.
    @Bean
    public RSAPublicKey jwtPublicKey(@Value("${jwt.public-key}") Resource resource) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(decodePem(resource)));
    }

    private byte[] decodePem(Resource resource) throws Exception {
        String content = new String(resource.getInputStream().readAllBytes());
        String base64 = content
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
