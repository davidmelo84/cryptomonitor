// back/src/main/java/com/crypto/controller/UserController.java
package com.crypto.controller;

import com.crypto.model.User;
import com.crypto.repository.UserRepository;
import com.crypto.security.JwtUtil;
import com.crypto.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

    // ------------------ REGISTRO COM VERIFICAÇÃO ------------------
    @PostMapping
    public ResponseEntity<?> register(@RequestBody User newUser) {
        log.info("📝 Tentativa de registro: {}", newUser.getEmail());

        // valida formato de email
        if (!isValidEmail(newUser.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email inválido"));
        }

        // verifica se usuário já existe
        Optional<User> existingUser = userRepository.findByUsername(newUser.getUsername());
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuário já existe"));
        }

        // verifica se email já está cadastrado
        Optional<User> existingEmail = userRepository.findByEmail(newUser.getEmail());
        if (existingEmail.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email já cadastrado"));
        }

        // cria usuário desativado
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setEnabled(false);
        User savedUser = userRepository.save(newUser);

        // gera e envia código de verificação
        String token = verificationService.createVerificationToken(savedUser);

        log.info("✅ Usuário registrado (aguardando verificação): {}", savedUser.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuário criado! Verifique seu email para ativar a conta.",
                "email", savedUser.getEmail(),
                "requiresVerification", true
        ));
    }

    // ------------------ VERIFICAR CÓDIGO ------------------
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        if (code == null || code.length() != 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Código inválido"));
        }

        boolean verified = verificationService.verifyCode(code);

        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verificado com sucesso! Você já pode fazer login."
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Código inválido ou expirado",
                            "message", "Verifique se digitou corretamente ou solicite um novo código."
                    ));
        }
    }

    // ------------------ REENVIAR CÓDIGO ------------------
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || !isValidEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email inválido"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuário não encontrado"));
        }

        User user = userOpt.get();
        String token = verificationService.createVerificationToken(user);

        log.info("📩 Novo código enviado para {}", email);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Novo código enviado para seu email"
        ));
    }

    // ------------------ PERFIL ------------------
    @GetMapping("/me")
    public ResponseEntity<User> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        return userRepository.findByUsername(username)
                .map(user -> {
                    user.setPassword(null);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ------------------ ATUALIZAÇÃO DE PERFIL ------------------
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
                    User savedUser = userRepository.save(user);
                    savedUser.setPassword(null);
                    return ResponseEntity.ok(savedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ------------------ UTIL ------------------
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
