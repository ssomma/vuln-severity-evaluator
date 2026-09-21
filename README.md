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

## Proveedores externos y modelos

Cada proveedor de producción tiene un archivo `application-production-<provider>.yaml` y activa la misma
implementación `LLMSeverityReasoningModel`. Las configuraciones no sensibles —proveedor, endpoint, modelo, timeout y
reintentos— viven versionadas en ese archivo. Solo la API key llega por variable de entorno.

El perfil `production-openai` usa OpenAI con `gpt-4o-mini`:

```bash
export OPENAI_API_KEY='tu-api-key'
./gradlew bootRun --args='--spring.profiles.active=production-openai'
```

El perfil `production-grok` usa la API compatible con OpenAI de xAI Grok con `grok-4`:

```bash
export GROK_API_KEY='tu-api-key'
./gradlew bootRun --args='--spring.profiles.active=production-grok'
```

En PowerShell, definir `$env:OPENAI_API_KEY = '...'` o `$env:GROK_API_KEY = '...'` y ejecutar el mismo comando con
`./gradlew.bat`. No guardar claves en el repositorio. El arranque falla si la key requerida está ausente.

Para incorporar otro proveedor compatible, crear `application-production-<provider>.yaml` con la misma forma que
los perfiles existentes: `app.ai.provider-id`, `spring.ai.model.chat`, `spring.ai.openai.base-url`,
`spring.ai.openai.api-key`, `timeout`, `max-retries`, `chat.model` y `chat.temperature`. Luego agregar el perfil a
`DataSourceConfiguration` y al bloque de H2 del desafío. El endpoint debe implementar OpenAI Chat y devolver el
formato que consume Spring AI.

Los selectores de audio, imagen, embeddings y moderación quedan desactivados en `application.yaml`: este servicio
solo consume el modelo de chat. Así, el perfil `local` no intenta crear beans de OpenAI ni exige credenciales; cada
perfil de producción habilita únicamente `spring.ai.model.chat: openai`.

En este desafío, `local`, `production-openai` y `production-grok` cargan el mismo catálogo CVSS/contexto en **H2 en memoria**.
Esto permite ejercitar la integración externa sin aprovisionar una base; las evaluaciones se pierden al reiniciar.
Arquitectónicamente, el determinismo para una entrada repetida se obtiene al persistir y consultar la evaluación por
su huella antes de invocar al modelo. H2 efímera solo permite comprobar ese flujo mientras el proceso sigue activo;
un despliegue que conserve esa garantía tras reinicios necesita una base persistente y migraciones versionadas, como
se explica en [Modelo de datos](docs/guide/architecture/data-model.md). Los perfiles de proveedor seleccionan el
cliente y sus credenciales; no cambian el cálculo de severidad.

## Tests

```bash
./gradlew test
```

La suite incluye los vectores oficiales de la especificación CVSS v3.1, que validan tanto las fórmulas como los
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
| [ADRs](docs/guide/adr/) | Las siete decisiones, con sus alternativas descartadas |
| [Endpoints](docs/guide/architecture/endpoints.md) | El contrato HTTP |
| [Arquitectura](docs/guide/architecture/README.md) | Capas y guardrails |
| [Proceso de investigación](docs/guide/_meta/research-log.md) | Qué hubo que mirar y qué cambió por haberlo mirado |

---

## Requisitos

JDK 25 LTS. El build fija Java 25 mediante el toolchain de Gradle y genera bytecode para esa versión. No hace falta
instalar Gradle: usar el wrapper (`./gradlew`), que descargará o seleccionará un JDK 25 compatible según la
configuración local de Gradle.
