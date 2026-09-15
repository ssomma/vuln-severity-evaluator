# Architecture Decision Records (ADRs)

Este directorio contiene las decisiones arquitectónicas del proyecto
**vuln-severity-evaluator**. Las ADRs son **inmutables una vez aceptadas** —
solo se puede crear una nueva ADR que la reemplace o anule.

## Índice

| ID | Título | Status | Fecha |
|----|--------|--------|-------|
| *(sin ADRs aún)* | — | — | — |

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
