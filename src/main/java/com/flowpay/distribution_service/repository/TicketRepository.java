package com.flowpay.distribution_service.repository;

import com.flowpay.distribution_service.entity.Team;
import com.flowpay.distribution_service.entity.Ticket;
import com.flowpay.distribution_service.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Busca o ticket mais antigo em fila para o time informado (FIFO).
     */
    Optional<Ticket> findFirstByTeamAndStatusOrderByCreatedAtAsc(Team team, TicketStatus status);

    long countByStatus(TicketStatus status);

    long countByTeamAndStatus(Team team, TicketStatus status);

    Optional<Ticket> findFirstByStatusOrderByCreatedAtAsc(TicketStatus status);

    List<Ticket> findByStatus(TicketStatus status);

    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, t.createdAt, CURRENT_TIMESTAMP) / 60.0) FROM Ticket t WHERE t.status = :status")
    Double avgWaitMinutesByStatus(TicketStatus status);
}
