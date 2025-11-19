package com.crypto.integration;

import com.crypto.model.User;
import com.crypto.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ✅ TESTE DE INTEGRAÇÃO - AuthController
 *
 * Testa fluxo completo de autenticação:
 * - Registro
 * - Login
 * - Verificação
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController - Testes de Integração")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String BASE_URL = "/crypto-monitor/api/auth";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Deve registrar novo usuário")
    void shouldRegisterNewUser() throws Exception {
        // Arrange
        User newUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requiresVerification").value(true))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // Verificar se foi salvo no banco
        assertThat(userRepository.findByUsername("testuser")).isPresent();
        assertThat(userRepository.findByUsername("testuser").get().getEnabled())
                .isFalse(); // Não verificado ainda
    }

    @Test
    @Order(2)
    @DisplayName("Deve rejeitar registro de usuário duplicado")
    void shouldRejectDuplicateUser() throws Exception {
        // Arrange
        User user = User.builder()
                .username("duplicate")
                .email("duplicate@example.com")
                .password("password123")
                .enabled(true)
                .role("USER")
                .build();
        userRepository.save(user);

        User duplicateUser = User.builder()
                .username("duplicate")
                .email("another@example.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Usuário já existe"));
    }

    @Test
    @Order(3)
    @DisplayName("Deve rejeitar login sem verificação")
    void shouldRejectLoginWithoutVerification() throws Exception {
        // Arrange
        User unverifiedUser = User.builder()
                .username("unverified")
                .email("unverified@example.com")
                .password("$2a$10$HASHED_PASSWORD") // BCrypt hash
                .enabled(false)
                .role("USER")
                .build();
        userRepository.save(unverifiedUser);

        User loginRequest = User.builder()
                .username("unverified")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Conta não verificada"));
    }

    @Test
    @Order(4)
    @DisplayName("Deve rejeitar senha incorreta")
    void shouldRejectWrongPassword() throws Exception {
        // Arrange
        User user = User.builder()
                .username("validuser")
                .email("valid@example.com")
                .password("$2a$10$HASHED_PASSWORD")
                .enabled(true)
                .role("USER")
                .build();
        userRepository.save(user);

        User loginRequest = User.builder()
                .username("validuser")
                .password("wrongpassword")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    @Order(5)
    @DisplayName("Deve validar entrada com SQL injection")
    void shouldRejectSqlInjection() throws Exception {
        // Arrange
        User maliciousUser = User.builder()
                .username("admin' OR '1'='1")
                .email("malicious@example.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(6)
    @DisplayName("Deve validar formato de email")
    void shouldValidateEmailFormat() throws Exception {
        // Arrange
        User invalidEmail = User.builder()
                .username("testuser2")
                .email("invalid-email")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @DisplayName("Deve validar tamanho mínimo da senha")
    void shouldValidatePasswordLength() throws Exception {
        // Arrange
        User shortPassword = User.builder()
                .username("testuser3")
                .email("test3@example.com")
                .password("123")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Senha deve ter no mínimo 8 caracteres"));
    }
}