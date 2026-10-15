# SPEC-002: Contrato de integração Cielo Smart (Deep Link)

Fonte: documentação oficial da Cielo Smart (seções "Integração via Deep Link", "Pagamento",
"Recuperando dados do pagamento", "Códigos de erro", "Configurando o Android Manifest") e sample oficial
`matheus-caldeira/cielo_sample` (bridge Android).

## Requisição de pagamento

```
<terminalScheme>://payment?request=<enc(base64(json))>&urlCallback=<enc(callbackUri)>
```

| Campo | Tipo | Origem |
|---|---|---|
| `accessToken`, `clientID` | string | `local.properties` / env (nunca no git) |
| `reference` | string | `Purchase.id` (chave de idempotência) |
| `merchantCode` | string? | omitido (multi-EC não usado) |
| `installments` | int | `0` (à vista) |
| `items[]` | `{name, quantity, sku, unitOfMeasure:"unidade", unitPrice(centavos)}` | evento × quantidade |
| `paymentCode` | string? | `CREDITO_AVISTA` / `DEBITO_AVISTA` / `PIX` / omitido |
| `value` | **string** em centavos | `Purchase.total` |

- Base64 padrão **sem quebras** (≡ `Base64.NO_WRAP`). Valores percent-encoded (≡ `Uri.Builder.appendQueryParameter`).
- `terminalScheme`: `lio` (flavor `cielo`) ou `lio-sim` (flavor `simulator`).
- Callback: `ingressos://payment-result` (`cielo`) ou `ingressos-sim://payment-result` (`simulator`).

## Resposta

`<callbackUri>?response=<base64 json>&responsecode=<n>`

| Formato do JSON | Interpretação |
|---|---|
| Objeto com `id`/`payments` | Pedido. Pagamentos com `paymentFields.statusCode` ∈ {0 (Pix), 1 (autorizado)} ou ausente → `Approved`. Sem pagamento autorizado → `Failed` |
| `{code, reason}` | 1 → `Canceled`; 3 → `Declined`; 2, 4, outros → `Failed` |
| Sem `response`, Base64 ou JSON inválido | `Failed` sem `reference` |

Dados extraídos da aprovação: `reference`, `id` (pedido Cielo), `paidAmount` (ou soma dos pagamentos
autorizados), `authCode`, `cieloCode` (NSU), `brand`, `mask`, `installments`, `terminal`,
`paymentFields.productName`.

Robustez obrigatória:
- `responsecode` **ignorado** (vem `0` em erros também).
- Decoder tolerante a `\n`/`\r` (Base64 com quebras de linha) e a `+` convertido em espaço.
- Campos desconhecidos ignorados; todos os campos opcionais.

## Manifest
- `<queries>`: pacote `com.ads.lio.uriappclient` e intents `VIEW` com esquema `lio` (Android 11+).
- `<meta-data android:name="cs_integration_type" android:value="uri"/>` (Cielo Store).
- Callback em `MainActivity` (`singleTask`), intent-filter `VIEW` + `DEFAULT`, **sem** `BROWSABLE`.

## Testes de contrato
`CieloCallbackParserTest` (payloads oficiais da documentação), `CieloDeepLinkFactoryTest`, `TerminalSimulatorTest` (ida e volta).
