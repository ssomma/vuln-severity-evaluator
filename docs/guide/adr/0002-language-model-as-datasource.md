# ADR-0002: El modelo de lenguaje es un datasource, no un motor de decisión

| Campo    | Valor |
|----------|-------|
| Status   | accepted |
| Fecha    | 2026-09-15 |
| Contexto | Requisito del desafío de "abordar críticamente los límites y riesgos asociados al uso de IA en este contexto" |

## Contexto

La tentación evidente al construir esto es pedirle al modelo la respuesta: *"dada esta vulnerabilidad y esta
aplicación, devolvé el score y la justificación"*. Funciona en una demo y es indefendible en producción. Un LLM no
sabe aritmética de forma confiable, no es determinista, y su salida es tan influenciable como el texto que recibe —
y acá el texto de entrada lo escriben terceros.

Al mismo tiempo, sí hay algo en lo que el modelo aporta genuinamente: leer una descripción en prosa y un contexto
declarado, y juzgar qué tan reachable o qué tan impactante es esa vulnerabilidad *en ese entorno*. Ese juicio es
difícil de expresar en reglas y es exactamente lo que hoy hace una persona a mano.

La decisión es dónde poner el límite entre las dos cosas.

## Decisión

El modelo vive en `datasource/llm` como un **origen de datos** —se le pide una opinión y responde con datos— y no
como un servicio de decisión. Concretamente:

- **El modelo nunca devuelve un número.** Devuelve, para cada métrica contextual, un valor tomado del vocabulario
  cerrado del esquema activo, más su rationale.
- **La aritmética la hace el dominio**, de forma determinista y verificada contra vectores oficiales de la
  especificación.
- **El baseline provisto por el caller no es modificable por el modelo.**
- **La propuesta se valida al construirse.** `ModelSeverityProposal` no puede existir en estado inválido: métrica
  desconocida, valor no admitido o respuesta incompleta son rechazo explícito, nunca coerción a un default.
- **`X` (Not Defined) es una respuesta válida y preferida** cuando el contexto no da evidencia. Abstenerse está
  mejor visto que adivinar, y muchas abstenciones degradan la confianza reportada.
- **La salida es `advisory`.** Nunca dispara remediación automática; los casos más propensos a estar mal son los que
  se marcan para revisión humana.
- `datasource/llm` es una **capa propia** en el guardrail de arquitectura, con sus propias reglas de acceso, para que
  la distinción entre "dato de mi infraestructura" y "opinión de un modelo" sea estructural y no una convención.

## Consecuencias

- **Bueno:** un score alucinado no puede llegar a un consumidor. El peor caso del modelo es una *elección de métrica*
  mal justificada, acotada por el vocabulario y visible en la justificación por métrica.
- **Bueno:** el núcleo se prueba sin red. La aritmética se verifica contra la especificación; el flujo, con un modelo
  determinista.
- **Bueno:** cambiar de proveedor —o del proveedor público a un gateway interno— toca una clase.
- **Bueno:** la procedencia persistida (modelo + versión de prompt) hace detectable el drift; sin ella, dos
  resultados distintos para la misma pregunta serían inexplicables. Es también lo que permite que el servicio sea
  determinista pese a depender de algo que no lo es (ver [ADR-0006](/adr/0006-determinism-by-persistence)).
- **Malo:** el modelo no puede expresar un juicio que el vocabulario no contemple. Es el precio de acotarlo, y se
  compensa parcialmente con el rationale por métrica.
- **Malo:** dos llamadas al modelo cuando el caller no envía el vector, porque derivarlo es un pedido aparte.
- **Riesgo:** el vocabulario cerrado evita la invención, no el **sesgo**. El modelo puede elegir consistentemente
  valores demasiado altos o bajos. Eso se detecta comparando contra evaluadores humanos, y ese dato todavía no existe.

## Alternativas descartadas

| Alternativa | Razón del rechazo |
|-------------|-------------------|
| Pedirle el score directamente al modelo | El número sería no verificable, no determinista e influenciable por la descripción, que es entrada no confiable |
| Aceptar el score del modelo y validarlo contra un rango | Un rango no detecta un número plausible pero mal derivado. Valida la forma, no el razonamiento |
| Solo reglas determinísticas, sin modelo | Reproducible, pero incapaz de leer prosa: volvería a pedirle al caller que haga a mano el juicio que el servicio debería asistir |
| Coercionar valores desconocidos al default más cercano | Convertiría una falla visible del modelo en un score plausible y silencioso, que es el peor resultado posible |
