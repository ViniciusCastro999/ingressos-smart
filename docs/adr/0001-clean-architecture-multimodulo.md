# ADR-0001: Clean Architecture em módulos Gradle

**Status:** Aceito

## Contexto
O desafio avalia organização, manutenção, SOLID e Clean Architecture. Em um único módulo, a regra de
dependência (o domínio não conhece frameworks) depende de disciplina e code review. Além disso, a forma
de pagamento é a parte mais volátil: outro terminal, TEF, API remota.

## Decisão
Separar em módulos Gradle por camada:
- `:domain`: Kotlin/JVM puro. Entidades (`Event`, `Purchase`, `Ticket`, `Money`), casos de uso e portas.
- `:data`: Room e repositórios. Exporta apenas interfaces do domínio (`DataLayer`).
- `:payment:cielo`: adapter da Cielo Smart (implementa `PaymentTerminal` e `PaymentResultParser`).
- `:payment:simulator`: simulador de terminal (ver ADR-0005).
- `:app`: UI Compose, ViewModels, navegação e composition root (ADR-0006).

## Consequências
- O compilador garante a regra de dependência: o domínio não consegue importar Android ou Room.
- Testes do domínio rodam na JVM em segundos.
- Trocar o meio de pagamento é escrever um novo adapter (OCP/DIP).
- Custo: mais arquivos de build. É aceitável, e o ganho cresce com o app.
