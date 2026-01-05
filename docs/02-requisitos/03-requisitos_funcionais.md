# Requisitos Funcionais

_Projeto:_ **Darwin — AI Product & Platform**  
_Documento gerado em:_ **2025-09-14 23:57**  
_Status:_ **esqueleto inicial (EDLN-23)**  

> **Nota:** conteúdo abaixo foi importado do arquivo enviado.

# ONTO-TOOLS — Especificação de Requisitos (v0.1)

## 2. Requisitos Funcionais (refatorados — padrão “Gerenciar”)

> Os requisitos a seguir consolidam ações afins sob macroações “**Gerenciar**”, com detalhamento dos subcomportamentos na descrição. Mantive a prioridade e a origem e incorporei itens do `project_fabricio.md`.

Para facilitar rastreabilidade e manutenção dos requisitos, os IDs seguem faixas por domínio:

- RF-1xx = Ontologia TTL · RF-2xx = SPARQL · RF-3xx = Exportação · RF-4xx = Entrada Externa.

Essa organização melhora a gestão e a evolução do conjunto de requisitos ao longo do ciclo de vida (boa prática de engenharia de requisitos) conforme a norma [ISO/IEC/IEEE 29148:2018](https://www.iso.org/standard/72089.html), que especifica os processos e produtos envolvidos na engenharia de requisitos ao longo do ciclo de vida de sistemas e software.

### 2.1. Gerenciar Ontologia TTL

| ID | Nome | Descrição | Prioridade | Check |
|:---:|:---:|---|:---:|:---:|
| **RF-101** | **Carregar TTL** | Carregar ontologia em Turtle (.ttl) a partir de caminho/arquivo. | Alta | ❌ |
| **RF-102** | **Validar TTL (Sintaxe)** | Validar sintaxe RDF/Turtle; em caso de erro, abortar e informar linha/coluna. | Alta |  ❌ |
| **RF-103** | **Reordenar TTL** | Padronizar o conteúdo para ordem canônica: type → subclass → annotations; em cada bloco: sujeito → predicado → objeto; mesma entrada ⇒ mesma saída (determinístico). | Alta | ❌ |
| **RF-104** | **Gerar Saída p/ Revisão** | Gerar TTL normalizado e log (entrada, resultado, duração) em diretório de saída; aplicação/merge é manual pelo curador. | Alta | ❌ |
| **RF-105** | **Listar Classes e Propriedades** | A partir do TTL carregado (RF-101) e validado (RF-102), gerar listagem de classes e propriedades com campos mínimos. Classes: iri, label, definition?, parent?. Propriedades: iri, label, domain, range. Ordenar por IRI (aplicar ordenação canônica — RF-103), permitir filtro opcional (IRI/prefixo) e exportar opcionalmente em CSV/JSON via Exportar Ontologia (RF-301: json_estrutural / xls_ontologia). Saída determinística e sem gravar no repositório. (Preferir consultas SPARQL conforme BR-08; pode reutilizar RF-201/203 para execução/parametrização.) | Alta | ❌ |
| **RF-106** | **Aplicar Diff Fonte→TTL (CRUD & Log)** | Dado `diff-report` gerado em **Comparar Artefatos** (RF-501…RF-504), **aplicar** operações CRUD no TTL de forma **determinística**; gerar **`apply-log.json`** (ações, IRIs afetados, sucesso/erros) e **persistir** o TTL resultante em diretório de saída. Suportar **dry-run** e **código de saída ≠ 0** em caso de falhas. | Alta | ❌ |
| **RF-107** | **Ordenar Aplicação (Merge)** | Definir e **aplicar** a ordem ao executar o merge automático de diffs no TTL: (1) `type`, (2) `subClassOf`, (3) `annotations` (*label*, *definition*, propriedades funcionais). **Pré-condições:** validação de esquema (RF-402) e ausência de conflitos impeditivos. **Falhas** devem bloquear etapas subsequentes. | Alta | ❌ |


| ID | Nome | Descrição | Dependências | Prioridade | Check |
|:---:|:---:|---|:---:|:---:|:---:|
| **RF-101** | **Carregar TTL** | Carregar ontologia em **Turtle (.ttl)** a partir de **caminho/arquivo**; detectar **encoding** (*UTF-8* padrão), resolver `@base`/**prefixos** e produzir **grafo em memória** com metadados de diagnóstico (`hash SHA-256`, **nº de triplas**). Em **erro de I/O**, **abortar** com **mensagem objetiva**. | — | Alta | ❌ |
| **RF-102** | **Validar TTL (Sintaxe)** | Validar sintaxe RDF/Turtle do arquivo/stream carregado; em erro, **abortar** e informar **linha/coluna**, trecho e causa. Não deve alterar a fonte. | RF-101 | Alta | ❌ |
| **RF-103** | **Reordenar TTL** | Reordenar para forma **canônica** (ordem: `type` → `subClassOf` → `annotations`; em cada bloco: `sujeito` → `predicado` → `objeto`), **normalizar prefixos/base** e **serializar de modo determinístico e idempotente** (mesma entrada válida ⇒ mesma saída). | RF-101, RF-102 | Alta | ❌ |
| **RF-104** | **Gerar Saída p/ Revisão** | Gerar **pacote de revisão** contendo TTL **normalizado** (RF-103) + `review-log.json` (entrada, resultado, duração, hash antes/depois) em diretório de saída; **não** altera a fonte; merge posterior é manual. | RF-101, RF-102, RF-103 | Alta | ❌ |
| **RF-105** | **Listar Classes e Propriedades** | A partir do TTL válido, listar **Classes** (`iri`, `label`, `definition?`, `parent?`) e **Propriedades** (`iri`, `label`, `domain`, `range`); **ordenar por IRI** (canônico), permitir **filtro opcional** (IRI/prefixo) e **exportação opcional** via Exportar Ontologia (RF-301/304). **Preferir SPARQL** reutilizando RF-201/RF-203. Saída determinística, sem gravação no repositório. | RF-101, RF-102; usa RF-201/203; opc.: RF-301/304 | Alta | ❌ |
| **RF-106** | **Aplicar Diff Fonte→TTL (CRUD & Log)** | Aplicar `diff-report` (RF-501…RF-504) com operações **CRUD** no TTL de forma **determinística** e segundo a **ordem de merge** (RF-107); suportar `--dry-run`; gerar `apply-log.json` (ações, IRIs afetados, sucesso/erros) e **persistir** o TTL resultante; **código de saída ≠ 0** em falha. | RF-101, RF-102, RF-402, RF-501…RF-504, RF-107 | Alta | ❌ |
| **RF-107** | **Ordenar Aplicação (Merge)** | Definir e **aplicar** a ordem de merge automática ao processar diffs no TTL: (1) `type`, (2) `subClassOf`, (3) `annotations` (*label*, *definition*, propriedades funcionais). **Pré-condições:** esquema válido (RF-402) e ausência de conflitos impeditivos; falhas **bloqueiam** etapas subsequentes. | RF-402 (consumido por RF-106) | Alta | ❌ |


### 2.2. Gerenciar SPARQL

| ID | Nome | Descrição | Prioridade | Check |
|:---:|:---:|---|:---:|:---:|
| **RF-201** | **Executar SPARQL** | Carregar e executar SPARQL (SELECT/ASK/CONSTRUCT) contra o TTL; bloquear UPDATE; exportar resultado CSV/JSON. | Alta | ❌ |
| **RF-202** | **Organizar Categorias SPARQL** | Organizar consultas por categorias (pastas/nomes) e permitir listar/selecionar por categoria (ex.: validação, extração, relatórios). | Alta | ❌ |
| **RF-203** | **Parametrizar SPARQL** | Permitir parâmetros dinâmicos (ex.: `${classe}`, `${limite}`) com valores padrão e validação simples de tipo (string/número/IRI); aplicar substituição segura antes da execução. | Alta | ❌ |
| **RF-204** | **Validar SPARQL (Semântica e Regressão)** | Executar suítes de validação (consultas SPARQL e shapes SHACL) e testes de regressão quando o TTL mudar (detectar por hash do arquivo); relatar consultas que falham (compilação, shape/colunas esperadas ou ASK) e IRIs impactados. Correção é manual pela equipe; retornar código de saída ≠ 0 e salvar relatório. | Alta | ❌ |

| ID | Nome | Descrição | Dependências | Prioridade | Check |
|:---:|:---:|---|:---:|:---:|:---:|
| **RF-201** | **Executar SPARQL** | Executar **SPARQL** (`SELECT`/`ASK`/`CONSTRUCT`) contra o **grafo em memória** (TTL carregado); **bloquear `UPDATE`**; aceitar **arquivo `.rq`** ou **string**; suportar **parâmetros** (RF-203); **timeout** configurável; **ordenar colunas/linhas de forma determinística** (reprodutibilidade); **exportar** resultado em **CSV/JSON**; em erro, **relatar linha/coluna** e **causa**. | RF-101, RF-102; opc.: RF-203 | Alta | ❌ |
| **RF-202** | **Organizar Categorias SPARQL** | Organizar consultas por **categorias** (pastas/nomes) com **convenção** (`<categoria>/<nome>.rq`); **listar/selecionar** por categoria; manter **metadados** (`title`, `description`, `params`) em `query.yaml`; **validar duplicidades**. Integra-se à RF-201 para execução por **identificador**. | — | Alta | ❌ |
| **RF-203** | **Parametrizar SPARQL** | Permitir **parâmetros dinâmicos** com **valores padrão** e **validação de tipo** (*string/número/IRI*); **substituição segura** (sem injeção; **whitelist** de `${param}`); suportar `required`, `enum`, `min/max` e **normalização de IRI**. | — (consumido por RF-201) | Alta | ❌ |
| **RF-204** | **Validar SPARQL (Semântica & Regressão)** | Executar **suítes** (consultas SPARQL/`ASK`, **shapes SHACL**) em **mudanças do TTL** (detectar por **hash**); **relatar** falhas (compilação, colunas esperadas, `ASK=false`) e **IRIs impactados**; **salvar relatório** (`validation-report.json`) e **retornar código ≠ 0** em falha. | RF-101, RF-102, RF-201 | Alta | ❌ |


### 2.3. Exportar Ontologia

> **Base normativa:**
> **[ISO/IEC/IEEE 29148:2018 — Requirements engineering](https://www.iso.org/standard/72026.html)**  exige separação de responsabilidades para redução de aclopamento e facilitar manutenção; mantemos **Exportar** separado de **Gerenciar Ontologia TTL**: aqui são **produtos de saída** (JSON/XLS/HTML) para consumo/revisão, enquanto “Gerenciar TTL” trata do arquivo-fonte.
>
> **Base normativa (por que dividir por formato):**  
> **[ISO/IEC/IEEE 29148:2018 — Requirements engineering](https://www.iso.org/standard/72026.html)** exige requisitos **singulares** (C5) e **verificáveis** (C7). Reunir JSON, HTML e XLS no mesmo RF fere essas propriedades; ao **separar por formato**, cada RF expressa uma única capacidade e tem critério de teste próprio.
>
> **Apoio complementar:**  
> **[INCOSE Guide to Writing Requirements (v4, 2023)](https://www.incose.org/products-and-publications/se-store/guide-to-writing-requirements)** recomenda redação estruturada e requisitos **atômicos** , o que se alcança ao decompor “Exportar Ontologia” em RF-302…RF-306.
>
>**Boas práticas de análise:** **[BABOK® Guide v3 — A Guide to the Business Analysis Body of Knowledge](https://www.iiba.org/standards-and-resources/babok/)** orienta a **decomposição funcional** para reduzir complexidade e incerteza; dividir por formato melhora estimativas, ownership e rastreabilidade.
>
> **Evidência no projeto:**  
> Os formatos enviados têm finalidades e esquemas distintos — *ICD JSON com/sem herança* (`Arquivo ICD com herança.json`, `Arquivo ICD sem herança.json`), *MDA JSON* (`Arquivo MDA Json.json`), *XLS MDA LOIN* (`Arquivo XLS MDA Loin.xlsx`) e *HTML navegável* — exigindo regras e critérios de aceite próprios; portanto, cada um deve ser um **RF separado**.

O objetivo é disponibilizar exportações derivadas de um arquivo-fonte TTL da ontologia, para múltiplos formatos de consumo (máquina e humano), com saídas determinísticas, versionadas e verificáveis.

| ID | Nome | Descrição | Prioridade | Check |
|:---:|:---:|---|:---:|:---:|
| **RF-301** | **Exportar Ontologia (Framework)** | Prover um mecanismo de exportação extensível (plugin por formato) a partir de um TTL válido e deve produzir artefatos em diretório de saída configurável. | Alta | ❌ |
| **RF-302** | **Exportar JSON Estrutural** | Exportar `json_estrutural`, refletindo classes, propriedades e relações do TTL, conforme **schema versionado**, com **ordenação determinística** de chaves/listas. **Parâmetro:** `withInheritance={true\|false}`. | Alta | ❌ |
| **RF-303** | **Exportar JSON Hierárquico** | Exportar `json_hierarquico` (árvore por `subClassOf`) com `breadcrumb` e `depth`; **deve** respeitar `withInheritance` para atributos resolvidos por herança. | Média | ❌ |
| **RF-304** | **Exportar XLS Ontologia (Catálogo)** | Exportar `xls_ontologia` com catálogo técnico da ontologia, com abas e colunas padronizadas para revisão tabular (ex: abas mín.: `Classes`, `Propriedades`, `Vocabulário`), colunas padronizadas e **validações de dados**; saída determinística. | Alta | ❌ |
| **RF-305** | **Exportar XLS MDA/LOIN (Comentários)** | Exportar `xls_mda_loin` para ciclo de comentários (abas `Itens`, `Dicionário`, `Instruções`) e **deve** preservar IDs/Status para **reimportação** (*round-trip*) sem perda. | Alta | ❌ |
| **RF-306** | **Exportar HTML Navegável** | Exportar site estático navegável com índice, página por classe, sem backend, busca client-side e links cruzados; **deve** ser reprodutível (mesmo TTL ⇒ mesmo build). | Média | ❌ |
| **RF-307 (opcional)** | **Exportar MDA JSON (compatibilidade)** | Se habilitado, exportar `mda_json` conforme **schema MDA v1** para integração legada, declarando versão e mapeamentos. | Baixa | ❌ |
| **RF-308** | **Consolidar Comentários (HTML → TTL)** | A partir do HTML exportado (RF-306), **coletar** comentários dos revisores, **consolidar** em `merge-suggestions.md` (humano) e `comments.json` (máquina) e **oferecer** aplicação opcional via RF-106 (feature flag). Registrar `comments-log.json`. | Média | ❌ |
| **RF-309** | **Publicar API JSON (read-only)** | Expor **endpoints** derivados do TTL/SPARQL (somente leitura), com **respostas determinísticas** e **versionamento**: `GET /classes`, `GET /classes/{iri}`, `GET /classes/{iri}/properties`, `GET /hierarchy`. Incluir **paginação**, **filtros** (IRI/prefixo), `Content-Type: application/json`, **CORS** configurável e **request-id** por chamada. | Alta | ❌ |

### 2.4. Gerenciar Entrada de Dados Externos (MDA, LOIN, ICD)

| ID | Nome | Descrição | Prioridade | Origem |
|---|---|---|---|---|
| RF-401 | Carregar Fonte Externa (Formato) | Carregar ICD (JSON/XLS), MDA (JSON) e LOIN (XLS MDA) para estrutura de classes em memória. Variantes: `icd_json` \| `icd_xls` \| `mda_json` \| `loin_xls`. Saídas: estrutura em memória + relatório de ingestão (lidos/ignorados/erros) em diretório de saída. | Alta | Fabrício / LOIN / ICD |
| RF-402 | Validar Fonte (Esquema) | Validar a estrutura antes de carregar. JSON → JSON Schema; XLS → regras declarativas (CSVW-like); em erro, abortar e gerar relatório (aba/linha/coluna).. | Alta | Boas práticas |

> **Base normativa (por que decompor):**  
> **[ISO/IEC/IEEE 29148:2018 — Requirements engineering](https://www.iso.org/standard/72026.html)** exige requisitos **singulares** (C5) e **verificáveis** (C7). Misturar ingestões com **regras distintas** (JSON vs. XLS, herança vs. não) no mesmo RF prejudica singularidade e teste.  
> **Apoio complementar:** [INCOSE Guide to Writing Requirements (v4, 2023)](https://www.incose.org/products-and-publications/se-store/guide-to-writing-requirements) · [BABOK® Guide v3 — IIBA](https://www.iiba.org/standards-and-resources/babok/)
> **Decisão:** manter um **framework de ingestão** (épico) + **um RF por formato** + validação comum. Isso preserva rastreabilidade e facilita extrair **Casos de Uso**.

| ID | Nome | Descrição | Prioridade | Check |
|:---:|:---:|---|:---:|:---:|
| **RF-401** | **Ingestão de Fonte Externa (Framework)** | Prover mecanismo extensível para ingestão de fontes externas, que **invoca RF-402** e, **aprovado o esquema**, **deve** normalizar e popular a estrutura em memória, gerando relatório de saída (ex: `ingest-report.json`). | Alta | ❌ |
| **RF-402** | **Validar Fonte (Esquema/Regra, pré-carregamento)** | Antes de carregar, validar **apenas o esquema**: **JSON** por **JSON Schema**; **XLS** por **regras declarativas** (CSVW-like - abas, cabeçalhos, tipos, domínios, unicidade). Em erro, **deve** abortar e gerar relatório (ex: `validation-report.json`) com **aba/linha/coluna** e mensagem. | Alta | ❌ |
| **RF-403** | **Carregar ICD (JSON)** | Após **RF-402 sem erros**, carregar **ICD JSON** (com/sem herança), mapeando `classes`, `propertysets`, `IfcProperty{valueType, unit, validValues}` para estrutura em memória. | Alta | ❌ |
| **RF-404** | **Carregar ICD (XLS)** | Após **RF-402 sem erros**, carregar **ICD XLS**, mapeando abas/colunas previstas, normalizando tipos/unidades e registrando linhas ignoradas/inconsistentes no relatório de ingestão. | Média | ❌ |
| **RF-405** | **Carregar MDA (JSON)** | Após **RF-402 sem erros**, carregar **MDA JSON** (`classes`, `subclasses`, `attributes{type, enum?, bound_to{TYPE\|OCCURRENCE}}`) e aplicar regras de herança, preservando `sourceClassIRI`. | Alta | ❌ |
| **RF-406** | **Carregar LOIN (XLS MDA)** | Após **RF-402 sem erros**, carregar **LOIN XLS** (MDA LOIN), reconhecendo abas (ex: `Itens/Dicionário/Instruções`), preservando `ItemID`/`Status` para conciliação posterior com a exportação. | Alta | ❌ |
| **RF-407** | **Sincronizar ICD→TTL (One-Shot)** | Executar **importação inicial única** do **ICD** para o **TTL**. **Pré-condições:** RF-402 (esquema ok) e RF-502 (diff gerado). **Regras:** criar **marca one-shot** (`one-shot.lock` com hash do ICD, versão, timestamp, autor); **bloquear reimportações** posteriores a menos que haja `--force` **ou** **mudança de versão** do ICD; em `--apply`, **invocar RF-106** (merge) respeitando **RF-107** (ordem). **Saídas:** `one-shot-report.json` (ação, hash/versão, itens aplicados/ignorados, erros) e **código de saída ≠ 0** em falha. | Alta | ❌ |

### 2.5. Comparar Artefatos (ICD/MDA/LOIN ↔ TTL e TTL ↔ TTL)

> **Base normativa:** [ISO/IEC/IEEE 29148:2018 — Requirements engineering](https://www.iso.org/standard/72026.html)  
> Mantemos **Comparar** separado de **Entrada Externa (2.4)** e **Exportar (2.3)** por separação de responsabilidades. Cada comparação é **singular** e **verificável** (C5/C7): recebe artefatos **já validados por esquema** (pré-condição: RF-402) e produz relatórios objetivos de diferenças para apoiar decisão/merge.

| ID | Nome | Descrição | Prioridade | Check |
|:---:|:---:|---|:---:|:---:|
| **RF-501** | **Comparar Artefatos (Framework)** | Prover **pipeline de comparação** entre uma fonte externa válida (pré-condição: **RF-402**) e o **TTL**; **deve** gerar `diff-report.json` (máquina) e `merge-suggestions.md` (humano), com correspondência determinística por **IRI** e regras por tipo de artefato. | Alta | ❌ |
| **RF-502** | **Comparar ICD ↔ TTL** | Comparar **ICD** (JSON/XLS) ao TTL; **deve** identificar **adições/remoções/mudanças** de **classes**, `propertysets` e `IfcProperty{valueType, unit, validValues}`; **deve** sinalizar impactos de **herança** e inconsistências de domínio/alcance. | Alta | ❌ |
| **RF-503** | **Comparar MDA ↔ TTL** | Comparar **MDA JSON** ao TTL; **deve** verificar presença e divergências de `classes/subclasses` e `attributes{type, enum?, bound_to{TYPE\|OCCURRENCE}}`; **deve** apontar origem (`sourceClassIRI`) e sugerir ajustes de multiplicidade/obrigatoriedade. | Média | ❌ |
| **RF-504** | **Comparar LOIN (XLS MDA) ↔ TTL** | Comparar **LOIN XLS** ao TTL; **deve** verificar cobertura de `ItemID` (IRI) no TTL, **status** e itens órfãos; **deve** produzir lista de lacunas (classes/props não encontradas) para priorização de modelagem. | Média | ❌ |
| **RF-505** | **Comparar TTL ↔ TTL (versões)** | Comparar duas versões do **TTL**; **deve** reportar diffs em `classes/propriedades/subClassOf/domínio/alcance/labels/definitions`; **deve** gerar `CHANGELOG.md` com classificação de impacto (**patch/minor/major**) conforme o tipo de alteração. | Média | ❌ |

