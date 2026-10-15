# Uso de IA no desenvolvimento — Cielo LIO

## Ferramenta e contexto

Utilizei o Claude Code como ferramenta de apoio durante o desenvolvimento do aplicativo, principalmente para pesquisa técnica, implementação, revisão de código, investigação de falhas e elaboração de testes.

Para manter as alterações alinhadas às decisões do projeto, organizei o contexto técnico em três referências:

- `CLAUDE.md`: restrições, padrões e invariantes do projeto.
- `docs/specs`: comportamentos esperados e critérios de aceite.
- `docs/adr`: decisões arquiteturais e seus respectivos trade-offs.

Essa estrutura ajudou a orientar as tarefas com base nos requisitos reais da aplicação, evitando depender de instruções genéricas ou de sugestões sem contexto.

## Forma de utilização

Conduzi o desenvolvimento de forma incremental, dividindo o trabalho em tarefas menores e definindo o comportamento esperado antes de aplicar mudanças relevantes.

Utilizei a ferramenta para explorar alternativas de implementação, acelerar tarefas repetitivas e investigar problemas identificados durante a compilação, os testes e a execução no emulador.

As sugestões eram avaliadas considerando a arquitetura existente, as regras de negócio e os possíveis impactos em outros componentes. Quando uma abordagem não atendia aos requisitos, ajustava a implementação e repetia as verificações necessárias.

## Revisão e validação

A validação foi uma parte importante do processo, principalmente nos fluxos relacionados a pagamentos, nos quais erros de concorrência, callbacks duplicados ou perda de estado poderiam comprometer uma compra.

Um exemplo foi a revisão do teste de concorrência do fluxo de compra. Identifiquei que o teste de duplo toque continuava passando mesmo quando o `Mutex` era removido. A causa era o comportamento do fake utilizado no teste, que não suspendia a execução e, portanto, não reproduzia adequadamente uma condição real de concorrência.

Ajustei o fake para simular uma operação suspensiva e verifiquei o teste sem o mecanismo de exclusão mútua. Após a alteração, o teste passou a detectar a ausência do `Mutex`.

Esse caso reforçou a importância de avaliar não apenas se os testes passam, mas também se são capazes de detectar as falhas que deveriam impedir.

Além dos testes automatizados, utilizei o build, o lint e a execução no emulador para verificar o comportamento da aplicação e investigar problemas encontrados durante o desenvolvimento.

## Exemplos práticos

| Situação | Abordagem adotada |
|---|---|
| Integração com a Cielo LIO | Pesquisa do protocolo e análise dos formatos de resposta, com validação dos comportamentos relevantes. |
| Concorrência no fluxo de compra | Revisão do teste, reprodução de uma operação suspensiva e verificação da eficácia do `Mutex`. |
| Callbacks duplicados ou desconhecidos | Verificação dos critérios de correlação com a compra e do tratamento de retornos inválidos ou repetidos. |
| Problemas de compilação e lint | Investigação das causas, aplicação de correções e execução das verificações novamente. |
| Inconsistências visuais no emulador | Análise do comportamento observado e ajustes na interface para preservar a legibilidade e o fluxo de compra. |

## Estrutura das instruções

Para tarefas mais relevantes, procurei fornecer contexto suficiente para que as alterações respeitassem as decisões já tomadas no projeto. Isso incluía o módulo envolvido, o objetivo da mudança, as restrições técnicas e os critérios de aceite.

As tarefas eram então verificadas de acordo com seu impacto, utilizando testes direcionados e, quando necessário, validações mais amplas.

Essa abordagem permitiu trabalhar de maneira incremental, facilitando a revisão das alterações e a identificação de regressões.

## Considerações finais

A IA foi utilizada como ferramenta de produtividade ao longo do desenvolvimento, apoiando atividades de pesquisa, implementação, revisão e investigação de problemas.

O direcionamento técnico, a avaliação das alternativas, a definição dos critérios de qualidade e a validação dos resultados fizeram parte do meu processo de engenharia.

O objetivo foi aproveitar a velocidade proporcionada pela ferramenta sem abrir mão da compreensão do código, da consistência arquitetural e da verificação do comportamento da aplicação, especialmente nos fluxos críticos de pagamento.

As evidências apresentadas correspondem às verificações realizadas nos ambientes de teste. A validação operacional em um terminal físico e outras verificações dependentes do ambiente de produção permanecem como etapas distintas.
