# CLAUDE.md: harness do agente

Instruções que o agente de IA (Claude Code) carrega em toda sessão neste repositório. São as regras do
projeto: o agente deve segui-las e um humano as revisa como qualquer outro código.

## Contexto

App Android (Kotlin, Compose) de venda de ingressos com pagamento pela **Cielo Smart** via Deep Link `lio://`.
Domínio crítico: **dinheiro de verdade**. Uma cobrança duplicada ou uma aprovação perdida é o pior bug possível.

- Specs: `docs/specs/` (fonte da verdade do comportamento esperado)
- Decisões: `docs/adr/` (não contrarie um ADR sem propor um novo)
- Uso de IA: `docs/ai/USO-DE-IA.md`

## Arquitetura (regra de dependência)

```
:app ──► :domain ◄── :data
  │          ▲
  └──► :payment:cielo ◄── :payment:simulator (só flavor "simulator")
```

- `:domain` é **Kotlin/JVM puro**. Proibido importar `android.*`, Room, Hilt/Dagger/javax.inject ou kotlinx.serialization.
- Casos de uso dependem só de **portas** do domínio (`PaymentTerminal`, `PaymentResultParser`, repositórios, `Clock`, `IdGenerator`, `Telemetry`, `TransactionRunner`).
- `:data` expõe **apenas interfaces do domínio** (via `DataLayer`). O Room não vaza para o `:app`.
- `:app` é o **composition root** (módulos Hilt em `di/`). Nenhuma outra camada usa o framework de DI.
- UI lê estado do banco via `Flow` (fonte única da verdade). Nada de estado de pagamento só em memória.

## Invariantes de pagamento (não negociáveis)

1. **Persistir antes de despachar**: a `Purchase` `PENDING` existe no banco antes de chamar o terminal.
2. **Uma chave de idempotência = no máximo uma cobrança.** A chave é o `id` da compra e o `reference` enviado à Cielo.
3. **Um pagamento aguardando o terminal por vez.**
4. **Mudanças de status são compare-and-set** (`PurchaseRepository.update(purchase, expectedStatus)`).
5. **Aprovação nunca se perde**: aprovação válida vence estados não aprovados; `APPROVED` é final.
6. **Ingressos só nascem na transição para `APPROVED`** e só se o valor pago == total.
7. **Callbacks não são confiáveis**: resultado com `reference` desconhecido é ignorado; sem `reference`, só vale para a compra `PENDING`.
8. `StartPurchaseUseCase` é **singleton** (o lock single-flight depende disso).

Qualquer mudança que toque esses pontos exige teste novo ou ajustado que **falhe antes** da mudança.

## Integração Cielo Smart

- Somente a camada de abstração da Cielo (Deep Link `lio://`). **Proibido** usar SDK/biblioteca do fabricante do terminal.
- Request: JSON → Base64 sem quebras → valores percent-encoded. Valores **em centavos**; `value` é string.
- Response: decidir pelo **formato do JSON** (`responsecode` vem `0` também em erro). Decoder tolerante a `\n` e a `+`→espaço.
- Códigos de erro: 1 cancelado, 2 genérico, 3 erro no pagamento (negado), 4 autenticação.
- Antes de mudar o protocolo, confirme na documentação oficial (`https://docs.cielo.com.br/cielo-smart/llms.txt`) e cite a página no PR.

## Segurança

- Nunca commitar credenciais. `clientId`/`accessToken` vêm de `local.properties` ou variáveis de ambiente.
- Nunca logar token, request completo ou dados de cartão. `Telemetry` só recebe ids, status e valores.
- O esquema do callback é exclusivo do app e **sem** `BROWSABLE`.

## Convenções

- Kotlin oficial, linhas até 120 colunas, vírgula final em listas multilinha.
- Identificadores e KDoc em inglês; textos de UI em pt-BR **somente** em `strings.xml`.
- Dinheiro sempre `Money` (centavos, `Long`). Nunca `Double`/`Float`.
- UI: só componentes e tokens de `ui/designsystem` (`BrandColors`, `BrandIcons`, `BrandCard`, `PrimaryButton`…).
  Cor, raio ou ícone "solto" em tela é proibido; uma necessidade nova vira componente no design system (ADR-0009).
- Erros esperados como `sealed interface`, não exceções nem `null`.
- Comentários explicam o **porquê**, não o quê.

## Testes

- **Fakes em vez de mocks** (`domain/src/testFixtures`). Fakes devem honrar o contrato real (inclusive o compare-and-set).
- Nome do teste descreve o comportamento: `` `duplicated approval callback changes tickets and stock only once` ``.
- **Nunca** alterar um teste para fazê-lo passar sem explicar por que o comportamento esperado mudou.
- Para testes de concorrência, verifique que o teste **falha** sem a proteção (mutação manual) antes de confiar nele.

## Comandos

```bash
./gradlew test                       # todos os testes unitários
./gradlew :domain:test               # só o domínio (rápido, JVM puro)
./gradlew lintSimulatorDebug         # lint
./gradlew :app:installSimulatorDebug # app + simulador de terminal
./gradlew :app:installCieloDebug     # app integrado à Cielo Smart (emulador Cielo/terminal)
```

## Definition of done

- [ ] `./gradlew test` verde e lint sem erros
- [ ] Spec/ADR atualizado quando o comportamento ou uma decisão mudar
- [ ] Fluxo afetado executado no emulador (flavor `simulator`) quando houver mudança de UI ou de integração
- [ ] Nenhum invariante de pagamento enfraquecido; README atualizado se o uso mudar

## Proibido

- Acessar hardware do terminal diretamente, desabilitar testes ou baixar o `minSdk` abaixo de 25.
- Introduzir estado de pagamento fora do banco ou contornar os casos de uso a partir da UI.
- Fazer push, publicar ou criar recursos externos sem pedido explícito do desenvolvedor.
