# ADR-0007: La observabilidad puede leer los tipos que mide

| Campo    | Valor |
|----------|-------|
| Status   | accepted |
| Fecha    | 2026-09-17 |
| Contexto | `src/main/java/org/challenge/vulnseverityevaluator/infrastructure/metric/`, `ArchitectureTest.givenLayeredArchitectureWhenCheckThenDoNotThrowException` |

## Contexto

La convención de métricas adoptada pasa **el objeto medido** como parámetro: `collectWorkOutputEvaluationRecorded(VulnerabilityEvaluation)`,
no `collect...(String scheme, String rating, boolean review)`. Eso es lo que mantiene el mapeo de objeto a
dimensiones en un solo lugar, deja los call sites sin armado de métricas y permite agregar una dimensión sin cambiar
ninguna firma.

Pero el colector vive en `..infrastructure..`, y la regla de capas de `ArchitectureTest` corre con
`consideringAllDependencies()` con `MODEL_ACCESS` **sin** `INFRASTRUCTURE` — lo mismo para `LLM_ACCESS`. La
implementación anterior (`EvaluationMetrics`) lo documentaba como una decisión: *"Deliberately free of domain
types"*. Con contadores en memoria sin dimensiones eso no costaba nada, porque no había nada que leer del dominio.
Con métricas dimensionadas, cuesta todo: sin acceso a los tipos, cada call site tendría que desarmar el objeto y
pasar seis primitivos.

Las dos restricciones son reales y están en conflicto directo. Había que elegir cuál ceder y con qué alcance.

## Decisión

Se agrega **una única excepción dirigida** a la regla de capas: el subpaquete `..infrastructure.metric..` puede
depender de `..domain.model..` y de `..datasource.llm..`.

```java
.ignoreDependency(resideInAPackage("..infrastructure.metric.."),
                  resideInAPackage("..domain.model..").or(resideInAPackage("..datasource.llm..")))
```

`MODEL_ACCESS` **no** se amplía. La excepción es por dependencia y unidireccional: la observabilidad lee los tipos
que mide, y nada gana acceso a la observabilidad más allá de las capas que ya podían llegar a `INFRASTRUCTURE`.

## Consecuencias

- **Bueno:** el resto de `..infrastructure..` sigue sin poder leer el dominio. `EvaluationExceptionHandler` maneja
  tipos de excepción estándar por esa razón, y esa propiedad se conserva intacta.
- **Bueno:** la excepción es legible como una oración —*la observabilidad puede leer lo que mide*— y el test falla si
  alguien la usa para otra cosa, porque está anclada a un subpaquete y no a una capa entera.
- **Bueno:** el dominio sigue sin saber que existen métricas. Los tipos viajan hacia la observabilidad, nunca al
  revés, así que no hay acoplamiento que pueda invertirse por descuido.
- **Malo:** una regla con excepciones es una regla más débil. El diagrama de capas ya no se lee del todo en
  `layeredArchitecture()`: hay que leer también el `ignoreDependency`.
- **Malo:** cambiar un accessor del dominio ahora puede romper la compilación del colector. Es el costo de no
  duplicar el mapeo, y es visible en compilación y no en runtime.
- **Riesgo:** el subpaquete es una tentación. Si mañana alguien mete ahí una clase que no sea de métricas, hereda un
  permiso que no le corresponde y nada lo señala.

## Alternativas descartadas

| Alternativa | Razón del rechazo |
|-------------|-------------------|
| Agregar `INFRASTRUCTURE` a `MODEL_ACCESS` | Una línea, pero habilita a toda la infraestructura —presente y futura— a leer el dominio. Se pagaría el permiso completo para usarlo en un subpaquete |
| Una capa `..observability..` propia con acceso a `MODEL` | Resuelve lo mismo con más ceremonia: capa nueva en el ArchUnit, constantes nuevas, y dos lugares distintos donde vive lo *cross-cutting* |
| Colector con parámetros primitivos | No rompe ninguna regla, pero traslada el armado de dimensiones a cada call site: seis primitivos por llamada, y agregar una dimensión toca todas las firmas. Es exactamente lo que la convención existe para evitar |
| Duplicar en infraestructura los datos del dominio | Un DTO de telemetría por métrica. Cuesta un mapper por métrica y el mapper se desincroniza en silencio, que es peor que la dependencia |
