# 📦 distribution-service — Documentação Técnica

## Visão Geral

Sistema de distribuição de atendimentos para fintech, desenvolvido em **Java 21 + Spring Boot 3**.  
Distribuição automática de tickets entre agentes de 3 times, com controle de limite simultâneo e fila de espera.

---

## 🗂️ Estrutura de Pacotes

```
com.flowpay.distribution_service/
├── config/
│   └── DataInitializer.java          # Seed inicial de times e agentes
├── controller/
│   ├── AtendimentoController.java    # POST /atendimentos, PATCH /atendimentos/{id}/finalizar
│   ├── DashboardController.java      # GET /dashboard
│   └── StatusController.java         # GET /api/status
├── dto/
│   ├── AtendimentoRequest.java       # Payload de entrada
│   ├── AtendimentoResponse.java      # Payload de saída (inclui posicaoFila)
│   └── dashboard/
│       ├── DashboardResponse.java    # Envelope raiz do dashboard
│       ├── ResumoGeralDto.java       # Totais globais
│       ├── TimeDashboardDto.java     # Breakdown por time
│       ├── AgenteDashboardDto.java   # Estado de cada agente
│       └── FilaDashboardDto.java     # Métricas de fila
├── entity/
│   ├── Agent.java                    # Entidade agente com Optimistic Locking
│   ├── Team.java                     # Entidade time
│   └── Ticket.java                   # Entidade ticket
├── enums/
│   ├── TeamKeyword.java              # Roteamento automático por assunto
│   ├── TeamName.java                 # CARTOES | EMPRESTIMOS | OUTROS
│   └── TicketStatus.java             # ABERTO | FILA | FINALIZADO
├── exception/
│   └── GlobalExceptionHandler.java   # Tratamento centralizado de erros
├── repository/
│   ├── AgentRepository.java
│   ├── TeamRepository.java
│   └── TicketRepository.java
└── service/
    ├── DashboardService.java          # Lógica de estatísticas para o dashboard
    ├── DistributionOrchestrator.java  # Camada de Retry (@Retryable)
    └── DistributionService.java       # Lógica de distribuição e finalização (@Transactional)
```

---

## 🔒 Estratégia Anti-Race Condition

### Por que Race Condition é um problema aqui?
Em Java, requisições rodam em threads concorrentes que compartilham a mesma memória.  
Sem proteção, dois tickets poderiam ser atribuídos ao mesmo agente simultaneamente, estourando o limite de 3.

### Solução: Optimistic Locking + Retry

**Camada 1 — Optimistic Locking (`@Version`)**
```java
@Version
private Long version; // em Agent.java
```
O Hibernate incrementa `version` a cada `UPDATE`. Se duas transações leram o mesmo agente e a segunda tentar commitar, o banco rejeita com `ObjectOptimisticLockingFailureException`.

**Camada 2 — Retry automático (`@Retryable`)**
```java
@Retryable(
    retryFor = ObjectOptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 50, multiplier = 2)
)
```
Em caso de conflito, o sistema tenta novamente até 3 vezes com backoff exponencial (50ms, 100ms).

**Por que duas classes separadas (Service + Orchestrator)?**  
O `@Retryable` precisa envolver o `@Transactional`, nunca o contrário. Classes separadas garantem proxies independentes: a cada retry, uma nova transação limpa é aberta.

---

## 📡 Endpoints

### `POST /atendimentos`
Cria um novo atendimento e distribui para um agente disponível.

**Request:**
```json
{
  "assunto": "Problema no cartão de crédito",
  "time": "CARTOES"
}
```
Valores válidos para `time`: `CARTOES`, `EMPRESTIMOS`, `OUTROS`

**Response 201 — Agente disponível:**
```json
{
  "id": 1,
  "assunto": "Problema no cartão de crédito",
  "status": "ABERTO",
  "agente": "Ana Lima",
  "time": "CARTOES",
  "criadoEm": "2026-04-21T17:30:20.021536"
}
```

O campo `time` é **opcional**: se omitido, o sistema roteia automaticamente pelo conteúdo do `assunto` (ver [Roteamento Automático](#-roteamento-automático)).

**Response 201 — Agente disponível:**
```json
{
  "id": 1,
  "assunto": "Problema no cartão de crédito",
  "status": "ABERTO",
  "agente": "Ana Lima",
  "posicaoFila": null,
  "time": "CARTOES",
  "criadoEm": "2026-04-21T17:30:20.021536"
}
```

**Response 201 — Sem agentes disponíveis (ticket entra na fila):**
```json
{
  "id": 5,
  "assunto": "Ticket que deve ir pra fila",
  "status": "FILA",
  "agente": null,
  "posicaoFila": 2,
  "time": "CARTOES",
  "criadoEm": "2026-04-21T17:30:30.426857"
}
```
> `posicaoFila` indica a posição do ticket na fila **do seu time**. Retorna `null` quando `status=ABERTO` ou `status=FINALIZADO`.

**Response 400 — Validação:**
```json
{
  "timestamp": "2026-04-21T17:30:20",
  "status": 400,
  "erro": "assunto: O campo 'assunto' é obrigatório."
}
```

---

### `PATCH /atendimentos/{id}/finalizar`
Finaliza um atendimento ativo, libera o slot do agente e redistribui automaticamente o próximo ticket em fila do mesmo time (política FIFO).

**Response 200:**
```json
{
  "id": 1,
  "assunto": "Problema no cartão de crédito",
  "status": "FINALIZADO",
  "agente": "Ana Lima",
  "posicaoFila": null,
  "time": "CARTOES",
  "criadoEm": "2026-04-21T17:30:20.021536"
}
```

**Response 404 — Ticket não encontrado:**
```json
{ "status": 404, "erro": "Atendimento #99 não encontrado." }
```

**Response 422 — Ticket não está ABERTO:**
```json
{ "status": 422, "erro": "Atendimento #1 não pode ser finalizado pois está com status: FINALIZADO" }
```

---

### `GET /dashboard`
Retorna um snapshot completo do sistema em tempo real para consumo por frontends.

**Response 200:**
```json
{
  "geradoEm": "2026-04-21T22:50:59.611125",
  "resumo": {
    "totalTickets": 5,
    "abertos": 3,
    "emFila": 2,
    "finalizados": 0,
    "totalAgentes": 3,
    "agentesDisponiveis": 2,
    "agentesOcupados": 1
  },
  "times": [
    {
      "time": "CARTOES",
      "abertos": 3,
      "emFila": 2,
      "finalizados": 0,
      "totalAtendimentos": 5,
      "agentes": [
        {
          "id": 1,
          "nome": "Ana Lima",
          "atendimentosAtivos": 3,
          "disponivel": false,
          "capacidadeMaxima": 3
        }
      ]
    }
  ],
  "fila": {
    "totalEmFila": 2,
    "ticketMaisAntigoEm": "2026-04-21T22:44:19.231454",
    "tempoEsperaMaximoMinutos": 6,
    "tempoEsperaMedioMinutos": 5.5,
    "porTime": [
      { "time": "CARTOES", "emFila": 2, "ticketMaisAntigoEm": "2026-04-21T22:44:19.231454" },
      { "time": "EMPRESTIMOS", "emFila": 0, "ticketMaisAntigoEm": null },
      { "time": "OUTROS", "emFila": 0, "ticketMaisAntigoEm": null }
    ]
  }
}
```

| Campo | Descrição |
|---|---|
| `resumo.agentesDisponiveis` | Agentes com menos de 3 atendimentos ativos |
| `fila.ticketMaisAntigoEm` | Data de criação do ticket mais antigo em fila |
| `fila.tempoEsperaMaximoMinutos` | Minutos de espera do ticket mais antigo |
| `fila.tempoEsperaMedioMinutos` | Média de espera de todos os tickets em fila |

---

### `GET /api/status`
Health check da aplicação.
```
Backend FlowPay está online e operando!
```

---

## 🔀 Roteamento Automático

O campo `time` no `POST /atendimentos` é opcional. Quando omitido, o sistema analisa o conteúdo do `assunto` para determinar o time:

| Keyword detectada (case-insensitive) | Time roteado |
|---|---|
| `problemas com cartão` | `CARTOES` |
| `contratação de empréstimo` | `EMPRESTIMOS` |
| *(qualquer outro assunto)* | `OUTROS` |

Implementado em `TeamKeyword.java` via `TeamKeyword.resolve(String subject)`.

---

## 🗃️ Banco de Dados

- **Engine:** MySQL 8.0 (container Docker)
- **DDL:** gerenciado pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
- **Seed:** `DataInitializer` popula times e agentes na primeira inicialização

### Times e Agentes iniciais

| Time        | Agente        | Capacidade máxima  |
|-------------|---------------|--------------------|
| CARTOES     | Ana Lima      | 3 simultâneos      |
| EMPRESTIMOS | Carla Mendes  | 3 simultâneos      |
| OUTROS      | Eva Rocha     | 3 simultâneos      |

### Regra de negócio
- Cada agente suporta no máximo **3 atendimentos simultâneos**
- O **4º ticket** de um time sem agente disponível entra automaticamente na fila
- Ao finalizar um atendimento, o próximo ticket em fila é redistribuído imediatamente (FIFO)
- A distribuição prioriza o agente com **menos atendimentos ativos** no time

---

## 🐳 Infraestrutura Docker

```yaml
services:
  db:   mysql:8.0  → porta 3307 (host) / 3306 (interno)
  app:  java 21    → porta 8080
```
Ambos os containers se comunicam via rede interna `distribution-network`.

### Comandos Makefile

| Comando         | Ação                                      |
|-----------------|-------------------------------------------|
| `make up`       | Build e sobe todos os containers          |
| `make down`     | Para os containers                        |
| `make clean`    | Para e remove volumes (apaga dados do BD) |
| `make logs`     | Logs em tempo real                        |
| `make logs-app` | Logs da aplicação                         |
| `make logs-db`  | Logs do banco                             |
| `make ps`       | Status dos containers                     |
| `make status`   | Verifica se a API está respondendo        |
| `make db-shell` | Shell MySQL no container                  |
| `make app-shell`| Shell no container da aplicação           |

---

## 🧪 Testes manuais via curl

```bash
# Criar atendimento — time explícito
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"assunto":"Problema no cartão","time":"CARTOES"}'

# Criar atendimento — roteamento automático (sem time)
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"assunto":"Problemas com cartão bloqueado"}'

# Finalizar atendimento #1 (libera agente e redistribui fila)
curl -s -X PATCH http://localhost:8080/atendimentos/1/finalizar

# Dashboard completo
curl -s http://localhost:8080/dashboard | python3 -m json.tool

# Health check
curl http://localhost:8080/api/status
```

---

## 📦 Dependências adicionadas

| Dependência                    | Finalidade                         |
|--------------------------------|------------------------------------|
| `spring-boot-starter-validation` | Validação de DTOs com Bean Validation |
| `spring-retry`                 | Mecanismo de retry para Optimistic Locking |
| `spring-aspects`               | Suporte AOP necessário para `@Retryable` |
