# Contexto Tecnico do Processo de Exemplo

Ultima analise: 2026-10-23

Este documento e ficticio. Serve para demonstrar o assistente sem expor regras de negocio de um cliente.

## 1. Visao

O processo `exemplo` e um fluxo Fluig de demonstracao com tres etapas: abertura, analise e encerramento.

## 2. Atividades

| ID | Nome | Tipo |
|----|------|------|
| `0` | Inicio | Usuario |
| `10` | Analisar solicitacao | Usuario |
| `20` | Encerrar | Usuario |

## 3. Campos principais

- `solicitante`: preenchido automaticamente no inicio.
- `descricao`: texto obrigatorio na abertura.
- `decisao`: `aprovar` ou `devolver` na atividade `10`.

## 4. Regras

- Na atividade `10`, se `decisao` for `devolver`, o fluxo volta para a atividade `0`.
- Se `decisao` for `aprovar`, o fluxo segue para a atividade `20`.
- O documento nao cobre integracoes externas.
