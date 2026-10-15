# ADR-0009: Design system a partir da identidade visual

**Status:** Aceito

## Contexto
A identidade visual do app foi criada no Figma Make ("Visual Identity for Ticket App", React + Tailwind):
variáveis CSS de cor e raio, fonte DM Sans, ícones de traço em SVG e protótipos das telas (home, detalhe,
resumo, pagamento, processamento, resultado, ingresso). A primeira versão do app usava o Material 3 padrão.

## Decisão
- Criar `app/.../ui/designsystem` como **única fonte da aparência**:
  - `BrandColors`/`BrandRadius`: tokens copiados 1:1 das variáveis CSS;
  - `IngressosTheme`: `ColorScheme`, `Typography` e `Shapes` do Material 3 derivados desses tokens;
  - `BrandIcons`: os ícones do protótipo portados para `ImageVector` a partir do path SVG original;
  - `BrandComponents`: header com logo e pílula "Smart", cards, botões, stepper, `RowsCard`, `InfoCard`, `Pill`, `DateBlock` e barra inferior.
- As telas compõem esses componentes; cor ou tamanho "solto" em tela é proibido (ver `CLAUDE.md`).
- **Tema somente claro**: a identidade define uma paleta única de alto contraste, adequada ao terminal.
- Barra inferior com **Início** e **Ingressos** (a aba "Perfil" do protótipo foi removida a pedido do autor).

## Adaptações em relação ao protótipo
| Protótipo | App | Motivo |
|---|---|---|
| Detalhe → Resumo → Pagamento → Processando → Resultado | Checkout único → Comprovante (pendente/resultado/ingressos) | Mínimo de passos no terminal (boas práticas Cielo) e invariantes de idempotência já ancorados nesse fluxo |
| Fotos de capa remotas (Unsplash) | Cards sem foto; capa do ingresso em gradiente da marca | Offline-first, sem dependência de rede ou de biblioteca de imagens |
| Passos animados "Conectando… Processando…" | Anel de processamento + texto | O app não conhece o progresso interno do terminal; não exibimos progresso fictício |
| DM Sans via Google Fonts | `BrandFont` parametrizado (sans-serif do sistema por ora) | Terminais sem Google Play Services; o TTF (OFL) entra em `res/font` |
| Busca decorativa | Busca funcional, sem acento/maiúsculas (`Event.matches`) | Não deixar controle sem função |

## Consequências
- Trocar a marca (ou atender a um white label) é mudar um pacote, sem tocar nas telas.
- A lógica de apresentação (ViewModels) e o domínio não mudaram; os testes continuaram verdes.
- Custo: componentes próprios a manter em vez dos padrões do Material.
