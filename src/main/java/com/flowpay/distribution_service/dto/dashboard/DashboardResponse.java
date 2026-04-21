package com.flowpay.distribution_service.dto.dashboard;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DashboardResponse(
        LocalDateTime geradoEm,
        ResumoGeralDto resumo,
        List<TimeDashboardDto> times,
        FilaDashboardDto fila
) {}
