# ADR-0007: Segurança do callback de pagamento

**Status:** Aceito

## Contexto
O resultado do pagamento chega por um **intent implícito exportado**. Qualquer app do dispositivo pode
enviar `esquema://host?response=<json de aprovação forjado>`. Os exemplos da documentação usam
`order://response`, que outros apps no mesmo terminal provavelmente também usam.

## Decisão
1. Esquema **exclusivo** (`ingressos://payment-result`), sem categoria `BROWSABLE` (a web não dispara o callback).
2. Aprovação só é aplicada se o `reference` corresponder a uma compra existente. Resultados com `reference`
   desconhecido são descartados (`Unmatched`), sem fallback.
3. Aprovação com valor ≠ total vira `UNCONFIRMED`, **sem emitir ingressos**.
4. Credenciais fora do git; `toString()` mascarado; telemetria sem token ou dados de cartão.
5. `allowBackup=false` + `dataExtractionRules` excluindo tudo.

## Consequências
- Forjar uma aprovação exige conhecer o UUID da compra pendente, que só o app e a Cielo conhecem.
- A garantia forte (validar a transação no servidor via Order Manager) depende de backend: fica como evolução.
