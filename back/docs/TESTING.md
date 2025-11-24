# 🧪 **GUIA DE TESTES - CRYPTO MONITOR**

## **Índice**
- [Visão Geral](#visão-geral)
- [Estrutura de Testes](#estrutura-de-testes)
- [Executar Testes](#executar-testes)
- [Cobertura de Testes](#cobertura-de-testes)
- [Testes Implementados](#testes-implementados)
- [Boas Práticas](#boas-práticas)

---

## **Visão Geral**

Este projeto possui **3 tipos de testes**:

1. **Testes Unitários** → Testam classes isoladas (com mocks)
2. **Testes de Integração** → Testam endpoints completos (com banco H2)
3. **Testes de Segurança** → Validam sanitização de inputs

**Meta de Cobertura**: 70% do código

---

## **Estrutura de Testes**

```
src/test/java/
├── com/crypto/
│   ├── util/
│   │   └── InputSanitizerTest.java         # Segurança
│   ├── security/
│   │   └── JwtUtilTest.java                # Autenticação
│   ├── service/
│   │   ├── PortfolioServiceTest.java       # Lógica de negócio
│   │   └── AlertServiceTest.java
│   └── integration/
│       ├── AuthControllerIntegrationTest.java
│       └── CryptoControllerIntegrationTest.java
```

---

## **Executar Testes**

### **1. Todos os Testes**
```bash
./mvnw test
```

### **2. Apenas Testes Unitários**
```bash
./mvnw test -Dtest="*Test"
```

### **3. Apenas Testes de Integração**
```bash
./mvnw test -Dtest="*IntegrationTest"
```

### **4. Classe Específica**
```bash
./mvnw test -Dtest=InputSanitizerTest
```

### **5. Método Específico**
```bash
./mvnw test -Dtest=InputSanitizerTest#shouldDetectBasicSqlInjection
```

---

## **Cobertura de Testes**

### **Instalar Plugin Jacoco**

Adicione ao `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### **Gerar Relatório de Cobertura**

```bash
./mvnw clean test jacoco:report
```

Abra: `target/site/jacoco/index.html`

---

## **Testes Implementados**

### **✅ Testes de Segurança (InputSanitizerTest)**

| Teste | Descrição |
|-------|-----------|
| `shouldDetectBasicSqlInjection` | Detecta `admin' OR '1'='1` |
| `shouldDetectUnionAttack` | Detecta `UNION SELECT` |
| `shouldDetectScriptTag` | Detecta `<script>alert()</script>` |
| `shouldDetectJavascriptProtocol` | Detecta `javascript:` |
| `shouldDetectDotDotSlash` | Detecta `../../etc/passwd` |
| `shouldSanitizeEmail` | Valida formato de email |
| `shouldSanitizeUsername` | Valida username (3-30 chars) |
| `shouldSanitizeCoinId` | Valida coinId (alfanumérico + hífen) |

### **✅ Testes de Autenticação (JwtUtilTest)**

| Teste | Descrição |
|-------|-----------|
| `shouldGenerateValidToken` | Gera token JWT válido |
| `shouldExtractUsername` | Extrai username do token |
| `shouldValidateValidToken` | Valida token correto |
| `shouldRejectMalformedToken` | Rejeita token malformado |
| `shouldRejectInvalidSignature` | Rejeita assinatura inválida |
| `shouldCheckExpiration` | Verifica expiração do token |

### **✅ Testes de Negócio (PortfolioServiceTest)**

| Teste | Descrição |
|-------|-----------|
| `shouldAddBuyTransaction` | Adiciona transação de compra |
| `shouldUpdateExistingPortfolioOnBuy` | Atualiza custo médio |
| `shouldThrowExceptionWhenSellingWithoutBalance` | Valida saldo antes de vender |
| `shouldRemovePortfolioWhenSellingAll` | Remove portfolio ao vender tudo |
| `shouldCalculatePortfolioWithProfitLoss` | Calcula lucro/prejuízo |

### **✅ Testes de Integração (AuthControllerIntegrationTest)**

| Teste | Descrição |
|-------|-----------|
| `shouldRegisterNewUser` | Registra novo usuário |
| `shouldRejectDuplicateUser` | Valida usuário duplicado |
| `shouldRejectLoginWithoutVerification` | Exige verificação de email |
| `shouldRejectWrongPassword` | Valida senha incorreta |
| `shouldRejectSqlInjection` | Protege contra SQL injection |
| `shouldValidateEmailFormat` | Valida formato de email |

---

## **Boas Práticas**

### **1. Estrutura AAA (Arrange-Act-Assert)**

```java
@Test
void shouldCalculateProfitLoss() {
    // Arrange (preparar)
    Portfolio portfolio = createTestPortfolio();
    
    // Act (executar)
    BigDecimal result = portfolioService.calculateProfit(portfolio);
    
    // Assert (verificar)
    assertThat(result).isEqualByComparingTo(new BigDecimal("2500.00"));
}
```

### **2. Nomes Descritivos**

✅ **BOM**: `shouldRejectSqlInjectionInCoinId`  
❌ **RUIM**: `test1`

### **3. Um Teste = Um Conceito**

```java
// ✅ BOM
@Test
void shouldValidateEmailFormat() { ... }

@Test
void shouldSanitizeEmailToLowercase() { ... }

// ❌ RUIM - testa 2 coisas
@Test
void shouldValidateAndSanitizeEmail() { ... }
```

### **4. Usar AssertJ para Asserções**

```java
// ✅ BOM (fluent API)
assertThat(result)
    .isNotNull()
    .hasSize(5)
    .contains("bitcoin");

// ❌ RUIM (JUnit clássico)
assertNotNull(result);
assertEquals(5, result.size());
assertTrue(result.contains("bitcoin"));
```

### **5. Mocks Apenas Quando Necessário**

```java
// ✅ BOM - Mock de dependência externa
@Mock
private CryptoService cryptoService;

// ❌ RUIM - Não mockar classes simples
@Mock
private InputSanitizer sanitizer; // ← Pode instanciar diretamente
```

### **6. Testes de Integração com @Transactional**

```java
@SpringBootTest
@Transactional // ✅ Rollback automático após cada teste
class MyIntegrationTest {
    @Test
    void shouldSaveUser() {
        userRepository.save(user);
        // Rollback automático
    }
}
```

---

## **Troubleshooting**

### **Erro: "No tests found"**

**Causa**: Maven não encontrou os testes

**Solução**:
```bash
# Verificar se arquivos terminam com "Test.java"
find src/test -name "*Test.java"

# Limpar e recompilar
./mvnw clean test
```

### **Erro: "BeanCreationException" nos testes**

**Causa**: Dependências circulares ou configuração incorreta

**Solução**: Use `@MockBean` ou `@Lazy`:

```java
@SpringBootTest
class MyTest {
    @MockBean
    private CryptoService cryptoService; // ✅ Mock do bean
}
```

### **Erro: "JUnit 4 vs JUnit 5"**

**Solução**: Use sempre JUnit 5:

```java
// ✅ JUnit 5
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// ❌ JUnit 4 (não usar)
import org.junit.Test;
import org.junit.Before;
```

---

## **Comandos Úteis**

```bash
# Executar testes em modo watch
./mvnw test -Dspring-boot.run.arguments="--spring.devtools.restart.enabled=true"

# Executar testes com logs detalhados
./mvnw test -X

# Pular testes no build
./mvnw clean install -DskipTests

# Executar apenas testes rápidos (< 5s)
./mvnw test -Dgroups="fast"
```

---

## **Próximos Passos**

- [ ] Adicionar testes para `AlertService`
- [ ] Adicionar testes para `TradingBotService`
- [ ] Implementar testes E2E com Testcontainers
- [ ] Configurar CI/CD (GitHub Actions)
- [ ] Atingir 70% de cobertura
- [ ] Implementar mutation testing (PIT)

---

## **Recursos**

- [JUnit 5 Docs](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Docs](https://assertj.github.io/doc/)
- [Mockito Docs](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)