# ADR-0003: Idempotência: chave por checkout, single-flight e compare-and-set

**Status:** Aceito

## Contexto
Requisito: "evitar duplicidade de cobrança em reenvio de ação". Formas de reenvio: duplo toque, toque
após rotação/morte do processo, retentativa após timeout, callback entregue duas vezes (reentrega pelo
Recents, recriação da Activity) e callback tardio depois de o operador desistir.

## Decisão
1. **Chave de idempotência por checkout**, gerada uma vez e guardada no `SavedStateHandle`. Ela é o `id`
   da `Purchase` (PK no Room) e o `reference` enviado à Cielo.
2. **`StartPurchaseUseCase` singleton com `Mutex`** (single-flight) em "procurar chave → checar
   pendência → criar → despachar".
3. **Replay**: chave já usada devolve a compra existente (`AwaitingPayment` ou `AlreadyProcessed`) e nunca despacha de novo.
4. **Um pagamento `PENDING` por vez**. Isso também permite correlacionar respostas de erro, que vêm sem `reference`.
5. **Máquina de estados na entidade `Purchase`** + **compare-and-set** no repositório (`update(purchase, expectedStatus)`)
   dentro de transação, com retentativa curta se perder a corrida.
6. **Aprovação nunca se perde**: aprovação vence `DECLINED`/`CANCELED`/`FAILED`/`UNCONFIRMED`. `APPROVED` é final.
7. **Sem retorno do terminal → `UNCONFIRMED`**, não `FAILED`: libera novas vendas, mas ainda aceita aprovação tardia.
8. "Tentar novamente" abre um **novo** checkout (nova chave). Retentativa é decisão explícita do operador.

## Consequências
- No máximo uma cobrança por chave, garantido em três níveis (UI, caso de uso, banco).
- Callbacks repetidos são inofensivos: tickets e estoque mudam uma única vez.
- Vendas são serializadas no terminal (que já processa um pagamento por vez).
- `UNCONFIRMED` exige conferência manual; a conciliação automática ficou como evolução (README §12).
