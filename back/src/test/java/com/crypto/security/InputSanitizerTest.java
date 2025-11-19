package com.crypto.security;

import com.crypto.util.InputSanitizer;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * ✅ TESTES DE SEGURANÇA - InputSanitizer
 *
 * Localização: back/src/test/java/com/crypto/security/InputSanitizerTest.java
 *
 * MUDANÇA: Convertido para teste unitário simples (sem Spring)
 * MOTIVO: Evita conflitos com PropertyPlaceholderHelper do Spring
 */
@DisplayName("InputSanitizer Security Tests")
class InputSanitizerTest {

    private InputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        // Instanciação manual (sem Spring)
        sanitizer = new InputSanitizer();
    }

    @Nested
    @DisplayName("SQL Injection Detection")
    class SqlInjectionTests {

        @Test
        @DisplayName("Deve detectar SQL injection básico")
        void shouldDetectBasicSqlInjection() {
            String malicious = "admin' OR '1'='1";

            assertThat(sanitizer.containsSqlInjection(malicious)).isTrue();
            assertThat(sanitizer.isSafe(malicious)).isFalse();
        }

        @Test
        @DisplayName("Deve detectar UNION attack")
        void shouldDetectUnionAttack() {
            String malicious = "1 UNION SELECT * FROM users";

            assertThat(sanitizer.containsSqlInjection(malicious)).isTrue();
        }

        @Test
        @DisplayName("Deve detectar DROP TABLE")
        void shouldDetectDropTable() {
            String malicious = "'; DROP TABLE users; --";

            assertThat(sanitizer.containsSqlInjection(malicious)).isTrue();
        }

        @Test
        @DisplayName("Deve permitir inputs seguros")
        void shouldAllowSafeInputs() {
            String safe = "bitcoin-cash";

            assertThat(sanitizer.containsSqlInjection(safe)).isFalse();
            assertThat(sanitizer.isSafe(safe)).isTrue();
        }
    }

    @Nested
    @DisplayName("XSS Detection")
    class XssTests {

        @Test
        @DisplayName("Deve detectar <script> tag")
        void shouldDetectScriptTag() {
            String malicious = "<script>alert('XSS')</script>";

            assertThat(sanitizer.containsXss(malicious)).isTrue();
            assertThat(sanitizer.isSafe(malicious)).isFalse();
        }

        @Test
        @DisplayName("Deve detectar javascript: protocol")
        void shouldDetectJavascriptProtocol() {
            String malicious = "javascript:alert('XSS')";

            assertThat(sanitizer.containsXss(malicious)).isTrue();
        }

        @Test
        @DisplayName("Deve detectar event handlers")
        void shouldDetectEventHandlers() {
            String malicious = "<img src=x onerror=alert('XSS')>";

            assertThat(sanitizer.containsXss(malicious)).isTrue();
        }
    }

    @Nested
    @DisplayName("Path Traversal Detection")
    class PathTraversalTests {

        @Test
        @DisplayName("Deve detectar ../ traversal")
        void shouldDetectDotDotSlash() {
            String malicious = "../../etc/passwd";

            assertThat(sanitizer.containsPathTraversal(malicious)).isTrue();
        }

        @Test
        @DisplayName("Deve detectar URL encoded traversal")
        void shouldDetectEncodedTraversal() {
            String malicious = "%2e%2e%2f";

            assertThat(sanitizer.containsPathTraversal(malicious)).isTrue();
        }
    }

    @Nested
    @DisplayName("Email Sanitization")
    class EmailSanitizationTests {

        @Test
        @DisplayName("Deve aceitar emails válidos")
        void shouldAcceptValidEmails() {
            assertThatCode(() -> sanitizer.sanitizeEmail("user@example.com"))
                    .doesNotThrowAnyException();

            assertThatCode(() -> sanitizer.sanitizeEmail("test.user+tag@domain.co.uk"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve rejeitar emails inválidos")
        void shouldRejectInvalidEmails() {
            assertThatThrownBy(() -> sanitizer.sanitizeEmail("notanemail"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email inválido");

            assertThatThrownBy(() -> sanitizer.sanitizeEmail("user@"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve rejeitar SQL injection em email")
        void shouldRejectSqlInjectionInEmail() {
            assertThatThrownBy(() -> sanitizer.sanitizeEmail("admin'--@example.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve converter para lowercase")
        void shouldConvertToLowercase() {
            String result = sanitizer.sanitizeEmail("User@EXAMPLE.COM");
            assertThat(result).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("Username Sanitization")
    class UsernameSanitizationTests {

        @Test
        @DisplayName("Deve aceitar usernames válidos")
        void shouldAcceptValidUsernames() {
            assertThatCode(() -> sanitizer.sanitizeUsername("user123"))
                    .doesNotThrowAnyException();

            assertThatCode(() -> sanitizer.sanitizeUsername("test_user-name"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve rejeitar usernames muito curtos")
        void shouldRejectShortUsernames() {
            assertThatThrownBy(() -> sanitizer.sanitizeUsername("ab"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("3-30 caracteres");
        }

        @Test
        @DisplayName("Deve rejeitar usernames muito longos")
        void shouldRejectLongUsernames() {
            String longUsername = "a".repeat(31);

            assertThatThrownBy(() -> sanitizer.sanitizeUsername(longUsername))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve rejeitar caracteres especiais")
        void shouldRejectSpecialCharacters() {
            assertThatThrownBy(() -> sanitizer.sanitizeUsername("user@123"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> sanitizer.sanitizeUsername("user 123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Coin Symbol Sanitization")
    class CoinSymbolTests {

        @Test
        @DisplayName("Deve aceitar símbolos válidos")
        void shouldAcceptValidSymbols() {
            assertThat(sanitizer.sanitizeCoinSymbol("BTC")).isEqualTo("BTC");
            assertThat(sanitizer.sanitizeCoinSymbol("eth")).isEqualTo("ETH");
            assertThat(sanitizer.sanitizeCoinSymbol("USDT")).isEqualTo("USDT");
        }

        @Test
        @DisplayName("Deve rejeitar símbolos inválidos")
        void shouldRejectInvalidSymbols() {
            assertThatThrownBy(() -> sanitizer.sanitizeCoinSymbol("BTC-USD"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> sanitizer.sanitizeCoinSymbol("B"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve converter para uppercase")
        void shouldConvertToUppercase() {
            assertThat(sanitizer.sanitizeCoinSymbol("btc")).isEqualTo("BTC");
        }
    }

    @Nested
    @DisplayName("Coin ID Sanitization")
    class CoinIdTests {

        @Test
        @DisplayName("Deve aceitar coin IDs válidos")
        void shouldAcceptValidCoinIds() {
            assertThat(sanitizer.sanitizeCoinId("bitcoin")).isEqualTo("bitcoin");
            assertThat(sanitizer.sanitizeCoinId("ethereum")).isEqualTo("ethereum");
            assertThat(sanitizer.sanitizeCoinId("bitcoin-cash")).isEqualTo("bitcoin-cash");
        }

        @Test
        @DisplayName("Deve rejeitar coin IDs inválidos")
        void shouldRejectInvalidCoinIds() {
            assertThatThrownBy(() -> sanitizer.sanitizeCoinId("<script>alert(1)</script>"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> sanitizer.sanitizeCoinId("bitcoin' OR '1'='1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve converter para lowercase")
        void shouldConvertToLowercase() {
            assertThat(sanitizer.sanitizeCoinId("BITCOIN")).isEqualTo("bitcoin");
        }
    }

    @Nested
    @DisplayName("General Sanitization")
    class GeneralSanitizationTests {

        @Test
        @DisplayName("Deve remover null bytes")
        void shouldRemoveNullBytes() {
            String input = "test\0string";
            String result = sanitizer.sanitize(input);

            assertThat(result).doesNotContain("\0");
            assertThat(result).isEqualTo("teststring");
        }

        @Test
        @DisplayName("Deve remover caracteres de controle")
        void shouldRemoveControlCharacters() {
            String input = "test\u0001\u0002string";
            String result = sanitizer.sanitize(input);

            assertThat(result).isEqualTo("teststring");
        }

        @Test
        @DisplayName("Deve fazer trim")
        void shouldTrim() {
            String result = sanitizer.sanitize("  bitcoin  ");
            assertThat(result).isEqualTo("bitcoin");
        }
    }
}