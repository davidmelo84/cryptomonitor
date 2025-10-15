# Crypto Monitor 🚀

Sistema completo de monitoramento de criptomoedas com alertas automáticos por email.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![React](https://img.shields.io/badge/React-19.2.0-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-12+-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Executando o Projeto](#executando-o-projeto)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [API Endpoints](#api-endpoints)
- [Como Usar](#como-usar)
- [Troubleshooting](#troubleshooting)
- [Exemplo de Uso](#exemplo-de-uso-real)

---

## 🎯 Sobre o Projeto

O **Crypto Monitor** é um sistema de monitoramento de criptomoedas em tempo real que permite:

- 📊 Acompanhar cotações de múltiplas criptomoedas
- 🔔 Configurar alertas personalizados de preço
- 📧 Receber notificações automáticas por email
- 👤 Gerenciar monitoramento individual por usuário
- ⏱️ Definir intervalos de verificação customizados

O sistema busca dados da **CoinGecko API** e envia alertas quando detecta variações significativas nos preços configurados pelo usuário.

---

## 🛠️ Tecnologias

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - Spring Mail
  - Spring Validation
- **PostgreSQL** (produção)
- **H2 Database** (desenvolvimento/testes)
- **JWT** (autenticação)
- **Flyway** (migrations)
- **Lombok**
- **WebFlux** (chamadas assíncronas)

### Frontend
- **React 19.2.0**
- **Lucide React** (ícones)
- **Tailwind CSS**
- **Fetch API**

---

## ✨ Funcionalidades

### Autenticação
- ✅ Cadastro de usuários
- ✅ Login com JWT
- ✅ Proteção de rotas

### Monitoramento
- ✅ Seleção de múltiplas criptomoedas
- ✅ Configuração de intervalos (1min - 1h)
- ✅ Thresholds personalizados (compra/venda)
- ✅ Início/parada de monitoramento sob demanda
- ✅ Sistema anti-spam de notificações (cooldown de 30min)

### Alertas
- ✅ Alerta de queda de preço (oportunidade de compra)
- ✅ Alerta de alta de preço (oportunidade de venda)
- ✅ Variação percentual em 24h
- ✅ Notificações por email

### Dashboard
- ✅ Visualização em tempo real
- ✅ Estatísticas consolidadas
- ✅ Busca e filtros
- ✅ Ordenação por market cap/preço/variação
- ✅ Interface responsiva

---

## 📦 Pré-requisitos

### Backend
- Java JDK 17+
- Maven 3.6+
- PostgreSQL 12+ (ou usar H2 para desenvolvimento)

### Frontend
- Node.js 16+
- npm 8+

### Email (Opcional, mas recomendado)
- Conta Gmail com senha de app configurada

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
CREATE DATABASE SEUBANCO;
CREATE USER SEUUSER WITH PASSWORD SUASENHA;
GRANT ALL PRIVILEGES ON DATABASE SEUBANCO TO SEUUSER;
```

2. Configure `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/SEUBANCO
    username: SEUUSER
    password: SUASENHA
```

#### Opção B: H2 (Desenvolvimento)

O projeto já vem configurado para usar H2 como alternativa. Basta comentar as configurações do PostgreSQL no `application.yml`.

### 3. Configure Email (Gmail)

1. Ative a verificação em duas etapas na sua conta Google
2. Gere uma senha de app em: https://myaccount.google.com/apppasswords
3. Configure no `application.yml`:
```yaml
spring:
  mail:
    username: seu-email@gmail.com
    password: sua-senha-de-app-aqui

notification:
  email:
    from: seu-email@gmail.com
```

### 4. Instale dependências do Frontend
```bash
cd front/crypto-monitor-frontend
npm install
```

---

## ⚙️ Configuração

### application.yml (Backend)
```yaml
# Porta do servidor
server:
  port: 8080
  servlet:
    context-path: /crypto-monitor

# Criptomoedas monitoradas
crypto:
  coins: bitcoin,ethereum,cardano,polkadot,chainlink,solana,avalanche-2,polygon,litecoin,bitcoin-cash

# Configurações de alerta
alert:
  buy:
    threshold: -5.0  # Alerta quando cair 5%
  sell:
    threshold: 10.0  # Alerta quando subir 10%

# Cooldown de notificações (minutos)
notification:
  email:
    cooldown-minutes: 30

# JWT
jwt:
  secret: sua-chave-secreta-aqui
  expiration: 86400000  # 24 horas
```

### Variáveis de Ambiente (Opcional)

Crie um arquivo `.env` na raiz do backend:
```env
# Email
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-app

# JWT
JWT_SECRET=sua-chave-jwt-segura

# Telegram (Opcional)
TELEGRAM_ENABLED=false
TELEGRAM_BOT_TOKEN=seu-token
TELEGRAM_CHAT_ID=seu-chat-id
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

### Verificação

- **API Health Check**: http://localhost:8080/crypto-monitor/actuator/health
- **API Status**: http://localhost:8080/crypto-monitor/api/crypto/status
- **H2 Console** (se habilitado): http://localhost:8080/crypto-monitor/h2-console

---

## 📁 Estrutura do Projeto
```
crypto-monitor/
│
├── back/                          # Backend Spring Boot
│   ├── src/main/java/com/crypto/
│   │   ├── config/               # Configurações (Security, WebClient, etc)
│   │   ├── controller/           # REST Controllers
│   │   ├── model/                # Entidades JPA
│   │   ├── repository/           # Repositórios JPA
│   │   ├── security/             # JWT e autenticação
│   │   └── service/              # Lógica de negócio
│   │       ├── AlertService      # Gerenciamento de alertas
│   │       ├── CryptoService     # Integração CoinGecko API
│   │       ├── CryptoMonitoringService  # Coordenação de monitoramento
│   │       ├── MonitoringControlService # Controle por usuário
│   │       ├── NotificationService      # Envio de notificações
│   │       └── EmailService      # Envio de emails
│   │
│   └── src/main/resources/
│       ├── application.yml       # Configurações principais
│       └── db/migration/         # Scripts Flyway
│
└── front/crypto-monitor-frontend/  # Frontend React
    ├── public/
    └── src/
        ├── components/
        │   ├── auth/             # Login e Registro
        │   ├── common/           # Componentes reutilizáveis
        │   ├── dashboard/        # Dashboard e cards
        │   └── pages/            # Páginas principais
        ├── utils/                # Utilitários e constantes
        └── App.jsx               # Componente principal
```

---

## 🔌 API Endpoints

### Autenticação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Cadastrar novo usuário |
| POST | `/api/auth/login` | Login e obtenção de JWT |

### Criptomoedas

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/crypto/current` | Buscar cotações atuais (público) |
| GET | `/api/crypto/current/{coinId}` | Buscar cotação específica |
| GET | `/api/crypto/saved` | Buscar cryptos salvas no banco |
| POST | `/api/crypto/update` | Forçar atualização manual |

### Monitoramento

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/api/monitoring/start` | Iniciar monitoramento | ✅ |
| POST | `/api/monitoring/stop` | Parar monitoramento | ✅ |
| GET | `/api/monitoring/status` | Status do monitoramento | ✅ |

### Alertas

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/api/crypto/alerts` | Criar regra de alerta | ✅ |
| GET | `/api/crypto/alerts` | Listar alertas ativos | ✅ |
| DELETE | `/api/crypto/alerts/{id}` | Desativar alerta | ✅ |

---

## 🎮 Como Usar

### 1. Cadastro e Login

1. Acesse `http://localhost:3000`
2. Clique em "Cadastre-se"
3. Preencha: usuário, email e senha
4. Faça login com suas credenciais

### 2. Configurar Monitoramento

1. **Email**: Insira o email que receberá os alertas
2. **Intervalo**: Escolha a frequência de verificação (recomendado: 5 minutos)
3. **Threshold de Compra**: % de queda para alerta (ex: 5%)
4. **Threshold de Venda**: % de alta para alerta (ex: 10%)

### 3. Selecionar Criptomoedas

1. Na seção "Criptomoedas Disponíveis", clique nas moedas desejadas
2. Use a busca para encontrar rapidamente
3. Ordene por Market Cap, Preço ou Variação
4. As moedas selecionadas ficam destacadas em azul

### 4. Iniciar Monitoramento

1. Clique no botão "▶ Iniciar"
2. O sistema criará automaticamente:
   - 1 regra de alerta de QUEDA por moeda (oportunidade de compra)
   - 1 regra de alerta de ALTA por moeda (oportunidade de venda)
3. O status mudará para "✓ Monitoramento Ativo"

### 5. Receber Alertas

Você receberá emails quando:
- Uma crypto cair mais que o threshold de compra configurado
- Uma crypto subir mais que o threshold de venda configurado

**Exemplo de email**:
```
🔔 Alerta Crypto: Bitcoin (BTC)

📉 Bitcoin (BTC) caiu para $40,250.00 (limite $42,000.00)
Variação 24h: -5.25%

Esta é uma oportunidade de compra!
```

### 6. Parar Monitoramento

1. Clique em "■ Parar"
2. O sistema para as verificações automáticas
3. Os alertas são mantidos no banco para uso futuro

---

## 🐛 Troubleshooting

### Problema: Emails não são enviados

**Solução**:
1. Verifique se a senha de app do Gmail está correta
2. Confirme que a verificação em duas etapas está ativa
3. Verifique logs: `SMTP Authentication Failed`
4. Teste com endpoint: `POST /api/crypto/test-notification`

### Problema: Erro ao conectar com PostgreSQL

**Solução**:
1. Verifique se o PostgreSQL está rodando: `sudo systemctl status postgresql`
2. Confirme credenciais no `application.yml`
3. Teste conexão: `psql -U crypto_user -d crypto_monitor`
4. Alternativa: Use H2 para desenvolvimento

### Problema: Frontend não conecta com Backend

**Solução**:
1. Verifique se backend está rodando na porta 8080
2. Confirme `proxy` no `package.json`: `"proxy": "http://localhost:8080"`
3. Limpe cache do navegador (Ctrl+Shift+Delete)
4. Reinicie ambos servidores

### Problema: CoinGecko API Rate Limit

**Solução**:
1. A API gratuita tem limite de 30 requisições/minuto
2. Use intervalos maiores (10-15 minutos)
3. Selecione menos criptomoedas
4. Considere upgrade para CoinGecko Pro

### Problema: JWT Token Inválido

**Solução**:
1. Faça logout e login novamente
2. Limpe localStorage do navegador
3. Verifique `jwt.secret` no `application.yml`
4. Token expira em 24h por padrão

---

## 📊 Exemplo de Uso Real
```bash
# Cenário: Monitorar Bitcoin e Ethereum

1. Selecione BTC e ETH no dashboard
2. Configure:
   - Email: investidor@exemplo.com
   - Intervalo: 5 minutos
   - Compra: -5% (alerta se cair 5%)
   - Venda: +10% (alerta se subir 10%)

3. Inicie monitoramento

# O que acontece:
✅ Sistema cria 4 alertas automaticamente:
   - BTC: Alerta de queda (-5%)
   - BTC: Alerta de alta (+10%)
   - ETH: Alerta de queda (-5%)
   - ETH: Alerta de alta (+10%)

✅ A cada 5 minutos:
   - Busca preços atuais da CoinGecko
   - Compara com thresholds configurados
   - Envia email se condições atendidas
   - Aguarda 30min antes de enviar novo alerta da mesma moeda (cooldown)

📧 Você receberá email quando:
   - BTC cair para $41,000 (se estava em $43,000)
   - ETH subir para $2,500 (se estava em $2,250)
```

---

## 🔐 Segurança

- ✅ Senhas criptografadas com BCrypt
- ✅ JWT para autenticação stateless
- ✅ CORS configurado
- ✅ Validação de inputs com Bean Validation
- ✅ SQL Injection prevention (JPA)
- ✅ Rate limiting na CoinGecko API

---

## 📈 Melhorias Futuras

- [ ] Dashboard com gráficos históricos
- [ ] Notificações via Telegram
- [ ] Alertas por volume de negociação
- [ ] Exportação de relatórios
- [ ] Modo escuro
- [ ] App mobile (React Native)
- [ ] Integração com exchanges

---

## 👨‍💻 Autor

Desenvolvido como projeto de monitoramento de criptomoedas com foco em:
- Arquitetura escalável
- Código limpo e documentado
- Boas práticas Spring Boot
- UX/UI moderna e responsiva

---

## 📄 Licença

Este projeto é de código aberto para fins educacionais.

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Sinta-se à vontade para:
1. Fazer fork do projeto
2. Criar uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona MinhaFeature'`)
4. Push para a branch (`git push origin feature/MinhaFeature`)
5. Abrir um Pull Request

---

## 📞 Suporte

Para dúvidas ou problemas:
- Abra uma issue no GitHub
- Consulte a documentação do Spring Boot
- Verifique logs em `logs/crypto-monitor.log`

---

**⭐ Se este projeto foi útil, considere dar uma estrela!**