# ADR-0002: Integração Cielo Smart via Deep Link `lio://`

**Status:** Aceito

## Contexto
A Cielo oferece integração **local** (Deep Link no terminal) e **remota** (API Order Manager). A
[política de integração](https://docs.cielo.com.br/cielo-smart/docs/politica-de-integracao-camada-de-abstracao-obrigatoria)
proíbe o acesso direto ao SDK/hardware do fabricante e torna obrigatória a camada de abstração da Cielo
(UriApp, `lio://`) para pagamentos, pedidos e impressão. O desafio não avalia backend.

## Decisão
Usar o Deep Link `lio://payment`:
- Request: JSON (`accessToken`, `clientID`, `reference`, `items`, `value` em centavos, `paymentCode` opcional) → Base64 sem quebras → percent-encoding, equivalente ao `Uri.Builder.appendQueryParameter` do sample oficial.
- Response no `urlCallback`: `?response=<base64 json>&responsecode=<n>`. O tipo é decidido pelo **formato do JSON** (pedido com `payments` vs `{code, reason}`), porque `responsecode` vem `0` também em erros.
- Disparo com `Context.startActivity(ACTION_VIEW, FLAG_ACTIVITY_NEW_TASK)` a partir do application context. Assim o adapter pode ser singleton sem vazar `Activity`.

## Consequências
- Funciona sem backend e respeita a política da Cielo.
- O `accessToken` fica no APK (limitação do modelo; ver ADR-0007 e README §12).
- A correlação depende do `reference` devolvido pela Cielo. Respostas de erro não o trazem (ver ADR-0003).
