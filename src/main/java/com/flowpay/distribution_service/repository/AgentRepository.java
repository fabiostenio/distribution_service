package com.flowpay.distribution_service.repository;

import com.flowpay.distribution_service.entity.Agent;
import com.flowpay.distribution_service.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    /**
     * Busca o agente com menos atendimentos ativos no time, desde que abaixo do limite.
     * O Optimistic Locking (via @Version) garante que, se dois processos lerem
     * o mesmo agente ao mesmo tempo, apenas um conseguirá commitar a atualização.
     */
    Optional<Agent> findFirstByTeamAndActiveTicketsLessThanOrderByActiveTicketsAsc(
            Team team, int maxTickets);
}
