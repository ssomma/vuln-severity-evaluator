# Arquitectura de capas: Presentation Domain Data Layering

## 1. Qué es este patrón

**Presentation Domain Data Layering (PDL)** es el estilo arquitectónico adoptado por este proyecto. Divide la aplicación en tres grandes bloques de responsabilidad:

- **Presentation**: traduce el mundo exterior (HTTP, serialización, validación de entrada) hacia el dominio y viceversa. No contiene reglas de negocio.
- **Domain**: el núcleo de negocio. Contiene el **modelo** (entidades y objetos de valor) y los **servicios** (contratos + implementaciones) que expresan los casos de uso.
- **Data (datasource)**: acceso a persistencia y a sistemas externos (bases de datos, colas de mensajería, almacenamiento de objetos, clientes REST salientes).

A esto se suman dos capas transversales que no forman parte de la tríada básica pero que este proyecto formaliza como capas propias porque tienen reglas de dependencia distintas:

- **Infrastructure**: utilidades de bajo nivel y cross-cutting (manejo de errores, métricas, serialización, validación) usadas por el resto de las capas de negocio.
- **Configuration**: la raíz de composición del contexto. Es la única capa que conoce y conecta (wiring) a todas las demás.

Este documento describe cómo se implementa (o debe implementarse, dado que este repositorio recién está iniciando su código de producción) esta arquitectura como modelo propio del proyecto.

## 2. Estructura de paquetes de este proyecto

El paquete raíz de la aplicación es `org.challenge.vulnseverityevaluator`. La convención de paquetes por capa está definida hoy como *contrato* en [`ArchitectureTest.java`](../../src/test/java/org/challenge/vulnseverityevaluator/ArchitectureTest.java):

```
org.challenge.vulnseverityevaluator
├── Main.java                      → raíz de composición (bootstrap Spring Boot)
├── configuration/                 → wiring de beans de todas las capas
├── presentation/
│   └── controller/                → REST controllers (entrada HTTP)
│       └── response/              → conversión de modelo de dominio → payload HTTP (si aplica)
├── domain/
│   ├── model/                     → entidades y objetos de valor del dominio
│   └── service/                   → contratos (interfaces) de los casos de uso
│       └── impl/                  → implementaciones de esos contratos
├── datasource/
│   ├── llm/                       → origen de datos: el modelo de lenguaje
│   └── repository/                → origen de datos: la infraestructura propia
└── infrastructure/                → utilidades transversales (errores, métricas, serialización)
```

| Capa (`ArchitectureTest`) | Paquete | Rol |
|---|---|---|
| `Application` | `org.challenge` (raíz) | Composition root: arranca la app y no contiene lógica de negocio |
| `Configuration` | `..configuration..` | Wiring: construye e inyecta las implementaciones concretas de cada capa |
| `Controller` | `..presentation.controller..` | Entrada HTTP, traduce request/response |
| `Model` | `..domain.model..` | Entidades y VOs del dominio |
| `Service` | `..domain.service..` | Contratos e implementaciones de los casos de uso |
| `Llm` | `..datasource.llm..` | El modelo de lenguaje, tratado como origen de datos |
| `Repository` | `..datasource.repository..` | Persistencia: aquello de lo que la aplicación es fuente directa |
| `Infrastructure` | `..infrastructure..` | Cross-cutting: errores, métricas, serialización, validación |

Las capas están pobladas: hay un caso de uso completo, su esquema de scoring, dos familias de datasource y los catálogos. Lo que sigue describe las reglas que gobiernan cómo crece ese código.

**Dos familias de datasource, no una.** `datasource/repository` es información de la que la aplicación es fuente directa; `datasource/llm` es una opinión de un modelo. Tienen confiabilidad distinta, así que son **capas distintas** con sus propias reglas de acceso, y el guardrail lo verifica en cada build.

## 3. Responsabilidad de cada capa

### 3.1 Presentation

Contiene los `@RestController`/`@Controller`, filtros HTTP y, cuando la respuesta requiere un formato distinto al modelo de dominio, clases de conversión.

- **Controllers** (`presentation/controller/`): reciben el request, delegan en un `domain.service` y devuelven un `ResponseEntity`. Ejemplo: `VulnerabilityController` recibe el request de evaluación de severidad y delega en `VulnerabilitySeverityService`.
- **Conversión de salida** (`presentation/controller/response/`): cuando el endpoint necesita un formato distinto al modelo de dominio (por ejemplo, paginado o un envoltorio con metadata), se agrega una clase dedicada que transforma el modelo de dominio en el payload de respuesta. Cuando el modelo de dominio ya es serializable directamente (anotando campos con `@JsonIgnore` donde corresponda), no se crea esta clase intermedia.
- **Filtros** (`presentation/filter/`): filtros HTTP transversales, por ejemplo un filtro de autorización.

La capa de presentation **no contiene reglas de negocio**: valida forma (tipos, formatos) pero delega toda decisión de negocio al dominio.

### 3.2 Domain

El núcleo de negocio, dividido en dos subcapas con reglas de dependencia distintas:

- **`domain/model`**: entidades y objetos de valor. En este patrón, cuando la persistencia es JPA, **la entidad de persistencia y el modelo de dominio son la misma clase** — no existe una entidad separada en otro paquete. Esto está forzado explícitamente por la regla de `ArchitectureTest` que exige que toda clase anotada `@Entity` resida en `..domain.model..`. Ejemplo: `Vulnerability` sería a la vez `@Entity` y portadora de sus propias reglas de negocio (p. ej. calcular o exponer su severidad).
- **`domain/service`**: la interfaz es el contrato que consume `presentation` (p. ej. `VulnerabilitySeverityService`). `domain/service/impl` contiene la(s) implementación(es) concreta(s), inyectadas por `configuration` a través de la interfaz — nunca se depende del tipo concreto. Un patrón válido dentro de esta subcapa es el **Decorator**: por ejemplo, una implementación con caché que envuelve a la implementación real, ambas satisfaciendo la misma interfaz, sin que el controller conozca la diferencia.

### 3.3 Datasource

Contratos e implementaciones de acceso a datos y a sistemas externos, organizados por tecnología cuando hay más de una implementación posible:

- Repositorios sobre base de datos (Spring Data / JPA), operando directamente sobre las entidades de `domain.model` (p. ej. `VulnerabilityRepository extends JpaRepository<Vulnerability, Long>`).
- Clientes salientes con interfaz + implementación separadas por tecnología, replicando el patrón interfaz-detrás-de-implementación: por ejemplo, una interfaz de cliente hacia un servicio externo de scoring (CVSS, feeds de vulnerabilidades) con una implementación REST concreta.
- Implementaciones *mock* para el perfil `local`, activadas solo por `configuration` en ese perfil, de forma que el resto de las capas nunca sabe si está hablando con la implementación real o el mock.

### 3.4 Infrastructure

Utilidades de bajo nivel, transversales a varias capas de negocio: manejo global de errores (`@ControllerAdvice`), serialización, métricas, validación (`@Constraint`/`Validator`), aspectos (AOP). No debe mezclarse con lógica de negocio ni con detalles de transporte HTTP.

### 3.5 Configuration

La raíz de composición del módulo. Es la **única** capa que puede depender de todas las demás, porque su trabajo es exactamente ensamblar el grafo de dependencias: construye los beans concretos de `datasource` y `domain.service`, los inyecta entre sí y en los controllers, típicamente separando por *profile* de Spring (`production`/`test` con implementaciones reales vs. `local` con mocks).

### 3.6 Application (composition root)

`Main.java`. Su única responsabilidad es levantar el contexto de Spring Boot. No contiene lógica de negocio ni conoce el detalle de wiring: eso es trabajo de `configuration`.

## 4. Dirección de las dependencias

Las dependencias apuntan siempre **hacia el dominio**, nunca al revés:

```
                 ┌───────────────┐
                 │ configuration │  ← única capa que puede depender de todas
                 └──┬──┬──┬──┬───┘
                    │  │  │  │
        ┌───────────┘  │  │  └────────────┐
        ▼               │  │              ▼
   presentation      domain ◄───────  datasource
   (controller)    (model + service)  (repository)
                        │
                        ▼
                  infrastructure
```

| Desde \ Hacia | Model | Service | Repository | Controller | Infrastructure | Configuration |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Model** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Service** | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| **Repository** | ✅ | ❌ | ✅ | ❌ | ✅ | ❌ |
| **Controller** | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ |
| **Infrastructure** | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| **Configuration** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

Por qué importa esta dirección:

1. **El núcleo de negocio es testeable en aislamiento**: `model` y `service` no dependen de `presentation` ni de `configuration`, por lo que las implementaciones de servicio se pueden probar con interfaces de repositorio, sin servidor ni base de datos real.
2. **Los bordes son intercambiables**: como `presentation` y `datasource` dependen de interfaces del dominio (y no al revés), reemplazar un controller o una implementación de repositorio no toca la lógica de negocio.
3. **El wiring está centralizado**: solo `configuration` sabe qué tipo concreto satisface cada interfaz. Todo el grafo de dependencias es legible y modificable en un solo lugar.

## 5. Flujo de una petición de punta a punta

Ejemplo de un caso de uso típico (evaluar la severidad de una vulnerabilidad):

```
Controller (presentation.controller)
    → interfaz de Service (domain.service)
        → [opcional] Decorator (domain.service.impl) — p. ej. caché
            → implementación real (domain.service.impl)
                → Repository (datasource.repository)
                    → Model/Entity (domain.model)
```

1. El controller recibe el request y llama a la interfaz de `domain.service`, inyectada por `configuration` (nunca el tipo concreto).
2. Si existe una variante decorada (por ejemplo, con caché), la cadena pasa primero por el decorator y luego por la implementación real — ambas implementan la misma interfaz, de forma transparente para el controller.
3. La implementación real usa una o más interfaces de `datasource.repository` para leer/escribir datos, y opera sobre entidades de `domain.model`.
4. El resultado (una entidad de dominio, o una colección de ellas) vuelve hasta el controller. Si el modelo de dominio ya es serializable tal cual, el controller lo devuelve directo en el `ResponseEntity`; si el formato de salida requiere transformación (paginado, envoltorio con metadata, etc.), se usa una clase de `presentation.controller.response` para esa conversión puntual.

**Nota sobre mappers**: como el modelo de dominio y la entidad de persistencia son la misma clase, este patrón **no** tiene un mapper de tres niveles DTO↔Domain↔Entity. El único punto de conversión real está entre `domain.model` y el payload de presentación, y solo se agrega cuando el formato de salida difiere del modelo de dominio.

## 6. Cómo se refuerzan estas reglas: `ArchitectureTest.java`

Las reglas anteriores no son una convención "de honor": están **verificadas automáticamente en cada build** mediante ArchUnit, en [`ArchitectureTest.java`](../../src/test/java/org/challenge/vulnseverityevaluator/ArchitectureTest.java).

### 6.1 Regla de capas (`givenLayeredArchitectureWhenCheckThenDoNotThrowException`, líneas 56-75)

Define las 7 capas con `layeredArchitecture().consideringAllDependencies()` y aplica:

- `Configuration.mayNotBeAccessedByAnyLayer()` — nadie puede depender de una clase de `configuration`; es un nodo terminal que solo consume a las demás.
- `Controller.mayOnlyBeAccessedByLayers(Configuration)` — ninguna capa de negocio puede depender de un controller.
- `Infrastructure.mayOnlyBeAccessedByLayers(Application, Configuration, Controller, Repository, Service)` — es transversal, pero **`Model` no figura en esta lista**: el modelo no debe depender hacia abajo de infraestructura.
- `Model.mayOnlyBeAccessedByLayers(Application, Configuration, Controller, Repository, Service)` — cualquier capa de negocio puede usar el modelo de dominio.
- `Repository.mayOnlyBeAccessedByLayers(Configuration, Service)` — **un controller no puede llamar directo a un repository**; debe pasar por `Service`.
- `Llm.mayOnlyBeAccessedByLayers(Configuration, Service)` — igual que `Repository`: el modelo de lenguaje solo se alcanza desde el caso de uso.
- `Service.mayOnlyBeAccessedByLayers(Configuration, Controller)` — un repository no puede depender de un service (evita ciclos hacia arriba).

Esto convierte en un chequeo automático la cadena descrita en la sección 5: `Controller → Service → Repository/Model`, y prohíbe explícitamente los saltos indebidos (Controller → Repository directo, Repository → Service, cualquiera → Configuration).

### 6.2 Reglas de anotación → paquete obligatorio (líneas 78-110)

Usando el helper `annotatedClassesShouldResideIn`, el test obliga a que:

| Anotación | Debe residir en |
|---|---|
| `@Configuration` (Spring) | `..configuration..` |
| `@Controller` (Spring) | `..presentation.controller..` |
| `@Service` (Spring) | `..domain.service..` |
| `@Entity` (JPA) | `..domain.model..` |
| `@Repository` (Spring) | `..datasource.repository..` |

La regla sobre `@Entity` es la que formaliza, a nivel de CI, la decisión de la sección 3.2: **el modelo de dominio y la entidad JPA deben ser la misma clase**, y esa clase vive únicamente en `domain.model`. Si alguien crea una entidad JPA fuera de ese paquete (por ejemplo, dentro de `datasource`), el build falla.

### 6.3 Convención de nombres de test (líneas 112-118)

Además de las reglas de capas, el archivo valida que todo método `@Test` siga el patrón `given.+When.+Then[DoNotThrow|Return|Set|Throw].+` (Given-When-Then). No es una regla de capas, pero forma parte del mismo mecanismo de fitness functions que mantiene la arquitectura de tests consistente en todo el repositorio.

### 6.4 Consideraciones prácticas

- Todas las reglas usan `allowEmptyShould(true)`, lo que permitió incorporar la arquitectura de forma incremental. Ya no es relevante: las capas están pobladas y las reglas validan código real.
- Las reglas de residencia de `@Controller`, `@Service` y `@Repository` usan `areMetaAnnotatedWith`, no `areAnnotatedWith`: la versión directa **no detecta `@RestController`**, que es meta-anotación de `@Controller`. `@Configuration` se verifica directa a propósito, porque `@SpringBootApplication` la lleva como meta-anotación y la regla meta exigiría mover el composition root.
- La verificación correcta de una regla de arquitectura incluye un **chequeo negativo**: introducir temporalmente una dependencia prohibida (p. ej. un `Repository` inyectando un `Service`), confirmar que `ArchitectureTest` falla, y revertir el cambio. Esto se recomienda al agregar o modificar cualquier regla de capas.

## 7. Alcance excluido: procesamiento batch

Cualquier flujo de procesamiento batch (jobs, steps, tasklets, readers/writers/processors) queda deliberadamente fuera de este documento. Si este proyecto en algún momento incorpora procesamiento batch, deberá decidirse explícitamente si se lo somete a las mismas reglas de capas descritas aquí o si se lo trata como un módulo aparte no gobernado por `ArchitectureTest`.

## 8. Cómo agregar un nuevo caso de uso

Para un nuevo caso de uso `[Caso]`, la estructura a crear es:

```
domain/
├── model/       [Caso].java              ← entidad/objeto de valor de dominio
└── service/     [Caso]Service.java       ← interfaz
    └── impl/    [Caso]ServiceImpl.java   ← implementación
datasource/
└── repository/  [Caso]Repository.java         ← interfaz (si hay persistencia)
                 [Caso]RepositoryJpa.java       ← implementación (si aplica)
presentation/
└── controller/  [Caso]Controller.java    ← entrada HTTP
```

Y en `configuration`: instanciar el repositorio (si aplica), inyectarlo en el service, inyectar el service en el controller. Las reglas de `ArchitectureTest.java` se aplican automáticamente sobre las clases nuevas sin necesidad de tocar el propio test.
