# vuln-severity-evaluator

Servicio backend en Java / Spring Boot para evaluar la severidad de vulnerabilidades.

## Requisitos

- JDK compatible con el proyecto
- No requiere instalar Gradle: usar el wrapper incluido (`./gradlew`)

## Cómo correr la aplicación

```bash
./gradlew build
./gradlew bootRun
```

## Cómo correr los tests

```bash
./gradlew test
```

## Documentación (Docsify)

La documentación viva del proyecto vive en `docs/guide/` y se navega con
[Docsify](https://docsify.js.org/).

### Cómo levantarla localmente

```bash
npx docsify-cli serve docs/guide
```

Por defecto queda disponible en `http://localhost:3000`.
