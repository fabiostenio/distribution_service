package com.flowpay.distribution_service.enums;

/**
 * Palavras-chave que mapeiam automaticamente o assunto do ticket para um time.
 * A comparação é feita em lowercase para garantir case-insensitive.
 */
public enum TeamKeyword {

    CARTOES("problemas com cartão", TeamName.CARTOES),
    EMPRESTIMOS("contratação de empréstimo", TeamName.EMPRESTIMOS);

    private final String keyword;
    private final TeamName team;

    TeamKeyword(String keyword, TeamName team) {
        this.keyword = keyword;
        this.team = team;
    }

    public TeamName getTeam() {
        return team;
    }

    /**
     * Resolve o time com base no assunto informado.
     * Se nenhuma palavra-chave for encontrada, retorna OUTROS.
     */
    public static TeamName resolve(String subject) {
        if (subject == null) return TeamName.OUTROS;
        String lower = subject.toLowerCase();
        for (TeamKeyword tk : values()) {
            if (lower.contains(tk.keyword)) {
                return tk.team;
            }
        }
        return TeamName.OUTROS;
    }
}
