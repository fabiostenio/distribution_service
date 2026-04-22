# 🏦 FlowPay — Distribution Service

> Sistema de distribuição automática de atendimentos para fintech, com controle de fila, anti-race condition e dashboard em tempo real.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Stack Tecnológica](#-stack-tecnológica)
- [Arquitetura](#-arquitetura)
- [Regras de Negócio](#-regras-de-negócio)
- [Instalação Local — Backend](#-instalação-local--backend)
- [Makefile — Referência Completa](#-makefile--referência-completa)
- [Instalação Local — Frontend](#-instalação-local--frontend)
- [Endpoints da API](#-endpoints-da-api)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Documentação Adicional](#-documentação-adicional)

---

## 🌐 Visão Geral

O **FlowPay Distribution Service** distribui tickets de atendimento entre agentes organizados em 3 times. Cada agente suporta até **3 atendimentos simultâneos**. Quando todos os slots estão ocupados, o ticket entra em **fila FIFO por time** e é redistribuído automaticamente assim que um agente fica disponível.

O sistema protege contra **race conditions** com Optimistic Locking (`@Version` no Hibernate) combinado com **retry automático** (`@Retryable` do Spring Retry).

---

## 🛠️ Stack Tecnológica

### Backend
| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 3.5.x | Framework principal |
| Spring Data JPA | — | Persistência |
| Spring Retry | — | Retry com backoff exponencial |
| Hibernate | 6.x | ORM + Optimistic Locking |
| MySQL | 8.0 | Banco de dados |
| Docker + Compose | — | Infraestrutura local |
| Maven | 3.9.x | Build |

### Frontend _(em desenvolvimento)_
| Tecnologia | Versão | Papel |
|---|---|---|
| Angular | 17+ | Framework SPA |
| TypeScript | strict | Linguagem |
| CSS puro | — | Estilos (dark theme) |
| RxJS | — | Polling reativo |

---

## 🏗️ Arquitetura

```
┌────────────────────────────────────────────────────┐
│                   Docker Network                   │
│                                                    │
│  ┌──────────────────┐     ┌──────────────────────┐ │
│  │   distribution-  │     │   distribution-db    │ │
│  │       app        │────▶│   (MySQL 8.0)        │ │
│  │  Spring Boot 3   │     │   porta interna 3306 │ │
│  │  porta 8080      │     │   porta host 3307    │ │
│  └──────────────────┘     └──────────────────────┘ │
└────────────────────────────────────────────────────┘
          │
          ▼ HTTP
   ┌─────────────┐
   │   Frontend  │  Angular — GET /dashboard (polling 10s)
   │  porta 4200 │
   └─────────────┘
```

### Fluxo de distribuição

```
POST /atendimentos
       │
       ▼
 Resolver time ── time informado ────────────────────┐
       │                                             │
       └── time omitido → TeamKeyword.resolve() ─────┘
                                                     │
                                                     ▼
                                           Agente disponível?
                                          /                  \
                                       SIM                   NÃO
                                        │                     │
                                  status=ABERTO         status=FILA
                                  agente atribuído      posicaoFila=N
                                        │
                          ┌─────────────────────────┐
                          │  Optimistic Locking      │
                          │  @Version em Agent       │
                          │  Conflito → @Retryable   │
                          │  (3x, backoff 50ms×2)    │
                          └─────────────────────────┘

PATCH /atendimentos/{id}/finalizar
       │
       ▼
  Libera slot do agente
       │
       ▼
  Próximo da fila (FIFO) → promovido para ABERTO automaticamente
```

---

## 📐 Regras de Negócio

| Regra | Valor |
|---|---|
| Máximo de atendimentos simultâneos por agente | **3** |
| Política de fila | **FIFO por time** (não global) |
| Redistribuição ao finalizar | **Automática e imediata** |
| Roteamento sem `time` | Por keyword no assunto |
| Times disponíveis | `CARTOES`, `EMPRESTIMOS`, `OUTROS` |

### Agentes iniciais (seed automático)
| Time | Agente |
|---|---|
| CARTOES | Ana Lima |
| EMPRESTIMOS | Carla Mendes |
| OUTROS | Eva Rocha |

### Roteamento automático por assunto
| Keyword detectada (case-insensitive) | Time roteado |
|---|---|
| `problemas com cartão` | `CARTOES` |
| `contratação de empréstimo` | `EMPRESTIMOS` |
| _(qualquer outro assunto)_ | `OUTROS` |

---

## 🚀 Instalação Local — Backend

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) 24+
- [Docker Compose](https://docs.docker.com/compose/) (incluso no Docker Desktop)
- `make` (Linux/macOS: nativo | Windows: [WSL2](https://learn.microsoft.com/pt-br/windows/wsl/install) recomendado)
- `curl` (para verificar status — opcional)

> **Não é necessário** ter Java, Maven ou MySQL instalados localmente. Tudo roda dentro dos containers Docker.

---

### Passo a passo

#### 1. Clone o repositório

```bash
git clone https://github.com/fabiostenio/distribution_service.git
cd distribution_service
```

#### 2. Suba o ambiente completo

O comando abaixo faz o build da imagem Java, sobe o banco MySQL e inicia a aplicação:

```bash
make up
```

> O primeiro `make up` pode levar alguns minutos — o Maven baixa as dependências e compila o projeto dentro do container.

#### 3. Acompanhe os logs até a aplicação estar pronta

```bash
make logs-app
```

Aguarde a mensagem:
```
Started DistributionServiceApplication in X.XXX seconds
```

Pressione `Ctrl+C` para sair dos logs (os containers continuam rodando em background).

#### 4. Verifique se está funcionando

```bash
make status
```

Resposta esperada:
```
Backend FlowPay está online e operando!
```

#### 5. (Opcional) Consulte a dashboard via curl

```bash
curl -s http://localhost:8080/dashboard | python3 -m json.tool
```

---

### Ciclo de desenvolvimento

```bash
# Subir tudo (build + containers)
make up

# Acompanhar logs da aplicação
make logs-app

# Rebuildar e reiniciar após mudanças no código
make build && make restart

# Parar os containers (preserva os dados do banco)
make down

# Apagar tudo incluindo o volume do banco (ambiente 100% limpo)
make clean && make up
```

---

## 🧰 Makefile — Referência Completa

| Comando | Descrição |
|---|---|
| `make up` | Build da imagem + sobe todos os containers em background |
| `make up-db` | Sobe apenas o banco de dados (sem a aplicação) |
| `make build` | Rebuilda a imagem da aplicação sem subir |
| `make down` | Para os containers (preserva os dados do banco) |
| `make clean` | Para + remove volumes (⚠️ apaga todos os dados do banco) |
| `make restart` | Reinicia todos os containers |
| `make logs` | Logs de todos os serviços em tempo real |
| `make logs-app` | Logs apenas da aplicação Spring Boot |
| `make logs-db` | Logs apenas do MySQL |
| `make ps` | Lista o status e portas dos containers |
| `make status` | Faz `GET /api/status` e imprime a resposta |
| `make db-shell` | Abre o shell MySQL dentro do container do banco |
| `make app-shell` | Abre um shell `sh` dentro do container da aplicação |

> **Dica:** `make clean && make up` é o comando padrão para reiniciar com banco zerado durante desenvolvimento.

---

## 🖥️ Instalação Local — Frontend

O frontend é uma **SPA Angular** que consome a API do backend via proxy. O backend precisa estar rodando antes de iniciar o frontend.

### Pré-requisitos

- [Node.js](https://nodejs.org/) 20+
- [Angular CLI](https://angular.io/cli) 17+

```bash
# Instalar Angular CLI globalmente (se ainda não tiver)
npm install -g @angular/cli
```

---

### Passo a passo

#### 1. Certifique-se de que o backend está rodando

Abra um terminal na pasta do backend e execute:

```bash
make up
```

Confirme que está saudável:

```bash
make status
# Backend FlowPay está online e operando!
```

#### 2. Abra um novo terminal e acesse a pasta do frontend

```bash
cd ../distribution-dashboard   # ajuste conforme o nome da sua pasta
```

#### 3. Instale as dependências

```bash
npm install
```

#### 4. Verifique o arquivo de proxy

O arquivo `proxy.conf.json` deve existir na raiz do projeto Angular com o seguinte conteúdo:

```json
{
  "/dashboard": {
    "target": "http://localhost:8080",
    "changeOrigin": true
  },
  "/atendimentos": {
    "target": "http://localhost:8080",
    "changeOrigin": true
  },
  "/api": {
    "target": "http://localhost:8080",
    "changeOrigin": true
  }
}
```

E no `angular.json`, dentro de `"serve" > "options"`, confirme:

```json
"proxyConfig": "proxy.conf.json"
```

> O proxy evita erros de CORS durante o desenvolvimento local. Sem ele, o navegador bloqueará as requisições ao backend.

#### 5. Inicie o servidor de desenvolvimento

```bash
ng serve
```

Acesse em: **[http://localhost:4200](http://localhost:4200)**

A dashboard atualiza automaticamente a cada **10 segundos** sem necessidade de refresh manual. O badge **"Online"** (verde) ou **"Sem conexão"** (vermelho) no header indica o estado da comunicação com o backend.

---

### Portas em uso

| Serviço | Porta |
|---|---|
| Backend — Spring Boot | `8080` |
| Banco de dados MySQL (host) | `3307` |
| Banco de dados MySQL (interno Docker) | `3306` |
| Frontend Angular (dev server) | `4200` |

---

## 📡 Endpoints da API

| Método | Rota | Descrição | Status |
|---|---|---|---|
| `GET` | `/api/status` | Health check | `200` |
| `POST` | `/atendimentos` | Criar e distribuir ticket | `201` |
| `PATCH` | `/atendimentos/{id}/finalizar` | Finalizar atendimento | `200` |
| `GET` | `/dashboard` | Snapshot completo para o frontend | `200` |

### Criar ticket (time explícito)

```bash
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"assunto": "Problema no cartão de crédito", "time": "CARTOES"}'
```

### Criar ticket (roteamento automático — sem `time`)

```bash
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"assunto": "Quero fazer a contratação de empréstimo"}'
```

### Finalizar atendimento

```bash
curl -s -X PATCH http://localhost:8080/atendimentos/1/finalizar
```

### Consultar dashboard

```bash
curl -s http://localhost:8080/dashboard | python3 -m json.tool
```

---

## 📁 Estrutura do Projeto

```
distribution-service/
├── Dockerfile                              # Multi-stage build: Maven → JRE 21
├── docker-compose.yml                      # Serviços: app + db + rede
├── Makefile                                # ← Principal ponto de entrada de dev
├── .dockerignore
├── pom.xml
│
├── docker/mysql/init/                      # Scripts SQL de inicialização do banco
│
├── src/main/java/com/flowpay/distribution_service/
│   ├── config/
│   │   └── DataInitializer.java            # Seed automático (times + agentes)
│   ├── controller/
│   │   ├── AtendimentoController.java      # POST /atendimentos, PATCH /finalizar
│   │   ├── DashboardController.java        # GET /dashboard
│   │   └── StatusController.java          # GET /api/status
│   ├── dto/
│   │   ├── AtendimentoRequest.java
│   │   ├── AtendimentoResponse.java        # Inclui posicaoFila
│   │   └── dashboard/                     # DTOs do snapshot de dashboard
│   ├── entity/
│   │   ├── Agent.java                      # @Version — Optimistic Locking
│   │   ├── Team.java
│   │   └── Ticket.java
│   ├── enums/
│   │   ├── TeamKeyword.java                # Roteamento automático por assunto
│   │   ├── TeamName.java                  # CARTOES | EMPRESTIMOS | OUTROS
│   │   └── TicketStatus.java              # ABERTO | FILA | FINALIZADO
│   ├── exception/
│   │   └── GlobalExceptionHandler.java    # 400 | 404 | 422 | 500
│   ├── repository/
│   │   ├── AgentRepository.java
│   │   ├── TeamRepository.java
│   │   └── TicketRepository.java
│   └── service/
│       ├── DashboardService.java          # Estatísticas em tempo real
│       ├── DistributionOrchestrator.java  # @Retryable (anti-race condition)
│       └── DistributionService.java       # @Transactional (lógica principal)
│
├── src/main/resources/
│   └── application.properties             # Config via variáveis de ambiente
│
├── .DOCS/
│   ├── distribution-service.md            # Documentação técnica completa
│   ├── distribution-service.toon          # Resumo rápido em texto plano
│   └── collections/
│       └── distribution-service.postman_collection.json
│
└── .github/
    ├── agents/                            # Definições dos agentes de IA
    ├── prompts/                           # Prompts por agente
    └── instructions/                     # Regras de comportamento de cada agente
```

---

## 📚 Documentação Adicional

| Arquivo | Conteúdo |
|---|---|
| `.DOCS/distribution-service.md` | Documentação técnica completa do backend |
| `.DOCS/distribution-service.toon` | Resumo rápido em texto plano |
| `.DOCS/collections/*.json` | Coleção Postman com todos os cenários de teste |
| `.github/agents/` | Identidade e responsabilidades de cada agente de IA |
| `.github/prompts/` | Prompts de atuação (PO, Planning, Front Engineer, Angular Dev) |
| `.github/instructions/` | Regras e padrões obrigatórios por agente |

---

## 🤝 Contribuindo

Este projeto utiliza agentes de IA definidos em `.github/` para padronizar contribuições:

| Agente | Responsabilidade |
|---|---|
| **Planning** | Organiza tarefas e mantém o backlog atualizado |
| **Product Owner** | Define e valida requisitos com critérios de aceite |
| **Front Engineer** | Decisões arquiteturais do frontend Angular |
| **Senior Angular Dev** | Implementação de componentes e serviços |

> Toda documentação gerada deve ser criada em `/.DOCS/` no formato `.md` com resumo `.toon` correspondente.

---

<div align="center">
  <strong>FlowPay · Distribution Service</strong><br/>
  Java 21 · Spring Boot 3 · MySQL 8 · Docker · Angular 17
</div>
