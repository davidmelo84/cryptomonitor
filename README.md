# Crypto Monitor 🚀

Sistema completo de monitoramento de criptomoedas com alertas automáticos por email e Telegram.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![React](https://img.shields.io/badge/React-18.3.1-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-12+-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Executando o Projeto](#executando-o-projeto)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [API Endpoints](#api-endpoints)
- [Sistema de Cache Inteligente](#sistema-de-cache-inteligente)
- [Rate Limiting](#rate-limiting)
- [Segurança](#segurança)
- [Monitoramento e Métricas](#monitoramento-e-métricas)
- [Como Usar](#como-usar)
- [Troubleshooting](#troubleshooting)
- [Testes](#testes)

---

## 🎯 Sobre o Projeto

O **Crypto Monitor** é um sistema de monitoramento de criptomoedas em tempo real que permite:

- 📊 Acompanhar cotações de múltiplas criptomoedas
- 🔔 Configurar alertas personalizados de preço
- 📧 Receber notificações automáticas por email (SendGrid)
- 📱 Notificações via Telegram Bot (opcional)
- 👤 Gerenciar monitoramento individual por usuário
- ⏱️ Definir intervalos de verificação customizados
- 💼 Gerenciar portfolio de investimentos
- 🤖 Criar bots de trading automatizado (simulação)
- 📈 Visualizar histórico de preços com gráficos interativos
- 🔐 Autenticação JWT com verificação de email
- 🌙 Modo escuro completo
- 📱 Interface totalmente responsiva

O sistema busca dados da **CoinGecko API** e envia alertas quando detecta variações significativas nos preços configurados pelo usuário.

---

## 🛠️ Tecnologias

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - Spring Validation
  - Spring WebFlux (requisições assíncronas)
  - Spring Retry
  - Spring WebSocket
- **PostgreSQL** (produção)
- **H2 Database** (desenvolvimento/testes)
- **JWT** (autenticação)
- **Flyway** (migrations)
- **Lombok**
- **SendGrid** (envio de emails)
- **Caffeine** (cache local)
- **Bucket4j** (rate limiting)
- **Resilience4j** (circuit breaker)
- **Micrometer + Prometheus** (métricas)
- **Logback + Logstash** (logs estruturados)

### Frontend
- **React 18.3.1**
- **React Query (TanStack Query) 5.90.5** - Gerenciamento de estado e cache
- **Lucide React 0.263.1** - Ícones modernos
- **Recharts 2.5.0** - Gráficos e visualizações
- **Tailwind CSS 3.4.1** - Framework CSS utilitário
- **Crypto-JS 4.2.0** - Criptografia de dados sensíveis
- **React Window 2.2.1** - Virtualização de listas
- **Fetch API** - Requisições HTTP
- **Context API** - Gerenciamento de estado global
- **CSS Modules** - Estilos organizados
- **Web Vitals** - Métricas de performance

### DevOps & Deploy
- **Docker** (containerização)
- **Render** (hospedagem backend)
- **Vercel** (hospedagem frontend)

---

## ✨ Funcionalidades

### Autenticação
- ✅ Cadastro de usuários com validação
- ✅ Verificação de email com código de 6 dígitos
- ✅ Login com JWT e "Lembrar de mim"
- ✅ Proteção de rotas
- ✅ Sessão persistente (localStorage) ou temporária (sessionStorage)
- ✅ Limpeza automática de contas não verificadas (7 dias)
- ✅ Logout automático por inatividade

### Monitoramento
- ✅ Seleção de múltiplas criptomoedas
- ✅ Configuração de intervalos (1min - 1h)
- ✅ Thresholds personalizados (compra/venda)
- ✅ Início/parada de monitoramento sob demanda
- ✅ Sistema anti-spam de notificações (cooldown de 60min)
- ✅ Controle de usuários inativos (parada automática após 60min)
- ✅ Heartbeat automático para manter sessão ativa
- ✅ WebSocket para atualizações em tempo real

### Alertas
- ✅ Alerta de queda de preço (oportunidade de compra)
- ✅ Alerta de alta de preço (oportunidade de venda)
- ✅ Variação percentual em 24h
- ✅ Notificações por email (SendGrid)
- ✅ Notificações por Telegram com configuração visual
- ✅ Suporte a múltiplos canais simultâneos

### Portfolio
- ✅ Gerenciamento de transações (compra/venda)
- ✅ Cálculo automático de custo médio
- ✅ Visualização de lucro/prejuízo em tempo real
- ✅ Histórico completo de transações com filtros
- ✅ Gráficos de distribuição (PieChart)
- ✅ Estatísticas consolidadas

### Trading Bots (Simulação)
- ✅ Grid Trading
- ✅ DCA (Dollar Cost Average)
- ✅ Stop Loss / Take Profit
- ✅ Controle FIFO para vendas
- ✅ Estatísticas de performance
- ✅ Status ativo/inativo/pausado

### Dashboard
- ✅ Visualização em tempo real
- ✅ Estatísticas consolidadas
- ✅ Busca com debounce (300ms)
- ✅ Filtros e ordenação (market cap/preço/variação)
- ✅ Interface responsiva mobile-first
- ✅ WebSocket para atualizações instantâneas
- ✅ Skeleton loaders durante carregamento
- ✅ Toast notifications elegantes

### Gráficos e Visualizações
- ✅ Gráfico individual de preços (LineChart/AreaChart)
- ✅ Comparação entre múltiplas criptomoedas
- ✅ Períodos configuráveis (24h, 7d, 30d, 90d, 1y)
- ✅ Tooltips customizados
- ✅ Estatísticas de variação (min/max/change)
- ✅ Histórico real da CoinGecko API

### Tema e UI/UX
- ✅ Modo escuro/claro com persistência
- ✅ Transições suaves entre temas
- ✅ CSS Variables para cores dinâmicas
- ✅ Animações elegantes (fade, slide, float)
- ✅ Loading states com spinners
- ✅ Error boundaries para erros fatais
- ✅ Feedback visual em todas as ações

---

## 🏗️ Arquitetura

### Sistema de Cache Inteligente (SmartCache)

O projeto utiliza uma estratégia de cache em 3 camadas para otimizar requisições:

```
┌─────────────────────────────────────────────────────┐
│         SmartCache Service (Orquestrador)           │
├─────────────────────────────────────────────────────┤
│  1. Memória (Caffeine) - TTL 30min                 │
│     └─ Primeira verificação                        │
│                                                     │
│  2. Banco de Dados - TTL 2h                        │
│     └─ Fallback se memória expirou                │
│                                                     │
│  3. CoinGecko API (via Fila)                       │
│     └─ Apenas se BD expirou + rate limit OK        │
└─────────────────────────────────────────────────────┘
```

**Benefícios:**
- ✅ Redução de 99.89% nas requisições à API
- ✅ ~2 requests/hora vs 1800 teóricos
- ✅ Proteção contra rate limit 429
- ✅ Fallback automático em caso de erro

### Sistema de Filas (CoinGeckoRequestQueue)

```
┌──────────────────────────────────────────────────┐
│     CoinGeckoRequestQueue                        │
├──────────────────────────────────────────────────┤
│  • Fila com prioridade (HIGH, NORMAL, LOW)      │
│  • Intervalo mínimo: 30s entre requests         │
│  • Máximo: 3 req/min (buffer de segurança)      │
│  • Timeout: 60s por request                     │
│  • Deduplicação de requests idênticos           │
└──────────────────────────────────────────────────┘
```

### Rate Limiting por IP

```
┌──────────────────────────────────────────────────┐
│      RateLimitFilter (Bucket4j)                  │
├──────────────────────────────────────────────────┤
│  • API Geral: 100 req/min                       │
│  • Autenticação: 10 req/min                     │
│  • Admin: 50 req/min                            │
│  • Por IP (X-Forwarded-For)                     │
└──────────────────────────────────────────────────┘
```

### Arquitetura Frontend

```
┌─────────────────────────────────────────────┐
│           React Application                 │
├─────────────────────────────────────────────┤
│  • Context API (Theme, Telegram)           │
│  • React Query (Cache & State)             │
│  • Custom Hooks (Performance)              │
│  • Lazy Loading (Code Splitting)           │
│  • Error Boundaries                        │
│  • Web Workers (cálculos pesados)          │
└─────────────────────────────────────────────┘
```

### Gerenciamento de Estado

```
┌─────────────────────────────────────────────┐
│        State Management Strategy            │
├─────────────────────────────────────────────┤
│  1. React Query                             │
│     └─ Server state (cryptos, portfolio)    │
│                                             │
│  2. Context API                             │
│     └─ Global state (theme, telegram)       │
│                                             │
│  3. Local Storage / Session Storage         │
│     └─ Persistência (auth, preferences)     │
│                                             │
│  4. Crypto-JS                               │
│     └─ Dados sensíveis criptografados       │
└─────────────────────────────────────────────┘
```

---

## 📦 Pré-requisitos

### Backend
- Java JDK 17+
- Maven 3.9+
- PostgreSQL 12+ (ou usar H2 para desenvolvimento)

### Frontend
- Node.js 16+
- npm 8+

### Email (Obrigatório)
- Conta SendGrid com API Key
- Email verificado no SendGrid

### Telegram (Opcional)
- Bot Token do @BotFather
- Chat ID do @userinfobot

---

## 🚀 Instalação

### 1. Clone o repositório
```bash
git clone https://github.com/seu-usuario/crypto-monitor.git
cd crypto-monitor
```

### 2. Configure o Backend

#### Opção A: PostgreSQL (Produção)

1. Crie o banco de dados:
```sql
CREATE DATABASE crypto_monitor;
CREATE USER crypto_user WITH PASSWORD 'crypto_password';
GRANT ALL PRIVILEGES ON DATABASE crypto_monitor TO crypto_user;
```

2. Configure variáveis de ambiente:
```bash
# Banco de Dados
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/crypto_monitor
export SPRING_DATASOURCE_USERNAME=crypto_user
export SPRING_DATASOURCE_PASSWORD=crypto_password

# SendGrid (obrigatório)
export SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxx
export SENDGRID_FROM_EMAIL=seu-email@dominio.com
export SENDGRID_FROM_NAME="Crypto Monitor"

# JWT
export JWT_SECRET=sua-chave-jwt-com-no-minimo-64-caracteres-aleatorios

# Opcional - Telegram
export TELEGRAM_ENABLED=false
export TELEGRAM_BOT_TOKEN=seu-token
export TELEGRAM_CHAT_ID=seu-chat-id
```

#### Opção B: H2 (Desenvolvimento)

O projeto já vem configurado para usar H2 como alternativa. Basta não configurar as variáveis do PostgreSQL.

### 3. Configure SendGrid (Obrigatório)

1. Crie uma conta em [SendGrid](https://sendgrid.com)
2. Verifique um email/domínio de envio
3. Gere uma API Key com permissões de envio
4. Configure as variáveis de ambiente acima

**⚠️ Sem o SendGrid configurado, o sistema não envia emails de verificação e os usuários não conseguem fazer login.**

### 4. Configure o Frontend

Crie um arquivo `.env` na pasta `front/crypto-monitor-frontend`:

```bash
# API URL (ajuste conforme ambiente)
REACT_APP_API_URL=http://localhost:8080/crypto-monitor/api

# Encryption key (use uma chave forte em produção)
REACT_APP_ENCRYPTION_KEY=sua-chave-de-criptografia-segura
```

### 5. Instale dependências do Frontend
```bash
cd front/crypto-monitor-frontend
npm install
```

---

## ⚙️ Configuração

### application.yml (Backend)

O arquivo `application.yml` usa variáveis de ambiente. Configure os valores conforme necessário:

```yaml
# Porta do servidor
server:
  port: ${PORT:8080}
  servlet:
    context-path: /crypto-monitor

# Criptomoedas monitoradas
coingecko:
  coins:
    ids: bitcoin,ethereum,cardano,polkadot,chainlink,solana,avalanche-2,matic-network,litecoin,bitcoin-cash,ripple,dogecoin,binancecoin

# Configurações de alerta
alert:
  buy:
    threshold: -5.0  # Alerta quando cair 5%
  sell:
    threshold: 10.0  # Alerta quando subir 10%

# Cooldown de notificações (minutos)
notification:
  email:
    cooldown-minutes: 60

# Rate Limiting
rate-limit:
  coingecko:
    requests-per-minute: 30
    request-interval-ms: 500
  api:
    requests-per-minute: 100
  auth:
    requests-per-minute: 10

# Cache
coingecko:
  cache:
    ttl-minutes: 30
    enabled: true
```

### Variáveis de Ambiente (Produção - Render)

Configure no painel do Render:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=senha-segura

# SendGrid
SENDGRID_API_KEY=SG.xxxxx
SENDGRID_FROM_EMAIL=noreply@seudominio.com
SENDGRID_FROM_NAME=Crypto Monitor

# JWT
JWT_SECRET=chave-jwt-segura-64-caracteres-minimo

# CORS
CORS_ORIGINS=https://seu-frontend.vercel.app,http://localhost:3000
```

---

## ▶️ Executando o Projeto

### Backend
```bash
cd back
mvn clean install
mvn spring-boot:run
```

O backend estará disponível em: `http://localhost:8080/crypto-monitor`

### Frontend
```bash
cd front/crypto-monitor-frontend
npm start
```

O frontend estará disponível em: `http://localhost:3000`

### Build para Produção (Frontend)
```bash
npm run build
```

O build otimizado será gerado na pasta `build/`.

### Verificação

- **API Health Check**: http://localhost:8080/crypto-monitor/actuator/health
- **API Status**: http://localhost:8080/crypto-monitor/api/crypto/status
- **Prometheus Metrics**: http://localhost:8080/crypto-monitor/actuator/prometheus
- **H2 Console** (se habilitado): http://localhost:8080/crypto-monitor/h2-console
- **Frontend**: http://localhost:3000

---

## 📁 Estrutura do Projeto

```
crypto-monitor/
│
├── back/                          # Backend Spring Boot
│   ├── src/main/java/com/crypto/
│   │   ├── config/               # Configurações
│   │   │   ├── SecurityConfig.java
│   │   │   ├── WebClientConfig.java
│   │   │   ├── CacheConfig.java
│   │   │   ├── RateLimitConfig.java
│   │   │   ├── WebSocketConfig.java
│   │   │   └── MetricsConfig.java
│   │   │
│   │   ├── controller/           # REST Controllers
│   │   │   ├── AuthController.java
│   │   │   ├── CryptoController.java
│   │   │   ├── MonitoringController.java
│   │   │   ├── PortfolioController.java
│   │   │   ├── TradingBotController.java
│   │   │   └── AdminCleanupController.java
│   │   │
│   │   ├── model/                # Entidades JPA
│   │   │   ├── User.java
│   │   │   ├── CryptoCurrency.java
│   │   │   ├── AlertRule.java
│   │   │   ├── Portfolio.java
│   │   │   ├── Transaction.java
│   │   │   ├── TradingBot.java
│   │   │   └── VerificationToken.java
│   │   │
│   │   ├── repository/           # Repositórios JPA
│   │   │   ├── UserRepository.java
│   │   │   ├── CryptoCurrencyRepository.java
│   │   │   ├── AlertRuleRepository.java
│   │   │   ├── PortfolioRepository.java
│   │   │   └── TradingBotRepository.java
│   │   │
│   │   ├── security/             # JWT e autenticação
│   │   │   ├── JwtUtil.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── CustomUserDetailsService.java
│   │   │   ├── RateLimitFilter.java
│   │   │   └── MdcFilter.java
│   │   │
│   │   ├── service/              # Lógica de negócio
│   │   │   ├── AlertService.java
│   │   │   ├── CryptoService.java
│   │   │   ├── SmartCacheService.java       # ⭐ Cache inteligente
│   │   │   ├── CoinGeckoApiService.java
│   │   │   ├── CoinGeckoRequestQueue.java   # ⭐ Sistema de filas
│   │   │   ├── CryptoMonitoringService.java
│   │   │   ├── MonitoringControlService.java
│   │   │   ├── NotificationService.java
│   │   │   ├── EmailService.java
│   │   │   ├── SendGridEmailService.java
│   │   │   ├── PortfolioService.java
│   │   │   ├── TradingBotService.java
│   │   │   ├── VerificationService.java
│   │   │   ├── UserCleanupService.java      # ⭐ Limpeza automática
│   │   │   ├── UserActivityTracker.java     # ⭐ Controle de inativos
│   │   │   ├── WebSocketService.java
│   │   │   └── RateLimitMetricsService.java
│   │   │
│   │   ├── util/                 # Utilitários
│   │   │   ├── InputSanitizer.java          # ⭐ Segurança
│   │   │   ├── LogMasker.java               # ⭐ Privacidade
│   │   │   └── CryptoSymbolMapper.java
│   │   │
│   │   ├── exception/            # Exceções personalizadas
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── RateLimitExceededException.java
│   │   │   └── CryptoNotFoundException.java
│   │   │
│   │   ├── event/                # Sistema de eventos
│   │   │   ├── CryptoUpdateEvent.java
│   │   │   └── AlertEventListener.java
│   │   │
│   │   └── health/               # Health checks
│   │       └── CoinGeckoHealthIndicator.java
│   │
│   ├── src/main/resources/
│   │   ├── application.yml       # Configurações principais
│   │   ├── logback-spring.xml    # Logs estruturados
│   │   └── db/migration/         # Scripts Flyway
│   │
│   ├── src/test/java/            # Testes
│   │   ├── integration/
│   │   ├── security/
│   │   └── service/
│   │
│   ├── Dockerfile
│   ├── render-build.sh
│   └── pom.xml
│
└── front/crypto-monitor-frontend/  # Frontend React
    ├── public/
    │   ├── index.html
    │   └── manifest.json
    │
    └── src/
        ├── components/
        │   ├── auth/             # Autenticação
        │   │   ├── AuthContainer.jsx
        │   │   ├── PasswordStrength.jsx
        │   │   └── VerifyEmailPage.jsx
        │   │
        │   ├── bots/             # Trading Bots
        │   │   └── CreateBotModal.jsx
        │   │
        │   ├── common/           # Componentes reutilizáveis
        │   │   ├── Button.jsx
        │   │   ├── ErrorMessage.jsx
        │   │   ├── Input.jsx
        │   │   ├── Skeleton.jsx         # ⭐ Loading states
        │   │   ├── ThemeToggle.jsx
        │   │   └── Toast.jsx            # ⭐ Notificações
        │   │
        │   ├── dashboard/        # Dashboard principal
        │   │   ├── ChartTabs.jsx
        │   │   ├── CryptoCard.jsx
        │   │   ├── CryptocurrenciesCard.jsx
        │   │   ├── Header.jsx
        │   │   ├── MultiCryptoChart.jsx
        │   │   ├── PriceChart.jsx
        │   │   ├── SettingsCard.jsx
        │   │   ├── StatCard.jsx
        │   │   ├── StatsCards.jsx
        │   │   └── StatusCard.jsx
        │   │
        │   ├── pages/            # Páginas
        │   │   ├── DashboardPage.jsx
        │   │   ├── LoginPage.jsx
        │   │   ├── RegisterPage.jsx
        │   │   ├── PortfolioPage.jsx
        │   │   └── TradingBotsPage.jsx
        │   │
        │   ├── portfolio/        # Portfolio
        │   │   ├── AddTransactionModal.jsx
        │   │   ├── PortfolioChart.jsx
        │   │   ├── PortfolioSummary.jsx
        │   │   ├── PortfolioTable.jsx
        │   │   └── TransactionHistory.jsx
        │   │
        │   ├── telegram/         # Telegram
        │   │   └── TelegramConfig.jsx
        │   │
        │   └── ErrorBoundary.jsx # ⭐ Error handling
        │
        ├── contexts/             # Context API
        │   ├── TelegramContext.jsx  # ⭐ Telegram state
        │   └── ThemeContext.jsx     # ⭐ Theme state
        │
        ├── hooks/                # Custom Hooks
        │   ├── useCryptoData.js     # ⭐ React Query
        │   ├── useFormValidation.js # ⭐ Form validation
        │   └── useHeartbeat.js      # ⭐ Keep-alive
        │
        ├── styles/               # Estilos organizados
        │   ├── base.css              # Reset + variáveis
        │   ├── utils.css             # Classes utilitárias
        │   ├── components/
        │   │   ├── auth.css
        │   │   ├── dashboard.css
        │   │   ├── telegram.css
        │   │   └── transactions.css
        │   ├── crypto-card.css
        │   ├── portfolio.css
        │   ├── trading-bots.css
        │   └── theme-toggle.css
        │
        ├── utils/                # Utilitários
        │   ├── constants.js          # ⭐ API URLs
        │   ├── debounce.js           # ⭐ Performance
        │   ├── formatters.js         # ⭐ Formatação
        │   ├── performance.js        # ⭐ Métricas
        │   └── storage.js            # ⭐ Storage manager
        │
        ├── workers/              # Web Workers
        │   └── crypto.worker.js      # ⭐ Cálculos pesados
        │
        ├── App.jsx               # Componente principal
        ├── index.js              # Entry point
        └── index.css             # Estilos globais
```

## 🔌 API Endpoints

### Autenticação

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/api/auth/register` | Cadastrar novo usuário | ❌ |
| POST | `/api/auth/login` | Login e obtenção de JWT | ❌ |
| POST | `/api/auth/verify` | Verificar código de 6 dígitos | ❌ |
| POST | `/api/auth/resend-code` | Reenviar código de verificação | ❌ |
| POST | `/api/auth/test-email` | Testar envio de email | ❌ |

### Criptomoedas

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| GET | `/api/crypto/current` | Buscar cotações atuais | ❌ |
| GET | `/api/crypto/current/{coinId}` | Buscar cotação específica | ❌ |
| GET | `/api/crypto/history/{coinId}?days=7` | Buscar histórico | ❌ |
| GET | `/api/crypto/status` | Status da API e cache | ❌ |
| POST | `/api/crypto/force-update` | Forçar atualização | ✅ |

### Monitoramento

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/api/monitoring/start` | Iniciar monitoramento | ✅ |
| POST | `/api/monitoring/stop` | Parar monitoramento | ✅ |
| GET | `/api/monitoring/status` | Status do monitoramento | ✅ |

### Portfolio

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| GET | `/api/portfolio` | Buscar portfolio | ✅ |
| POST | `/api/portfolio/transaction` | Adicionar transação | ✅ |
| GET | `/api/portfolio/transactions` | Listar transações | ✅ |
| DELETE | `/api/portfolio/transaction/{id}` | Deletar transação | ✅ |

### Trading Bots

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/api/bots` | Criar bot | ✅ |
| GET | `/api/bots` | Listar bots | ✅ |
| POST | `/api/bots/{id}/start` | Iniciar bot | ✅ |
| POST | `/api/bots/{id}/stop` | Parar bot | ✅ |
| GET | `/api/bots/{id}/trades` | Histórico de trades | ✅ |
| DELETE | `/api/bots/{id}` | Deletar bot | ✅ |

### Admin

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/api/admin/cleanup/run` | Executar limpeza manual | ✅ |
| GET | `/api/admin/cleanup/stats` | Estatísticas de limpeza | ✅ |
| POST | `/api/admin/cache/clear` | Limpar cache | ✅ |
| POST | `/api/admin/cache/warmup` | Aquecer cache | ✅ |

### WebSocket

| Endpoint | Descrição |
|----------|-----------|
| `/ws/crypto` | Conexão WebSocket |
| `/topic/prices` | Broadcast de preços |
| `/topic/system/status` | Status do sistema |

---

## 🎨 Frontend - Arquitetura e Funcionalidades

### Estrutura de Componentes
```
src/
├── components/
│   ├── auth/                    # Autenticação
│   │   ├── AuthContainer.jsx    # Container de autenticação
│   │   ├── PasswordStrength.jsx # Indicador de força da senha
│   │   └── VerifyEmailPage.jsx  # Verificação de email
│   │
│   ├── common/                  # Componentes reutilizáveis
│   │   ├── Button.jsx
│   │   ├── ErrorMessage.jsx
│   │   ├── Input.jsx
│   │   ├── Skeleton.jsx         # Loading skeletons
│   │   ├── ThemeToggle.jsx      # Toggle de tema
│   │   └── Toast.jsx            # Notificações toast
│   │
│   ├── dashboard/               # Dashboard principal
│   │   ├── Header.jsx
│   │   ├── StatusCard.jsx       # Card de status do monitoramento
│   │   ├── StatsCards.jsx       # Cards de estatísticas
│   │   ├── SettingsCard.jsx     # Configurações de monitoramento
│   │   ├── CryptocurrenciesCard.jsx  # Lista de criptos
│   │   ├── CryptoCard.jsx       # Card individual de cripto
│   │   ├── ChartTabs.jsx        # Tabs de gráficos
│   │   ├── PriceChart.jsx       # Gráfico de preço individual
│   │   └── MultiCryptoChart.jsx # Gráfico comparativo
│   │
│   ├── portfolio/               # Portfolio
│   │   ├── PortfolioTable.jsx   # Tabela de holdings
│   │   ├── PortfolioChart.jsx   # Gráfico de distribuição
│   │   ├── PortfolioSummary.jsx # Resumo do portfolio
│   │   ├── AddTransactionModal.jsx  # Modal de adicionar transação
│   │   └── TransactionHistory.jsx   # Histórico de transações
│   │
│   ├── bots/                    # Trading Bots
│   │   └── CreateBotModal.jsx   # Modal de criação de bot
│   │
│   ├── telegram/                # Telegram
│   │   └── TelegramConfig.jsx   # Configuração do Telegram
│   │
│   └── pages/                   # Páginas
│       ├── LoginPage.jsx
│       ├── RegisterPage.jsx
│       ├── DashboardPage.jsx
│       ├── PortfolioPage.jsx
│       └── TradingBotsPage.jsx
│
├── contexts/                    # Contexts do React
│   ├── ThemeContext.jsx         # Tema dark/light
│   └── TelegramContext.jsx      # Configurações do Telegram
│
├── hooks/                       # Custom Hooks
│   ├── useCryptoData.js         # React Query para cryptos
│   ├── useFormValidation.js     # Validação de formulários
│   └── useHeartbeat.js          # Heartbeat para manter sessão ativa
│
├── utils/                       # Utilitários
│   ├── constants.js             # Constantes (API_BASE_URL)
│   ├── formatters.js            # Formatação de valores
│   ├── storage.js               # Gerenciamento de storage
│   ├── debounce.js              # Debounce utility
│   └── performance.js           # Medição de performance
│
├── styles/                      # Estilos organizados
│   ├── base.css                 # Reset e variáveis CSS
│   ├── utils.css                # Classes utilitárias
│   ├── components/              # Estilos por componente
│   │   ├── auth.css
│   │   ├── dashboard.css
│   │   ├── telegram.css
│   │   └── transactions.css
│   ├── crypto-card.css
│   ├── portfolio.css
│   ├── trading-bots.css
│   └── theme-toggle.css
│
└── workers/                     # Web Workers
    └── crypto.worker.js         # Cálculos em background
```

### Tecnologias Frontend

- **React 18.3.1** - Biblioteca UI
- **React Query (TanStack Query)** - Gerenciamento de estado server
- **Lucide React** - Ícones
- **Recharts** - Gráficos
- **CryptoJS** - Criptografia (configs Telegram)
- **Tailwind CSS** - Utility-first CSS

### Funcionalidades do Frontend

#### 🎨 Sistema de Temas
- ✅ Dark Mode / Light Mode
- ✅ Persistência no localStorage
- ✅ Transições suaves
- ✅ Variáveis CSS para fácil customização

#### 🔐 Autenticação Completa
- ✅ Login com "Lembrar de mim"
- ✅ Registro com validação em tempo real
- ✅ Verificação de email com código de 6 dígitos
- ✅ Indicador de força de senha
- ✅ Sessão automática (localStorage vs sessionStorage)
- ✅ Timeout automático de requisições

#### 📊 Dashboard Interativo
- ✅ Cards de criptomoedas com animações
- ✅ Busca com debounce (300ms)
- ✅ Filtros e ordenação (market cap, preço, variação, nome)
- ✅ Seleção múltipla de criptomoedas
- ✅ Status do monitoramento em tempo real
- ✅ Skeleton loaders durante carregamento

#### 📈 Gráficos Avançados
- ✅ Gráfico individual de preço (line/area)
- ✅ Gráfico comparativo de múltiplas cryptos
- ✅ Períodos: 24h, 7d, 30d, 90d, 1y
- ✅ Tooltips customizados
- ✅ Responsivo

#### 💼 Portfolio Management
- ✅ Gerenciamento de transações (compra/venda)
- ✅ Cálculo automático de lucro/prejuízo
- ✅ Gráfico de distribuição (PieChart)
- ✅ Histórico completo de transações
- ✅ Uso de preço atual ao adicionar transação
- ✅ Filtros por tipo (todas, compras, vendas)

#### 🤖 Trading Bots (Simulação)
- ✅ Criação de bots (Grid Trading, DCA, Stop Loss)
- ✅ Dashboard de bots com estatísticas
- ✅ Controle de bots (start/stop/delete)
- ✅ Métricas de performance
- ✅ Filtros (todos, ativos, inativos)

#### 📱 Integração Telegram
- ✅ Configuração de bot do Telegram
- ✅ Teste de conexão antes de ativar
- ✅ Armazenamento criptografado das credenciais (CryptoJS)
- ✅ Persistência das configurações
- ✅ Envio de alertas via Telegram
- ✅ Instruções passo-a-passo para configuração

#### 🔔 Sistema de Notificações
- ✅ Toast notifications (success, error, info)
- ✅ Alertas visuais no dashboard
- ✅ Suporte a email e Telegram simultaneamente

#### ⚡ Performance e Otimização
- ✅ React.memo para evitar re-renders
- ✅ useMemo e useCallback estratégicos
- ✅ Lazy loading de páginas
- ✅ Code splitting automático
- ✅ Debounce em buscas
- ✅ Skeleton loaders
- ✅ Error boundaries
- ✅ Web Workers para cálculos pesados

#### 🛡️ Segurança Frontend
- ✅ Validação de formulários em tempo real
- ✅ Sanitização de inputs
- ✅ Criptografia de dados sensíveis (Telegram)
- ✅ Proteção contra XSS
- ✅ Gerenciamento seguro de tokens JWT

#### 💾 Gerenciamento de Estado
- ✅ React Query para dados server
- ✅ Context API para tema e Telegram
- ✅ localStorage/sessionStorage para persistência
- ✅ Heartbeat para manter sessão ativa

---

## 🧠 Sistema de Cache Inteligente

### SmartCacheService

O `SmartCacheService` implementa uma estratégia de cache em 3 camadas:

**1. Memória (Caffeine)**
- TTL: 30 minutos
- Primeira verificação
- Mais rápido

**2. Banco de Dados**
- TTL: 2 horas
- Fallback se memória expirou
- Dados persistidos

**3. CoinGecko API**
- Apenas se BD expirou
- Verificação de rate limit
- Via fila com prioridade

### Proteção contra Rate Limit
```java
// Verificações antes de fazer request:
✅ 1. Requests/minuto < 25 (buffer de segurança)
✅ 2. Última atualização > 60 minutos
✅ 3. Proteção não ativa (após 429)
✅ 4. Fila não saturada
```

### Estatísticas
```bash
# Sem cache (teoria):
1800 requests/hora (30 req/min * 60 min)

# Com SmartCache (prática):
~2 requests/hora

# Redução: 99.89%
```

---

## 🛡️ Rate Limiting

### Por IP (RateLimitFilter)
```java
// Limites por tipo de endpoint
API Geral:       100 req/min
Autenticação:     10 req/min
Admin:            50 req/min
```

### CoinGecko API (CoinGeckoRequestQueue)
```java
// Proteção inteligente
Intervalo mínimo:  30 segundos entre requests
Máximo/minuto:     3 requests (buffer de segurança)
Timeout:           60 segundos por request
Fila priorizada:   HIGH > NORMAL > LOW
```

### Headers de Rate Limit
```http
X-Rate-Limit-Remaining: 95
X-Rate-Limit-Retry-After: 30
```

---

## 🔒 Segurança

### Proteções Implementadas

#### Input Sanitization
- ✅ Proteção contra SQL Injection
- ✅ Proteção contra XSS
- ✅ Proteção contra Path Traversal
- ✅ Validação de emails, usernames e coin IDs

#### Rate Limiting
- ✅ Bucket4j para controle de requisições
- ✅ Limites por tipo de endpoint:
  - API: 100 req/min
  - Auth: 10 req/min
  - Admin: 50 req/min
- ✅ Headers informativos (X-Rate-Limit-Remaining)

#### Autenticação JWT
- ✅ Tokens com expiração configurável (24h padrão)
- ✅ Refresh tokens (7 dias)
- ✅ Validação de issuer
- ✅ Senhas com BCrypt

#### Sistema de Verificação
- ✅ Código de 6 dígitos por email
- ✅ Expiração de 24 horas
- ✅ Retry automático com backoff
- ✅ Cleanup de tokens expirados

#### Logs Estruturados
- ✅ Logback com JSON (Logstash)
- ✅ Audit logs separados (90 dias)
- ✅ MDC para rastreamento (requestId, clientIp, username)
- ✅ Mascaramento de dados sensíveis

---

## 📊 Monitoramento

### Actuator Endpoints
```
/actuator/health          # Health check
/actuator/prometheus      # Métricas Prometheus
```

### Métricas Customizadas
```yaml
# CoinGecko
crypto_coingecko_request_duration_seconds
coingecko_ratelimit_hits_total
coingecko_requests_success_total

# Cache
crypto_cache_hit_rate

# Alertas
crypto_alert_processing_duration_seconds
crypto_active_monitoring_users

# WebSocket
crypto_websocket_connections_total
crypto_websocket_messages_total

# Rate Limit
crypto_rate_limit_hits_total
crypto_rate_limit_queue_size
```

### Health Indicators

- ✅ CoinGecko API availability
- ✅ Cache status
- ✅ Database connectivity
- ✅ Rate limit status

---

## 🚀 Deploy

### Frontend (Vercel)
```bash
# 1. Conectar repositório ao Vercel
# 2. Configurar variáveis de ambiente:
REACT_APP_API_URL=https://seu-backend.onrender.com/crypto-monitor/api

# 3. Build automático a cada push
# 4. Preview deployments para branches
```

### Backend (Render)

1. Crie conta no [Render](https://render.com)
2. New → Web Service
3. Conecte seu repositório GitHub
4. Configure:
   - **Root Directory**: `back`
   - **Build Command**: `./mvnw clean install -DskipTests`
   - **Start Command**: `java -jar target/crypto-monitor.jar`
5. Adicione variáveis de ambiente:
```
   SPRING_DATASOURCE_URL
   SPRING_DATASOURCE_USERNAME
   SPRING_DATASOURCE_PASSWORD
   JWT_SECRET
   SENDGRID_API_KEY
   SENDGRID_FROM_EMAIL
   CORS_ORIGINS=https://seu-frontend.vercel.app
```

### Docker
```bash
# Build Backend
cd back
docker build -t crypto-monitor-backend .

# Build Frontend
cd front/crypto-monitor-frontend
docker build -t crypto-monitor-frontend .

# Run com Docker Compose
docker-compose up -d
```

### Docker Compose
```yaml
version: '3.8'
services:
  backend:
    build: ./back
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/crypto
      - JWT_SECRET=${JWT_SECRET}
      - SENDGRID_API_KEY=${SENDGRID_API_KEY}
    depends_on:
      - db
  
  frontend:
    build: ./front/crypto-monitor-frontend
    ports:
      - "3000:3000"
    environment:
      - REACT_APP_API_URL=http://localhost:8080/crypto-monitor/api
  
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: crypto
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## 🧪 Testes

### Backend
```bash
# Todos os testes
./mvnw test

# Apenas unitários
./mvnw test -Dtest="*Test"

# Apenas integração
./mvnw test -Dtest="*IntegrationTest"

# Com coverage (Jacoco)
./mvnw clean test jacoco:report
```

### Frontend
```bash
cd front/crypto-monitor-frontend

# Executar testes
npm test

# Com coverage
npm test -- --coverage

# Watch mode
npm test -- --watch
```

### Testes Implementados

**Backend:**
- ✅ InputSanitizerTest: SQL Injection, XSS, Path Traversal
- ✅ JwtUtilTest: Geração e validação de JWT
- ✅ PortfolioServiceTest: Lógica de compra/venda
- ✅ AuthControllerIntegrationTest: Fluxo de autenticação
- ✅ CryptoControllerIntegrationTest: Endpoints públicos

**Frontend:**
- ✅ App.test.js: Renderização básica
- ✅ Componentes isolados
- ✅ Hooks customizados

---

## 🔧 Troubleshooting

### Frontend

**Problema: Erro de CORS**
```bash
# Solução: Verificar variável de ambiente no backend
CORS_ORIGINS=https://seu-frontend.vercel.app,http://localhost:3000
```

**Problema: Token expirado**
```bash
# Solução: Fazer logout e login novamente
# Ou implementar refresh token
```

**Problema: Telegram não conecta**
```bash
# Soluções:
1. Verificar token do bot com @BotFather
2. Confirmar Chat ID com @userinfobot
3. Clicar em /start no bot
4. Testar conexão no modal
```

**Problema: Gráficos não aparecem**
```bash
# Solução: Verificar se há dados de histórico
# Backend deve retornar array de pontos
```

### Backend

**Problema: SendGrid não envia emails**
```bash
# Soluções:
1. Verificar API Key
2. Verificar email verificado no SendGrid
3. Checar logs: logs/crypto-monitor.log
4. Testar endpoint: POST /api/auth/test-email
```

**Problema: Rate limit 429**
```bash
# Solução: Aguardar 60 segundos
# Sistema de cache evita isso automaticamente
```

**Problema: PostgreSQL connection refused**
```bash
# Soluções:
1. Verificar se PostgreSQL está rodando
2. Confirmar credenciais no application.yml
3. Usar H2 para testes: remover variáveis SPRING_DATASOURCE_*
```

---

## 📚 Documentação Adicional

### Formatters Utility

O projeto inclui um conjunto completo de formatadores em `src/utils/formatters.js`:
```javascript
import { 
  formatCurrency,        // "$1,234.56"
  formatPercent,         // "5.25%"
  formatPercentWithSign, // "+5.25%"
  formatDate,            // "01/12/2024"
  formatCompactNumber,   // "1.5M"
  formatMarketCap,       // "$845.2B"
  formatVolume           // "$1.2B"
} from './utils/formatters';
```

### Custom Hooks

**useCryptoData** - React Query para criptomoedas
```javascript
const { data, isLoading, refetch } = useCryptos(token);
```

**useFormValidation** - Validação de formulários
```javascript
const { values, errors, handleChange, handleSubmit } = useFormValidation(
  initialValues,
  validationRules
);
```

**useHeartbeat** - Manter sessão ativa
```javascript
useHeartbeat(isActive, username, stompClient);
```

### Contexts

**ThemeContext** - Gerenciamento de tema
```javascript
const { isDark, toggleTheme } = useTheme();
```

**TelegramContext** - Configurações do Telegram
```javascript
const { telegramConfig, updateConfig, isConfigured } = useTelegram();
```

---

## 🎓 Como Usar

### 1. Primeiro Acesso

1. Acesse o frontend
2. Clique em "Cadastre-se agora"
3. Preencha username, email e senha
4. Verifique seu email e insira o código de 6 dígitos
5. Faça login

### 2. Configurar Monitoramento

1. No Dashboard, selecione as criptomoedas que deseja monitorar
2. Configure seu email para alertas
3. Defina intervalo de verificação (recomendado: 5 minutos)
4. Configure thresholds de compra/venda
5. Clique em "Iniciar Monitoramento"

### 3. Configurar Telegram (Opcional)

1. Clique no botão "Telegram" no header
2. Siga as instruções para criar um bot com @BotFather
3. Obtenha seu Chat ID com @userinfobot
4. Cole as credenciais e teste a conexão
5. Salve as configurações

### 4. Gerenciar Portfolio

1. Clique em "Portfolio" no header
2. Adicione transações (compra/venda)
3. Visualize lucro/prejuízo em tempo real
4. Acompanhe distribuição no gráfico de pizza

### 5. Criar Trading Bots

1. Clique em "Trading Bots" no header
2. Clique em "+ Novo Bot"
3. Escolha estratégia (Grid Trading, DCA, Stop Loss)
4. Configure parâmetros
5. Inicie o bot (simulação)

---

## 🏆 Roadmap

### Frontend
- [ ] PWA (Progressive Web App)
- [ ] Notificações push nativas
- [ ] App mobile com React Native
- [ ] Dashboard customizável (drag & drop)
- [ ] Temas personalizados
- [ ] Modo offline
- [ ] Gráficos mais avançados (candlestick)
- [ ] Comparação de portfolios

### Backend
- [ ] API pública com rate limiting
- [ ] Integração com exchanges reais
- [ ] Backtesting de estratégias
- [ ] Alertas por volume de negociação
- [ ] Machine Learning para predições
- [ ] Exportação de relatórios (PDF/CSV)
- [ ] Sistema de notificações por Discord
- [ ] Multi-tenancy

### DevOps
- [ ] CI/CD com GitHub Actions
- [ ] Testes E2E com Cypress
- [ ] Monitoramento com Grafana
- [ ] Logs centralizados com ELK Stack
- [ ] Kubernetes deployment
- [ ] Auto-scaling

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Siga os passos:

1. Fork o projeto
2. Crie uma branch para sua feature:
```bash
   git checkout -b feature/MinhaFeature
```
3. Commit suas mudanças:
```bash
   git commit -m 'feat: adiciona MinhaFeature'
```
4. Push para a branch:
```bash
   git push origin feature/MinhaFeature
```
5. Abra um Pull Request

### Padrões de Commit

Seguimos o [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` Nova funcionalidade
- `fix:` Correção de bug
- `docs:` Documentação
- `style:` Formatação
- `refactor:` Refatoração
- `test:` Testes
- `chore:` Tarefas de manutenção

---

## 📞 Suporte

Para dúvidas ou problemas:

- 📧 Abra uma [issue no GitHub](https://github.com/seu-usuario/crypto-monitor/issues)
- 📖 Consulte a [documentação do Spring Boot](https://spring.io/projects/spring-boot)
- 📖 Consulte a [documentação do React](https://react.dev/)
- 📝 Verifique logs:
  - Backend: `logs/crypto-monitor.log`
  - Frontend: Console do navegador
- 📚 Documentação de testes: `back/docs/TESTING.md`

---

## 📄 Licença

Este projeto é de código aberto para fins educacionais.

---

## 🙏 Agradecimentos

- [CoinGecko](https://www.coingecko.com/) - API de cotações
- [SendGrid](https://sendgrid.com/) - Serviço de email
- [Recharts](https://recharts.org/) - Biblioteca de gráficos
- [Lucide](https://lucide.dev/) - Ícones
- [Spring Boot](https://spring.io/projects/spring-boot) - Framework backend
- [React](https://react.dev/) - Biblioteca frontend

---

## 📊 Status do Projeto

![Status](https://img.shields.io/badge/status-active-success)
![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-85%25-green)
![License](https://img.shields.io/badge/license-MIT-blue)

---

**⭐ Se este projeto foi útil, considere dar uma estrela no GitHub!**

**🚀 Desenvolvido com ❤️ para a comunidade de desenvolvedores**
