# vuln-severity-evaluator

API backend que evalúa **qué tan grave es una vulnerabilidad para una aplicación concreta**, asistida por un modelo
de lenguaje.

El CVSS Base Score describe la vulnerabilidad en abstracto: un 9.8 en un batch aislado y un 9.8 en un servicio
expuesto con datos personales se ven iguales. Este servicio los separa.

```
CVE-2021-44228 (Log4Shell), mismo vector base en las dos aplicaciones:

  checkout-api    internet-facing, PII, Tier 1   →  10.0 CRITICAL   (baseline 10.0)
  batch-reporter  aislada, público,   Tier 3     →   7.6 HIGH       (delta -2.4)
```

---

## Cómo correrlo

No hace falta API key ni red: el perfil por defecto es `local`, con modelo determinista y base en memoria.

```bash
./gradlew bootRun
```

```bash
curl -s -X POST http://localhost:8080/vulnerability-evaluations \
  -H 'Content-Type: application/json' \
  -d @docs/examples/log4shell-tier1.json | jq
```

Para ver la contextualización, comparar con `docs/examples/log4shell-tier3.json`: el mismo CVE, otra aplicación.

Contra un proveedor real (la key nunca va en el repositorio):

```bash
OPENAI_API_KEY=... ./gradlew bootRun --args='--spring.profiles.active=production'
```

## Tests

```bash
./gradlew test
```

57 tests. Incluyen los vectores oficiales de la especificación CVSS v3.1, que validan tanto las fórmulas como los
coeficientes cargados en la base.

---

## La idea en una línea

> **El modelo nunca devuelve un número.** Devuelve elecciones de métricas de un vocabulario cerrado, con su
> justificación. La aritmética la hace el dominio, de forma determinista y verificable.

Eso acota lo que puede salir mal: el modelo no puede inventar un score, no puede alterar el vector base que mandó el
caller, y cualquier valor fuera del vocabulario se rechaza en vez de coercionarse. El razonamiento completo —incluido
lo que **no** está resuelto— está en [Uso de IA y sus límites](docs/guide/architecture/ai-usage-and-limits.md).

---

## Documentación

```bash
npx docsify-cli serve docs/guide
```

| Por dónde empezar | Qué responde |
|---|---|
| [ADD](docs/guide/add/README.md) | El diseño de la solución: drivers, alcance, flujo, seguridad, riesgos |
| [Uso de IA y sus límites](docs/guide/architecture/ai-usage-and-limits.md) | Dónde se usa IA, dónde no, y qué puede salir mal |
| [ADRs](docs/guide/adr/) | Las seis decisiones, con sus alternativas descartadas |
| [Endpoints](docs/guide/architecture/endpoints.md) | El contrato HTTP |
| [Arquitectura](docs/guide/architecture/README.md) | Capas y guardrails |
| [Proceso de investigación](docs/guide/_meta/research-log.md) | Qué hubo que mirar y qué cambió por haberlo mirado |

---

## Requisitos

JDK compatible con Spring Boot 4. No hace falta instalar Gradle: usar el wrapper (`./gradlew`).
