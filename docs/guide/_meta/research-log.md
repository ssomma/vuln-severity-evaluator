# Proceso de investigación

Qué hubo que investigar para tomar cada decisión, y qué salió de cada lectura. No es una bibliografía: es el rastro
de dónde el diseño cambió por haber mirado algo.

---

## 1. ¿Qué versión de CVSS, y por qué importa la pregunta?

**Qué se miró:** la especificación de CVSS v3.1 (sección 7, las fórmulas Base y Environmental) y el estado de
cobertura de vectores v4.0 en feeds públicos.

**Qué salió:** dos hallazgos, uno esperado y uno que cambió el diseño.

El esperado: las métricas **Environmental** —Security Requirements y Modified Base Metrics— existen exactamente para
el problema del desafío, re-puntuar según el entorno. No hacía falta inventar una escala.

El que cambió el diseño: v4.0 es más expresivo, pero su cobertura de vectores en feeds todavía es parcial. Eso
significa que en muchos CVEs el modelo tendría que derivar el Base completo — justo la parte que debería ser el ancla
dura. La conclusión no fue "usemos v3.1" sino algo más fuerte: **el valor del sistema depende de que el baseline sea
un dato y no una inferencia**. De ahí que `baseline_vector` sea opcional pero preferido, y que derivarlo degrade la
confianza. → [ADR-0001](/adr/0001-cvss-31-behind-a-scheme-interface)

## 2. ¿Cómo se rompe un sistema que usa un LLM sobre texto de terceros?

**Qué se miró:** OWASP Top 10 for LLM Applications, en particular LLM01 (Prompt Injection) y LLM09 (Overreliance).

**Qué salió:** la descripción de una vulnerabilidad es entrada **atacante-influenciable** — cualquiera puede escribir
un advisory. Las mitigaciones habituales (delimitar, instruir que se ignore) son necesarias pero no suficientes,
porque dependen de que el modelo obedezca.

Lo que se adoptó en cambio fue estructural: **restringir el espacio de salida**. Si el modelo solo puede responder
valores de un conjunto finito, una inyección exitosa como máximo cambia una elección acotada y visible, nunca produce
un número arbitrario. Eso derivó en la decisión central del diseño: el modelo elige métricas, el dominio calcula. →
[ADR-0002](/adr/0002-language-model-as-datasource)

De LLM09 salió el corolario: `advisory`, `confidence` y `review_required` en cada respuesta.

## 3. ¿Cómo se pasa texto no confiable a una plantilla de prompt?

**Qué se miró:** la API de `ChatClient` de Spring AI 2.0.1, inspeccionando el jar resuelto en vez de confiar en la
memoria.

**Qué salió:** `system()` y `user()` aceptan un `Resource` con parámetros. La diferencia relevante es que los valores
de parámetro se **sustituyen**, no se re-parsean. Así que una descripción con sintaxis de plantilla entra como dato.
Si en cambio se concatenara el texto en la plantilla, se sumaría inyección de plantilla encima de inyección de
prompt. Decidió cómo se arma el prompt.

## 4. ¿Dónde termina la configuración y empieza el código?

**Qué se miró:** el propio código, después de una revisión que señaló sobreingeniería.

**Qué salió:** la especificación CVSS estaba escrita como ocho enums de coeficientes. Es una tabla publicada escrita
como Java: corregir un peso mal transcripto era un deploy, y el texto que lee el modelo quedaba enterrado en un
archivo de 600 líneas junto a la aritmética. Moverla a base redujo `Cvss31` a un tercio y, como efecto lateral, hizo
que los vectores oficiales validen también los coeficientes cargados. →
[ADR-0005](/adr/0005-scoring-specification-as-data)

## 5. Si el LLM no es determinista, ¿cómo lo es el servicio?

**Qué se miró:** el propio diseño, a partir de una pregunta de revisión.

**Qué salió:** el hallazgo más incómodo. `temperature: 0.0` reduce la variación pero ningún proveedor garantiza
salidas idénticas, así que el score contextual podía diferir entre llamadas. Lo único que daba estabilidad era un
caché en memoria con TTL — determinismo de mejor esfuerzo, que se pierde al expirar y al reiniciar, justo cuando
alguien pregunta por qué cambió el número.

La corrección fue buscar la evaluación **por huella en la base** antes de preguntarle al modelo. De paso eliminó tres
piezas (dependencia de caché, su configuración y un decorator). →
[ADR-0006](/adr/0006-determinism-by-persistence)

## 6. ¿Los guardrails miden lo que dicen medir?

**Qué se miró:** `MethodComplexityTest` en ejecución, después de un resultado sospechoso: reportó una sola violación
donde claramente había más candidatos.

**Qué salió:** JavaParser rechazaba los records con el nivel de lenguaje por defecto. **12 de 45 archivos no se
parseaban y el gate los salteaba en silencio**, además de mis-reportar los que sí. Un guardrail que no puede leer el
código pasa en verde midiendo nada.

Se corrigió fijando el nivel de lenguaje y, sobre todo, **haciendo que el test falle si un archivo no se puede
parsear**. La lección general: un guardrail necesita fallar cuando no puede hacer su trabajo, no solo cuando
encuentra una violación.

La misma revisión encontró que `areAnnotatedWith(Controller.class)` no detecta `@RestController` (es meta-anotación),
y que el regex de naming era una *character class* y no una alternación — dos reglas que la documentación afirmaba
enforcear y no enforceaban.

## 7. ¿Qué exige la plataforma en materia de desarrollo seguro?

**Qué se miró:** las reglas de seguridad de MercadoLibre para Java y las mitigaciones por CWE aplicables (CWE-20,
209, 532, 798, 841, 862, 89, 918, 1390).

**Qué salió:** la mayoría se aplicó directo. La decisión no obvia fue sobre authn/authz: el camino correcto es Access
Groups más el SDK de autorización de plataforma, y **nada de eso está disponible fuera de ella**. La conclusión fue
no implementar un esquema propio: un auth casero en un servicio de seguridad sería peor que declarar la dependencia y
documentar el camino. Es una ausencia deliberada, registrada como tal.
