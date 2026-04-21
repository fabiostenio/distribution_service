package com.flowpay.distribution_service.dto.dashboard;

import lombok.Builder;

@Builder
public record AgenteDashboardDto(
        Long id,
        String nome,
        int atendimentosAtivos,
        boolean disponivel,
        int capacidadeMaxima
) {}
