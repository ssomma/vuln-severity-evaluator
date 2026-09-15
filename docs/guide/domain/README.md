# Dominio — Evaluación de Severidad de Vulnerabilidades

**vuln-severity-evaluator** todavía no tiene Services ni modelos implementados,
por lo que su dominio de negocio preciso no está fijado en código. El nombre
del proyecto y sus dependencias actuales (`spring-ai-starter-model-openai`,
`spring-boot-starter-data-jpa`) sugieren que el propósito es **evaluar la
severidad de vulnerabilidades** (por ejemplo, hallazgos de seguridad o CVEs)
con asistencia de un modelo de lenguaje, pero esto debe confirmarse y
completarse a medida que se implemente el primer subdominio real.

> Este repositorio es greenfield. Esta sección es un placeholder intencional
> (no un stub vacío): describe el marco esperado del dominio; debe
> reemplazarse por la descripción real del negocio en cuanto exista el primer
> `Service`.

---

## Cómo se organizará el dominio en el código

Según las capas definidas en `ArchitectureTest`
(ver [Arquitectura](/architecture/)), cada subdominio se modelará como:

```
domain/
├── model/        ← entidades JPA (@Entity)
└── service/      ← lógica de negocio (@Service)
```

con su acceso a datos en `datasource/repository/` (`@Repository`) y su
superficie HTTP en `presentation/controller/` (`@Controller`).

---

## Cómo documentar el primer subdominio real

Cuando se implemente el primer `Service` (por ejemplo, un scoring de
severidad tipo CVSS asistido por IA), documentar acá:

1. Qué pregunta de negocio responde el subdominio (¿qué se evalúa? ¿con qué
   insumos? ¿qué escala de severidad se usa?).
2. Sus modelos y su significado (entidades `@Entity` en `domain/model`).
3. El contrato del Service (qué expone, a quién, y qué rol cumple Spring AI /
   OpenAI en la evaluación).
4. Los términos nuevos → agregarlos al [Glosario](/domain/glossary).

---

## Enlaces

- [Glosario](/domain/glossary) — términos del dominio y de la arquitectura
- [Arquitectura](/architecture/) — cómo el dominio encaja en las capas
- [Modelo de capas](/architecture/layering-model) — por qué el dominio es el núcleo
