# Dominio — Evaluación de Severidad de Vulnerabilidades

## La pregunta que responde el subdominio

*¿Qué tan grave es esta vulnerabilidad **para esta aplicación**?*

El matiz está en las últimas tres palabras. El CVSS Base Score describe la vulnerabilidad en abstracto, y por eso un
9.8 en un batch aislado y un 9.8 en un servicio expuesto con datos personales se ven iguales aunque no lo sean.
Este dominio existe para separarlos.

---

## Los tres insumos

| Insumo | De dónde sale | Confiabilidad |
|---|---|---|
| **Vulnerabilidad** — identificador y descripción | Del caller | Texto no confiable: cualquiera puede escribir un advisory |
| **Baseline** — el vector de severidad base | Del caller, o derivado por el modelo | Dato duro si lo manda el caller; inferencia si no |
| **Contexto** — exposición, datos, criticidad, runtime, controles | Declarado por el caller | Auto-declarado: registrado como tal |

---

## El resultado

Una `VulnerabilityEvaluation`: un registro auditable e inmutable con dos scores —el base y el contextualizado—, el
delta entre ambos, la justificación métrica por métrica, y la procedencia de cada insumo.

Se persiste **siempre**, no solo cuando es grave. Un score asistido por IA que no se puede recuperar y revisar
después no es auditable, y la auditabilidad es lo que hace aceptable la asistencia.

---

## Cómo se organiza en el código

- **`domain/model`** — solo datos: la entidad, sus embeddables, los value objects y los enums. La única lógica que
  vive ahí es la validación de `ModelSeverityProposal`, porque ese value object *es* la traducción de la respuesta de
  un origen de datos y su razón de existir es no poder construirse en estado inválido.
- **`domain/service`** — el comportamiento: el esquema de scoring (`SeverityScheme` y su implementación), la política
  de confianza y revisión, el catálogo, y el caso de uso.
- **`datasource/llm`** — el modelo de lenguaje, tratado como un origen de datos más.
- **`datasource/repository`** — la infraestructura propia: evaluaciones y catálogos.

El detalle está en [Arquitectura](/architecture/) y el diseño completo en el [ADD](/add/).

---

## Las tres reglas que definen el dominio

1. **El número lo calcula el dominio, no el modelo.** El modelo elige valores de un vocabulario cerrado; la
   aritmética es determinista y está verificada contra la especificación publicada.
2. **Abstenerse es una respuesta válida.** Cuando el contexto no da evidencia, `X` (Not Defined) es preferible a
   adivinar, y baja la confianza reportada.
3. **El resultado es soporte a la decisión, nunca una autorización.** `advisory` viaja en cada respuesta y
   `review_required` marca los casos donde equivocarse cuesta más.

---

## Enlaces

- [Glosario](/domain/glossary) — términos del dominio y de la arquitectura
- [Uso de IA y sus límites](/architecture/ai-usage-and-limits) — qué puede salir mal y qué se hace al respecto
- [Endpoints](/architecture/endpoints) — el contrato
- [Modelo de datos](/architecture/data-model) — cómo se persiste
