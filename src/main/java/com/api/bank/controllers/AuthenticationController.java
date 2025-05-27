package com.api.bank.controllers;


import com.api.bank.dtos.AuthenticationDTO;
import com.api.bank.dtos.LoginResponseDTO;
import com.api.bank.dtos.RegisterDTO;
import com.api.bank.repositories.UserRepository;
import com.api.bank.security.TokenService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.api.bank.entities.user.User;

@RestController
@RequestMapping("auth")
@Slf4j
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository repository;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data){
        log.info("Login attempt for user: {}", data.email());
        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
            var auth = this.authenticationManager.authenticate(usernamePassword);

            var token = tokenService.generateToken((User) auth.getPrincipal());

            log.info("User '{}' authenticated successfully", data.email());
            return ResponseEntity.ok(new LoginResponseDTO(token));
        } catch (Exception e) {
            log.warn("Failed login attempt for user '{}': {}", data.email(), e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data){
        log.info("Registration attempt for user: {}", data.email());
        if (this.repository.findByEmail(data.email()) != null) {
            log.warn("Registration failed: email '{}' already registered", data.email());
            return ResponseEntity.badRequest().build();
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = new User(data.email(), encryptedPassword, data.role());

        this.repository.save(newUser);
        log.info("User '{}' registered successfully", data.email());

        return ResponseEntity.ok().build();
    }
}
