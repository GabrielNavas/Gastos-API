# Como gerar o "antes e depois" do OWASP ZAP

O pipeline (`.github/workflows/ci.yml`) sobe a API com Docker Compose e roda o
**ZAP API Scan** contra o OpenAPI (`/v3/api-docs`), ja autenticado com um token JWT.
Os headers de seguranca extras sao controlados por `HARDENING_ENABLED`.

## Passo a passo

1. No GitHub: **Actions > CI > Run workflow**, escolha `hardening = false` e rode.
   Isso gera o relatorio **ANTES** (artefato `zap-report-hardening-false`).
2. Baixe o artefato, abra o `report_html.html` e **tire print**. Anote cada alerta
   (nome, risco, rota afetada) na tabela abaixo.
3. Rode de novo com `hardening = true`. Gera o relatorio **DEPOIS**
   (artefato `zap-report-hardening-true`). Tire print e compare.
4. Para alertas que sobrarem, corrija no codigo, rode de novo e registre.
   Alerta que for falso positivo ou aceito deve ser justificado (e pode entrar em `.zap/rules.tsv`).

## Tabela de achados (preencha com o que o ZAP realmente reportar)

| # | Alerta | Risco | Onde | Correcao | Status |
|---|--------|-------|------|----------|--------|
|   |        |       |      |          |        |

## Dicas

- Nao invente achados: o post vale mais com o que o ZAP de fato encontrou.
- Se o scan autenticado nao enviar o token (muitos 401 no relatorio), o ponto
  provavel de ajuste e o `cmd_options` do step do ZAP no `ci.yml`.
