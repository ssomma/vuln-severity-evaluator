# Architecture Decision Records (ADRs)

Este directorio contiene las decisiones arquitectónicas del proyecto
**vuln-severity-evaluator**. Las ADRs son **inmutables una vez aceptadas** —
solo se puede crear una nueva ADR que la reemplace o anule.

## Índice

| ID | Título | Status | Fecha |
|----|--------|--------|-------|
| [0001](/adr/0001-cvss-31-behind-a-scheme-interface) | Adoptar CVSS v3.1 Base+Environmental detrás de una interfaz de esquema | accepted | 2026-09-15 |
| [0002](/adr/0002-language-model-as-datasource) | El modelo de lenguaje es un datasource, no un motor de decisión | accepted | 2026-09-15 |
| [0003](/adr/0003-caller-declared-application-context) | El contexto de la aplicación lo declara el caller | accepted | 2026-09-15 |
| [0004](/adr/0004-responsibility-per-domain) | Granularidad del modelo por dominio, no por clase | accepted | 2026-09-15 |
| [0005](/adr/0005-scoring-specification-as-data) | La especificación de scoring y los catálogos son dato en base | accepted | 2026-09-15 |
| [0006](/adr/0006-determinism-by-persistence) | Determinismo por persistencia, no por caché | accepted | 2026-09-15 |
| [0007](/adr/0007-observability-reads-the-types-it-measures) | La observabilidad puede leer los tipos que mide | accepted | 2026-09-17 |

---

## Plantilla ADR

```markdown
# ADR-NNNN: <decisión en imperativo>

| Campo     | Valor |
|-----------|-------|
| Status    | draft \| accepted \| rejected \| superseded |
| Fecha     | YYYY-MM-DD |
| Contexto  | <link al PR, código o issue> |

## Contexto
<por qué hubo que decidir, qué restricciones existían>

## Decisión
<qué se decidió, en una oración clara>

## Consecuencias
- **Bueno:** ...
- **Malo:** ...
- **Riesgo:** ...

## Alternativas descartadas
| Alternativa | Razón del rechazo |
|-------------|-------------------|
| ... | ... |
```

## Convenciones

- El nombre de archivo sigue el patrón `NNNN-<slug-de-la-decision>.md`
- Los números son secuenciales y nunca se reutilizan
- Status válidos: `draft` → `accepted` o `rejected`; `accepted` puede pasar a `superseded` si hay una nueva ADR que la reemplaza
- Una ADR aceptada **no se edita** — si la decisión cambia, se crea una nueva ADR con `supersedes: NNNN`
- El campo `Contexto` siempre apunta al código, PR o issue que originó la decisión

## ¿Qué va en una ADR?

Una ADR documenta decisiones que impactan la arquitectura de forma significativa,
son difíciles de revertir, tienen alternativas no obvias, o generaron debate.
**No van en ADRs:** decisiones de implementación día a día, naming, o cambios menores.
