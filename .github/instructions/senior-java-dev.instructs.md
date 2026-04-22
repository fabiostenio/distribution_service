# Instruções: Desenvolvedor Senior Java

## Regras de Implementação

### 1. Padrão de Service — @Transactional obrigatório
```java
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExemploService {
    // Toda lógica de negócio aqui
    // @Transactional garante rollback automático em exceção
}
```

### 2. Padrão de Orchestrator — @Retryable sem @Transactional
```java
@Service
@RequiredArgsConstructor
public class ExemploOrchestrator {

    private final ExemploService exemploService;

    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public RetornoDto executar(EntradaDto dto) {
        return exemploService.executar(dto);
    }

    @Recover
    public RetornoDto recover(ObjectOptimisticLockingFailureException ex, EntradaDto dto) {
        log.error("Race condition esgotada após 3 tentativas: {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Tente novamente em instantes.");
    }
}
```

### 3. Padrão de Entity com Optimistic Locking
```java
@Entity
@Table(name = "agents")
@Getter @Setter
@NoArgsConstructor
public class Agent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;  // obrigatório — previne race condition na distribuição

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
```

### 4. Padrão de Controller — apenas delegar
```java
@RestController
@RequestMapping("/atendimentos")
@RequiredArgsConstructor
public class AtendimentoController {

    private final DistributionOrchestrator orchestrator;
    private final DistributionService service;

    @PostMapping
    public ResponseEntity<AtendimentoResponse> criar(@Valid @RequestBody AtendimentoRequest req) {
        // Apenas delega — nenhuma lógica aqui
        return ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.distribute(req));
    }
}
```

### 5. Regras de JPQL
- Sempre usar `@Query` com JPQL (não SQL nativo) para queries customizadas
- Parâmetros nomeados com `@Param`
- Projeções com `SELECT new com.flowpay.dto.MinhaProjecao(...)` quando necessário

### 6. Posição na fila (padrão obrigatório)
Para calcular posição na fila de um ticket:
```java
Long posicao = ticketRepository.countQueuePositionByTeamAndCreatedAt(
    ticket.getTeam(), TicketStatus.FILA, ticket.getCreatedAt()
) + 1;
```
Retornar `null` quando status ≠ FILA.

### 7. DataInitializer — idempotência obrigatória
```java
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Sempre verificar existência antes de inserir
        if (teamRepository.findByName(TeamName.CARTOES).isEmpty()) {
            // criar apenas se não existe
        }
    }
}
```

### 8. Proibições
- `@Retryable` + `@Transactional` na mesma classe → race condition não resolvida
- Lógica de negócio fora do Service
- `Optional.get()` direto → usar `orElseThrow(() -> new ResponseStatusException(...))`
- SQL nativo sem justificativa documentada
