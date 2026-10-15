# ADR-0005: Flavor `simulator` com simulador do protocolo Cielo

**Status:** Aceito

## Contexto
O Emulador Cielo oficial roda apenas em Android 7.1/10 e depende de APK externo. Avaliadores, CI e
desenvolvimento do dia a dia precisam exercitar o fluxo completo (incluindo negação, cancelamento,
divergência de valor e ausência de resposta) em qualquer dispositivo.

## Decisão
Criar o módulo `:payment:simulator`, incluído **apenas** no flavor `simulator`. Ele contém uma Activity
que atende `lio-sim://payment` e responde no **mesmo formato** da Cielo (Base64 com quebras de linha,
códigos de erro oficiais), em **processo e task próprios**, como um app externo.
O flavor muda só a configuração (`terminalScheme`, esquema de callback). **O adapter Cielo é o mesmo.**

## Consequências
- O caminho de produção (factory, parser, callback e casos de uso) é exercitado de ponta a ponta sem hardware.
- Permite testar a morte do processo do app durante o pagamento.
- Esquemas distintos (`lio-sim`, `ingressos-sim`) evitam conflito com o Emulador Cielo instalado no mesmo aparelho.
- O APK `cielo` não contém o simulador.
