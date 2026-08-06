package com.flickpick.userservice.controller;

import com.flickpick.userservice.dto.AuthResponse;
import com.flickpick.userservice.dto.LoginRequest;
import com.flickpick.userservice.dto.SignupRequest;
import com.flickpick.userservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(new AuthResponse(authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new AuthResponse(authService.login(request)));
    }
}
