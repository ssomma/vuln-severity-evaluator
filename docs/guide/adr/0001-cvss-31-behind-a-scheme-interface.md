# ADR-0001: Adoptar CVSS v3.1 Base+Environmental detrás de una interfaz de esquema

| Campo    | Valor |
|----------|-------|
| Status   | accepted |
| Fecha    | 2026-09-15 |
| Contexto | Alcance mínimo del desafío: "evaluación de severidad inspirada en CVSS, versión a elección, justificada" |

## Contexto

El desafío pide una severidad inspirada en CVSS y deja la versión a elección, pedida con justificación. Hay dos
decisiones enredadas en una: **qué esquema usar** y **qué tan atado queda el sistema a esa elección**.

Sobre el esquema, las opciones reales eran v3.1, v4.0 o una escala propia. El punto que decide es de dónde sale el
dato duro: el valor del servicio está en *re-puntuar* según el entorno, así que el puntaje base conviene anclarlo a
algo que ya exista y no derivarlo del modelo. Las métricas Environmental de v3.1 existen exactamente para esto, y su
cobertura en feeds públicos es prácticamente universal. v4.0 es más expresivo (Threat, Supplemental, Safety) pero su
cobertura de vectores todavía es parcial, así que en muchos CVEs el modelo tendría que derivar el Base completo — más
superficie de alucinación justo en la parte que debería ser el ancla.

Sobre el acoplamiento: cualquier elección va a envejecer. v4.0 va a ganar cobertura, y una organización puede querer
su propia escala o correr dos en paralelo para comparar.

## Decisión

El esquema activo es **CVSS v3.1 Base + Environmental**, y la aplicación **no depende de él**: depende de la interfaz
`SeverityScheme`, que expone `id()` y `assess(baselineVector, proposal, metrics)`.

Dos detalles hacen que la abstracción sirva de verdad. El primero: **el prompt del modelo y la validación de su
respuesta se derivan de la misma especificación** que usa el cálculo, así que agregar un esquema no deja nada para
sincronizar a mano. El segundo: la especificación llega **como argumento**, no la busca el esquema, así que una
implementación es una función pura de sus entradas y no puede alcanzar un origen de datos
(ver [ADR-0005](/adr/0005-scoring-specification-as-data)).

## Consecuencias

- **Bueno:** el puntaje base se ancla a un dato que el caller ya tiene; el modelo solo razona sobre el entorno, que
  es donde su aporte es real.
- **Bueno:** migrar a v4.0 es implementar la interfaz y cargar sus filas de catálogo. No se toca el service, el
  controller, la entidad ni el datasource del modelo.
- **Bueno:** las columnas persistidas son agnósticas y guardan el `schemeId`, así que evaluaciones de esquemas
  distintos conviven y las históricas siguen siendo interpretables.
- **Malo:** una indirección más para leer el cálculo. El costo se paga una vez.
- **Riesgo:** la interfaz está validada contra **una sola** implementación. Hasta que exista la segunda, no hay
  prueba empírica de que las costuras estén en el lugar correcto.
- **Riesgo:** `SeverityRating` fija los rangos cualitativos de CVSS como vocabulario compartido. Un esquema con otra
  escala tendría que mapear a ellos.

## Alternativas descartadas

| Alternativa | Razón del rechazo |
|-------------|-------------------|
| Acoplar la aplicación a CVSS v3.1 | Menos código hoy, pero convierte un cambio de versión en una refactorización transversal |
| Ir directo a CVSS v4.0 | Cobertura de vectores parcial en feeds: obligaría al modelo a derivar el Base en muchos casos, debilitando el ancla |
| Escala de severidad propia | Máxima libertad, pero pierde interoperabilidad con todo el ecosistema y es más difícil de auditar: nadie puede recomputar el número |
