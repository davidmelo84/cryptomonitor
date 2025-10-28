package com.crypto.controller;

import com.crypto.model.User;
import com.crypto.model.dto.VerificationRequest;
import com.crypto.repository.UserRepository;
import com.crypto.security.JwtUtil;
import com.crypto.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;

    /**
     * Registro de usuário
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        log.info("📝 Tentativa de registro: {}", user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuário já existe"));
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email já cadastrado"));
        }

        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setEnabled(false);
            user.setRole("USER");

            User saved = userRepository.save(user);

            // Criar token + enviar código numérico
            verificationService.createVerificationToken(saved);

            log.info("✅ Usuário registrado: {}", saved.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuário criado! Verifique seu email.");
            response.put("requiresVerification", true);
            response.put("email", saved.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro no registro:", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao criar conta"));
        }
    }

    /**
     * Login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        log.info("🔐 Tentativa de login: {}", user.getUsername());

        try {
            User dbUser = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            if (!dbUser.getEnabled()) {
                // Sempre retorna JSON válido
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("error", "Conta não verificada");
                resp.put("message", "Verifique seu email antes de fazer login");
                resp.put("email", dbUser.getEmail());
                return ResponseEntity.status(403).body(resp);
            }

            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );

            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(Map.of("token", token));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "error", "Credenciais inválidas"));
        } catch (DisabledException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "error", "Conta desabilitada"));
        } catch (Exception e) {
            log.error("❌ Erro no login:", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Erro ao processar login"));
        }
    }

    /**
     * Verificação de código
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerificationRequest request) {
        try {
            boolean verified = verificationService.verifyCode(request.getCode());

            if (verified) {
                // Ativa o usuário
                User user = verificationService.getUserByCode(request.getCode());
                user.setEnabled(true);
                userRepository.save(user);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Conta verificada com sucesso!"
                ));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "Código inválido, expirado ou já utilizado"
                ));
            }
        } catch (Exception e) {
            // Sempre retornar JSON, mesmo em erro
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Erro interno ao verificar o código"
            ));
        }
    }


    /**
     * Reenvio de código
     */
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestParam String email) {
        boolean sent = verificationService.resendCode(email);

        Map<String, Object> response = new HashMap<>();
        if (sent) {
            response.put("success", true);
            response.put("message", "Código reenviado com sucesso!");
        } else {
            response.put("success", false);
            response.put("error", "Falha ao reenviar código");
        }
        return ResponseEntity.ok(response); // ⚡ Retorno 200 sempre com JSON válido
    }
}
