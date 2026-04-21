package com.flowpay.distribution_service.controller;

import com.flowpay.distribution_service.dto.AtendimentoRequest;
import com.flowpay.distribution_service.dto.AtendimentoResponse;
import com.flowpay.distribution_service.service.DistributionOrchestrator;
import com.flowpay.distribution_service.service.DistributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/atendimentos")
@RequiredArgsConstructor
public class AtendimentoController {

    private final DistributionOrchestrator orchestrator;
    private final DistributionService distributionService;

    /**
     * Cria um novo atendimento e distribui para um agente disponível.
     * O time pode ser informado ou resolvido automaticamente pelo assunto:
     * "problemas com cartão" → CARTOES, "contratação de empréstimo" → EMPRESTIMOS, demais → OUTROS.
     * Se todos os agentes estiverem ocupados, o ticket entra na fila.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtendimentoResponse criar(@RequestBody @Valid AtendimentoRequest request) {
        return orchestrator.distribute(request);
    }

    /**
     * Finaliza um atendimento ativo, libera o agente e redistribui automaticamente
     * o próximo ticket em fila do mesmo time (política FIFO).
     */
    @PatchMapping("/{id}/finalizar")
    public AtendimentoResponse finalizar(@PathVariable Long id) {
        return distributionService.finalizar(id);
    }
}
