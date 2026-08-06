package com.flickpick.userservice.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class JwtKeyConfig {

    @Bean
    public RSAPrivateKey jwtPrivateKey(@Value("${jwt.private-key}") Resource resource) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(decodePem(resource)));
    }

    @Bean
    public RSAPublicKey jwtPublicKey(@Value("${jwt.public-key}") Resource resource) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(decodePem(resource)));
    }

    // Strips the "-----BEGIN/END ...-----" header/footer lines a PEM file
    // wraps around its content, and base64-decodes what's left — a PEM file
    // is nothing more than base64-encoded DER bytes with that text framing.
    private byte[] decodePem(Resource resource) throws Exception {
        String content = new String(resource.getInputStream().readAllBytes());
        String base64 = content
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
