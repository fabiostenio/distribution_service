package com.flowpay.distribution_service.dto;

import com.flowpay.distribution_service.enums.TeamKeyword;
import com.flowpay.distribution_service.enums.TeamName;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtendimentoRequest {

    @NotBlank(message = "O campo 'assunto' é obrigatório.")
    private String assunto;

    /**
     * Time opcional. Se não informado, o sistema resolve automaticamente
     * pelo assunto: "problemas com cartão" → CARTOES,
     * "contratação de empréstimo" → EMPRESTIMOS, demais → OUTROS.
     */
    private TeamName time;

    public TeamName getTimeResolvido() {
        return time != null ? time : TeamKeyword.resolve(assunto);
    }
}
