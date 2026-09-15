# Cómo usar esta documentación

Este manual explica dónde vive cada tipo de información en
**vuln-severity-evaluator**, para quién está destinada, y cómo mantenerla
sincronizada con el código.

---

## Las audiencias

```
┌─────────────────────┬───────────────────────────────────────────────────────────┐
│ Audiencia           │ Sus fuentes de verdad                                     │
├─────────────────────┼───────────────────────────────────────────────────────────┤
│ ✅ Enforcement       │ src/test/java/.../ArchitectureTest.java +                 │
│                     │ MethodComplexityTest.java                                  │
│                     │ Los guardrails que hacen fallar el build si se rompe una  │
│                     │ convención de capas, anotaciones, naming o complejidad     │
├─────────────────────┼───────────────────────────────────────────────────────────┤
│ 👥 Humanos          │ docs/guide/                                               │
│                     │ Estado vivo del sistema: arquitectura, capas, dominio,    │
│                     │ ADRs, RFCs, runbooks, postmortems                         │
└─────────────────────┴───────────────────────────────────────────────────────────┘
```

> Todavía no existen `AGENTS.md` / `CLAUDE.md` en este repo (a diferencia de
> otros servicios Fury). Si se agregan, deben referenciar `ArchitectureTest.java`
> y `MethodComplexityTest.java` como fuente de verdad de las reglas en lugar de
> duplicarlas en prosa — así se evita que la doc para IA se desincronice del
> guardrail real.

---

## Tabla de decisión: ¿dónde va X?

| Tengo... | Va en... |
|----------|----------|
| Una propuesta que necesita debate del equipo | `docs/guide/rfcs/NNNN-<slug>.md` |
| Una decisión arquitectónica ya tomada | `docs/guide/adr/NNNN-<slug>.md` |
| Cómo está esquematizada la app (capas, wiring) | `docs/guide/architecture/README.md` |
| Cómo funciona el sistema de capas y por qué | `docs/guide/architecture/layering-model.md` |
| Un término del dominio que hay que definir | `docs/guide/domain/glossary.md` |
| Contexto de negocio del dominio | `docs/guide/domain/README.md` |
| Cómo operar / debugear en producción | `docs/guide/runbooks/NNNN-<slug>.md` |
| Post-incidente con lecciones aprendidas | `docs/guide/postmortems/PM-NNNN-<slug>.md` |
| Una convención de código que hay que **enforcar** | `src/test/java/.../ArchitectureTest.java` o `MethodComplexityTest.java` |
| Stack, versiones, getting started | `docs/guide/README.md` (no hay `README.md` en la raíz del repo todavía) |

---

## Reglas de código enforced (no son opcionales)

Estas convenciones fallan el build (`./gradlew test`) si se rompen:

- **Capas** — `Application` / `Configuration` / `Controller` / `Service` /
  `Repository` / `Model` / `Infrastructure`, con la matriz de acceso definida
  en `ArchitectureTest`.
- **Anotación ↔ paquete** — `@Configuration`, `@Controller`, `@Service`,
  `@Entity` y `@Repository` deben residir en su paquete de capa correspondiente.
- **Nomenclatura de tests** — `given<Setup>When<Action>Then[DoNotThrow|Return|Set|Throw]<Outcome>`.
- **Complejidad de métodos** — cyclomatic complexity > 10 tolerado en, como
  máximo, 5 métodos de todo `src/main`.

Detalle en [Arquitectura](/architecture/) y el
[Modelo de capas](/architecture/layering-model).

---

## Sync checklist post-cambio significativo

```
□ ¿Cambió cómo está esquematizada la app o se agregó una capa/subdominio?
  → Actualizar docs/guide/architecture/README.md (+ layering-model si cambia el modelo)

□ ¿Se agregó el primer controller/service/repository/entity real?
  → Documentar el endpoint / modelo en Arquitectura (y considerar crear
    architecture/endpoints.md y architecture/data-model.md cuando haya
    contenido real que documentar — ver regla de oro #5)
  → Agregar tests espejo en src/test/…
  → Los guardrails deben seguir en verde

□ ¿Se tomó una decisión arquitectónica nueva?
  → Crear/actualizar ADR en docs/guide/adr/

□ ¿Se propone un cambio grande que necesita debate?
  → Abrir RFC en docs/guide/rfcs/

□ ¿Se creó un procedimiento operacional?
  → Crear docs/guide/runbooks/NNNN-<slug>.md

□ ¿Hubo un incidente?
  → Crear docs/guide/postmortems/PM-NNNN-<slug>.md

□ ¿Se introdujeron términos nuevos del dominio?
  → Actualizar docs/guide/domain/glossary.md

□ ¿Cambió una convención de código?
  → Actualizar el guardrail en src/test/ + esta doc
```

---

## Reglas de oro

1. **Una sola fuente por tipo de doc** — si el mismo contenido está en dos lugares, uno está desactualizado.
2. **`docs/guide/` es el estado vivo** — documenta cómo funciona el sistema HOY, no la historia (eso va en ADRs y postmortems).
3. **Las convenciones se enforcan, no se piden por favor** — si una regla importa, vive en un guardrail de `src/test/`, no solo en prosa.
4. **ADRs no se editan post-accepted** — si una decisión cambia, se crea una nueva ADR que la reemplaza.
5. **Sin stubs vacíos** — si no hay contenido real, no crear el archivo; mejor un índice de sección (por eso todavía no existen `architecture/endpoints.md` ni `architecture/data-model.md`).
6. **Los diagramas son parte del código** — un diagrama desactualizado es peor que no tenerlo.
7. **Si dudás dónde va algo** — usá la tabla de decisión de arriba; si sigue sin quedar claro, abrilo en el PR.

---

## Navegación del Docsify

- **Sidebar:** controlado por `docs/guide/_sidebar.md`
- **Mermaid:** habilitado en `index.html`
- **Correr localmente:** `npx docsify-cli serve docs/guide`
