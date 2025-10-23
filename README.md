# Shop Backend

Backend de inventario para una tienda construido con Java 17, Spring Boot y PostgreSQL.

## Requisitos

- Docker y Docker Compose
- Java 17
- Maven 3.9+

## Arranque rápido

```bash
docker compose up -d
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Una vez en marcha:

- `GET http://localhost:8080/health`
- `GET http://localhost:8080/api/products`

## Configuración de entornos

### Desarrollo (`dev`)

- Base de datos: Postgres del `docker-compose.yml` (host `localhost`, puerto `5432`, db `shopdb`, user/pass `app`).
- Flyway ejecuta migraciones al iniciar la aplicación.
- `spring.jpa.hibernate.ddl-auto=validate` para validar el esquema.
- Zona horaria UTC configurada para JDBC.

### Pruebas (`test`)

- Tests de integración usan [Testcontainers](https://testcontainers.com/) con Postgres 16.
- Propiedades inyectadas dinámicamente desde el contenedor.
- Flyway aplica migraciones antes de correr los tests.

### Producción (`prod`)

Configura las siguientes variables de entorno antes de ejecutar el `jar`:

- `DB_URL` (por ejemplo `jdbc:postgresql://db:5432/shopdb`)
- `DB_USER`
- `DB_PASS`
- `SPRING_PROFILES_ACTIVE=prod`

## Comandos útiles

| Acción | Comando |
| --- | --- |
| Levantar Postgres para desarrollo | `docker compose up -d` |
| Detener servicios | `docker compose down` |
| Limpiar datos de Postgres | `docker compose down -v` |
| Ejecutar la aplicación en dev | `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` |
| Compilar | `mvn -B -ntp package` |
| Ejecutar tests (incluye Testcontainers) | `mvn -B -ntp test` |
| Ejecutar verificación completa | `mvn -B -ntp verify` |

## Endpoints principales

### `GET /api/products`
Lista productos con paginación y filtros opcionales `name` (contiene) y `sku` (exacto).

### `GET /api/products/{id}`
Obtiene un producto por su identificador.

### `POST /api/products`
Crea un producto. Ejemplo:

```bash
curl -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{
    "sku": "SKU-123",
    "name": "Gorra",
    "description": "Gorra negra",
    "priceCents": 1999,
    "quantity": 10
  }'
```

### `PUT /api/products/{id}`
Actualiza todos los campos del producto (idempotente).

### `PATCH /api/products/{id}/adjust-quantity`
Ajusta el stock de forma atómica.

```bash
curl -X PATCH http://localhost:8080/api/products/1/adjust-quantity \
  -H 'Content-Type: application/json' \
  -d '{ "delta": -3 }'
```

### `DELETE /api/products/{id}`
Elimina un producto.

### `GET /health`
Respuesta de estado simple de la aplicación.

## Manejo de errores

Las respuestas de error siguen la estructura:

```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "A product with sku 'SKU-123' already exists",
  "path": "/api/products"
}
```

- Validaciones de payload retornan 400.
- Recursos no encontrados retornan 404.
- Conflictos de negocio (ej. SKU duplicado, stock negativo) retornan 422.
- Errores no controlados retornan 500.

## Migraciones de base de datos

Las migraciones de Flyway se encuentran en `src/main/resources/db/migration`. Se ejecutan automáticamente al iniciar la aplicación en cualquier perfil.

## Dev Container / Codespaces

Incluye configuración en `.devcontainer/` para abrir el proyecto en VS Code Dev Containers o GitHub Codespaces. El contenedor expone los puertos 8080 y 5432 y prepara Maven (descarga dependencias con `mvn dependency:go-offline`). Usa la red del host para acceder al Postgres levantado por Docker Compose.

## Integración continua

El flujo de GitHub Actions (`.github/workflows/ci.yml`) ejecuta `mvn -B -ntp verify` en cada push y pull request.

## Despliegue en producción

1. Construye el artefacto:
   ```bash
   mvn -B -ntp clean package
   ```
2. Copia el `jar` generado (`target/shop-backend-0.0.1-SNAPSHOT.jar`) al servidor.
3. Configura variables de entorno (`DB_URL`, `DB_USER`, `DB_PASS`, `SPRING_PROFILES_ACTIVE=prod`).
4. Ejecuta:
   ```bash
   java -jar target/shop-backend-0.0.1-SNAPSHOT.jar
   ```

También puedes construir una imagen Docker personalizada que apunte a tu Postgres gestionado en producción.

## Verificación

- `docker compose up -d` levanta Postgres y pasa el healthcheck.
- Con `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` el endpoint `/health` devuelve 200.
- El flujo CRUD completo (POST, GET, PUT, PATCH, DELETE) funciona según los criterios definidos y valida SKU duplicados con respuesta 422.
- `mvn test` ejecuta los tests de integración con Testcontainers.
- La acción de CI ejecuta `mvn verify`.
