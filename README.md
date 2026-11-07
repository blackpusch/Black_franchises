# Franchises API

API REST reactiva para la gestión de franquicias, sucursales y productos (nombre y stock), construida con Spring Boot 3 y arquitectura no bloqueante (WebFlux + Reactive MongoDB).

## 🚀 Stack Tecnológico

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3.5
- **Enfoque Reactivo**: Spring WebFlux & Project Reactor
- **Persistencia**: Spring Data MongoDB Reactive
- **Validación**: Spring Boot Starter Validation
- **Documentación API**: Springdoc OpenAPI WebFlux UI (Swagger UI)
- **Utilidades**: Project Lombok
- **Pruebas**: Spring Boot Starter Test & Reactor Test
- **Gestor de Construcción**: Apache Maven

## 📁 Estructura del Proyecto

El proyecto sigue una arquitectura por capas reactiva:

```text
com.frank.franchises
├── config/         # Configuraciones de la aplicación (OpenAPI, Mongo, etc.)
├── controller/     # Controladores REST reactivos
├── domain/         # Modelos de dominio y documentos de MongoDB
├── dto/            # Data Transfer Objects (Requests / Responses)
├── exception/      # Manejo global de excepciones y errores de validación
├── repository/     # Repositorios reactivos (ReactiveMongoRepository)
└── service/        # Interfaces y servicios de lógica de negocio reactiva
```

## ⚙️ Configuración y Variables de Entorno

La conexión a la base de datos MongoDB está parametrizada mediante variable de entorno:

| Variable | Descripción | Valor por Defecto |
| :--- | :--- | :--- |
| `MONGODB_URI` | URI de conexión a la base de datos MongoDB | `mongodb://localhost:27017/franchises` |

## 🛠️ Compilación y Pruebas

Para compilar el proyecto y ejecutar los tests:

```bash
mvn clean verify
```

O en modo silencioso:

```bash
mvn -q clean verify
```

Para levantar la aplicación:

```bash
mvn spring-boot:run
```

## 📖 Documentación OpenAPI / Swagger

Una vez iniciada la aplicación, la documentación interactiva estará disponible en:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Docs: `http://localhost:8080/v3/api-docs`
