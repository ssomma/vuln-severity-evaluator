# Operations

Qué mirar cuando este servicio se comporta raro. Lo particular acá es que **la falla más peligrosa es silenciosa**:
el servicio puede seguir respondiendo 201 y producir severidades incorrectas.

## Monitores

| Señal | Métrica | Qué significa | Por qué importa |
|---|---|---|---|
| **Tasa de rechazo de respuestas del modelo** | `event.model.answer.rejected` | El modelo respondió fuera de su vocabulario | El síntoma **más temprano** de drift de modelo o de un prompt desactualizado. Un salto acá precede a resultados malos |
| Tasa de 502 | `resource.http.incoming.request` con `status:502`, y `event.evaluation.failed` para la causa | El proveedor falla, agota cuota o excede timeout | El servicio no produce severidades; los consumidores quedan sin priorización |
| Tasa de 400 | `resource.http.incoming.request` con `status:400`, y `event.context.value.rejected` para la causa | Los callers mandan contexto o vectores inválidos | Si sube de golpe, suele ser un integrador nuevo mal configurado |
| Reutilizaciones por huella | `work.input.evaluation.lookup` sobre `work.input.evaluation.request` | Cuántas requests se respondieron sin llamar al modelo | Baja inesperada = más costo: revisar si cambió el modelo, prompt, catálogo o política |
| Llamadas al modelo | `resource.model.call`, separadas por `operation` y `outcome` | Cuántas veces se preguntó al proveedor y con qué resultado | Distingue un proveedor que falla de un servicio que falla |
| Proporción de `MODEL_DERIVED` | `work.process.baseline.derived`, o la dimensión `baseline_source` | Callers que no mandan el vector base | Cuanto más alta, más severidades apoyadas en una inferencia en vez de un dato |
| Proporción de `confidence: LOW` | `work.output.evaluation.recorded` con `confidence:LOW` | Evaluaciones poco respaldadas | Si crece, el contexto declarado está aportando poca evidencia |
| Distribución de ratings | `work.process.severity.assessed`, y `event.severity.rating.moved` para los cruces de banda | Cuánto mueve el contexto declarado a la severidad base | Con una mezcla de entrada estable, un corrimiento es lo más cerca que se llega a detectar drift del lado contextual |

Todas se emiten como líneas de log en el logger **`application.metric`**, con el formato
`<tag> nombre:valor nombre:valor`. El inventario completo de tags y dimensiones está en `MetricUtils`, y qué emite
cada una en `ApplicationMetricCollector`.

El prefijo del tag dice qué tipo de pregunta responde: `work.` es el flujo que el servicio ejecuta, `event.` es una
anomalía de negocio, `resource.` es una interacción con algo externo. La distinción que más importa es la última:
un proveedor que responde 500 es `resource`, un proveedor que responde 200 con un cuerpo inutilizable es `event`.

## Lo que ningún monitor detecta

**Sesgo sistemático del modelo.** Puede elegir consistentemente valores altos o bajos y todo el sistema funcionaría
correctamente produciendo severidades sesgadas. Solo se detecta comparando contra evaluadores humanos, y ese dato no
existe todavía. Ver [Uso de IA y sus límites](/architecture/ai-usage-and-limits).

## Antes de tocar nada

Una evaluación es un registro inmutable con su procedencia. Ante un resultado sospechoso, recuperarlo por
`GET /vulnerability-evaluations/{id}` y mirar `provenance`: qué modelo y versiones de prompt, catálogo y política se
usaron, si el baseline lo mandó el caller o lo derivó el modelo, y qué contexto se declaró. Casi siempre la
explicación está ahí.
