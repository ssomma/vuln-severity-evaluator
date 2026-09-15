# Glosario

Términos del dominio y de la arquitectura de **vuln-severity-evaluator**.
Es un glosario semilla: expandir con cada término nuevo que introduzca un
subdominio real (ver [sync checklist](/_meta/how-to-use)).

---

## Dominio — evaluación de severidad

> Todavía no hay subdominios implementados, por lo que no hay términos de
> negocio confirmados. Completar esta sección junto con
> [Dominio → Cómo documentar el primer subdominio real](/domain/) en cuanto
> se implemente el primer `Service` (por ejemplo, definiciones como *severidad*,
> *hallazgo*, *CVSS*, *CVE*, según aplique al diseño final).

---

## Arquitectura — vocabulario del código

| Término | Definición |
|---------|------------|
| **Layer / capa** | Una de: `Application`, `Configuration`, `Controller`, `Service`, `Repository`, `Model`, `Infrastructure`, definidas en `ArchitectureTest`. |
| **Application** | Paquete raíz `org.challenge`; capa de ensamblaje/arranque. |
| **Configuration** | Beans `@Configuration`; capa de wiring, no accedida por ninguna otra capa. |
| **Controller** | Endpoint HTTP `@Controller` en `presentation.controller`. |
| **Service** | Clase de lógica de negocio `@Service` en `domain.service`. |
| **Repository** | Clase de acceso a datos `@Repository` en `datasource.repository`. |
| **Model** | Entidad JPA `@Entity` en `domain.model`. |
| **Infrastructure** | Helpers transversales, sin dependencias internas propias. |
| **Fitness test / guardrail** | Test en `src/test/` que enforcea capas, anotaciones, naming o complejidad y falla el build si se violan (`ArchitectureTest`, `MethodComplexityTest`). |
| **Cyclomatic complexity budget** | Máximo de métodos (5) que pueden superar un umbral de complejidad (10) en todo `src/main` antes de que falle el build. |
