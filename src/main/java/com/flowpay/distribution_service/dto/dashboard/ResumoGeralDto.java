package com.flowpay.distribution_service.dto.dashboard;

import lombok.Builder;

@Builder
public record ResumoGeralDto(
        long totalTickets,
        long abertos,
        long emFila,
        long finalizados,
        long totalAgentes,
        long agentesDisponiveis,
        long agentesOcupados
) {}
