# ADR-0003: El contexto de la aplicación lo declara el caller

| Campo    | Valor |
|----------|-------|
| Status   | accepted |
| Fecha    | 2026-09-15 |
| Contexto | El alcance mínimo del desafío recibe "el nombre de la aplicación potencialmente afectada"; no hay inventario disponible |

## Contexto

La entrada especificada es el nombre de la aplicación. Un nombre por sí solo no permite contextualizar nada: para
mover una severidad hace falta saber desde dónde se alcanza la aplicación, qué datos maneja y qué tan crítica es.

Ese dato normalmente vive en un inventario autoritativo. Acá no hay uno accesible, así que las opciones eran
resolverlo contra un inventario real (no reproducible por quien evalúe el desafío, y dependiente de credenciales
internas), mantener un catálogo local versionado, o pedirle el contexto al caller.

Se eligió la tercera. Hay que ser explícito sobre lo que eso implica: **convierte al cliente en fuente de verdad de
un insumo que afecta una decisión de seguridad.** Un equipo puede declarar su aplicación como aislada y de baja
criticidad y obtener una severidad más baja para su propia vulnerabilidad.

## Decisión

El request carga el contexto tecnológico declarado, y el riesgo se acepta con mitigaciones explícitas:

- **Vocabulario cerrado.** El perfil de riesgo es un conjunto tipado y cerrado (`Exposure`, `DataClassification`,
  `BusinessCriticality`); los runtimes y controles compensatorios son códigos validados contra el catálogo en base
  (ver [ADR-0005](/adr/0005-scoring-specification-as-data)). En ninguno de los dos casos hay texto libre que
  interpretar, y un código desconocido se rechaza con 400 en vez de ignorarse.
- **Se rechaza lo desconocido.** `FAIL_ON_UNKNOWN_PROPERTIES` está activo: un campo mal escrito falla con 400 en vez
  de ignorarse silenciosamente. Ignorarlo dejaría al caller creyendo que declaró un contexto que el servicio nunca
  leyó, y en este servicio el contexto mueve el score.
- **Snapshot persistido.** El contexto declarado se guarda junto a la evaluación: la influencia es posible, pero
  queda registrada y es auditable después.
- **Etiquetado en la respuesta.** `provenance.context_source = CALLER_DECLARED`, y el resultado se expone como
  `advisory`. Un consumidor puede distinguir un contexto auto-declarado de uno resuelto contra un inventario.
- **El nombre de la aplicación no se envía al modelo.** No hace falta para la evaluación, y así no salen nombres de
  inventario interno hacia un proveedor externo.

## Consecuencias

- **Bueno:** el servicio es reproducible por cualquiera, sin credenciales ni acceso a inventario.
- **Bueno:** el catálogo hace que el contrato, la validación y lo que lee el modelo sean una sola definición: el
  significado que el modelo recibe para cada valor sale de la misma fila que lo admite.
- **Bueno:** el contexto es un `@Embeddable` del agregado, así que migrar a un inventario cambia quién lo llena, no
  la forma del dominio.
- **Malo:** la severidad es tan buena como la honestidad del declarante. El servicio no puede detectar un contexto
  optimista.
- **Malo:** dos equipos pueden clasificar la misma aplicación distinto y obtener severidades distintas.
- **Riesgo:** si alguien automatiza remediación sobre esta salida sin leer `context_source`, estaría confiando en un
  dato auto-declarado. Por eso `advisory` y `review_required` viajan en cada respuesta.

## Alternativas descartadas

| Alternativa | Razón del rechazo |
|-------------|-------------------|
| Resolver el contexto contra el inventario real | Más fiel, pero acopla el desafío a credenciales y red internas y lo vuelve no reproducible por el evaluador |
| Catálogo local versionado de aplicaciones | Evita que el caller sea fuente de verdad, pero es un inventario inventado: mueve el problema de veracidad del request al fixture |
| Recibir solo el nombre y que el modelo infiera el contexto | La peor opción: el modelo inventaría la exposición y la criticidad, y el insumo más determinante del score sería alucinación |
