# ADR-0005: La especificación de scoring y los catálogos son dato en base, no código

| Campo    | Valor |
|----------|-------|
| Status   | accepted |
| Fecha    | 2026-09-15 |
| Contexto | Revisión de sobreingeniería: cuánto de lo definido como información podía ser configuración o persistencia |

## Contexto

La primera implementación tenía la especificación CVSS v3.1 escrita como código: ocho enums de pesos dentro de
`Cvss31`, un enum con el vocabulario contextual y su texto de guía, dos enums más para runtimes y controles
compensatorios, y cuatro mapas estáticos en el modelo determinista para inferir métricas a partir del contexto.

Eso es una tabla publicada escrita como Java. Tiene tres problemas concretos. Corregir un coeficiente mal transcripto
es un cambio de código y un deploy. El texto que lee el modelo queda mezclado con la aritmética, en un archivo de 600
líneas donde nadie lo va a revisar. Y las listas abiertas —runtimes, controles— crecen: cada valor nuevo es una
recompilación para agregar una constante que no cambia ninguna lógica.

## Decisión

La especificación y los catálogos son **filas**, no constantes.

- `scheme_metric` + `scheme_metric_value`: qué métricas existen para un esquema, qué valores admite cada una y cuánto
  pesa cada valor. **Una fila por métrica, no una por pasada de scoring**: la misma métrica se puntúa con el valor del
  vector baseline y con el que el modelo derivó del contexto, así que ningún coeficiente ni valor admitido se guarda
  dos veces.
- `context_attribute` + `context_suggestion`: el vocabulario del contexto declarado, el significado que lee el
  modelo, y qué valores de métrica argumenta cada atributo.

Dos consecuencias de diseño que no son obvias:

**El esquema recibe la especificación como argumento**, no la busca. `Cvss31` sigue siendo una función pura de sus
entradas: no toca datasource, y se testea sin base. Quien lee el catálogo es el caso de uso, a través de
`SpecificationCatalog`.

**El modelo determinista se quedó sin tabla de mapeo propia.** Cada atributo del catálogo declara qué métricas
argumenta, así que el stub solo recolecta sugerencias. La inferencia dejó de ser código.

**Lo que es sintaxis del esquema se quedó en código, no bajó a la base.** Dos cosas parecían dato y no lo son: la
abstención `X`, que aplica a toda métrica por construcción y no pertenece a ninguna tabla de pesos, y el prefijo `M`
de las métricas Environmental de CVSS, que es el formato del vector publicado. Ambas viven en `Cvss31` junto al
separador y al asignador del vector. El criterio: la tabla de la especificación es dato, la gramática con que se
escribe es del esquema.

**La revisión del catálogo vive en `scheme_metric`.** Todas las métricas de un esquema llevan el mismo
`catalog_version`, que representa tanto sus pesos y textos como el catálogo contextual asociado. Antes de buscar una
evaluación por huella, el servicio consulta únicamente el valor distinto de esa columna; las métricas, valores y
atributos completos se cargan solo cuando no hay coincidencia. Modificar cualquier fila de ambos catálogos exige
incrementar la versión en todas las métricas del esquema dentro de la misma transacción.

## Consecuencias

- **Bueno:** `Cvss31` pasó de ~600 líneas y doce tipos anidados a ~260 con un único record privado. Lo que queda son
  las fórmulas de la especificación, que es lo que debería estar en una clase llamada así.
- **Bueno:** el catálogo quedó en 11 métricas y 31 valores. Modelar cada métrica dos veces —una con los pesos, otra
  con el vocabulario— costaba 19 filas, 74 valores (36 de ellos con peso cero que nunca se leían) y un puntero entre
  ambas mitades para volver a unirlas al puntuar.
- **Bueno:** los vectores oficiales de la spec ahora **validan también el seed**. Un peso mal cargado falla un golden
  case en vez de correr todos los scores una décima.
- **Bueno:** el texto de guía del prompt vive junto a los valores que admite. Antes eran dos lugares que podían
  desincronizarse en silencio — el modelo recibiría una instrucción que la validación no acepta.
- **Bueno:** agregar un runtime o un control es una fila. Agregar un esquema entero es un archivo de seed más una
  implementación de la interfaz.
- **Malo:** se pierde la verificación en compilación. Un código mal escrito en el seed no falla al compilar, falla al
  arrancar o al evaluar. Se compensa con el arranque (el seed corre al levantar) y con los golden cases.
- **Malo:** el arranque depende de que el catálogo esté cargado. En local lo resuelve el seed; un despliegue real
  necesita que esos datos existan antes de recibir tráfico.
- **Malo:** una modificación del catálogo y el incremento de `catalog_version` forman una sola operación lógica; las
  migraciones deben mantenerlas atómicas.
- **Riesgo:** una fila mal cargada produce un score plausible pero incorrecto, que es más difícil de notar que un
  error de compilación. Es la razón por la que los golden cases corren contra el catálogo real y no contra objetos
  construidos a mano en el test.

## Alternativas descartadas

| Alternativa | Razón del rechazo |
|-------------|-------------------|
| Dejarla en código, consolidada en menos enums | Reduce el ruido pero no resuelve ninguno de los tres problemas: sigue siendo deploy para corregir un peso, y el texto del prompt sigue mezclado con el cálculo |
| Configuración en YAML en vez de base | Mejor que código, pero deja la especificación fuera de la base donde ya vive todo lo demás del dominio, y no permite consultarla ni versionarla con los datos |
| Que el esquema consulte el repositorio él mismo | Menos plomería en el caso de uso, pero convierte al calculador en un componente que toca un origen de datos, que es exactamente lo que se quiso evitar |
