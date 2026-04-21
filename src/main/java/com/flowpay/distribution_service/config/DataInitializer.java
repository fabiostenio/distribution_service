package com.flowpay.distribution_service.config;

import com.flowpay.distribution_service.entity.Agent;
import com.flowpay.distribution_service.entity.Team;
import com.flowpay.distribution_service.enums.TeamName;
import com.flowpay.distribution_service.repository.AgentRepository;
import com.flowpay.distribution_service.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Popula o banco com times e agentes iniciais caso esteja vazio.
 * Executado uma única vez na inicialização da aplicação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            log.info("Dados iniciais já existem. Pulando inicialização.");
            return;
        }

        log.info("Inicializando dados de times e agentes...");

        Team cartoes    = teamRepository.save(Team.builder().name(TeamName.CARTOES).build());
        Team emprestimos = teamRepository.save(Team.builder().name(TeamName.EMPRESTIMOS).build());
        Team outros     = teamRepository.save(Team.builder().name(TeamName.OUTROS).build());

        List<Agent> agents = List.of(
                Agent.builder().name("Ana Lima").team(cartoes).build(),
                Agent.builder().name("Bruno Souza").team(cartoes).build(),
                Agent.builder().name("Carla Mendes").team(emprestimos).build(),
                Agent.builder().name("Diego Ferreira").team(emprestimos).build(),
                Agent.builder().name("Eva Rocha").team(outros).build(),
                Agent.builder().name("Felipe Nunes").team(outros).build()
        );
        agentRepository.saveAll(agents);

        log.info("Times e agentes criados com sucesso. {} agentes registrados.", agents.size());
    }
}
