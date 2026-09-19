# ADR-0006: Determinismo por persistencia, no por caché

| Campo    | Valor |
|----------|-------|
| Status   | accepted |
| Fecha    | 2026-09-15 |
| Contexto | Pregunta de revisión: si el LLM no es determinista y la aplicación sí debe serlo, ¿cómo se logra? |

## Contexto

El servicio se apoya en un modelo de lenguaje, que **no es una fuente determinista**. La aplicación, en cambio, sí
debe serlo: la misma pregunta tiene que dar la misma respuesta, o el resultado no es auditable ni comparable.

`temperature: 0.0` reduce la variación pero no la elimina: ningún proveedor garantiza salidas idénticas, y el modelo
detrás de un alias cambia con el tiempo. Así que las *elecciones de métricas* pueden diferir entre llamadas, y con
ellas el score contextual.

La primera implementación resolvía esto con un caché en memoria (Caffeine, TTL de 60 minutos) presentado como control
de costo. Eso es determinismo de mejor esfuerzo: se vence la entrada, se reinicia el proceso, y la misma consulta
puede devolver otro número sin que nada lo explique.

## Decisión

La evaluación se busca **por huella en la tabla de evaluaciones, antes de preguntarle al modelo**.

La huella identifica los insumos: esquema, modelo, versión de prompt, revisión del catálogo, versión de la política,
identificador y vector de la vulnerabilidad, descripción y contexto declarado. Si existe una evaluación con esa
huella, se devuelve esa; si no, se evalúa y se registra. La revisión se obtiene con una consulta escalar sobre
`scheme_metric`, por lo que una coincidencia evita cargar el catálogo completo.

La decisión arquitectónica exige que esa tabla sea durable. La implementación actual es una prueba de concepto con
H2 en memoria: demuestra el lookup por huella dentro del proceso, pero no conserva la evaluación entre reinicios.
Para cumplir la garantía completa se requiere una base persistente y migraciones versionadas.

El caché en memoria se eliminó, junto con su configuración y el decorator que lo envolvía.

## Consecuencias

- **Bueno en un despliegue con base durable:** la propiedad sobrevive reinicios y no expira, porque es una fila y no
  una entrada con tiempo de vida.
- **Límite actual:** H2 en memoria descarta esas filas al reiniciar. Es una limitación consciente de la prueba de
  concepto, no una implementación completa de la garantía de este ADR.
- **Bueno:** el modelo, el prompt, el catálogo y la política están versionados en la huella. Cambiar cualquiera de
  ellos produce una evaluación nueva en vez de reusar razonamiento o reglas anteriores. Un caché con clave por CVE
  habría servido reasoning obsoleto tras uno de esos cambios.
- **Bueno:** desaparecieron tres piezas — la dependencia de Caffeine, su clase de configuración y el decorator — y la
  idempotencia pasó a ser parte del caso de uso, que es donde se entiende por qué existe.
- **Bueno:** el ahorro de costo se mantiene, pero deja de ser la justificación principal: es una consecuencia de una
  decisión sobre corrección.
- **Malo:** la tabla crece con cada combinación distinta de entrada. Para el volumen de este servicio no es un
  problema, pero a escala real requiere una política de retención.
- **Malo:** una evaluación queda congelada. Si el contexto real de la aplicación cambia sin que cambie lo declarado,
  se sigue devolviendo la evaluación vieja. Es correcto —los insumos son los mismos— pero hay que saberlo.
- **Riesgo:** esto da determinismo **por entrada repetida**, no reproducibilidad del razonamiento. Dos entradas
  distintas pero equivalentes pueden dar resultados distintos, y no hay forma de detectarlo automáticamente.

## Alternativas descartadas

| Alternativa | Razón del rechazo |
|-------------|-------------------|
| Caché en memoria con TTL | Determinismo de mejor esfuerzo: se pierde al expirar y al reiniciar, que es justo cuando alguien pregunta por qué el número cambió |
| Confiar en `temperature: 0.0` | Reduce la variación, no la garantiza. Apoyar una propiedad de corrección en una configuración del proveedor es apoyarla en algo que no controlamos |
| Recomputar siempre y aceptar la variación | Honesto pero inutilizable: dos consultas seguidas podrían justificar decisiones distintas sobre la misma vulnerabilidad |
