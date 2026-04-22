# Instruções: Planning

## Regras de Comportamento

### 1. Granularidade de tarefas
Toda tarefa deve ser executável em no máximo **4 horas** por um desenvolvedor sênior. Tarefas maiores devem ser quebradas.

### 2. Toda tarefa tem responsável e critério de pronto
Nunca crie uma tarefa sem:
- Responsável definido (agente)
- Pelo menos 2 critérios de pronto verificáveis
- Dependências mapeadas (ou `—` se não houver)

### 3. Sequenciamento obrigatório
Antes de atribuir tarefas de implementação, verificar:
1. A documentação de requisitos (PO) está pronta?
2. A decisão arquitetural (front-engineer) está documentada?
3. As interfaces TypeScript (models) estão definidas?

Implementação de componentes só começa após esses 3 pré-requisitos.

### 4. Status das tarefas
| Símbolo | Significado |
|---------|-------------|
| 🔲 | Backlog (não iniciado) |
| 🔄 | Em andamento |
| ✅ | Pronto (DoD cumprido) |
| ⛔ | Bloqueado (indicar bloqueio) |
| ❌ | Cancelado |

### 5. Definition of Done (DoD) global
Uma tarefa só é ✅ quando:
- [ ] Código funciona conforme critério de pronto
- [ ] Documentação `.md` criada em `/.DOCS/`
- [ ] Resumo `.toon` criado em `/.DOCS/`
- [ ] Sem erros de compilação
- [ ] Revisado pelo agente responsável (front-engineer para componentes)

## Regras de Documentação

### Manter sempre atualizados:
1. `/.DOCS/planning.md` — backlog técnico completo com todas as tarefas
2. `/.DOCS/planning.toon` — resumo rápido do estado atual:

```
planning.toon
=============
Projeto: FlowPay Distribution Dashboard
Atualizado: <data>

BACKEND     ✅ 100% completo
FRONTEND    🔄 Em andamento

PRÓXIMAS TAREFAS
  TASK-XX — <título> [responsável]
  TASK-XX — <título> [responsável]

BLOQUEIOS
  <lista ou "Nenhum">

CONCLUÍDAS RECENTEMENTE
  TASK-XX — <título>
```

### Ao iniciar nova sessão de trabalho:
1. Ler `/.DOCS/planning.md` para retomar o contexto
2. Atualizar status das tarefas em andamento
3. Identificar próxima tarefa de maior prioridade

### Proibido
- Criar documentação de planning fora de `/.DOCS/`
- Marcar tarefa como ✅ sem verificar o DoD
- Iniciar implementação sem pré-requisitos cumpridos
