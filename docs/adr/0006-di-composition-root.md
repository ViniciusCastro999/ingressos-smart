# ADR-0006: Hilt apenas no `:app`; domínio sem anotações

**Status:** Aceito

## Contexto
Hilt é o padrão no Android, mas anotar casos de uso (`@Inject`) acopla o domínio a um framework e exige
processamento de anotações em módulos que não precisam disso.

## Decisão
- Domínio, `:data` e `:payment:*` usam **injeção por construtor sem anotações**.
- O `:app` é o **composition root**: `CoreModule`, `DataModule`, `PaymentModule` e `UseCaseModule` montam o grafo com `@Provides`.
- `StartPurchaseUseCase` é `@Singleton` (o lock depende disso). Os demais casos de uso não têm escopo.

## Consequências
- O domínio é portátil (poderia ir para KMP) e testável sem DI.
- O grafo de dependências fica explícito em um só lugar.
- Custo: `@Provides` manuais. É aceitável para o tamanho do app.
