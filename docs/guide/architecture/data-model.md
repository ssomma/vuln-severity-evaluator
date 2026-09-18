# Modelo de datos

Tres grupos de tablas con roles distintos: la evaluación producida, la especificación de scoring y el catálogo del
contexto. Los dos últimos son **dato de referencia** — lo que antes eran enums en código
([ADR-0005](/adr/0005-scoring-specification-as-data)).

---

## La evaluación

`vulnerability_evaluation` es el agregado: qué se preguntó, qué propuso el modelo, qué calculó el esquema y bajo qué
condiciones.

| Columna | Rol |
|---|---|
| `id` | UUID, nunca secuencial |
| `fingerprint` | **único, SHA-256 hexadecimal de 64 caracteres.** Identidad de esquema, modelo, versión de prompt, fuente del baseline, vulnerabilidad y contexto. Cada campo se codifica con longitud para evitar ambigüedades; las listas se ordenan. Evita que una colisión de `String.hashCode()` reutilice una evaluación ajena ([ADR-0006](/adr/0006-determinism-by-persistence)) |
| `vulnerability_identifier`, `vulnerability_description` | El input, persistido para poder auditar la evaluación después |
| `name`, `exposure`, `data_classification`, `business_criticality` | Snapshot del contexto declarado |
| `scheme_id` | Con qué esquema se computó. Hace que evaluaciones de esquemas distintos convivan y que las históricas sigan siendo interpretables |
| `baseline_*`, `contextual_*` | Score, rating y vector de cada uno. Un mismo embeddable usado dos veces |
| `summary` | La justificación en prosa |
| `model`, `prompt_version`, `context_source`, `baseline_vector_source`, `confidence`, `review_required`, `evaluated_at` | Procedencia: bajo qué condiciones leer el resultado |

Colecciones: `evaluation_justification` (una fila por métrica, con su valor y su rationale), `evaluation_runtime` y
`evaluation_control`.

El snapshot del contexto se guarda entero a propósito. El caller es la fuente de verdad de ese dato, así que puede
influir en el score de su propia aplicación — pero no de forma invisible: queda registrado y es auditable después.

La huella tiene tamaño fijo incluso para las entradas máximas admitidas. El índice único arbitra solicitudes
concurrentes: si otra solicitud inserta primero la misma huella, el servicio lee y devuelve esa evaluación.
Esto preserva la respuesta idempotente, aunque ambas solicitudes podrían haber llamado al modelo antes de competir.
Una base persistente con huellas del formato anterior requiere migración de esas filas antes de adoptar la nueva
clave; los perfiles `local`, `production-openai` y `production-grok` de este desafío recrean su base y no conservan filas entre arranques.

---

## La especificación de scoring

| Tabla | Contenido |
|---|---|
| `scheme_metric` | Una fila por métrica de un esquema: código, etiqueta, la guía que lee el modelo y el orden en que se presenta |
| `scheme_metric_value` | Los valores admitidos de cada métrica y su peso. `weight_when_scope_changed` existe por la única métrica CVSS cuyo peso depende de Scope |

**Una fila por métrica, no una por pasada de scoring.** Una métrica se puntúa dos veces —con el valor del vector
baseline y con el que el modelo derivó del contexto— pero eso es cómo computa el esquema, no qué es el esquema: los
pesos, los valores admitidos y la guía son la misma tabla leída dos veces. `X` (Not Defined) tampoco se siembra: es
el protocolo de abstención, aplica a toda métrica por construcción, y la caída al valor baseline es explícita en el
calculador.

El prefijo `M` de las métricas Environmental de CVSS (`MAV`, `MAC`, …) es sintaxis del vector publicado, no una
métrica aparte: `Cvss31` lo agrega solo al serializar `contextual_vector`, para que cualquier calculadora CVSS pueda
reproducir los dos scores desde ese campo.

Estas filas son a la vez la aritmética y el prompt: el mismo registro define qué valores se admiten y qué significa
cada métrica para el modelo, así que no hay dos lugares que puedan desincronizarse.

---

## El catálogo de contexto

| Tabla | Contenido |
|---|---|
| `context_attribute` | Cada valor admitido del contexto, con su `kind` y el significado que lee el modelo |
| `context_suggestion` | Qué valores de métrica argumenta cada atributo |

Las sugerencias son lo que permite que el modelo determinista funcione sin tablas de mapeo propias: recolecta las
sugerencias de los atributos declarados y responde `X` donde el contexto no dice nada.

---

## Schema y migraciones

El schema se deriva de las entidades (`ddl-auto: create-drop`) y el catálogo se carga desde `sql/data.sql`, ambos
**en los perfiles `local`, `production-openai` y `production-grok`**: es una base en memoria y efímera. Comparten el mismo catálogo para que
las evaluaciones con el modelo determinista y con un proveedor externo se calculen contra la misma especificación.
Los perfiles de producción requieren su API key correspondiente;
ver el [README](../../../README.md#proveedores-externos-y-modelos).

> Un despliegue real necesita **migraciones versionadas**, no `ddl-auto`, y que el catálogo exista antes de recibir
> tráfico. Quedó fuera de alcance por tratarse de una prueba de concepto, y es deuda consciente, no un olvido.

Dos detalles que costaron una falla y vale dejar anotados: `value` es palabra reservada en H2, así que la columna de
`MetricChoice` se llama `chosen_value` explícitamente; y el seed necesita `defer-datasource-initialization`, porque
por defecto corre antes de que Hibernate cree las tablas.
