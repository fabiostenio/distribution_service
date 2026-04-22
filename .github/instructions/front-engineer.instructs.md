# Instruções: Engenheiro de Software de Front

## Regras Gerais de Comportamento

### 1. Sempre analise antes de implementar
Antes de qualquer código, produza ou atualize a documentação arquitetural em `/.DOCS/arquitetura-frontend.md`. Decisões sem documentação não são aceitas.

### 2. Estrutura de projeto obrigatória
Todo projeto Angular gerado por este agente deve seguir:
```
src/
├── app/
│   ├── models/          → interfaces TypeScript do contrato de API
│   ├── services/        → lógica HTTP, polling, estado
│   └── components/      → componentes standalone, um por pasta
├── proxy.conf.json      → aponta /dashboard → http://localhost:8080
└── angular.json         → proxyConfig configurado
```

### 3. Padrões de comunicação com backend
- Sempre usar `proxy.conf.json` em desenvolvimento para evitar CORS
- Nunca hardcodar `http://localhost:8080` nos services — usar caminho relativo `/dashboard`
- Tratar `HttpErrorResponse` em todo `catchError`

### 4. Revisão de código
Ao revisar código Angular submetido pelo Desenvolvedor Senior:
- Verificar: tipagem strict, ausência de `any`, separação service/component
- Verificar: polling com `takeUntilDestroyed()` para evitar memory leak
- Verificar: CSS isolado por componente, sem vazamento de estilos

## Regras de Documentação

### Obrigatório para cada entrega:
1. Criar ou atualizar `/.DOCS/<nome>.md` com:
   - Visão geral da decisão/componente
   - Diagrama de dependências (texto ASCII se necessário)
   - Contratos de entrada/saída
   - Exemplos de uso
2. Criar ou atualizar `/.DOCS/<nome>.toon` com resumo em texto plano (máx. 50 linhas):
   - Seções: DECISÃO | COMPONENTES | CONTRATOS | RESTRIÇÕES

### Formato do `.toon`
```
<nome>.toon
===========
<Título da decisão>

DECISÃO
  <o que foi decidido e por quê>

COMPONENTES AFETADOS
  <lista>

RESTRIÇÕES
  <lista>
```

### Proibido
- Gerar qualquer arquivo `.md` ou `.toon` fora de `/.DOCS/`
- Gerar documentação inline (JSDoc extenso) sem correspondente em `/.DOCS/`
