# RFCs — Request for Comments

Este directorio contiene propuestas formales de cambios significativos al
sistema **vuln-severity-evaluator**. Una RFC es una propuesta **antes de la
decisión** — una vez que se acepta, la decisión se formaliza en una [ADR](../adr/).

## Índice

| ID | Título | Status | Autor | Fecha |
|----|--------|--------|-------|-------|
| *(sin RFCs aún)* | — | — | — | — |

---

## Flujo RFC

```
draft → in-review → accepted → (se crea ADR + implementación)
                  ↘ rejected
```

## Plantilla RFC

```markdown
---
rfc: NNNN
title: <título descriptivo>
status: draft
author: <ldap>
date: YYYY-MM-DD
---

# RFC-NNNN: <título>

## Resumen
<1-2 párrafos que expliquen qué se propone y por qué>

## Motivación
<cuál es el problema o la oportunidad que justifica este cambio>

## Diseño detallado
<descripción técnica de la solución propuesta>

## Alternativas consideradas
<qué otras opciones se evaluaron y por qué se descartaron>

## Impacto
- **Consumidores de la API:** ...
- **Capas/arquitectura:** ...
- **Performance:** ...
- **Seguridad:** ...

## Resolución
**Status:** draft | accepted | rejected
**Fecha:** YYYY-MM-DD
**Razonamiento:** <por qué se tomó esta decisión>
**ADR relacionada:** `[ADR-NNNN](/adr/NNNN-<slug>)`
```

## Convenciones

- Nombre de archivo: `NNNN-<slug>.md`
- Los números son secuenciales
- Una RFC aceptada no se borra — se actualiza su status y se agrega la resolución
- El debate sucede en el PR de la RFC, no en el archivo mismo
