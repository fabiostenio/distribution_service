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
│   ├── AtendimentoController.java    # POST /atendimentos
│   └── StatusController.java         # GET /api/status
├── dto/
│   ├── AtendimentoRequest.java       # Payload de entrada
│   └── AtendimentoResponse.java      # Payload de saída
├── entity/
│   ├── Agent.java                    # Entidade agente com Optimistic Locking
│   ├── Team.java                     # Entidade time
│   └── Ticket.java                   # Entidade ticket
├── enums/
│   ├── TeamName.java                 # CARTOES | EMPRESTIMOS | OUTROS
│   └── TicketStatus.java             # ABERTO | FILA
├── exception/
│   └── GlobalExceptionHandler.java   # Tratamento centralizado de erros
├── repository/
│   ├── AgentRepository.java
│   ├── TeamRepository.java
│   └── TicketRepository.java
└── service/
    ├── DistributionOrchestrator.java  # Camada de Retry (@Retryable)
    └── DistributionService.java       # Lógica de distribuição (@Transactional)
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

**Response 201 — Sem agentes disponíveis:**
```json
{
  "id": 8,
  "assunto": "Ticket que deve ir pra fila",
  "status": "FILA",
  "agente": null,
  "time": "CARTOES",
  "criadoEm": "2026-04-21T17:30:30.426857"
}
```

**Response 400 — Validação:**
```json
{
  "timestamp": "2026-04-21T17:30:20",
  "status": 400,
  "erro": "assunto: O campo 'assunto' é obrigatório."
}
```

### `GET /api/status`
Health check da aplicação.
```
Backend FlowPay está online e operando!
```

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
# Criar atendimento — time Cartões
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"assunto":"Problema no cartão","time":"CARTOES"}'

# Criar atendimento — time Empréstimos
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"assunto":"Refinanciamento","time":"EMPRESTIMOS"}'

# Criar atendimento — time Outros
curl -s -X POST http://localhost:8080/atendimentos \
  -H "Content-Type: application/json" \
  -d '{"assunto":"Dúvida geral","time":"OUTROS"}'

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
