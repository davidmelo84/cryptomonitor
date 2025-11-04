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
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📝 TENTATIVA DE REGISTRO");
        log.info("   👤 Username: {}", newUser.getUsername());
        log.info("   📧 Email: {}", newUser.getEmail());

        // ✅ Valida formato de email
        if (!isValidEmail(newUser.getEmail())) {
            log.warn("❌ Email inválido");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email inválido"));
        }

        // ✅ Verifica se usuário já existe
        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            log.warn("❌ Username já existe");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuário já existe"));
        }

        // ✅ Verifica se email já está cadastrado
        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            log.warn("❌ Email já cadastrado");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email já cadastrado"));
        }

        try {
            // ✅ Cria usuário desativado
            newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
            newUser.setEnabled(false);
            newUser.setRole("USER");
            User savedUser = userRepository.save(newUser);
            log.info("✅ Usuário criado no banco - ID: {}", savedUser.getId());

            // ✅ Gera e envia código de verificação
            String token = verificationService.createVerificationToken(savedUser);
            log.info("✅ Token gerado: {}", token);

            log.info("🎉 REGISTRO CONCLUÍDO!");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuário criado! Verifique seu email para ativar a conta.",
                    "email", savedUser.getEmail(),
                    "requiresVerification", true
            ));

        } catch (Exception e) {
            log.error("❌ ERRO no registro:", e);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao criar conta",
                            "message", e.getMessage()
                    ));
        }
    }

    // ------------------ VERIFICAR CÓDIGO ------------------
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 TENTATIVA DE VERIFICAÇÃO");
        log.info("   Código recebido: {}", code);

        if (code == null || code.length() != 6) {
            log.warn("❌ Código inválido (tamanho)");
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

    // ------------------ REENVIAR CÓDIGO - CORRIGIDO ------------------
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 REQUISIÇÃO DE REENVIO DE CÓDIGO");
        log.info("   Email: {}", email);

        if (email == null || !isValidEmail(email)) {
            log.warn("❌ Email inválido");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email inválido"));
        }

        // ✅ CORREÇÃO: Chamar método correto do service
        boolean sent = verificationService.resendCode(email);

        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Novo código enviado para seu email!"
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Não foi possível reenviar o código",
                            "message", "Verifique se o email está correto ou se a conta já foi verificada."
                    ));
        }
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