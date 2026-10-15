# SPEC-003: Idempotência e máquina de estados da compra

## Invariantes
1. Uma chave de idempotência gera **no máximo um** despacho ao terminal.
2. A compra é persistida **antes** do despacho.
3. No máximo **uma** compra `PENDING` por vez.
4. Toda mudança de status é **compare-and-set** sobre o status lido.
5. Ingressos e consumo de estoque acontecem **uma única vez**, na mesma transação da aprovação.
6. Aprovação válida **nunca** é descartada; `APPROVED` é final.

## Estados

| Estado | Significado | Bloqueia novas vendas? | Final? |
|---|---|---|---|
| `PENDING` | Despachado, aguardando callback | Sim | Não |
| `APPROVED` | Pago, ingressos emitidos | Não | Sim |
| `DECLINED` / `CANCELED` / `FAILED` | Não cobrado | Não | Sim (exceto aprovação tardia) |
| `UNCONFIRMED` | Resultado incerto (sem retorno ou valor divergente) | Não | Não |

## Transições

| De | Evento | Para |
|---|---|---|
| — | Iniciar checkout (chave nova, sem pendência, quantidade válida) | `PENDING` |
| `PENDING` | App de pagamento ausente | `FAILED` (`TerminalUnavailable`) |
| `PENDING` | Aprovação, valor = total | `APPROVED` + ingressos + estoque |
| `PENDING` | Aprovação, valor ≠ total | `UNCONFIRMED` (`AmountMismatch`) |
| `PENDING` | Código 3 / 1 / 2, 4 ou ilegível | `DECLINED` / `CANCELED` / `FAILED` |
| `PENDING` | Operador libera | `UNCONFIRMED` (`NoTerminalResponse`) |
| `UNCONFIRMED`, `DECLINED`, `CANCELED`, `FAILED` | Aprovação | `APPROVED` (ou `UNCONFIRMED` se valor divergir) |
| `DECLINED`, `CANCELED`, `FAILED` | Erro | ignorado |
| `APPROVED` | Qualquer | ignorado |

## Correlação do callback
- Com `reference` → busca **estrita** por id. Não encontrado → `Unmatched` (ignorado).
- Sem `reference` (erros da Cielo) → a compra `PENDING`. Nenhuma pendente → `Unmatched`.

## Reenvio de ação (matriz)

| Situação | Resultado esperado | Teste |
|---|---|---|
| Duplo toque no botão | 1 despacho | `CheckoutViewModelTest`, `StartPurchaseUseCaseTest.concurrent double tap…` |
| Mesmo toque após rotação/morte do processo | Replay, 0 novo despacho | `CheckoutViewModelTest.the idempotency key survives recreation…` |
| Mesma chave com compra finalizada | Retorna a compra, 0 despacho | `StartPurchaseUseCaseTest.a key that already has an outcome…` |
| Outra venda com pagamento pendente | Bloqueada | `StartPurchaseUseCaseTest.a payment waiting…` |
| Callback de aprovação repetido | Ingressos/estoque inalterados | `ProcessPaymentResultUseCaseTest`, `RoomPurchaseRepositoryTest`, `MainViewModelTest` |
| Aprovação concorrente com "liberar" | Aprovação aplicada (retry do compare-and-set) | `ProcessPaymentResultUseCaseTest.approval racing…` |
| Aprovação após falha registrada | `APPROVED` (recuperada) | `ProcessPaymentResultUseCaseTest.late approval…` |
| Inserção duplicada da chave no banco | `SQLiteConstraintException` | `RoomPurchaseRepositoryTest` |
