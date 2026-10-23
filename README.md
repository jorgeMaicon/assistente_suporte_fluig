# Assistente Suporte Fluig

Widget Fluig (WAR) com API REST em `/rest`. Nesta versão a tela do widget só indica que a interface é a API; o uso real é por HTTP (em geral a partir de um dataset Fluig).

Cada processo publicado tem um markdown em `src/main/resources/knowledge/<processId>.md`. O chamador informa o `processo`; o servidor monta o prompt com esse documento e consulta a OpenAI. A resposta usa **apenas** o que está no markdown.

Este repositório traz um processo fictício (`exemplo`) para demonstração. Quem publica o widget escreve o conhecimento dos próprios processos; regras de negócio de clientes **não** ficam versionadas aqui.

## Objetivo

Permitir que suporte técnico pergunte sobre um processo Fluig e receba uma resposta rastreável:

- hash SHA-256 do markdown carregado (`contexto`);
- data da linha `Ultima analise:` do documento, quando existir (`ultimaAnalise`).

## Papel de cada parte

| Parte | Responsabilidade |
|-------|------------------|
| Dataset / portal Fluig | Autentica o usuário (ex.: papel `admin`) e chama o WAR |
| Este WAR | Valida o header `X-Internal-Token`, monta o prompt e chama a OpenAI |
| OpenAI | Gera a resposta com base no prompt |

O script de referência está em [datasets/ds_assistente_suporte_fluig.js](datasets/ds_assistente_suporte_fluig.js). Quem implanta publica o dataset no Fluig e configura o `INTERNAL_TOKEN` localmente.

## Fluxo

1. O cliente (dataset ou HTTP) faz `POST /rest/chat` com `X-Internal-Token`.
2. O catálogo carrega `knowledge/<processo>.md` do classpath.
3. O prompt vai para `https://api.openai.com`.
4. A resposta JSON traz `status`, `processo`, `contexto`, `resposta` e, se houver, `ultimaAnalise`.

## Pré-requisitos

- JDK 11
- Maven 3.x
- Fluig capaz de publicar widgets (WAR)
- Chave de API da OpenAI
- Saída HTTPS do servidor Fluig para `api.openai.com` (ou plano B via serviço Fluig; ver [Egress](#egress-openai-fluig-cloud))

## Segredos (fora do Git)

1. Copie os profiles de [settings.xml.example](settings.xml.example) para `~/.m2/settings.xml` (fora do repositório).
2. Preencha `openai.apiKey` e `api.internalToken` nos profiles `homolog` e `prod`.
   - `api.internalToken`: valor aleatório longo (mínimo 16 caracteres), **independente** da chave OpenAI. Ex.: dois UUIDs concatenados.
3. Use o **mesmo** `api.internalToken` no dataset (constante enviada no header `X-Internal-Token`).

O template [src/main/resources-filtered/credentials.properties](src/main/resources-filtered/credentials.properties) só tem placeholders; os valores entram no WAR no empacotamento Maven.

## Conhecimento do processo

1. Copie `src/main/resources/knowledge/exemplo.md` para `knowledge/<processId>.md`.
2. O `processId` deve usar apenas letras, números, `_` e `-`.
3. Inclua a linha `Ultima analise: AAAA-MM-DD` (o catálogo extrai essa data).
4. Liste o id em `knowledge.processes` em [src/main/resources/app.properties](src/main/resources/app.properties) (vários ids separados por vírgula).

### Overlay local (processo real sem versionar)

Para empacotar um processo que **não** deve ir para o Git:

1. Crie `src/main/resources/knowledge.local.properties` (já no `.gitignore`) com:

```properties
knowledge.processes=meu_processo
```

2. Coloque o markdown em `src/main/resources/knowledge/meu_processo.md` (arquivos nessa pasta, exceto `exemplo.md`, também são ignorados pelo Git).

Se `knowledge.local.properties` existir no classpath, ele substitui **somente** `knowledge.processes`. Sem o arquivo, vale o padrão do repositório (`exemplo`).

## Build

```bash
mvn -Phomolog clean verify   # homologacao
mvn -Pprod clean verify      # producao
```

Artefato: `target/assistente_suporte_fluig.war`. Não versionar `target/` nem o WAR.

## Publicar no Fluig

1. Painel de Controle → Aplicativos → Central de Componentes → publicar o WAR.
2. Context root: `/assistente_suporte_fluig`.

## Serviço REST e dataset no Fluig

1. Cadastre um serviço REST com código **`ASSISTENTE_SUPORTE_FLUIG`** apontando para a base do widget, por exemplo:

   `https://<servidor>/assistente_suporte_fluig`

2. Publique o script [datasets/ds_assistente_suporte_fluig.js](datasets/ds_assistente_suporte_fluig.js) como dataset no Fluig (código `ds_assistente_suporte_fluig`).
3. No script, troque `INTERNAL_TOKEN` pelo **mesmo** `api.internalToken` do build do WAR (não use a chave OpenAI).

O dataset autentica o usuário no papel `admin` (`fdn_userrole`), chama `POST /rest/chat` no serviço e devolve as colunas `RESPOSTA`, `PROCESSO`, `CONTEXTO` e `ERRO`.

### Constraints

| Constraint | Uso |
|------------|-----|
| `PROCESSO` | Id do markdown (`exemplo`, ou o seu processId) |
| `PERGUNTA` | Texto da pergunta |
| `HISTORICO` | Opcional: JSON array `[{role, content}, ...]` |

### Teste no console

Usuário no papel `admin`:

```javascript
var c1 = DatasetFactory.createConstraint("PROCESSO", "exemplo", "exemplo", ConstraintType.MUST);
var c2 = DatasetFactory.createConstraint("PERGUNTA", "Qual atividade analisa a solicitacao?", "Qual atividade analisa a solicitacao?", ConstraintType.MUST);
var ds = DatasetFactory.getDataset("ds_assistente_suporte_fluig", null, [c1, c2], null);
console.log(ds.values);
```

## API

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| `GET` | `/rest/health` | pública | Status, processos e hashes. Com `?probeOpenAi=true` tenta egress para a OpenAI. |
| `POST` | `/rest/chat` | header `X-Internal-Token` | Chat com contexto do processo. Header opcional `X-User-Code` para rate limit. |

### POST `/rest/chat`

```json
{
  "processo": "exemplo",
  "pergunta": "Qual atividade analisa a solicitacao?",
  "historico": [
    { "role": "user", "content": "..." },
    { "role": "assistant", "content": "..." }
  ]
}
```

Resposta:

```json
{
  "status": "ok",
  "processo": "exemplo",
  "contexto": "<sha256 do markdown>",
  "resposta": "...",
  "ultimaAnalise": "2026-10-23"
}
```

### Limites (configuráveis em `app.properties`)

| Limite | Padrão |
|--------|--------|
| Tamanho da pergunta | 2000 caracteres |
| Turnos de histórico | 6 (12 mensagens) |
| Rate limit por usuário (`X-User-Code`) | 20 / minuto |
| Rate limit por IP | 60 / minuto |
| Tamanho máximo do corpo | 1 MB |

### Erros HTTP

| Status | Situação |
|--------|----------|
| `400` | JSON inválido, pergunta vazia ou acima do limite |
| `401` | Token interno ausente ou inválido |
| `404` | `processo` desconhecido |
| `429` | Rate limit |

## Egress OpenAI (Fluig Cloud)

Após o deploy:

```text
GET https://<servidor>/assistente_suporte_fluig/rest/health?probeOpenAi=true
```

- `openai.reachable=true`: o WAR chama a OpenAI diretamente (desenho atual).
- `openai.reachable=false`: o Cloud bloqueou a saída do WAR. Cadastre um serviço Fluig `OPENAI` e mova o HTTP para o dataset; o WAR continua dono do prompt e do catálogo. O campo `egressHint` na resposta do health descreve isso.

## Atualizar o markdown de um processo

1. Edite `src/main/resources/knowledge/<processId>.md` (e a linha `Ultima analise:`).
2. Se for processo novo, acrescente o id em `knowledge.processes` (ou no overlay local).
3. `mvn -Phomolog clean verify` ou `mvn -Pprod clean verify` e republicar o WAR.

## O que não commitar

- Chave OpenAI e `api.internalToken` (ficam em `~/.m2/settings.xml` e no `INTERNAL_TOKEN` do dataset **no Fluig**, não no Git)
- `src/main/resources/knowledge.local.properties`
- Markdown de processo real (`knowledge/*` exceto `exemplo.md`)
- `target/` e arquivos `*.war`

## Estrutura

```
datasets/
  ds_assistente_suporte_fluig.js   # dataset Fluig que chama o WAR
src/main/java/widget/
  ApplicationConfig.java
  Services.java
  config/          AppConfig, Credentials, PropertiesLoader
  knowledge/       KnowledgeCatalog (*.md por processo)
  prompt/          PromptBuilder
  client/          OpenAiClient
  service/         ChatService
  security/        token interno, rate limit, tamanho, headers
  resource/        ChatResource, HealthResource
src/main/resources/
  app.properties
  knowledge/
    exemplo.md
  knowledge.local.properties   # opcional, gitignored
src/main/resources-filtered/
  credentials.properties       # placeholders; valores no build
```
