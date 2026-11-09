# Requerimientos y Dependencias del Proyecto

Este documento detalla los requerimientos del entorno, las dependencias directas y transitivas del proyecto **Franchise Reactive API** (`com.frank:franchises`), así como la generación de su árbol completo de resolución.

---

## 1. Requerimientos del Sistema (Runtime & Tooling)

| Herramienta | Versión Mínima Requerida | Propósito |
|-------------|--------------------------|-----------|
| **JDK (Java Development Kit)** | 21 (LTS) | Compilación y ejecución de la aplicación (Eclipse Temurin o compatible). |
| **Apache Maven** | 3.9+ | Gestor de construcción del ciclo de vida y resolución de artefactos. |
| **Docker Engine** | 24+ | Contenedorización de la API y de MongoDB en entorno local o cloud. |
| **Docker Compose** | v2+ | Orquestación multi-contenedor del stack local (`api` + `mongodb`). |
| **Terraform CLI** | >= 1.5.0 | Aprovisionamiento de infraestructura como código (MongoDB Atlas en AWS). |
| **Git** | 2.40+ | Control de versiones con convención Gitflow y commits semánticos. |

---

## 2. Dependencias Directas de Maven (`pom.xml`)

La aplicación utiliza **Spring Boot 3.3.5** como BOM (*Bill of Materials*) padre para garantizar la compatibilidad de versiones.

| Grupo / Artefacto | Versión | Alcance (*Scope*) | Propósito en el Proyecto |
|-------------------|---------|-------------------|--------------------------|
| `org.springframework.boot:spring-boot-starter-webflux` | `3.3.5` | `compile` | Framework web reactivo no bloqueante basado en Project Reactor y servidor Netty embebido. |
| `org.springframework.boot:spring-boot-starter-data-mongodb-reactive` | `3.3.5` | `compile` | Driver reactivo para MongoDB con soporte para `ReactiveMongoRepository`, `ReactiveMongoTemplate` y `ReactiveStreams`. |
| `org.springframework.boot:spring-boot-starter-validation` | `3.3.5` | `compile` | Validación declarativa de DTOs con Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Min`, `@NotNull`). |
| `org.springframework.boot:spring-boot-starter-actuator` | `3.3.5` | `compile` | Sondas operativas de salud y liveness (`/actuator/health`, `/actuator/health/liveness`) con exposición mínima (`show-details: never`). |
| `org.springdoc:springdoc-openapi-starter-webflux-ui` | `2.6.0` | `compile` | Generación automática de especificación OpenAPI 3.0 y UI interactiva Swagger en `/swagger-ui.html`. |
| `org.projectlombok:lombok` | `1.18.34` | `compile` (opcional) | Reducción de código repetitivo (`@Getter`, `@Builder`, `@Slf4j`, constructores). |
| `org.springframework.boot:spring-boot-starter-test` | `3.3.5` | `test` | Suite de pruebas: JUnit 5 Jupiter, Mockito, AssertJ, Hamcrest, JSONPath y `WebTestClient`. |
| `io.projectreactor:reactor-test` | `3.6.11` | `test` | Utilidades reactivas de testing (`StepVerifier`) para validar flujos asíncronos `Mono` y `Flux`. |

---

## 3. Principales Dependencias Transitivas Gestionadas

| Componente | Artefacto | Versión Resuelta | Rol |
|------------|-----------|------------------|-----|
| **Reactor Core** | `io.projectreactor:reactor-core` | `3.6.11` | Motor de programación reactiva no bloqueante. |
| **Reactor Netty** | `io.projectreactor.netty:reactor-netty-http` | `1.1.23` | Servidor HTTP reactivo de alto rendimiento. |
| **MongoDB Reactive Driver** | `org.mongodb:mongodb-driver-reactivestreams` | `5.0.1` | Driver asíncrono sobre el protocolo reactivo de MongoDB. |
| **Hibernate Validator** | `org.hibernate.validator:hibernate-validator` | `8.0.1.Final` | Motor de validación de Jakarta Validation 3.0.2. |
| **Spring Boot Actuator Autoconfigure** | `org.springframework.boot:spring-boot-actuator-autoconfigure` | `3.3.5` | Autoconfiguración de indicadores de salud reactivos (`ReactiveMongoHealthIndicator`, `LivenessStateHealthIndicator`). |
| **Jackson Databind** | `com.fasterxml.jackson.core:jackson-databind` | `2.17.2` | Serialización y deserialización JSON reactiva. |
| **JUnit 5 Jupiter** | `org.junit.jupiter:junit-jupiter` | `5.10.5` | Framework de pruebas unitarias y parametrizadas. |
| **Mockito** | `org.mockito:mockito-core` | `5.11.0` | Creación de mocks y stubs para pruebas en capas aisladas. |
| **AssertJ Core** | `org.assertj:assertj-core` | `3.25.3` | Aserciones fluidas para pruebas legibles. |

---

## 4. Árbol Completo de Dependencias

Para auditar o inspeccionar el árbol completo resuelto por Maven en local (excluido del control de versiones en `.gitignore` por ser un artefacto generado):

```powershell
mvn dependency:tree "-DoutputFile=dependencies.txt"
```
