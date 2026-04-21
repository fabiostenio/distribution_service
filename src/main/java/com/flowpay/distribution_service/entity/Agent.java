package com.flowpay.distribution_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Representa um atendente pertencente a um time.
 *
 * O campo {@code version} habilita o Optimistic Locking do Hibernate:
 * se duas transações tentarem atualizar o mesmo agente simultaneamente,
 * a segunda receberá uma {@link org.springframework.orm.ObjectOptimisticLockingFailureException},
 * evitando race conditions sem necessidade de locks pessimistas no banco.
 */
@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    private static final int MAX_SIMULTANEOUS_TICKETS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    @Builder.Default
    private int activeTickets = 0;

    /**
     * Controle de versão para Optimistic Locking.
     * O Hibernate incrementa este valor a cada UPDATE.
     * Se a versão lida difere da versão no banco no momento do commit,
     * a transação é abortada e pode ser reprocessada pelo mecanismo de retry.
     */
    @Version
    private Long version;

    public boolean isAvailable() {
        return this.activeTickets < MAX_SIMULTANEOUS_TICKETS;
    }

    public void assignTicket() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Agente " + this.name + " atingiu o limite máximo de atendimentos simultâneos.");
        }
        this.activeTickets++;
    }

    public void releaseTicket() {
        if (this.activeTickets > 0) {
            this.activeTickets--;
        }
    }
}
