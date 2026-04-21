package com.flowpay.distribution_service.service;

import com.flowpay.distribution_service.dto.dashboard.*;
import com.flowpay.distribution_service.entity.Agent;
import com.flowpay.distribution_service.entity.Team;
import com.flowpay.distribution_service.entity.Ticket;
import com.flowpay.distribution_service.enums.TicketStatus;
import com.flowpay.distribution_service.repository.AgentRepository;
import com.flowpay.distribution_service.repository.TeamRepository;
import com.flowpay.distribution_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int MAX_TICKETS = 3;

    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;

    public DashboardResponse getDashboard() {
        List<Team> times = teamRepository.findAll();

        long totalAgentes = agentRepository.count();
        long agentesDisponiveis = agentRepository.countByActiveTicketsLessThan(MAX_TICKETS);

        long abertos = ticketRepository.countByStatus(TicketStatus.ABERTO);
        long emFila = ticketRepository.countByStatus(TicketStatus.FILA);
        long finalizados = ticketRepository.countByStatus(TicketStatus.FINALIZADO);

        ResumoGeralDto resumo = ResumoGeralDto.builder()
                .totalTickets(abertos + emFila + finalizados)
                .abertos(abertos)
                .emFila(emFila)
                .finalizados(finalizados)
                .totalAgentes(totalAgentes)
                .agentesDisponiveis(agentesDisponiveis)
                .agentesOcupados(totalAgentes - agentesDisponiveis)
                .build();

        List<TimeDashboardDto> timesDto = times.stream()
                .map(this::buildTimeDashboard)
                .toList();

        FilaDashboardDto fila = buildFilaDashboard(times);

        return DashboardResponse.builder()
                .geradoEm(LocalDateTime.now())
                .resumo(resumo)
                .times(timesDto)
                .fila(fila)
                .build();
    }

    private TimeDashboardDto buildTimeDashboard(Team team) {
        long abertos = ticketRepository.countByTeamAndStatus(team, TicketStatus.ABERTO);
        long emFila = ticketRepository.countByTeamAndStatus(team, TicketStatus.FILA);
        long finalizados = ticketRepository.countByTeamAndStatus(team, TicketStatus.FINALIZADO);

        List<AgenteDashboardDto> agentes = agentRepository.findByTeam(team).stream()
                .map(this::buildAgenteDashboard)
                .toList();

        return TimeDashboardDto.builder()
                .time(team.getName())
                .abertos(abertos)
                .emFila(emFila)
                .finalizados(finalizados)
                .totalAtendimentos(abertos + emFila + finalizados)
                .agentes(agentes)
                .build();
    }

    private AgenteDashboardDto buildAgenteDashboard(Agent agent) {
        return AgenteDashboardDto.builder()
                .id(agent.getId())
                .nome(agent.getName())
                .atendimentosAtivos(agent.getActiveTickets())
                .disponivel(agent.isAvailable())
                .capacidadeMaxima(MAX_TICKETS)
                .build();
    }

    private FilaDashboardDto buildFilaDashboard(List<Team> times) {
        List<Ticket> emFila = ticketRepository.findByStatus(TicketStatus.FILA);

        Optional<LocalDateTime> maisAntigo = emFila.stream()
                .map(Ticket::getCreatedAt)
                .min(Comparator.naturalOrder());

        Long tempoMaximo = maisAntigo
                .map(t -> Duration.between(t, LocalDateTime.now()).toMinutes())
                .orElse(null);

        double tempoMedio = emFila.isEmpty() ? 0.0 : emFila.stream()
                .mapToLong(t -> Duration.between(t.getCreatedAt(), LocalDateTime.now()).toMinutes())
                .average()
                .orElse(0.0);

        List<FilaDashboardDto.FilaTimeDto> porTime = times.stream()
                .map(team -> {
                    long count = ticketRepository.countByTeamAndStatus(team, TicketStatus.FILA);
                    Optional<LocalDateTime> oldest = ticketRepository
                            .findFirstByTeamAndStatusOrderByCreatedAtAsc(team, TicketStatus.FILA)
                            .map(Ticket::getCreatedAt);
                    return FilaDashboardDto.FilaTimeDto.builder()
                            .time(team.getName())
                            .emFila(count)
                            .ticketMaisAntigoEm(oldest.orElse(null))
                            .build();
                })
                .toList();

        return FilaDashboardDto.builder()
                .totalEmFila((long) emFila.size())
                .ticketMaisAntigoEm(maisAntigo.orElse(null))
                .tempoEsperaMaximoMinutos(tempoMaximo)
                .tempoEsperaMedioMinutos(Math.round(tempoMedio * 10.0) / 10.0)
                .porTime(porTime)
                .build();
    }
}
