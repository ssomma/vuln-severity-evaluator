# Operations

Qué mirar cuando este servicio se comporta raro. Lo particular acá es que **la falla más peligrosa es silenciosa**:
el servicio puede seguir respondiendo 201 y producir severidades incorrectas.

## Monitores

| Señal | Qué significa | Por qué importa |
|---|---|---|
| **Tasa de rechazo de respuestas del modelo** | El modelo respondió fuera de su vocabulario | El síntoma **más temprano** de drift de modelo o de un prompt desactualizado. Un salto acá precede a resultados malos |
| Tasa de 502 | El proveedor falla, agota cuota o excede timeout | El servicio no produce severidades; los consumidores quedan sin priorización |
| Tasa de 400 | Los callers mandan contexto o vectores inválidos | Si sube de golpe, suele ser un integrador nuevo mal configurado |
| Reutilizaciones por huella | Cuántas requests se respondieron sin llamar al modelo | Baja inesperada = más costo y menos determinismo: revisar si cambió el prompt o el modelo |
| Latencia del endpoint | Dominada por la llamada al proveedor | — |
| Proporción de `MODEL_DERIVED` | Callers que no mandan el vector base | Cuanto más alta, más severidades apoyadas en una inferencia en vez de un dato |
| Proporción de `confidence: LOW` | Evaluaciones poco respaldadas | Si crece, el contexto declarado está aportando poca evidencia |

Los tres primeros contadores los expone `EvaluationMetrics`.

## Lo que ningún monitor detecta

**Sesgo sistemático del modelo.** Puede elegir consistentemente valores altos o bajos y todo el sistema funcionaría
correctamente produciendo severidades sesgadas. Solo se detecta comparando contra evaluadores humanos, y ese dato no
existe todavía. Ver [Uso de IA y sus límites](/architecture/ai-usage-and-limits).

## Antes de tocar nada

Una evaluación es un registro inmutable con su procedencia. Ante un resultado sospechoso, recuperarlo por
`GET /vulnerability-evaluations/{id}` y mirar `provenance`: qué modelo, qué versión de prompt, si el baseline lo
mandó el caller o lo derivó el modelo, y qué contexto se declaró. Casi siempre la explicación está ahí.
