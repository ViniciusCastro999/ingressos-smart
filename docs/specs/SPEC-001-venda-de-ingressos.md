# SPEC-001: Venda de ingressos

**Objetivo:** permitir que o operador de um terminal Cielo Smart venda ingressos de eventos locais em
poucos toques, com comprovante e QR Code por ingresso.

## Personas
- **Operador** (vendedor na bilheteria/portaria): usa o terminal em ritmo rápido e precisa de poucos passos e mensagens claras.
- **Comprador**: recebe o comprovante e os QR Codes.

## Histórias e critérios de aceite

### H1: Ver eventos disponíveis
- **Dado** o catálogo de eventos, **quando** o app abre, **então** vejo os eventos ordenados por data, com local, preço e disponibilidade.
- Evento esgotado aparece desabilitado com "Esgotado". Com ≤ 10 ingressos, mostra "Últimos N ingressos".
- Se houver pagamento aguardando o terminal, um aviso fixo no topo leva até ele.
- Busca por nome, local ou cidade, sem diferenciar acentos e maiúsculas; todos os termos precisam bater.
- Navegação principal por barra inferior: **Início** (eventos) e **Ingressos** (histórico).
- _Testes:_ `RoomEventRepositoryTest`, `EventTest`.

### H2: Escolher a quantidade
- Quantidade mínima 1, máxima `min(estoque, 10)`. Os botões −/+ respeitam os limites e o total = preço × quantidade (centavos).
- _Testes:_ `EventTest`, `CheckoutViewModelTest.quantity is clamped…`.

### H3: Pagar com a Cielo Smart
- **Quando** toco "Pagar", **então** a compra é registrada como `PENDING` **antes** de abrir o app de pagamento.
- Forma de pagamento opcional: Crédito, Débito, Pix ou "Na maquininha" (escolhida no terminal).
- Duplo toque, rotação ou reenvio **não geram segunda cobrança** (SPEC-003).
- Se outro pagamento aguarda o terminal, não despacho e informo, com atalho para ele.
- Se o app da Cielo não existir, a compra fica `FAILED` com "nada foi cobrado".
- _Testes:_ `StartPurchaseUseCaseTest`, `CheckoutViewModelTest`.

### H4: Registrar o resultado
- Aprovada → `APPROVED`, ingressos emitidos, estoque consumido (tudo em uma transação).
- Negada (código 3) → `DECLINED`; cancelada (código 1) → `CANCELED`; erro (2, 4, ilegível) → `FAILED`.
- Valor aprovado ≠ total → `UNCONFIRMED`, sem ingressos.
- Terminal sem resposta → operador libera → `UNCONFIRMED`.
- _Testes:_ `ProcessPaymentResultUseCaseTest`, `PurchaseTest`, `RoomPurchaseRepositoryTest`, `MainViewModelTest`.

### H5: Ver comprovante
- O comprovante mostra status, motivo, resumo (evento, data, local, quantidade × preço, forma, pedido, data da compra, total) e, se aprovado, os dados Cielo (valor pago, autorização, NSU, cartão, produto, terminal, pedido Cielo).
- O comprovante se atualiza sozinho quando o terminal responde.
- Compras não aprovadas oferecem "Tentar novamente" (novo checkout, nova chave).
- Histórico na aba "Ingressos" ("Meus ingressos"), com status de cada compra.
- Aparência conforme a identidade visual (ADR-0009): resultado com ícone e halo na cor do status, anel de processamento enquanto pendente, ingresso digital com picote.

### H6: QR Code do ingresso (opcional do desafio)
- Um QR por ingresso, gerado **apenas** para compra `APPROVED`, com conteúdo `INGRESSO:<evento>:<compra>:<ingresso>`.
- _Testes:_ `QrCodeEncoderTest` (ida e volta encode → decode).

## Fora de escopo
Backend, estorno, impressão, validação do QR na portaria, multi-terminal. Ver README §12.
