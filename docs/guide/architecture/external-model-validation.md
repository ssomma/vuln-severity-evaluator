# Validación E2E con modelos externos

| Campo | Valor |
|---|---|
| Tipo | Informe de validación E2E contra infraestructura externa |
| Fecha | 2026-09-20 |
| Aplicación | `vuln-severity-evaluator` `1.0-SNAPSHOT` |
| Perfil | `production-grok` |
| Proveedor | xAI mediante API Chat compatible con OpenAI |
| Modelos | Grok 4.3 y Grok 4.6 |
| Variante analítica | `low` en ambos modelos |
| Catálogo | `cvss31-context-v1` |
| Casos | 8 por modelo, 16 evaluaciones externas |
| Resultado | **16/16 PASS** |
| Evidencia del proveedor | Pendiente de correlación con el export de la API key |

## Resumen ejecutivo

Se validó la implementación end to end contra dos modelos reales. Las 16 solicitudes obtuvieron HTTP `201`, las
relaciones entre score baseline y contextual coincidieron con lo esperado, y todas las métricas sustentadas por cada
escenario tuvieron el valor esperado. Las respuestas persistidas por la aplicación fueron recuperadas mediante `GET`
sin diferencias y la repetición del primer caso de cada proceso devolvió el mismo `evaluation_id`.

El resultado demuestra que ambos modelos satisfacen el contrato funcional con la información que la aplicación les
entrega. No demuestra disponibilidad futura del proveedor ni convierte estas llamadas en tests del build.

## Por qué esta validación es externa al test automatizado

Los tests del repositorio usan un colaborador determinista y no necesitan red, credenciales, cuota ni disponibilidad
de terceros. Esta campaña es una prueba de aceptación de la integración real, separada deliberadamente de
`gradlew test`, y debe repetirse cuando cambien el proveedor, el modelo, el adaptador, el prompt, el catálogo o el
contrato estructurado.

La matriz externa queda limitada a `grok-4.3` con esfuerzo `low` y `grok-4.6` con esfuerzo `low`. El nivel de esfuerzo
se configura al iniciar el proceso únicamente para esta evaluación analítica; no forma parte del request público ni es
una opción configurable por cliente.

## Suite de aceptación

Los fixtures están en
[`scripts/model-evaluation-cases.json`](../../../scripts/model-evaluation-cases.json) y el runner portable en
[`scripts/model-evaluation-suite.py`](../../../scripts/model-evaluation-suite.py). El runner usa Python 3.10+ y no
depende de PowerShell.

Cada fixture mantiene las expectativas fuera de `body`, por lo que no se envían a la aplicación ni al modelo:

```json
{
  "case": "05-score-difiere-por-regla-waf-especifica",
  "expected": {
    "score_relation": "DIFFERENT",
    "metrics": { "AC": "H", "CR": "H", "IR": "H", "AR": "H" }
  },
  "body": { "...": "request público sin las expectativas" }
}
```

El oráculo tiene dos niveles:

1. `score_relation` verifica si los scores deben coincidir o diferir.
2. `metrics` verifica únicamente las decisiones que cuentan con evidencia explícita en el escenario.

El runner también valida antes de llamar al proveedor que los nombres y valores esperados pertenezcan al vocabulario
CVSS 3.1. Después de cada `POST`, verifica status, `Location`, provenance, lectura por `GET` y, cuando se solicita,
idempotencia.

## Corrección semántica previa a la campaña

La revisión de la primera versión de la suite detectó que las sugerencias usadas por el stub no siempre estaban
respaldadas por el texto enviado al modelo:

- `INTERNAL` decía solamente que no había datos personales o financieros, evidencia insuficiente para distinguir
  requisito de confidencialidad bajo o medio;
- la criticidad de negocio describía únicamente el impacto de un outage, pero el oráculo también esperaba un valor
  para el requisito de integridad;
- `FINANCIAL` no separaba la sensibilidad de divulgación de la importancia de una modificación no autorizada;
- un WAF genérico no demostraba que el exploit concreto fuese más difícil.

La corrección no agregó códigos de métricas ni valores esperados al prompt. El catálogo ahora describe consecuencias
observables en lenguaje de negocio:

- la clasificación de datos explica la consecuencia de una divulgación no autorizada;
- la criticidad explica por separado el efecto de modificación no autorizada y de indisponibilidad;
- `INTERNAL_NETWORK` especifica un segmento local confiable sin rutas desde otras redes;
- `WAF` permanece genérico y sin sugerencia de complejidad;
- `EXPLOIT_SPECIFIC_WAF_RULE` declara una regla probada que bloquea el exploit directo y exige construir una evasión.

La implementación conserva `catalog_version` en `cvss31-context-v1`; la revisión de redacción no introduce una nueva
versión del catálogo. La etiqueta se normalizó después de la campaña y no se repitieron las llamadas externas porque
el cambio es exclusivamente el string de provenance: prompt, contexto, expectativas, cálculo y respuestas evaluadas
permanecen iguales.

## Casos y resultados esperados

| # | Escenario | Relación | Métricas verificadas |
|---:|---|---|---|
| 01 | Datos internos y criticidad media | MATCH | `CR:M`, `IR:M`, `AR:M` |
| 02 | PII, criticidad alta y baseline saturado | MATCH | `CR:H`, `IR:H`, `AR:H` |
| 03 | Componente sin entrada de red | DIFFERENT | `AV:L`, `CR:L`, `IR:L`, `AR:L` |
| 04 | Segmento local y datos financieros | DIFFERENT | `AV:A`, `CR:H`, `IR:M`, `AR:M` |
| 05 | Regla WAF específica contra el exploit | DIFFERENT | `AC:H`, `CR:H`, `IR:H`, `AR:H` |
| 06 | Allowlist que exige evadir la validación | DIFFERENT | `AC:H`, `CR:L`, `IR:L`, `AR:L` |
| 07 | Protección runtime que exige evasión | DIFFERENT | `AC:H`, `CR:M`, `IR:M`, `AR:M` |
| 08 | Datos públicos y consecuencias limitadas | DIFFERENT | `CR:L`, `IR:L`, `AR:L` |

El caso 01 es deliberado: existen métricas contextuales, pero sus pesos medios son neutros. Demuestra que score igual
no significa ausencia de contextualización. El caso 02 prueba otra causa de igualdad: saturación en el máximo.

## Metodología

1. Se construyó el JAR y se ejecutó `gradlew test` con Java 25.
2. Se inició una instancia nueva con H2 vacía para cada modelo.
3. Se activó `production-grok`, temperatura `0.0`, timeout de 60 segundos y hasta tres retries.
4. Se seleccionó el modelo al arrancar el proceso y se configuró esfuerzo `low`.
5. Se enviaron los ocho casos secuencialmente.
6. Por caso se verificaron HTTP, provenance, relación de score, métricas esperadas y equivalencia del `GET`.
7. Se repitió el primer request para verificar que la persistencia evitara una nueva evaluación.

Ventanas de la campaña, en UTC:

| Modelo | Inicio | Fin |
|---|---|---|
| Grok 4.3 low | `2026-09-20T22:00:44.648939Z` | `2026-09-20T22:02:11.439944Z` |
| Grok 4.6 low | `2026-09-20T22:02:40.906661Z` | `2026-09-20T22:04:39.386383Z` |

## Resultados

### Resultado agregado

| Modelo | HTTP 201 | ERROR | Relación PASS | Métricas PASS | E2E PASS | GET PASS | Idempotencia |
|---|---:|---:|---:|---:|---:|---:|---|
| Grok 4.3 low | 8 | 0 | 8 | 8 | 8 | 8 | PASS |
| Grok 4.6 low | 8 | 0 | 8 | 8 | 8 | 8 | PASS |
| **Total** | **16** | **0** | **16** | **16** | **16** | **16** | **2/2 PASS** |

### Resultado por caso

`PASS` exige simultáneamente respuesta utilizable, modelo de provenance correcto, relación de score correcta,
subconjunto de métricas correcto y lectura persistida equivalente.

| # | Grok 4.3 low | Grok 4.6 low |
|---:|---|---|
| 01 | PASS | PASS |
| 02 | PASS | PASS |
| 03 | PASS | PASS |
| 04 | PASS | PASS |
| 05 | PASS | PASS |
| 06 | PASS | PASS |
| 07 | PASS | PASS |
| 08 | PASS | PASS |

La suma de latencias por caso registrada por el cliente fue 86.785 segundos para Grok 4.3 y 118.472 segundos para
Grok 4.6. Son datos descriptivos de una sola campaña, no una comparación de rendimiento ni un SLA.

## Validación de la implementación

| Capacidad | Resultado esperado | Evidencia | Veredicto |
|---|---|---|---|
| Integración real | Invocar el proveedor configurado | 16 evaluaciones completadas | PASS |
| Identidad del modelo | Provenance coincide con la configuración | `grok/grok-4.3` y `grok/grok-4.6` | PASS |
| Contrato estructurado | Convertir cada respuesta en propuesta válida | 16/16 | PASS |
| Scoring determinista | El dominio calcula desde el vector propuesto | 16 relaciones correctas | PASS |
| Semántica contextual | La evidencia sostiene las métricas verificadas | 16/16 casos | PASS |
| Persistencia | `GET` reproduce la evaluación creada | 16/16 | PASS |
| Idempotencia | Repetir el request conserva el identificador | 2/2 | PASS |
| Manejo de error externo | Rechazar output inválido o timeout | No ejercitado en esta campaña | NO EVALUADO |

## Evidencia y reproducibilidad

Los reportes locales completos de la campaña se generaron en:

- `build/model-evaluations/grok-4.3-low-current.json`;
- `build/model-evaluations/grok-4.6-low-current.json`.

No se versionan porque contienen identificadores y timestamps de una ejecución concreta. Fueron emitidos antes de
normalizar la etiqueta de versión y conservan ese valor transitorio en provenance; no se usa como evidencia funcional.
Los fixtures, el runner y la semántica del catálogo sí están versionados y son suficientes para repetir la campaña.

Falta correlacionar estas ventanas con el export del proveedor. Ese paso debe confirmar modelo servido, esfuerzo,
cantidad de llamadas físicas, retries y estado final de cada request. Hasta recibirlo, el informe no afirma que las 16
evaluaciones lógicas correspondan exactamente a 16 llamadas físicas.

## Conclusión

La implementación quedó validada exitosamente contra Grok 4.3 low y Grok 4.6 low para los ocho escenarios definidos.
La precisión no se obtuvo comunicando el vector esperado, sino eliminando contradicciones entre las reglas internas y
la evidencia textual disponible para el proveedor.

La suite conserva su función de aceptación externa: complementa, pero no reemplaza, los tests deterministas de la
aplicación. Una futura falla debe clasificarse separando transporte, formato, relación de score y métricas; reducirla a
`MATCH`/`DIFFERENT` volvería a ocultar diferencias semánticas relevantes.
