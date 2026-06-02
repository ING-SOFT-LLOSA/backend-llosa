# Backend Llosa Edificaciones

REST API Spring Boot 4 / Java 21 para la inmobiliaria Llosa Edificaciones. Implementa autenticación via Firebase, gestión de usuarios, roles y funciones, con un sistema completo de pruebas unitarias, de integración y estrés, y análisis de calidad con SonarQube.

---

## 🚀 Inicio rápido

### Prerrequisitos
- **Java 21**
- **Docker** (para PostgreSQL y Testcontainers)
- **Maven 3.9+**

### Configuración
```bash
# 1. Copiar credenciales Firebase
cp firebase-service-account.json src/main/resources/

# 2. Iniciar PostgreSQL (requerido para tests e integración)
docker compose up -d

# 3. Compilar
./mvnw clean package -DskipTests

# 4. Ejecutar tests
./mvnw test

# 5. Ejecutar aplicación localmente
./mvnw spring-boot:run
```

API disponible en `http://localhost:8080`.

---

## 🔨 Compilar y Ejecutar

### Compilación sin tests
```bash
./mvnw clean package -DskipTests
```

### Compilación con tests (suite completa)
```bash
./mvnw clean package
```

### Ejecutar solo tests
```bash
./mvnw test
```

### Ejecutar aplicación localmente
```bash
./mvnw spring-boot:run
```

### Ejecutar solo tests unitarios (sin Testcontainers, ~5s)
```bash
./mvnw test -Dtest="AuthServiceTest,UsuarioServiceTest,RolServiceTest,FirebaseTokenFilterTest,AuthControllerTest,UsuarioControllerTest"
```

### Ejecutar solo tests de integración (con Testcontainers, ~30s primera ejecución)
```bash
./mvnw test -Dtest="*RepositoryTest,SeguridadIntegrationTest"
```

### Ejecutar tests de estrés/concurrencia (excluidos por defecto)
```bash
./mvnw test -Dtest="*ConcurrencyTest"
```

---

## 📊 Análisis de Calidad con SonarQube

### Quality Gates Requeridos
- **Coverage:** ≥ 80%
- **Duplicated Lines:** ≤ 2%

### Ejecutar análisis localmente (requiere SonarQube disponible)
```bash
./mvnw clean package
./mvnw sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_SONARQUBE_TOKEN
```

### Verificar resultados
1. Accede a `http://localhost:9000`
2. Busca el proyecto `llosa-backend`
3. Verifica el estado del Quality Gate

### En Jenkins (automático)
El pipeline ejecuta el análisis automáticamente en el stage **"SonarQube Analysis"** y valida el Quality Gate. El pipeline **fallará si no cumple** los criterios de calidad (coverage ≥80%, duplicated lines ≤2%).

---

## 📋 Arquitectura de Pruebas

### 1️⃣ **Pruebas Unitarias & Controlador** (`@WebMvcTest`, `@ExtendWith(MockitoExtension)`)
- **Alcance:** Métodos aislados, sin BD, sin HTTP
- **Tecnologías:** Mockito, JUnit 5, MockMvc con `spring-security-test`
- **Velocidad:** ~200ms/test
- **Ubicación:** `src/test/java/com/llosa/backend/security/` y `module/seguridad/service/` y `module/seguridad/controller/`
- **Utilidades:** `@WithFirebaseAuth` (anotación personalizada para simular autenticación Firebase en controladores)

### 2️⃣ **Pruebas de Integración** (`@SpringBootTest`, `@DataJpaTest` + Testcontainers)
- **Alcance:** Stack completo: BD real PostgreSQL + Spring Boot + HTTP (MockMvc)
- **Tecnologías:**
  - `Testcontainers 1.20.4` + `docker-java 3.4.0` con PostgreSQL 16
  - `@ServiceConnection` (Spring Boot 3.1+ binding automático)
  - `Flyway` para migraciones en test
  - MockMvc con autenticación Firebase simulada
  - `SecurityTestConfiguration` para resolver `@AuthenticationPrincipal`
- **Velocidad:** ~30-40s/test (primera ejecución), ~3-5s subsecuentes
- **Ubicación:** `src/test/java/com/llosa/backend/module/seguridad/integration/` y `repository/`

---

## 🛠️ Stack de Pruebas

| Capa | Tecnología | Propósito |
|------|------------|-----------|
| **Dependencias** | `spring-boot-starter-test`, `spring-security-test` | Infraestructura base JUnit 5 + Mockito |
| **BD en Test** | `Testcontainers 1.20.4`, `PostgreSQL 16` | BD real con Flyway migrations automáticas |
| **Mocking HTTP/Firebase** | `MockMvc`, `MockedStatic<FirebaseAuth>` | Aislar sin hacer llamadas reales |
| **Autenticación Test** | `@WithFirebaseAuth`, `SecurityTestConfiguration` | Simular usuario autenticado en controladores |

### Configuración Especial
- **`pom.xml`:** `<api.version>1.41</api.version>` en Surefire (Docker 29.x exige API ≥1.40)
- **Flyway:** `V1__init_seguridad.sql` carga seeds (6 roles, 12 funciones) en cada test
- **Secuencia de ID:** Testcontainers resetea entre clases (`@DirtiesContext(AFTER_CLASS)`)
- **`@WithFirebaseAuth`:** Anotación personalizada para simular autenticación Firebase sin MockMvc.with()
  ```java
  @WithFirebaseAuth(uid = "test-user", email = "test@example.com")
  public void miTest() { ... }
  ```
- **`SecurityTestConfiguration`:** Resuelve `@AuthenticationPrincipal FirebaseAuthenticationToken` en tests con `@SpringBootTest`

---

## 📊 Casos de Prueba Cubiertos

### **Repositorio** (9 tests)
```bash
./mvnw test -Dtest=UsuarioRepositoryTest
```
- ✅ `findByFirebaseUuid` con usuario existente
- ✅ `existsByEmail` verdadero/falso  
- ✅ UNIQUE constraint en email
- ✅ CHECK constraint en tipo_usuario
- ✅ Seeds de Flyway presentes (6 roles + funciones)
- ✅ Relación rol↔usuario persistida

### **Servicio: AuthService** (5 tests)
```bash
./mvnw test -Dtest=AuthServiceTest
```
- ✅ Usuario no registrado → excepción
- ✅ Cuenta suspendida → RuntimeException
- ✅ EMPLEADO con email no corporativo → acceso denegado
- ✅ EMPLEADO con email `@llosaedificaciones.com` → OK
- ✅ Mapeo completo de perfil (id, nombre, email, activo, rol, funciones)

### **Servicio: UsuarioService** (9 tests)
```bash
./mvnw test -Dtest=UsuarioServiceTest
```
- ✅ Email duplicado → excepción sin llamar Firebase
- ✅ Crear usuario exitoso → Firebase.createUser() + BD persistencia
- ✅ Con rol asignado → Relación guardada
- ✅ `cambiarEstado(false)` → revoke tokens + disabled en Firebase + activo=false en BD
- ✅ `asignarRol` con rol inexistente → excepción
- ✅ `listarTodos()` devuelve lista

### **Controlador: AuthController** (3 tests)
```bash
./mvnw test -Dtest=AuthControllerTest
```
- ✅ GET `/api/auth/me` sin autenticar → 401
- ✅ GET `/api/auth/me` autenticado → 200 + JSON perfil
- ✅ Excepción en servicio propaga al caller

### **Controlador: UsuarioController** (7 tests)
```bash
./mvnw test -Dtest=UsuarioControllerTest
```
- ✅ POST `/api/users/register` sin autenticar → 401
- ✅ Body inválido (sin email, sin nombre) → 400
- ✅ Body válido → 200 + usuario creado
- ✅ GET `/api/users` → lista de usuarios
- ✅ PUT `/api/users/{id}/role` sin idRol → 400
- ✅ PUT `/api/users/{id}/role` válido → 200 + rol actualizado
- ✅ DELETE `/api/users/{id}` → 200 + usuario desactivado

### **Filtro: FirebaseTokenFilter** (4 tests)
```bash
./mvnw test -Dtest=FirebaseTokenFilterTest
```
- ✅ Sin header Authorization → pasa sin autenticar
- ✅ Header sin "Bearer " → ignora
- ✅ Token inválido → SecurityContext limpio
- ✅ Token válido → FirebaseAuthenticationToken en SecurityContext

### **Integración E2E** (11 tests)
```bash
./mvnw test -Dtest=SeguridadIntegrationTest
```
- ✅ Flujo completo: crear usuario → asignar rol → consultar `/api/auth/me` autenticado
- ✅ Desactivar usuario → `/api/auth/me` falla con "Cuenta suspendida"
- ✅ Email duplicado → excepción
- ✅ Listar usuarios después de crear dos
- ✅ Listar roles con sus funciones (seed de Flyway)

---

## 🔧 Comandos de Ejecución

### Toda la suite
```bash
./mvnw test
```

### Solo tests unitarios/controlador (sin Testcontainers)
```bash
./mvnw test -Dtest="AuthServiceTest,UsuarioServiceTest,RolServiceTest,FirebaseTokenFilterTest,AuthControllerTest,UsuarioControllerTest"
```

### Solo integración (con Testcontainers)
```bash
./mvnw test -Dtest="*RepositoryTest,SeguridadIntegrationTest"
```

### Una clase específica
```bash
./mvnw test -Dtest=UsuarioRepositoryTest
```

### Un test específico
```bash
./mvnw test -Dtest=UsuarioRepositoryTest#findByFirebaseUuid_devuelveUsuarioExistente
```

### Salida detallada (cuando algo falla)
```bash
./mvnw test -Dtest=NombreTest -e  # Full stack trace
./mvnw test -Dtest=NombreTest -X  # Debug mode
```

---

## 📁 Estructura de Directorios

```
src/test/
├── java/com/llosa/backend/
│   ├── BackendApplicationTests.java              (context load + Testcontainers)
│   ├── config/
│   │   ├── PostgresTestContainerConfig.java      (@TestConfiguration + @ServiceConnection)
│   │   ├── SecurityTestConfiguration.java        (resolver para @AuthenticationPrincipal en tests)
│   │   ├── WithFirebaseAuth.java                 (anotación @WithFirebaseAuth para autenticación simulada)
│   │   └── TestData.java                         (builders para entidades)
│   │
│   ├── security/
│   │   └── FirebaseTokenFilterTest.java          (@ExtendWith(MockitoExtension))
│   │
│   ├── module/seguridad/
│   │   ├── repository/
│   │   │   ├── UsuarioRepositoryTest.java        (@DataJpaTest + Testcontainers)
│   │   │   ├── RolRepositoryTest.java
│   │   │   └── FuncionRepositoryTest.java
│   │   │
│   │   ├── service/
│   │   │   ├── AuthServiceTest.java              (@ExtendWith(MockitoExtension))
│   │   │   ├── UsuarioServiceTest.java           (MockedStatic<FirebaseAuth>)
│   │   │   └── RolServiceTest.java
│   │   │
│   │   ├── controller/
│   │   │   ├── AuthControllerTest.java           (@WebMvcTest + @WithFirebaseAuth)
│   │   │   ├── UsuarioControllerTest.java
│   │   │   └── RolControllerTest.java
│   │   │
│   │   └── integration/
│   │       └── SeguridadIntegrationTest.java     (@SpringBootTest E2E)
│   │
│   └── stress/
│       └── SeguridadConcurrencyTest.java         (@Tag("stress") - concurrencia/race conditions)
```

---

## 🔍 Tecnologías & Versiones

| Componente | Versión | Notas |
|-----------|---------|-------|
| Spring Boot | 4.0.6 | Con `@ServiceConnection` para Testcontainers |
| Java | 21 | LTS, Sealed classes, Virtual threads ready |
| PostgreSQL | 16 | Testcontainers, Flyway migrations |
| JUnit | 5 (jupiter) | `@Test`, `@RepeatedTest`, `@Tag` |
| Mockito | 5.0+ | `MockedStatic`, inline mocks (Java 21) |
| Testcontainers | 1.20.4 | `docker-java 3.4.0` con `api.version=1.41` |
| Flyway | Latest (BOM) | `V1__init_seguridad.sql` seeds en tests |
| Spring Security | 6.1 | Stateless, Firebase tokens, `@EnableMethodSecurity` |

---

## 🐛 Troubleshooting

### Docker API version mismatch
**Error:** `client version 1.32 is too old. Minimum supported API version is 1.40`

**Solución:** Ya aplicada en `pom.xml`:
```xml
<systemPropertyVariables>
  <api.version>1.41</api.version>
</systemPropertyVariables>
```

### Testcontainers no encuentra Docker
**Verificar:**
```bash
docker ps  # Docker corriendo?
docker --version  # ≥20.x
# Si es necesario:
export DOCKER_HOST=unix:///var/run/docker.sock
```

### Tests con Testcontainers lentos
**Esperado:** Primer test de clase ~20-30s (crear contenedor)
- Subsecuentes: ~3-5s (reutiliza contenedor)
- `@DirtiesContext(AFTER_CLASS)` elimina al final

### MockedStatic contaminación entre tests
**Prevención:** `try (MockedStatic<FirebaseAuth> ms = mockStatic(...)) { ... }`
- Garantiza cleanup después de cada test

---

## ⚠️ ESTADO ACTUAL DE TESTS Y BRECHAS DE SEGURIDAD

### 📊 Resultados de Ejecución (2026-06-01)
```
Tests totales: 124
✅ Pasados: 106
❌ Fallidos: 18 (14.5%)
Errores: 0 (después de arreglar configuración de tests)
```

### 🔴 Brechas de Seguridad Detectadas: 6 Tickets Mantis

**CRÍTICAS (P1):**
1. 🎫 TICKET #001: Autenticación no requerida en endpoints (CP06, CP07)
   - GET `/api/roles` devuelve 200 sin autenticación (debería 401)
   - PUT `/api/roles/{id}/functions` devuelve 200 sin autenticación
   - GET `/api/usuarios` devuelve 200 sin autenticación
   - POST `/api/usuarios/register` devuelve 405 en lugar de 401

2. 🎫 TICKET #002: Usuario suspendido accede a endpoints (CP08, CP10)
   - Usuario con `activo=false` recibe 200 en GET `/api/roles`
   - Debería recibir 403 Forbidden

3. 🎫 TICKET #003: Validación de `activo` inconsistente
   - Solo `/api/auth/me` valida suspensión
   - Otros endpoints no validan

**ALTAS (P2):**
4. 🎫 TICKET #004: Validación de dominio corporativo comentada (CP06)
   - Código comentado en `AuthService.java` línea 32-37
   - Empleados pueden usar emails NO corporativos

5. 🎫 TICKET #005: Configuración de rutas (CP06) - REVISADO
   - `/api/users` está abierto sin autenticación
   - `/api/auth/**` ESTÁ correctamente permitida (devuelve 404, no 401)
   - **Hallazgo:** El problema NO es permitAll(), sino `/api/users`

**MEDIA (P3):**
6. 🎫 TICKET #006: `/api/auth/me` devuelve 403 con token válido (CP06)
   - Flujo de login roto
   - Usuario autenticado no puede obtener su perfil

### 📋 Casos de Prueba Afectados (CP006-CP016)

| Caso | Descripción | Estado | Bloqueador |
|------|-------------|--------|-----------|
| CP06 | Admin crear usuario | ❌ FALLIDO | B#005, B#004, B#006 |
| CP07 | Asignación permisos | ❌ FALLIDO | B#001 |
| CP08 | Desactivar usuario | ❌ FALLIDO | B#002, B#003 |
| CP09 | Login cliente (Vendido) | ⚠️ BLOQUEADO | Depende CP06-CP08 |
| CP10 | Cliente inactivo | ⚠️ BLOQUEADO | Depende CP08 |
| CP11 | Cliente (Separado) | ⚠️ BLOQUEADO | Depende CP06-CP08 |
| CP12 | Crear proyecto | ⚠️ BLOQUEADO | Depende B#001 |
| CP13 | Validar unicidad | ⚠️ BLOQUEADO | Depende B#001 |
| CP14 | Inmutabilidad hitos | ⚠️ BLOQUEADO | Depende B#001 |
| CP15 | Vincular cliente | ⚠️ BLOQUEADO | Depende B#001 |
| CP16 | Asignación múltiple | ⚠️ BLOQUEADO | Depende B#001 |

### 📄 Documentación Completa

**Reporte QA detallado con todos los tickets Mantis listos para crear:**
```bash
cat REPORTE_QA_SEGURIDAD.md
```

Este reporte incluye para cada brecha:
- Descripción detallada
- Pasos para reproducir
- Resultado esperado vs actual
- Tests que demuestran la brecha
- Impacto de seguridad
- Componentes afectados
- Tareas relacionadas

### ⚡ Próximos Pasos

**Para Desarrolladores:**
1. Revisar `REPORTE_QA_SEGURIDAD.md` completo
2. Crear tickets en Mantis con la información de cada brecha
3. Arreglar vulnerabilidades en orden de prioridad (P1 → P2 → P3)
4. Re-ejecutar tests para validar fixes

**Para QA:**
1. Tests han sido actualizados para detectar correctamente las brechas
2. Todos los tests fallidos documentan fallas reales del sistema
3. No hay falsos positivos en los tests

### 🔍 Comandos para Validar Brechas

```bash
# Ver todos los tests fallidos
./mvnw test 2>&1 | grep "❌\|ERROR"

# Tests de seguridad específicamente
./mvnw test -Dtest="SecurityConfigTest,AuthControllerTest,RolControllerTest,UsuarioControllerTest"

# Tests de integración con brechas
./mvnw test -Dtest="SeguridadIntegrationTest" -Dtest.method="usuarioSuspendido*"
```

---
**Última actualización:** 2026-06-01 | **Estado:** 🔴 18/124 tests fallidos (brechas de seguridad detectadas) | **Documentación QA:** REPORTE_QA_SEGURIDAD.md
