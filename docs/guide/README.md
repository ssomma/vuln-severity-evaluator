# vuln-severity-evaluator — Documentación

Servicio backend en **Java / Spring Boot** que evalúa **qué tan grave es una vulnerabilidad para una aplicación
concreta**, con un modelo de lenguaje como soporte del análisis.

El CVSS Base Score describe la vulnerabilidad en abstracto. Este servicio la re-puntúa según el contexto declarado de
la aplicación afectada, y deja registrado por qué.

```
CVE-2021-44228 (Log4Shell), mismo vector base:
  checkout-api    internet-facing, PII, Tier 1  →  10.0 CRITICAL
  batch-reporter  aislada, público,   Tier 3    →   7.6 HIGH  (delta -2.4)
```

---

## Por dónde empezar

| Documento | Qué responde |
|---|---|
| [**ADD**](/add/) | El diseño de la solución completo: drivers, alcance, flujo, seguridad, riesgos abiertos |
| [Uso de IA y sus límites](/architecture/ai-usage-and-limits) | Dónde se usa IA, dónde deliberadamente no, y qué puede salir mal |
| [ADRs](/adr/) | Las siete decisiones, cada una con sus alternativas descartadas |
| [Endpoints](/architecture/endpoints) | El contrato HTTP y los criterios que fijan su firma |
| [Arquitectura](/architecture/) | Capas, residencia de anotaciones y guardrails |
| [Dominio](/domain/) | Qué se evalúa, con qué insumos y qué confiabilidad tiene cada uno |
| [Validación con modelos reales](/architecture/external-model-validation) | Evidencia E2E contra xAI/Grok sin incorporar infraestructura externa a los tests |
| [Proceso de investigación](/_meta/research-log) | Qué hubo que mirar y qué cambió por haberlo mirado |

---

## La decisión que sostiene todo lo demás

> **El modelo nunca devuelve un número.** Devuelve elecciones de métricas de un vocabulario cerrado, con su
> justificación. La aritmética la hace el dominio, de forma determinista y verificada contra la especificación
> publicada.

Y como un LLM tampoco es una fuente determinista, una request idéntica se responde con **la evaluación ya
registrada** en vez de volver a preguntar. Esa garantía arquitectónica requiere persistencia durable. La prueba de
concepto usa H2 en memoria, por lo que conserva la evaluación solo durante la vida del proceso y no tras un reinicio.

---

## Cómo correrlo

El perfil por defecto es `local`: modelo determinista, base en memoria, sin API key ni red.
El proyecto requiere **JDK 25 LTS**: Gradle fija ese toolchain y compila con `--release 25`.

```bash
./gradlew bootRun
./gradlew test
```

---

## Stack

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| Framework | Spring Boot | 4.1.1 |
| JDK | Java LTS | 25 |
| IA | Spring AI · `spring-ai-starter-model-openai` | 2.0.1 |
| Web | `spring-boot-starter-webmvc` | gestionado |
| Persistencia | `spring-boot-starter-data-jpa` + H2 | gestionado |
| Validación | `spring-boot-starter-validation` | gestionado |
| Arquitectura (fitness tests) | ArchUnit | 1.5.0 |
| Complejidad (fitness tests) | JavaParser | 3.27.0 |
| Tests | JUnit | 6.0.0 |

---

## Convenciones

Las reglas de capas, residencia de anotaciones, nomenclatura de tests y complejidad son de cumplimiento
**obligatorio** y están enforced por los tests de fitness en `src/test/java/org/challenge/vulnseverityevaluator/`.
No son convenciones de honor: una violación falla el build.

> Esta documentación se mantiene viva. Si algo está desactualizado, actualizarlo es parte del ciclo de desarrollo.
> Ver [cómo usar esta documentación](/_meta/how-to-use).
