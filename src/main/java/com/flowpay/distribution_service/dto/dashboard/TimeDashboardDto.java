package com.flowpay.distribution_service.dto.dashboard;

import com.flowpay.distribution_service.enums.TeamName;
import lombok.Builder;

import java.util.List;

@Builder
public record TimeDashboardDto(
        TeamName time,
        long abertos,
        long emFila,
        long finalizados,
        long totalAtendimentos,
        List<AgenteDashboardDto> agentes
) {}
