# Backend Llosa Edificaciones

REST API Spring Boot 3.3 / Java 21 para la inmobiliaria Llosa Edificaciones. Implementa autenticación via Firebase, gestión de usuarios, roles y funciones, con un sistema completo de pruebas unitarias, de integración y estrés.

---

## 🚀 Inicio rápido
:
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

# 5. Ejecutar aplicación
./mvnw spring-boot:run
```

API disponible en `http://localhost:8080`.

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
| Spring Boot | 3.3.5 | Con `@ServiceConnection` para Testcontainers |
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
**Última actualización:** 2026-05-21 | **Estado:** ✅ 71/71 tests pasados
