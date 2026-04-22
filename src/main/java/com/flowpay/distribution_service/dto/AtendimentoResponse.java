package com.flowpay.distribution_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AtendimentoResponse {

    private Long id;
    private String assunto;

    /** ABERTO: atribuído a um agente | FILA: aguardando atendente disponível */
    private String status;

    /** Nome do agente atribuído, ou null se o ticket está em fila. */
    private String agente;

    /** Posição na fila do time. Preenchido apenas quando status=FILA; null caso contrário. */
    private Long posicaoFila;

    private String time;
    private LocalDateTime criadoEm;
}
