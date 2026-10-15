# ADR-0008: Fakes compartilhados em vez de mocks

**Status:** Aceito

## Contexto
Os cenários críticos (concorrência, compare-and-set, callbacks duplicados) dependem do **comportamento**
dos colaboradores, não de interações. Mocks tendem a testar a implementação e quebrar em refatorações.

## Decisão
- Fakes em `domain/src/testFixtures` (plugin `java-test-fixtures`), reutilizados por `:data`, `:payment:*` e `:app`.
- Os fakes **honram o contrato real**: `InMemoryPurchaseRepository` faz compare-and-set e suspende como I/O real (`yield`).
- `Clock` e `IdGenerator` determinísticos.
- Testes de persistência usam o SQLite real (Robolectric).

## Consequências
- Testes legíveis, rápidos e estáveis a refatorações.
- Verificação por mutação: com o fake antigo (sem suspensão), o teste de duplo toque passava mesmo sem o `Mutex`.
  O fake foi corrigido, e hoje o teste falha sem a proteção.
