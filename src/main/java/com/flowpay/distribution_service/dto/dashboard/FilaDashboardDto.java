package com.flowpay.distribution_service.dto.dashboard;

import com.flowpay.distribution_service.enums.TeamName;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record FilaDashboardDto(
        long totalEmFila,
        LocalDateTime ticketMaisAntigoEm,
        Long tempoEsperaMaximoMinutos,
        double tempoEsperaMedioMinutos,
        List<FilaTimeDto> porTime
) {
    @Builder
    public record FilaTimeDto(
            TeamName time,
            long emFila,
            LocalDateTime ticketMaisAntigoEm
    ) {}
}
