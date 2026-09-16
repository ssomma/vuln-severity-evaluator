# ADR-0004: Granularidad del modelo por dominio, no por clase

| Campo    | Valor |
|----------|-------|
| Status   | accepted |
| Fecha    | 2026-09-15 |
| Contexto | Convención que condiciona todo el código de este repositorio |

## Contexto

Aplicar "responsabilidad única" clase por clase, de forma literal, produce una explosión de clases satélite: una
fábrica al lado de cada objeto, un validador al lado de cada estructura, un mapper por cada conversión. El resultado
formalmente cumple SOLID y es difícil de leer: seguir un flujo simple requiere abrir diez archivos, y cada clase
satélite obliga a exponer públicamente detalles que solo ella usa.

El caso más claro acá es CVSS v3.1. Repartido en `Cvss31BaseVector`, `Cvss31EnvironmentalVector`,
`Cvss31Calculator`, `SeverityScore` y ocho enums sueltos, la aritmética queda separada de los pesos que necesita, y
los pesos tienen que hacerse visibles para que un colaborador externo los use. Son una sola cosa: una
especificación cerrada.

## Decisión

La responsabilidad se lee **por dominio**, no por clase. Una clase cubre un dominio abarcativo pero claramente
delimitado y absorbe lo que le pertenece, con tres límites que la mantienen honesta:

1. **Un tipo usado por varias clases no puede estar anidado.** Si dos clases lo necesitan, es un tipo de primer
   nivel. Lo anidado es lo que una sola clase usa, y va declarado `private` cuando el lenguaje lo permite — así el
   compilador prueba que no está compartido.
2. **Las interfaces no llevan records adentro.** Los datos que una interfaz intercambia son tipos propios.
3. **Lo que calcula no vive en el modelo.** El modelo son datos. El comportamiento va a `domain/service`: como
   `@Service` si es un caso de uso, como `@Component` si es colaboración interna que presentation no consume. Vale
   también para el agregado: recibe su veredicto ya decidido en vez de calcularlo. La excepción es un value object
   cuya razón de existir es traducir la respuesta de un origen de datos y no poder construirse en estado inválido.
4. **Un tipo se duplica solo cuando tiene que diferir.** El repositorio ya trata la entidad y el modelo de dominio
   como la misma clase; el mismo criterio vale para los bordes HTTP. `Vulnerability` y `ApplicationContext` llevan
   sus propias constraints y son el request; la respuesta carga `SeverityScore`, `MetricChoice` y `Provenance` tal
   cual. Solo se abstrae el agrupamiento, que es lo único genuinamente de presentación.

Además, los factory methods estáticos se llaman `create<ClassName>` y viven en la clase que construyen, no en una
fábrica aparte. Y donde una utilidad de Spring ya normaliza una comprobación (`StringUtils.hasText`,
`CollectionUtils.isEmpty`, `Assert`), se usa en vez de escribir un método privado de guarda.

El contrapeso que impide que esto degenere en clases gigantes es `MethodComplexityTest`, con cinco métricas y
**tolerancia cero**: complejidad ciclomática ≤ 5, nesting ≤ 3, ≤ 5 parámetros, ≤ 32 statements y ≤ 64 líneas por
método. Clases abarcativas, métodos chicos.

## Consecuencias

- **Bueno:** un dominio se lee en un archivo. `Cvss31` contiene sus métricas, sus pesos, su parseo y sus fórmulas, y
  nada de eso se filtra al resto del sistema.
- **Bueno:** desaparecen los objetos que pueden existir en estado inválido esperando que alguien los valide.
  `ModelSeverityProposal` se valida al construirse.
- **Bueno:** los bordes dejaron de tener 6 records espejo y 6 mappers que repetían los mismos campos.
- **Bueno:** cada clase tiene una razón para cambiar: cambia la especificación CVSS → cambia `Cvss31`; cambia el
  contrato HTTP → cambian los records de request/response; cambian los umbrales de revisión → cambia
  `EvaluationPolicy`.
- **Malo:** los archivos son más largos y un lector acostumbrado a una clase por concepto necesita orientarse.
- **Malo:** la regla "no anidar lo compartido" obliga a mover un tipo de adentro hacia afuera en cuanto aparece el
  segundo consumidor. Es una refactorización chica pero recurrente.
- **Riesgo:** sin el presupuesto de complejidad enforced, esta convención derivaría en clases inmanejables. Las dos
  decisiones se sostienen juntas; relajar el guardrail invalidaría esta ADR.

## Alternativas descartadas

| Alternativa | Razón del rechazo |
|-------------|-------------------|
| Una clase por concepto (fábricas, validadores y mappers separados) | Cumple SOLID en la letra y obliga a exponer detalles internos para que el satélite los use; el flujo se vuelve ilegible |
| Todo anidado dentro del dominio que lo origina | Más compacto, pero un tipo compartido anidado fuerza a los consumidores a importar la clase contenedora y acopla dominios que no se conocen |
| Sin convención, criterio caso por caso | Sin una regla escrita cada PR renegocia la granularidad, y el resultado es un código con dos estilos mezclados |
