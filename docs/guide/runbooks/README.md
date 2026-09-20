# Runbooks Operacionales

Este directorio contiene los procedimientos operacionales de
**vuln-severity-evaluator**. Cada runbook describe cómo responder a un evento
o ejecutar una tarea operacional específica.

Actualmente no hay procedimientos operacionales versionados en esta sección. La comprobación puntual del límite con
proveedores externos está documentada dentro de [Arquitectura](/architecture/external-model-validation), no como un
runbook.

---

## Plantilla de Runbook

```markdown
# RB-NNNN: <título del procedimiento>

| Campo | Valor |
|-------|-------|
| Trigger | <cuándo se ejecuta este runbook> |
| Duración estimada | <tiempo esperado para completarlo> |
| Criticidad | 🔴 P0 \| 🟠 P1 \| 🟡 P2 \| 🟢 P3 |
| Última revisión | YYYY-MM-DD |
| Dueño | <equipo o persona responsable> |

## Descripción
<qué hace este runbook y en qué contexto se usa>

## Prerequisitos
- [ ] Acceso a [sistema X]
- [ ] ...

## Pasos
### 1. <primer paso>
```bash
# comando ejemplo
```
**Resultado esperado:** ...

## Rollback
<pasos para deshacer>

## Escalation
Si no funciona después de X minutos: contactar [persona/canal].
```

## Convenciones

- El nombre de archivo sigue el patrón `NNNN-<slug>.md`
- Los números son secuenciales
- Cada runbook debe poder ejecutarse de forma independiente
- Los comandos deben ser copiables literalmente (sin placeholders ambiguos)
- Revisar al menos una vez por quarter, o después de cada incidente que lo usó
