# Como gerar o antes e o depois do ZAP

O job `zap-scan` do pipeline sobe a API, autentica com um usuário de teste e roda o ZAP API Scan contra `/v3/api-docs`. A variável `HARDENING_ENABLED` liga e desliga os headers de segurança extras, o que permite comparar os dois cenários.

## Passo a passo

1. No GitHub, vá em Actions, escolha o workflow CI e clique em Run workflow com `hardening = false`. Esse é o relatório "antes".
2. Baixe o artefato `zap-report-hardening-false`, abra o `report_html.html` e tire um print.
3. Rode de novo com `hardening = true` e baixe o `zap-report-hardening-true`. Esse é o "depois".
4. Se sobrarem alertas que valham correção, ajuste o código, rode outra vez e registre.
5. Alerta que for falso positivo ou que eu decidir aceitar entra na tabela com a justificativa.

## Achados

| Alerta | Risco | Onde | O que fiz | Situação |
|---|---|---|---|---|
| SQL Injection | High | `PUT /api/categories/{id}`, parâmetro `name` | Revisão das consultas e teste `SqlInjectionTest` | Falso positivo |
| Cross-Origin-Resource-Policy Header Missing or Invalid | Low | 5 respostas | Header `Cross-Origin-Resource-Policy: same-origin` | Corrigido |
| A Client Error response code was returned | Info | 59 respostas | Nada (a API rejeita entradas inválidas) | Esperado |
| Authentication Request Identified | Info | login | Nada | Informativo |
| Non-Storable Content | Info | sistêmico | Nada (`Cache-Control: no-store`) | Esperado |

## Se o scan não estiver autenticado

Se o relatório vier cheio de respostas 401, o token provavelmente não está chegando ao ZAP. O ponto a ajustar é o `cmd_options` do step do ZAP no `ci.yml`.