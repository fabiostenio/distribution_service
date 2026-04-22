# Instruções: Engenheiro de Software Backend

## Regras Gerais de Comportamento

### 1. Analise antes de implementar
Antes de qualquer código, verifique se existe documentação em `/.DOCS/`. Se não existir, crie antes de codar. Decisões sem documentação não são aceitas.

### 2. Separação obrigatória: Orchestrator × Service
NUNCA coloque `@Retryable` e `@Transactional` na mesma classe. Regra sem exceção:

| Classe | Anotação | Responsabilidade |
|---|---|---|
| `*Orchestrator` | `@Retryable` | Retry de race condition |
| `*Service` | `@Transactional` | Lógica de negócio + persistência |
| `*Controller` | nenhuma | Apenas delegar, nunca lógica |

### 3. Camadas — responsabilidades fixas
- **Controller:** Recebe request, valida com `@Valid`, delega ao Orchestrator ou Service, retorna ResponseEntity
- **Orchestrator:** `@Retryable` anti-race condition, chama Service, tem `@Recover`
- **Service:** `@Transactional`, lógica de negócio completa
- **Repository:** Spring Data JPA + JPQL com `@Query`
- **Entity:** `@Version` para otimistic locking quando necessário

### 4. Tratamento de erros
Todo novo endpoint que pode falhar de maneira específica deve ter case no `GlobalExceptionHandler`:
- 400 — Bad Request / @Valid falha
- 404 — Entidade não encontrada
- 409 — Conflito (race condition esgotada)
- 422 — Regra de negócio violada
- 500 — Erro inesperado

### 5. Documentação obrigatória
- Toda feature documentada em `/.DOCS/<nome>.md`
- Criar `/.DOCS/<nome>.toon` como resumo executivo do `.md`
- Nunca documentar fora de `/.DOCS/`
- Atualizar Postman collection em `/.DOCS/collections/` quando houver novo endpoint

### 6. Proibições absolutas
- `@Retryable` + `@Transactional` na mesma classe
- Lógica de negócio no Controller
- SQL nativo sem justificativa (preferir JPQL)
- `Optional.get()` sem `isPresent()` — usar `orElseThrow()`
- Retornar `null` de métodos de serviço — usar `Optional` ou lançar exceção

### 7. Padrão de entidades com Optimistic Locking
```java
@Entity
public class MinhaEntidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;  // obrigatório para entidades que sofrem atualização concorrente
}
```

### 8. Padrão de JPQL para queries customizadas
```java
@Query("""
    SELECT COUNT(t) FROM Ticket t
    WHERE t.team = :team
    AND t.status = :status
    AND t.createdAt < :criadoEm
    """)
long countQueuePosition(@Param("team") Team team,
                        @Param("status") TicketStatus status,
                        @Param("criadoEm") LocalDateTime criadoEm);
```
