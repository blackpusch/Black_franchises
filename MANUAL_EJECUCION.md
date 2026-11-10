# 🚀 Manual Paso a Paso de Ejecución Local — Franchises Reactive API

Esta guía explica cómo levantar, visualizar y probar localmente la API REST reactiva de Franquicias (`Spring Boot 3.3.5 WebFlux` + `MongoDB 7.0`).

---

## 1. ¿Cómo levantar el proyecto localmente?

Tienes **dos opciones** para ejecutar la aplicación desde la carpeta raíz del repositorio:

### Opción 1 (Recomendada): Todo con Docker Compose (API + MongoDB 7.0)

No requiere tener Java ni Maven instalados localmente; construye la imagen multi-etapa y levanta tanto MongoDB 7.0 como la API en contenedores con *healthcheck*:

```powershell
# 1. Ir a la carpeta del proyecto
cd C:\Desarrollo\franchises

# 2. Construir y levantar los contenedores en segundo plano
docker compose up -d --build
```

Para verificar que ambos contenedores (`franchises-mongodb` y `franchises-api`) ya estén en estado `healthy` (~15 segundos):

```powershell
docker compose ps
```

Para ver los logs de la API en tiempo real (opcional):

```powershell
docker compose logs -f franchises-api
```

---

### Opción 2: MongoDB en Docker y la API directo con Maven (`spring-boot:run`)

Si prefieres ejecutar la aplicación Java directamente en tu máquina (requiere **Java 21** y **Maven 3.9+**):

```powershell
# 1. Ir a la carpeta del proyecto
cd C:\Desarrollo\franchises

# 2. Levantar únicamente la base de datos MongoDB en Docker (127.0.0.1:27017)
docker compose up -d mongodb

# 3. Arrancar la API con Maven
mvn spring-boot:run
```

---

## 2. 🌐 ¿Dónde ver y probar el resultado en el navegador?

Una vez que la API esté arriba en el puerto `8080`, abre cualquiera de estos enlaces en tu navegador:

| Recurso | URL Local | ¿Para qué sirve? |
| :--- | :--- | :--- |
| **Swagger UI (Interactivo)** | **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)** | Interfaz visual donde puedes desplegar cada uno de los **11 endpoints**, hacer clic en **"Try it out"**, enviar peticiones JSON y ver las respuestas en vivo sin usar la terminal. |
| **Estado de Salud (Health)** | **[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)** | Verifica que la API y la conexión reactiva a MongoDB estén operativas (`{"status":"UP"}`). |
| **Liveness Probe** | **[http://localhost:8080/actuator/health/liveness](http://localhost:8080/actuator/health/liveness)** | Sonda de vida del proceso JVM (`{"status":"UP"}`). |
| **Readiness Probe** | **[http://localhost:8080/actuator/health/readiness](http://localhost:8080/actuator/health/readiness)** | Sonda de disponibilidad para recibir tráfico (`{"status":"UP"}`). |
| **OpenAPI 3.0 Spec (JSON)** | **[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)** | Especificación completa del contrato OpenAPI 3.0 en formato JSON. |

---

## 3. 💻 Prueba rápida paso a paso desde Terminal

### A) Desde Windows PowerShell 5.1 / PowerShell 7

> En Windows PowerShell 5.1, `curl` es un alias de `Invoke-WebRequest` y el parser nativo recorta las comillas dobles internas si pasas `-d '{...}'`. Por eso se usa `curl.exe` pasando el JSON por `stdin` con `-d "@-"`:

```powershell
# 1. Crear una franquicia (copia el "id" que devuelve la respuesta)
'{"name": "McDonalds"}' | curl.exe -s -X POST http://localhost:8080/api/v1/franchises -H "Content-Type: application/json" -d "@-"

# 2. Listar todas las franquicias
curl.exe -s http://localhost:8080/api/v1/franchises

# 3. Guardar el ID de la franquicia en una variable para los siguientes pasos
$fId = (curl.exe -s http://localhost:8080/api/v1/franchises | ConvertFrom-Json)[0].id

# 4. Agregar una sucursal a la franquicia
'{"name": "Sucursal Centro"}' | curl.exe -s -X POST "http://localhost:8080/api/v1/franchises/$fId/branches" -H "Content-Type: application/json" -d "@-"
$bId = ((curl.exe -s "http://localhost:8080/api/v1/franchises/$fId") | ConvertFrom-Json).branches[0].id

# 5. Agregar dos productos con stock a la sucursal
'{"name": "Big Mac", "stock": 50}' | curl.exe -s -X POST "http://localhost:8080/api/v1/franchises/$fId/branches/$bId/products" -H "Content-Type: application/json" -d "@-"
'{"name": "McFlurry", "stock": 30}' | curl.exe -s -X POST "http://localhost:8080/api/v1/franchises/$fId/branches/$bId/products" -H "Content-Type: application/json" -d "@-"
$pId = ((curl.exe -s "http://localhost:8080/api/v1/franchises/$fId") | ConvertFrom-Json).branches[0].products[0].id

# 6. Modificar el stock del primer producto a 120 unidades
'{"stock": 120}' | curl.exe -s -X PATCH "http://localhost:8080/api/v1/franchises/$fId/branches/$bId/products/$pId/stock" -H "Content-Type: application/json" -d "@-"

# 7. Consultar el producto con mayor stock por sucursal
curl.exe -s "http://localhost:8080/api/v1/franchises/$fId/top-stock-products"

# 8. [PLUS] Renombrar franquicia, sucursal y producto
'{"name": "McDonalds Colombia"}' | curl.exe -s -X PATCH "http://localhost:8080/api/v1/franchises/$fId/name" -H "Content-Type: application/json" -d "@-"
'{"name": "Sucursal Zona T"}' | curl.exe -s -X PATCH "http://localhost:8080/api/v1/franchises/$fId/branches/$bId/name" -H "Content-Type: application/json" -d "@-"
'{"name": "Big Mac Doble"}' | curl.exe -s -X PATCH "http://localhost:8080/api/v1/franchises/$fId/branches/$bId/products/$pId/name" -H "Content-Type: application/json" -d "@-"

# 9. Consultar el árbol actualizado de la franquicia por ID
curl.exe -s "http://localhost:8080/api/v1/franchises/$fId"

# 10. Eliminar un producto de la sucursal (devuelve 204 No Content)
curl.exe -i -X DELETE "http://localhost:8080/api/v1/franchises/$fId/branches/$bId/products/$pId"
```

---

### B) Desde Git Bash / WSL / Linux / macOS

```bash
# 1. Crear una franquicia
curl -X POST http://localhost:8080/api/v1/franchises \
  -H "Content-Type: application/json" \
  -d '{"name": "McDonalds"}'

# 2. Listar todas las franquicias
curl -X GET http://localhost:8080/api/v1/franchises
```

---

## 4. 🧪 Ejecutar la Suite de Pruebas Automatizadas (138 tests)

Para correr todas las pruebas unitarias y de contrato:

```powershell
mvn clean test
```

---

## 5. 🛑 ¿Cómo detener los contenedores al terminar?

```powershell
# Detener los contenedores conservando los datos de MongoDB en el volumen
docker compose down

# O detener y limpiar también el volumen de datos para empezar desde cero
docker compose down -v
```
