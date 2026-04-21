package com.flowpay.distribution_service.service;

import com.flowpay.distribution_service.dto.AtendimentoRequest;
import com.flowpay.distribution_service.dto.AtendimentoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Orquestrador responsável por envolver a lógica de distribuição com
 * mecanismo de retry para tratamento de Optimistic Locking.
 *
 * Por que duas classes separadas?
 * O @Retryable precisa envolver o @Transactional, não o contrário.
 * Se estivessem no mesmo bean, o retry ocorreria DENTRO da mesma transação
 * já corrompida — o que não adiantaria nada. Com classes separadas, o Spring
 * cria proxies independentes: o retry (externo) chama o transactional (interno),
 * descartando e recriando a transação a cada tentativa.
 */
@Component
@RequiredArgsConstructor
public class DistributionOrchestrator {

    private final DistributionService distributionService;

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public AtendimentoResponse distribute(AtendimentoRequest request) {
        return distributionService.distribute(request);
    }

    @Recover
    public AtendimentoResponse recover(ObjectOptimisticLockingFailureException ex,
                                       AtendimentoRequest request) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Sistema sobrecarregado. Por favor, tente novamente em instantes.");
    }
}
