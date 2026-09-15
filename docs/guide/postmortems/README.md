# Postmortems

Este directorio contiene los postmortems blameless de
**vuln-severity-evaluator**. Los postmortems documentan incidentes, sus
causas raíz y las acciones concretas para evitar que vuelvan a ocurrir.

## Índice

| ID | Título | Fecha | Severidad | Status |
|----|--------|-------|-----------|--------|
| *(sin postmortems aún)* | — | — | — | — |

---

## Plantilla Postmortem

```markdown
# PM-NNNN: <título descriptivo del incidente>

| Campo | Valor |
|-------|-------|
| Fecha del incidente | YYYY-MM-DD HH:MM (UTC-3) |
| Duración | <tiempo desde detección hasta resolución> |
| Severidad | SEV1 \| SEV2 \| SEV3 |
| Servicios afectados | vuln-severity-evaluator, [otros] |
| Autor | <ldap> |

## Resumen ejecutivo
<2-3 oraciones: qué pasó, impacto de negocio, cómo se resolvió>

## Timeline
| Hora (UTC-3) | Evento |
|-------------|--------|
| HH:MM | Alerta disparada |
| HH:MM | Root cause identificado |
| HH:MM | Servicio recuperado |

## Impacto
- **Usuarios afectados:** ...
- **Tiempo total de degradación:** ...

## Root cause
<descripción técnica precisa de la causa raíz>

## Action Items (AIs)
| AI | Dueño | Plazo | Status |
|----|-------|-------|--------|
| <acción concreta> | @ldap | YYYY-MM-DD | pendiente |

## Lecciones aprendidas
<reflexión sobre detección, respuesta o prevención>
```

## Principios del postmortem blameless

1. **El sistema falló, no la persona** — el objetivo es mejorar el sistema.
2. **Timeline basado en hechos** — solo eventos observados.
3. **Action Items accionables** — cada AI tiene un dueño y una fecha.
4. **Publicar rápido, mejorar después** — draft en 24h, completo en 72h.
5. **Los AIs se rastrean en Jira** — no quedan solo en el documento.
