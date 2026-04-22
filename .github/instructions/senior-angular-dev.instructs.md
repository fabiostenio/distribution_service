# Instruções: Desenvolvedor Senior Angular

## Regras de Implementação

### 1. Componentes — sempre standalone
```typescript
@Component({
  standalone: true,
  imports: [CommonModule],  // adicionar outros conforme necessário
  ...
})
```
Nunca declare componentes em NgModules.

### 2. Injeção de dependências — sempre inject()
```typescript
private dashboardService = inject(DashboardService);
```
Nunca use o construtor para injeção.

### 3. Polling — padrão obrigatório
```typescript
import { interval } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interval(10000).pipe(
  switchMap(() => this.dashboardService.getDashboard()),
  catchError(err => { this.online = false; return EMPTY; }),
  takeUntilDestroyed()
).subscribe(data => {
  this.data = data;
  this.online = true;
});
```

### 4. Tratamento de erro obrigatório
- Em falha HTTP: **não limpar** `this.data` — manter último estado
- Atualizar `this.online = false` para exibir badge "Sem conexão"
- Em recuperação: `this.online = true`

### 5. CSS — dark theme, por componente
Variáveis globais permitidas em `styles.css`:
```css
:root {
  --bg-primary: #0f1117;
  --bg-card: #1a1d27;
  --accent-green: #22c55e;
  --accent-red: #ef4444;
  --accent-yellow: #eab308;
  --text-primary: #f1f5f9;
  --text-muted: #64748b;
}
```
Cada componente usa apenas `var(--...)` — sem cores hardcoded.

### 6. Tipagem — nunca usar `any`
Todas as interfaces em `models/dashboard.model.ts`. Exportar e importar explicitamente.

## Regras de Documentação

### Para cada componente entregue:
1. Criar `/.DOCS/<nome-componente>.md` contendo:
   - Propósito do componente
   - Inputs/Outputs (se houver)
   - Dados consumidos do DashboardResponse
   - Comportamento em estado de erro
   - Screenshot ou descrição do layout

2. Criar `/.DOCS/<nome-componente>.toon` contendo resumo em texto plano:
```
<nome>.toon
===========
Componente: <NomeComponent>

PROPÓSITO
  <uma linha>

DADOS CONSUMIDOS
  <campos do DashboardResponse usados>

ESTADOS
  loading | online | offline

RESTRIÇÕES
  <lista>
```

### Para cada service entregue:
1. Criar `/.DOCS/<nome-service>.md` com métodos, tipos de retorno e tratamento de erro
2. Criar `/.DOCS/<nome-service>.toon` com resumo

### Proibido
- Criar arquivos de documentação fora de `/.DOCS/`
- Usar `any` em qualquer tipagem
- Usar NgModules
- Usar frameworks CSS externos
