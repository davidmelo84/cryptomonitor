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

    // ==========================================
    // ✅ REGISTRO COM RETRY E CLEANUP
    // ==========================================
    @PostMapping
    @Transactional
    public ResponseEntity<?> register(@RequestBody User newUser) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📝 TENTATIVA DE REGISTRO");
        log.info("   👤 Username: {}", newUser.getUsername());
        log.info("   📧 Email: {}", newUser.getEmail());

        // ✅ 1. VALIDAÇÕES BÁSICAS
        if (!isValidEmail(newUser.getEmail())) {
            log.warn("❌ Email inválido");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email inválido"));
        }

        // ✅ 2. VERIFICAR USUÁRIO EXISTENTE
        Optional<User> existingByUsername = userRepository.findByUsername(newUser.getUsername());
        Optional<User> existingByEmail = userRepository.findByEmail(newUser.getEmail());

        // ✅ 3. CENÁRIO: Usuário já existe e está VERIFICADO
        if (existingByUsername.isPresent() && existingByUsername.get().getEnabled()) {
            log.warn("❌ Username já existe e está ativo");
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Usuário já existe",
                            "message", "Este username já está em uso"
                    ));
        }

        if (existingByEmail.isPresent() && existingByEmail.get().getEnabled()) {
            log.warn("❌ Email já cadastrado e verificado");
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Email já cadastrado",
                            "message", "Este email já está em uso"
                    ));
        }

        // ✅ 4. CENÁRIO: Conta NÃO VERIFICADA existe (permitir retry)
        User userToRegister = null;
        boolean isRetry = false;

        if (existingByUsername.isPresent() && !existingByUsername.get().getEnabled()) {
            // Usuário não verificado existe
            userToRegister = existingByUsername.get();
            isRetry = true;

            log.info("♻️ RETRY DETECTADO: Conta não verificada existe");
            log.info("   📅 Criada em: {}", userToRegister.getCreatedAt());

            // Atualizar dados (caso tenha mudado email, etc)
            userToRegister.setEmail(newUser.getEmail());
            userToRegister.setPassword(passwordEncoder.encode(newUser.getPassword()));

        } else if (existingByEmail.isPresent() && !existingByEmail.get().getEnabled()) {
            // Email não verificado existe
            userToRegister = existingByEmail.get();
            isRetry = true;

            log.info("♻️ RETRY DETECTADO: Email não verificado existe");
            log.info("   📅 Criada em: {}", userToRegister.getCreatedAt());

            // Atualizar dados
            userToRegister.setUsername(newUser.getUsername());
            userToRegister.setPassword(passwordEncoder.encode(newUser.getPassword()));

        } else {
            // ✅ 5. CRIAR NOVA CONTA
            log.info("✨ Criando NOVA conta");
            userToRegister = new User();
            userToRegister.setUsername(newUser.getUsername());
            userToRegister.setEmail(newUser.getEmail());
            userToRegister.setPassword(passwordEncoder.encode(newUser.getPassword()));
            userToRegister.setEnabled(false);
            userToRegister.setRole("USER");
        }

        try {
            // ✅ 6. SALVAR USUÁRIO
            User savedUser = userRepository.save(userToRegister);
            log.info("✅ Usuário salvo no banco - ID: {}", savedUser.getId());

            // ✅ 7. TENTAR ENVIAR EMAIL COM RETRY
            String code = null;
            int maxRetries = 3;
            int retryCount = 0;
            Exception lastError = null;

            while (retryCount < maxRetries && code == null) {
                try {
                    log.info("📧 Tentativa {} de {} de envio de email...",
                            retryCount + 1, maxRetries);

                    code = verificationService.createVerificationToken(savedUser);

                    log.info("✅ EMAIL ENVIADO COM SUCESSO!");
                    break;

                } catch (Exception e) {
                    lastError = e;
                    retryCount++;

                    log.error("❌ Tentativa {} falhou: {}", retryCount, e.getMessage());

                    if (retryCount < maxRetries) {
                        log.info("⏳ Aguardando 2 segundos antes de retry...");
                        Thread.sleep(2000);
                    }
                }
            }

            // ✅ 8. VERIFICAR RESULTADO DO ENVIO
            if (code != null) {
                // ✅ SUCESSO!
                log.info("🎉 REGISTRO CONCLUÍDO COM SUCESSO!");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", isRetry
                                ? "Código reenviado! Verifique seu email."
                                : "Usuário criado! Verifique seu email.",
                        "email", savedUser.getEmail(),
                        "requiresVerification", true,
                        "isRetry", isRetry
                ));

            } else {
                // ❌ TODAS AS TENTATIVAS FALHARAM
                log.error("❌ TODAS AS {} TENTATIVAS DE ENVIO FALHARAM!", maxRetries);
                log.error("   Último erro: {}", lastError != null ? lastError.getMessage() : "unknown");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                // ⚠️ Usuário está salvo, mas email não foi enviado
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Erro ao enviar email de verificação",
                        "message", "Sua conta foi criada, mas o email não pôde ser enviado. " +
                                "Tente fazer login novamente em alguns minutos para reenviar o código.",
                        "email", savedUser.getEmail(),
                        "canRetry", true,
                        "username", savedUser.getUsername()
                ));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Thread interrompida durante retry");
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Processo interrompido"));

        } catch (Exception e) {
            log.error("❌ ERRO CRÍTICO no registro:", e);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao criar conta",
                            "message", e.getMessage()
                    ));
        }
    }

    // ==========================================
    // ✅ VERIFICAR CÓDIGO
    // ==========================================
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
            log.info("✅ VERIFICAÇÃO BEM-SUCEDIDA!");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verificado com sucesso! Você já pode fazer login."
            ));
        } else {
            log.warn("❌ Código inválido ou expirado");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Código inválido ou expirado",
                            "message", "Verifique se digitou corretamente ou solicite um novo código."
                    ));
        }
    }

    // ==========================================
    // ✅ REENVIAR CÓDIGO (MELHORADO)
    // ==========================================
    @PostMapping("/resend-code")
    @Transactional
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

        // ✅ TENTAR REENVIAR COM RETRY
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastError = null;

        while (retryCount < maxRetries) {
            try {
                log.info("📧 Tentativa {} de {} de reenvio...", retryCount + 1, maxRetries);

                boolean sent = verificationService.resendCode(email);

                if (sent) {
                    log.info("✅ CÓDIGO REENVIADO COM SUCESSO!");
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Novo código enviado para seu email!",
                            "email", email
                    ));
                } else {
                    log.warn("⚠️ Não foi possível reenviar (conta pode estar verificada)");
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "error", "Não foi possível reenviar o código",
                                    "message", "Verifique se o email está correto ou se a conta já foi verificada."
                            ));
                }

            } catch (Exception e) {
                lastError = e;
                retryCount++;

                log.error("❌ Tentativa {} falhou: {}", retryCount, e.getMessage());

                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // ❌ TODAS AS TENTATIVAS FALHARAM
        log.error("❌ TODAS AS TENTATIVAS DE REENVIO FALHARAM!");
        log.error("   Último erro: {}", lastError != null ? lastError.getMessage() : "unknown");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Erro ao reenviar código",
                "message", "Não foi possível enviar o email. Tente novamente mais tarde.",
                "details", lastError != null ? lastError.getMessage() : "Erro desconhecido"
        ));
    }

    // ==========================================
    // ✅ PERFIL
    // ==========================================
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

    // ==========================================
    // ✅ ATUALIZAÇÃO DE PERFIL
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
                    User savedUser = userRepository.save(user);
                    savedUser.setPassword(null);
                    return ResponseEntity.ok(savedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // ✅ VALIDAÇÃO DE EMAIL
    // ==========================================
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}