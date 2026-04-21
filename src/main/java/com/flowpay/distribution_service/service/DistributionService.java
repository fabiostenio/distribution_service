package com.flowpay.distribution_service.service;

import com.flowpay.distribution_service.dto.AtendimentoRequest;
import com.flowpay.distribution_service.dto.AtendimentoResponse;
import com.flowpay.distribution_service.entity.Agent;
import com.flowpay.distribution_service.entity.Team;
import com.flowpay.distribution_service.entity.Ticket;
import com.flowpay.distribution_service.enums.TicketStatus;
import com.flowpay.distribution_service.repository.AgentRepository;
import com.flowpay.distribution_service.repository.TeamRepository;
import com.flowpay.distribution_service.repository.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Serviço responsável pela lógica de distribuição e finalização de atendimentos.
 *
 * A anotação {@code @Transactional} garante que a leitura do agente disponível
 * e a atualização do seu contador ocorram atomicamente. Combinado com o
 * Optimistic Locking ({@code @Version} em {@link Agent}), previne race conditions.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DistributionService {

    private static final int MAX_TICKETS_PER_AGENT = 3;

    private final AgentRepository agentRepository;
    private final TeamRepository teamRepository;
    private final TicketRepository ticketRepository;

    /**
     * Cria um novo ticket e distribui para um agente disponível do time.
     * O time pode ser informado explicitamente ou resolvido automaticamente pelo assunto.
     * Se não houver agente disponível, o ticket entra na fila.
     */
    public AtendimentoResponse distribute(AtendimentoRequest request) {
        Team team = teamRepository.findByName(request.getTimeResolvido())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Time não encontrado: " + request.getTimeResolvido()));

        Ticket ticket = Ticket.builder()
                .subject(request.getAssunto())
                .team(team)
                .build();

        assignToAvailableAgent(ticket, team);

        ticket = ticketRepository.save(ticket);
        log.info("Ticket #{} criado com status={} para time={} agente={}",
                ticket.getId(), ticket.getStatus(), team.getName(),
                ticket.getAgent() != null ? ticket.getAgent().getName() : "FILA");

        return toResponse(ticket);
    }

    /**
     * Finaliza um atendimento ativo, libera o agente e redistribui
     * automaticamente o próximo ticket em fila do mesmo time (se houver).
     */
    public AtendimentoResponse finalizar(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Atendimento #" + ticketId + " não encontrado."));

        if (ticket.getStatus() != TicketStatus.ABERTO) {
            throw new IllegalStateException(
                    "Atendimento #" + ticketId + " não pode ser finalizado pois está com status: "
                            + ticket.getStatus());
        }

        Agent agent = ticket.getAgent();
        agent.releaseTicket();
        agentRepository.save(agent);

        ticket.setStatus(TicketStatus.FINALIZADO);
        ticketRepository.save(ticket);
        log.info("Ticket #{} finalizado. Agente {} liberado ({} ativos).",
                ticketId, agent.getName(), agent.getActiveTickets());

        // Redistribui automaticamente o próximo da fila para o agente recém-liberado
        redistributeFromQueue(agent, ticket.getTeam());

        return toResponse(ticket);
    }

    // -------------------------------------------------------------------------
    // Métodos internos
    // -------------------------------------------------------------------------

    private void assignToAvailableAgent(Ticket ticket, Team team) {
        Optional<Agent> availableAgent = agentRepository
                .findFirstByTeamAndActiveTicketsLessThanOrderByActiveTicketsAsc(
                        team, MAX_TICKETS_PER_AGENT);

        if (availableAgent.isPresent()) {
            Agent agent = availableAgent.get();
            agent.assignTicket();
            agentRepository.save(agent);
            ticket.setAgent(agent);
            ticket.setStatus(TicketStatus.ABERTO);
        } else {
            ticket.setStatus(TicketStatus.FILA);
        }
    }

    /**
     * Após liberar um slot do agente, verifica se existe ticket em fila no mesmo time
     * e, em caso positivo, o atribui imediatamente ao agente (FIFO).
     */
    private void redistributeFromQueue(Agent agent, Team team) {
        if (!agent.isAvailable()) return;

        ticketRepository.findFirstByTeamAndStatusOrderByCreatedAtAsc(team, TicketStatus.FILA)
                .ifPresent(queued -> {
                    agent.assignTicket();
                    agentRepository.save(agent);
                    queued.setAgent(agent);
                    queued.setStatus(TicketStatus.ABERTO);
                    ticketRepository.save(queued);
                    log.info("Ticket #{} retirado da fila e atribuído ao agente {}.",
                            queued.getId(), agent.getName());
                });
    }

    private AtendimentoResponse toResponse(Ticket ticket) {
        return AtendimentoResponse.builder()
                .id(ticket.getId())
                .assunto(ticket.getSubject())
                .status(ticket.getStatus().name())
                .agente(ticket.getAgent() != null ? ticket.getAgent().getName() : null)
                .time(ticket.getTeam().getName().name())
                .criadoEm(ticket.getCreatedAt())
                .build();
    }
}
