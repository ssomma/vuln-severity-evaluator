# Endpoints

Un recurso, dos operaciones. Los criterios que fijan la firma están abajo: no son implícitos, y varios de ellos son
consecuencia directa de que haya un modelo de lenguaje en el medio.

---

## `POST /vulnerability-evaluations`

Evalúa una vulnerabilidad contra una aplicación y **persiste el resultado**.

### Request

```json
{
  "vulnerability": {
    "identifier": "CVE-2021-44228",
    "description": "Apache Log4j2 JNDI features do not protect against attacker controlled LDAP endpoints...",
    "baseline_vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H"
  },
  "application": {
    "name": "checkout-api",
    "risk_profile": {
      "exposure": "INTERNET_FACING",
      "data_classification": "PII",
      "business_criticality": "TIER_1"
    },
    "runtime": ["JAVA", "SPRING_BOOT"],
    "compensating_controls": ["WAF"]
  }
}
```

| Campo | Requerido | Validación |
|---|---|---|
| `vulnerability.identifier` | sí | ≤ 64, identificador plano |
| `vulnerability.description` | sí | ≤ 8000. **Texto no confiable**: ver §Seguridad |
| `vulnerability.baseline_vector` | **no** | ≤ 512, lo interpreta el esquema activo |
| `application.name` | sí | ≤ 128, nombre plano |
| `application.risk_profile.*` | sí | Enums cerrados |
| `application.runtime` | sí | ≤ 16 códigos, validados contra el catálogo |
| `application.compensating_controls` | sí | ≤ 16 códigos, validados contra el catálogo |

`baseline_vector` es opcional a propósito. Si viene, el score se ancla a un dato duro y el modelo no puede tocarlo.
Si no viene, el modelo lo deriva, y eso queda registrado como `MODEL_DERIVED` con confianza degradada y revisión
obligatoria.

### Respuesta `201` + `Location`

```json
{
  "evaluation_id": "b4c13676-f1e0-40d7-8f23-71bcaf1dedf1",
  "vulnerability": "CVE-2021-44228",
  "application": "batch-reporter",
  "scheme": "CVSS:3.1",
  "baseline":   { "score": 10.0, "rating": "CRITICAL", "vector": "CVSS:3.1/AV:N/..." },
  "contextual": { "score": 7.6,  "rating": "HIGH",     "vector": "CVSS:3.1/AV:N/.../CR:L/IR:L/AR:L/MAV:L/..." },
  "delta": -2.4,
  "summary": "...",
  "justification": [
    { "metric": "CR", "value": "L", "rationale": "Confidentiality Requirement set to L because the application handles only public data." }
  ],
  "provenance": {
    "model": "stub-deterministic",
    "prompt_version": "stub-v1",
    "context_source": "CALLER_DECLARED",
    "baseline_vector_source": "CALLER_SUPPLIED",
    "confidence": "MEDIUM",
    "review_required": true,
    "evaluated_at": "2026-09-15T21:19:01.626613Z"
  },
  "advisory": true
}
```

Un consumidor que solo quiera priorizar puede quedarse con `contextual.score` y `contextual.rating`. Uno que quiera
auditar tiene el vector para recomputar el número y la justificación por métrica para discutirlo. Uno que quiera
automatizar **tiene que leer `provenance` y `advisory`** antes de hacerlo.

---

## `GET /vulnerability-evaluations/{id}`

Devuelve la evaluación persistida, con el mismo cuerpo. Es lo que hace auditable un resultado asistido por IA: sin
esta operación el score sería un número que apareció una vez y no se puede volver a mirar.

---

## Errores

Todos como RFC 7807 (`application/problem+json`).

| Status | Cuándo | Detalle |
|---|---|---|
| `400` | Input del caller inválido: vector malformado, código fuera de catálogo, enum desconocido, campo mal escrito, tamaño excedido | **Sí**, describe el propio input del caller |
| `404` | La evaluación no existe | Genérico |
| `502` | El modelo no produjo una respuesta usable, o el proveedor falló | **No**: el detalle del proveedor queda en el log |

La distinción 400/502 importa: una respuesta del modelo fuera de su vocabulario **no es culpa del caller**, así que
no se le devuelve un 400 que le haga revisar una request que estaba bien.

---

## Criterios de la firma

| Criterio | Consecuencia |
|---|---|
| Alcance mínimo del desafío | Campos obligatorios: texto de la vulnerabilidad + aplicación afectada; salida con severidad y justificación |
| Contexto declarado por el caller | El request carga el contexto, no solo el nombre ([ADR-0003](/adr/0003-caller-declared-application-context)) |
| `Never receive PII/tokens through query parameters` + `Never use GET for state-modifying operations` | Todo en el body; POST, que además persiste |
| Allowlist por construcción | Enums cerrados y códigos contra catálogo: el contrato público *es* el contrato de validación |
| Recurso, no acción | `201` + `Location`, recuperable por `GET /{id}`. Descartado `POST /evaluate`: no deja recurso auditable |
| Dos bloques por procedencia | `vulnerability` y `application` tienen confiabilidad y fuentes futuras distintas |
| La respuesta se describe a sí misma | `provenance` y `advisory` existen para que un consumidor sepa **cuándo no automatizar** |
| Justificación procesable | Lista por métrica además del `summary`, para diffear evaluaciones y detectar drift |
| El contrato no habla el vocabulario de un esquema | `baseline`/`contextual` en vez de términos CVSS, con `scheme` explícito |
| Prueba de concepto | Sin versionado en el path: agregar `/v1` sería ceremonia sin consumidores que proteger |
| Cardinalidad 1:1 sincrónica | Un CVE y un contexto por request |
| `snake_case` y RFC 7807 | Configurado una vez en `PresentationConfiguration` |

---

## Seguridad del borde

- La **descripción es texto no confiable**: cualquiera puede escribir un advisory. Viaja al modelo como parámetro de
  plantilla dentro de un bloque delimitado y declarado como dato, nunca como parte de la instrucción.
- **`FAIL_ON_UNKNOWN_PROPERTIES` está activo**: un campo mal escrito falla con 400 en vez de ignorarse. En este
  servicio el contexto mueve el score, así que un typo silencioso devolvería una severidad que el caller cree
  contextualizada y no lo está.
- El **nombre de la aplicación no se envía al modelo**: no hace falta para evaluar, y así no salen nombres de
  inventario interno hacia un proveedor externo.
