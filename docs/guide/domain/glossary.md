# Glosario

Términos del dominio y de la arquitectura de **vuln-severity-evaluator**.

---

## Dominio — evaluación de severidad

| Término | Definición |
|---------|------------|
| **Baseline** | El score de la vulnerabilidad en abstracto, sin contexto. Viene del vector que manda el caller o, si no lo manda, lo deriva el modelo |
| **Contextual** | El score después de aplicar el contexto declarado de la aplicación. Es el producto real del servicio |
| **Delta** | La diferencia entre ambos. Lo que la contextualización aportó, y lo que se puede discutir |
| **Vector** | La representación textual de las métricas de las que sale un score (`CVSS:3.1/AV:N/...`). Viaja con el score para que un revisor pueda recomputarlo |
| **Esquema de scoring** (`SeverityScheme`) | El sistema que traduce métricas a un número. Hoy CVSS v3.1; la aplicación no depende de cuál sea |
| **Vocabulario** | El conjunto cerrado de métricas de un esquema y los valores que admite cada una. Define a la vez qué se le pide al modelo y qué se le acepta |
| **Métrica** | Una dimensión de la severidad (alcance del ataque, impacto en confidencialidad, criticidad de la aplicación). Existe **una sola vez** en el catálogo y se lee dos veces: el valor que trae el vector baseline y el que el modelo deriva del contexto declarado |
| **Not Defined (`X`)** | Abstención: el contexto no da evidencia sobre esa métrica, y se conserva el valor base. Preferible a adivinar. No es un valor del catálogo: aplica a toda métrica por construcción |
| **Propuesta** (`ModelSeverityProposal`) | Lo que responde el modelo: un valor por métrica con su rationale. Se valida al construirse |
| **Procedencia** (`Provenance`) | Bajo qué condiciones se produjo la evaluación: modelo, versiones de prompt, catálogo y política, origen del contexto y del baseline, confianza y si requiere revisión |
| **Confianza** | Cuánto pesa la evaluación. Baja si el baseline lo derivó el modelo, o si el modelo se abstuvo en la mayoría de las métricas |
| **Advisory** | El resultado es soporte a una decisión humana, nunca autorización para remediar automáticamente |
| **Huella** (`fingerprint`) | Identidad de los insumos de una evaluación. Permite responder una request idéntica con el resultado ya registrado |
| **Contexto declarado** | Exposición, clasificación de datos, criticidad, runtime y controles compensatorios que el caller afirma sobre su aplicación |
| **Catálogo** | Los valores admitidos del contexto y la especificación del esquema, persistidos en base en vez de escritos como código |

---

## Arquitectura — vocabulario del código

| Término | Definición |
|---------|------------|
| **Layer / capa** | Una de: `Application`, `Configuration`, `Controller`, `Service`, `Repository`, `Llm`, `Model`, `Infrastructure`, definidas en `ArchitectureTest` |
| **Llm** | Capa propia para `datasource/llm`. Existe para que el guardrail distinga estructuralmente un dato de la infraestructura propia de una opinión de un modelo |
| **Datasource** | Dos familias: `repository`, donde la aplicación es fuente directa, y `llm`, donde no |
| **Fitness test / guardrail** | Test en `src/test/` que enforcea capas, anotaciones, naming o complejidad y falla el build (`ArchitectureTest`, `MethodComplexityTest`) |
| **Presupuesto de complejidad** | Cinco métricas con tolerancia cero: ciclomática ≤ 5, nesting ≤ 3, ≤ 5 parámetros, ≤ 32 statements, ≤ 64 líneas por método |
| **Responsabilidad por dominio** | Criterio de granularidad: una clase cubre un dominio delimitado y absorbe lo suyo; lo compartido nunca va anidado |
