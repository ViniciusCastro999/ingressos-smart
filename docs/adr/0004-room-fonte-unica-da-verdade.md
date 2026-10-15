# ADR-0004: Room como fonte única da verdade e persistir antes de despachar

**Status:** Aceito

## Contexto
O pagamento acontece **fora** do app (app da Cielo, outro processo). Durante esse tempo o Android pode
matar o processo do app. A FAQ da Cielo cita transações que "não retornam" quando o app de integração é
encerrado e recomenda um *foreground service*.

## Decisão
- A `Purchase` é gravada como `PENDING` **antes** do despacho ao terminal.
- O callback é tratado pela `MainActivity` (`singleTask`). Se o processo morreu, o Android o recria para
  entregar o intent, e o caso de uso grava o resultado em transação (`NonCancellable`).
- As telas observam o banco via `Flow`. O comprovante muda de "aguardando" para o resultado sem depender
  de estado em memória.
- Evento do catálogo vem de JSON local (seed no primeiro uso). Em produção viria de uma API, atrás da mesma interface.

## Consequências
- Resiliente à morte do processo: verificado no emulador matando o processo do app com o terminal aberto.
- Sem notificação persistente.
- Não adotamos o foreground service por ora (trade-off documentado). Ele seria uma camada extra contra
  terminais que matam o app de forma mais agressiva.
