// back/src/main/java/com/crypto/controller/UserController.java
package com.crypto.controller;

import com.crypto.model.User;
import com.crypto.repository.UserRepository;
import com.crypto.security.JwtUtil;
import com.crypto.service.VerificationService;
import com.crypto.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationService verificationService;

    // ✅ ADICIONADO
    private final InputSanitizer sanitizer;

    // ==========================================
    // ✅ REGISTRO COM SANITIZAÇÃO
    // ==========================================
    @PostMapping
    @Transactional
    public ResponseEntity<?> register(@RequestBody User newUser) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📝 TENTATIVA DE REGISTRO");
        log.info("   👤 Username: {}", newUser.getUsername());
        log.info("   📧 Email: {}", newUser.getEmail());

        // 🔒 SANITIZAÇÃO DE ENTRADA
        try {
            newUser.setUsername(sanitizer.sanitizeUsername(newUser.getUsername()));
            newUser.setEmail(sanitizer.sanitizeEmail(newUser.getEmail()));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Input inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        // 🔒 VALIDAÇÃO DE EMAIL (nova)
        if (!isValidEmail(newUser.getEmail())) {
            log.warn("❌ Email inválido");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email inválido"));
        }

        // ==========================================
        // 2. VERIFICAR USUÁRIO EXISTENTE
        // ==========================================
        Optional<User> existingByUsername = userRepository.findByUsername(newUser.getUsername());
        Optional<User> existingByEmail = userRepository.findByEmail(newUser.getEmail());

        // Usuário já verificado
        if (existingByUsername.isPresent() && existingByUsername.get().getEnabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuário já existe"));
        }

        if (existingByEmail.isPresent() && existingByEmail.get().getEnabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email já cadastrado"));
        }

        User userToRegister;
        boolean isRetry = false;

        // Retry para contas não verificadas
        if (existingByUsername.isPresent() && !existingByUsername.get().getEnabled()) {
            userToRegister = existingByUsername.get();
            isRetry = true;
            userToRegister.setEmail(newUser.getEmail());
            userToRegister.setPassword(passwordEncoder.encode(newUser.getPassword()));

        } else if (existingByEmail.isPresent() && !existingByEmail.get().getEnabled()) {
            userToRegister = existingByEmail.get();
            isRetry = true;
            userToRegister.setUsername(newUser.getUsername());
            userToRegister.setPassword(passwordEncoder.encode(newUser.getPassword()));

        } else {
            // Criar nova conta
            userToRegister = new User();
            userToRegister.setUsername(newUser.getUsername());
            userToRegister.setEmail(newUser.getEmail());
            userToRegister.setPassword(passwordEncoder.encode(newUser.getPassword()));
            userToRegister.setEnabled(false);
            userToRegister.setRole("USER");
        }

        try {
            User savedUser = userRepository.save(userToRegister);
            log.info("✅ Usuário salvo no banco - ID: {}", savedUser.getId());

            // Tentar enviar email c/ retry
            String code = null;
            int maxRetries = 3;
            int retry = 0;

            while (retry < maxRetries && code == null) {
                try {
                    code = verificationService.createVerificationToken(savedUser);
                    break;
                } catch (Exception e) {
                    retry++;
                    Thread.sleep(2000);
                }
            }

            if (code != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", isRetry
                                ? "Código reenviado! Verifique seu email."
                                : "Usuário criado! Verifique seu email.",
                        "email", savedUser.getEmail(),
                        "requiresVerification", true,
                        "isRetry", isRetry
                ));
            }

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Erro ao enviar email de verificação",
                    "canRetry", true
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao criar conta"));
        }
    }

    // ==========================================
    // ✅ VERIFICAR CÓDIGO
    // ==========================================
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        if (code == null || code.length() != 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código inválido"));
        }

        boolean verified = verificationService.verifyCode(code);

        return verified
                ? ResponseEntity.ok(Map.of("success", true, "message", "Email verificado!"))
                : ResponseEntity.badRequest().body(Map.of("error", "Código inválido ou expirado"));
    }

    // ==========================================
    // ✅ REENVIAR CÓDIGO COM SANITIZAÇÃO
    // ==========================================
    @PostMapping("/resend-code")
    @Transactional
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // 🔒 Sanitização
        try {
            email = sanitizer.sanitizeEmail(email);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email inválido"));
        }

        int maxRetries = 3;
        int retry = 0;

        while (retry < maxRetries) {
            try {
                boolean sent = verificationService.resendCode(email);

                if (sent) {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Novo código enviado!",
                            "email", email
                    ));
                }

                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Conta não encontrada ou já verificada"));

            } catch (Exception e) {
                retry++;
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }

        return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Erro ao reenviar código"
        ));
    }

    // ==========================================
    // PERFIL
    // ==========================================
    @GetMapping("/me")
    public ResponseEntity<User> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        return userRepository.findByUsername(username)
                .map(u -> {
                    u.setPassword(null);
                    return ResponseEntity.ok(u);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // ATUALIZAR PERFIL
    // ==========================================
    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody User updated) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        return userRepository.findByUsername(username)
                .map(user -> {
                    if (updated.getEmail() != null) user.setEmail(updated.getEmail());
                    if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
                        user.setPassword(passwordEncoder.encode(updated.getPassword()));
                    }
                    User saved = userRepository.save(user);
                    saved.setPassword(null);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // VALIDAÇÃO DE EMAIL
    // ==========================================
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
