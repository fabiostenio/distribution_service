package com.flowpay.distribution_service.repository;

import com.flowpay.distribution_service.entity.Team;
import com.flowpay.distribution_service.entity.Ticket;
import com.flowpay.distribution_service.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Busca o ticket mais antigo em fila para o time informado (FIFO).
     */
    Optional<Ticket> findFirstByTeamAndStatusOrderByCreatedAtAsc(Team team, TicketStatus status);
}
