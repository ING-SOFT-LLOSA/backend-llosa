# Backend — Llosa Edificaciones

REST API construida con **Spring Boot 3.3 / Java 21** para la inmobiliaria Llosa Edificaciones. Gestiona el ciclo completo de ventas inmobiliarias: proyectos, activos, expedientes comerciales, pagos, citas y documentos, con autenticación delegada a Firebase y almacenamiento de archivos en Google Cloud Storage.

---

## Tabla de Contenidos

1. [Arquitectura del sistema](#arquitectura-del-sistema)
2. [Módulos y lógica de negocio](#módulos-y-lógica-de-negocio)
3. [Stack tecnológico](#stack-tecnológico)
4. [Despliegue en producción](#despliegue-en-producción)
5. [Desarrollo local](#desarrollo-local)
6. [Arquitectura de pruebas](#arquitectura-de-pruebas)
7. [Troubleshooting](#troubleshooting)

---

## Arquitectura del sistema

### Estilo arquitectónico

La aplicación sigue una **arquitectura en capas por módulo de negocio** (Layered + Modular), donde cada módulo encapsula sus propias entidades, repositorios, servicios y controladores. No existe dependencia entre módulos de negocio a nivel de código fuente; la comunicación se realiza exclusivamente a través de identificadores UUID.

```
com.llosa.backend
├── seguridad/      → Autenticación, usuarios, roles, funciones
├── proyecto/       → Proyectos, torres, pisos, activos inmobiliarios
├── comercial/      → Expedientes, etapas, requisitos, hitos de compra
├── pagos/          → Cronogramas de pago, cuotas, comprobantes
├── agenda/         → Citas, disponibilidades, integración Google Calendar
├── documentos/     → Documentos polimórficos en GCS
└── config/         → Seguridad, Firebase, GCS, OpenAPI
```

### Patrones de diseño aplicados

| Patrón | Dónde se aplica |
|--------|----------------|
| **Repository** | Acceso a datos vía `JpaRepository`; toda consulta personalizada vive en la interfaz de repositorio |
| **Service Layer** | Toda la lógica de negocio reside en `@Service`; los controladores solo orquestan request/response |
| **DTO** | Clases de entrada (`Request`) y salida (`Response`) separadas de las entidades JPA |
| **Factory** | Construcción de entidades complejas (p.ej. estructura física de un proyecto) |
| **Filter Chain** | `FirebaseTokenFilter` intercepta cada request para validar el token JWT antes de que llegue a Spring Security |
| **Strategy / Polimorfismo** | Documentos polimórficos: un `Documento` referencia cualquier entidad mediante `idReferencia` + `entidadReferencia` |
| **Stateless JWT** | Sin sesiones en servidor; el estado del usuario se reconstruye desde el token Firebase en cada request |

### Seguridad

- **Autenticación**: Firebase Admin SDK valida el `Bearer <token>` en cada request. El filtro `FirebaseTokenFilter` extrae el UID y popula el `SecurityContext` con un `FirebaseAuthenticationToken`.
- **Autorización**: RBAC granular con `@PreAuthorize`. Cada endpoint declara la función requerida (p.ej. `PROY_CREAR`, `DOCS_VER`, `CONTRATO_EDITAR`). Las funciones se persisten en base de datos y se asignan por rol.
- **Dominio corporativo**: Los usuarios de tipo EMPLEADO deben poseer un email `@llosaedificaciones.com`; de lo contrario el login es rechazado.

---

## Módulos y lógica de negocio

### Seguridad (`/api/auth`, `/api/users`, `/api/roles`)

Gestiona el registro y ciclo de vida de usuarios. Al crear un usuario, el sistema llama a Firebase para crear la cuenta y simultáneamente persiste los datos en PostgreSQL. La desactivación revoca los tokens en Firebase y marca el registro como inactivo.

- `GET /api/auth/me` — Devuelve el perfil completo del usuario autenticado (id, rol, lista de funciones permitidas).

### Proyectos (`/api/proyectos`)

Administra la cartera inmobiliaria de la empresa. Un proyecto contiene una estructura física jerárquica (Torre → Piso → Activo) que puede cargarse en bloque. Cada proyecto puede tener hitos de avance y un link de recorrido virtual. Los activos (departamentos, oficinas, etc.) tienen estado comercial y precio.

- Soporta precertificación EDGE/LEED como atributo de proyecto.

### Comercial (`/api/expedientes`)

Implementa el funnel de ventas. Un `UsuarioActivo` asocia a un cliente con un activo. La venta avanza por `EtapaExpediente` (CONTRATO, MINUTA, etc.). Cada etapa tiene `HitoProcesoCompra` y `RequisitoDocumental` que el gestor debe completar para avanzar.

### Pagos (`/api/cronogramas`, `/api/pagos`)

Maneja los cronogramas de cuotas asociados a un expediente. Cada `Pago` tiene estado, monto programado vs. pagado, fecha de vencimiento y puede llevar adjunto un comprobante (referenciado como UUID de documento). El endpoint `PATCH /estado` gestiona la transición del flujo de cobro.

### Agenda (`/api/citas`)

Permite agendar citas entre gestores y clientes, vinculadas a un activo específico. Si el usuario tiene conectada su cuenta de Google, el evento se sincroniza con Google Calendar vía OAuth2 (refresh token almacenado por usuario). Si no, la cita se gestiona internamente con soporte de reprogramación tipo When2Meet.

### Documentos (`/api/documentos`)

Servicio transversal de almacenamiento. Cualquier módulo puede adjuntar documentos a sus entidades sin acoplamiento directo. Los archivos se suben a Google Cloud Storage y los metadatos se persisten en PostgreSQL. La descarga se realiza mediante **Signed URLs** temporales, garantizando acceso controlado sin exponer las credenciales de GCS.

- Validación de tipo MIME con **Apache Tika** (no confía en la extensión del archivo).
- Límite: 10 MB por archivo.

---

## Stack tecnológico

| Componente | Versión | Rol |
|-----------|---------|-----|
| Spring Boot | 3.3.5 | Framework principal |
| Java | 21 LTS | Runtime |
| PostgreSQL | 16 | Base de datos principal |
| Firebase Admin SDK | 9.3.0 | Autenticación y gestión de usuarios |
| Google Cloud Storage | (via firebase-admin) | Almacenamiento de archivos |
| Google Calendar API | v3 | Sincronización de citas |
| Flyway | (BOM) | Migraciones de base de datos |
| SpringDoc OpenAPI | 3.0.0 | Documentación Swagger |
| Apache Tika | 2.9.1 | Detección de tipo MIME |
| Testcontainers | 1.20.4 | Tests de integración con PostgreSQL real |
| Docker | — | Contenedorización |

---

## Despliegue en producción

### Prerrequisitos de infraestructura

Antes de dockerizar la aplicación se deben tener configurados dos servicios de Google:

#### 1. Firebase (Autenticación y GCS)

1. Crear un proyecto en [Firebase Console](https://console.firebase.google.com).
2. Activar **Authentication** con el proveedor Email/Password.
3. En **Project Settings → Service Accounts**, generar una nueva clave privada. Se descargará un archivo JSON.
4. En **Google Cloud Console** (el mismo proyecto subyacente), ir a **Cloud Storage** y crear un bucket (p.ej. `llosa-bucket`). Dar a la cuenta de servicio el rol **Storage Object Admin**.
5. Guardar el JSON como `firebase-service-account.json` — este archivo actúa como credencial tanto para Firebase Admin SDK como para GCS.

> **Nunca** subir `firebase-service-account.json` al repositorio. Está en `.gitignore`.

#### 2. Variables de entorno requeridas

Crear un archivo `.env` en la raíz del proyecto (no commitear):

```env
# Base de datos
POSTGRES_DB=llosa_db
POSTGRES_USER=llosa_user
POSTGRES_PASSWORD=strong_password_here
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/llosa_db
SPRING_DATASOURCE_USERNAME=llosa_user
SPRING_DATASOURCE_PASSWORD=strong_password_here

# Firebase / GCS
FIREBASE_CREDENTIALS_PATH=/app/secrets/firebase-service-account.json
GCS_BUCKET_NAME=llosa-bucket

# Google OAuth2 (para Google Calendar)
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret

# Dominio corporativo de empleados
CORPORATE_DOMAIN=llosaedificaciones.com

# Spring
SPRING_PROFILES_ACTIVE=prod
```

#### 3. Activar Flyway para producción

En `application.properties` (o mediante variable de entorno), asegurarse de que Flyway esté habilitado en el perfil `prod`. Esto garantiza que cada despliegue aplique las migraciones pendientes de forma controlada, evitando inconsistencias de esquema:

```properties
# Activar Flyway en producción
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# Deshabilitar el DDL automático de Hibernate en producción
spring.jpa.hibernate.ddl-auto=validate
```

> Con `ddl-auto=validate` + `flyway.enabled=true`, Hibernate verifica que el esquema coincide con las entidades pero nunca lo modifica. Flyway es el único responsable de los cambios de esquema.

### Construir y ejecutar con Docker

```bash
# 1. Copiar credenciales Firebase al directorio de secretos
mkdir -p secrets
cp /ruta/a/firebase-service-account.json secrets/

# 2. Construir la imagen
docker build -t llosa-backend:latest .

# 3. Levantar servicios (PostgreSQL + backend)
docker compose up -d

# 4. Verificar estado
docker compose ps
curl http://localhost:8080/actuator/health
```

#### Estructura del Dockerfile (multi-stage)

```
Stage 1 (build): maven:3.9.8-eclipse-temurin-21-alpine
  └── ./mvnw clean package -DskipTests → target/app.jar

Stage 2 (runtime): eclipse-temurin:21-jre-alpine
  ├── Usuario no-root: appuser (UID 1001)
  ├── Puerto expuesto: 8080
  ├── Secretos montados: /app/secrets/
  └── Healthcheck: curl /actuator/health
```

#### docker-compose.yml — servicios principales

```yaml
services:
  db:
    image: postgres:16-alpine
    volumes:
      - pgdata:/var/lib/postgresql/data   # Datos persistentes entre reinicios
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "${POSTGRES_USER}"]

  backend:
    build: .
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - ./secrets:/app/secrets:ro         # Credenciales Firebase (solo lectura)
    env_file: .env
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]

volumes:
  pgdata:                                  # Volumen nombrado para persistencia de BD
```

### API Docs

Una vez levantado el servidor, la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui/
http://localhost:8080/v3/api-docs/
```

Todos los endpoints requieren `Authorization: Bearer <firebase-token>` excepto `/actuator/health` y los endpoints de documentación.

---

## Desarrollo local

### Prerrequisitos

- Java 21
- Docker (para PostgreSQL y Testcontainers)
- Maven 3.9+

### Pasos

```bash
# 1. Copiar credenciales Firebase
cp firebase-service-account.json src/main/resources/

# 2. Iniciar PostgreSQL local
docker compose up -d db

# 3. Compilar
./mvnw clean package -DskipTests

# 4. Ejecutar tests
./mvnw test

# 5. Levantar aplicación
./mvnw spring-boot:run
```

API disponible en `http://localhost:8080`.

> En desarrollo, `spring.flyway.enabled=false` y `ddl-auto=update` están activos por defecto para facilitar la iteración rápida.

---

## Arquitectura de pruebas

### Niveles de prueba

| Nivel | Anotación | Alcance | Velocidad |
|-------|-----------|---------|-----------|
| **Unitario** | `@ExtendWith(MockitoExtension)` | Lógica de servicio aislada, sin BD ni HTTP | ~200 ms |
| **Controlador** | `@WebMvcTest` | HTTP + Security, sin BD real | ~500 ms |
| **Integración** | `@SpringBootTest` + Testcontainers | Stack completo con PostgreSQL 16 real | ~30-40 s (1ª vez) |
| **Estrés** | `@Tag("stress")` | Concurrencia y race conditions | Manual |

### Utilidades de test personalizadas

- **`@WithFirebaseAuth`** — Anotación que simula un usuario Firebase autenticado sin necesidad de token real ni `MockMvc.with()`.
- **`SecurityTestConfiguration`** — Resuelve `@AuthenticationPrincipal FirebaseAuthenticationToken` en tests `@SpringBootTest`.
- **`PostgresTestContainerConfig`** — Levanta PostgreSQL 16 vía Testcontainers con `@ServiceConnection` (binding automático Spring Boot 3.1+).

### Cobertura de casos

```
Repositorio:      9 tests  — findBy, existsBy, constraints, seeds Flyway
AuthService:      5 tests  — login, cuenta suspendida, dominio corporativo
UsuarioService:   9 tests  — create, cambiarEstado, asignarRol
Controladores:   10 tests  — AuthController, UsuarioController
FirebaseFilter:   4 tests  — token válido/inválido, sin header
Integración E2E: 11 tests  — flujo completo: crear → asignar rol → login → desactivar
```

### Comandos de ejecución

```bash
# Suite completa
./mvnw test

# Solo unitarios (sin Docker)
./mvnw test -Dtest="AuthServiceTest,UsuarioServiceTest,RolServiceTest,FirebaseTokenFilterTest,AuthControllerTest,UsuarioControllerTest"

# Solo integración (requiere Docker)
./mvnw test -Dtest="*RepositoryTest,SeguridadIntegrationTest"

# Test individual con stack trace
./mvnw test -Dtest=UsuarioRepositoryTest#findByFirebaseUuid_devuelveUsuarioExistente -e
```

---

## Troubleshooting

### Docker API version mismatch (Testcontainers)

**Error:** `client version 1.32 is too old. Minimum supported API version is 1.40`

**Solución:** Ya configurado en `pom.xml` vía Surefire:
```xml
<systemPropertyVariables>
  <api.version>1.41</api.version>
</systemPropertyVariables>
```

### Testcontainers no encuentra Docker

```bash
docker ps                                        # ¿Docker corriendo?
export DOCKER_HOST=unix:///var/run/docker.sock   # Si es necesario
```

### Flyway falla en el primer despliegue sobre BD existente

Si la base de datos tiene tablas previas creadas por Hibernate (sin historial Flyway), ejecutar una sola vez:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

Esto marca el estado actual como línea base y permite que Flyway aplique solo las migraciones nuevas.

### Signed URL de GCS devuelve 403

- Verificar que la cuenta de servicio tiene el rol **Storage Object Viewer** (o superior) en el bucket.
- Confirmar que `GCS_BUCKET_NAME` apunta al bucket correcto.
- Las Signed URLs expiran; regenerarlas si han pasado más de 15 minutos.

---

**Última actualización:** 2026-06-30 | **Estado de tests:** 71/71 pasados
