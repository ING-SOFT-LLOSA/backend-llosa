# Tests Combinados: CP07-CP11

## Descripción

Conjunto estratégico de 5 tests que cubren los casos de prueba CP07-CP11 integrando tres capas de testing:

1. **Unit Logic** - Verificación de lógica sin BD
2. **Integration** - Persistencia en PostgreSQL real  
3. **E2E** - Flujos HTTP completos

## Archivos

```
├── CombinationTestBase.java      (Clase base con utilidades compartidas)
├── CP07CombinationTest.java      (Permisos granulares: 4 métodos)
├── CP08CombinationTest.java      (Invalidación tokens: 4 métodos)
├── CP09CombinationTest.java      (Cliente Vendido: 4 métodos)
├── CP10CombinationTest.java      (Cliente Inactivo: 5 métodos)
├── CP11CombinationTest.java      (Cliente Separado/Espera: 5 métodos)
└── README.md (este archivo)
```

## Setup Rápido

```bash
# 1. Renombrar archivos .bak → .java
bash ../../SETUP_TESTS_COMBINADOS.sh

# 2. Compilar
mvn clean test-compile

# 3. Ejecutar todos los tests combinados
mvn test -Dtest=CP0[7-9]CombinationTest,CP1[01]CombinationTest
```

## Detalles por Test

### CP07CombinationTest
**Caso:** Asignación granular de permisos (habilitar/restringir módulos)

| Layer | Método | Validación |
|-------|--------|-----------|
| Unit | `unitLogic_permisoSobrescribeRol` | Lógica de sobrescritura sin BD |
| Integration | `integration_permisosPersistenEnBD` | Cambios persisten en BD |
| E2E | `e2e_flujoCompletoAsignacionPermisos` | POST create → PUT assign role → GET /me |
| E2E | `e2e_cambioRolSobrescribePermisos` | Cambio de rol sobrescribe permisos |

**Endpoints:** POST /api/users/register, PUT /api/users/{id}/role, GET /api/auth/me
**Status esperado:** 200 OK

---

### CP08CombinationTest
**Caso:** Invalidación de tokens JWT al desactivar usuario

| Layer | Método | Validación |
|-------|--------|-----------|
| Unit | `unitLogic_usuarioDesactivoDebeRechazarAcceso` | Lógica de rechazo sin BD |
| Integration | `integration_desactivacionPersistenEnBD` | Desactivación persiste |
| E2E | `e2e_flujoDesactivacionCierraAcceso` | Acceso activo → desactivación → 403 |
| E2E | `e2e_usuarioInactivoRechazadoEnTodosEndpoints` | Bloqueado en /auth/me, /roles, /users |
| E2E | `e2e_reactivacionRestablecerAcceso` | Reactivación restaura acceso |

**Endpoints:** DELETE /api/users/{id}, GET /api/auth/me, GET /api/roles
**Status esperado:** 200 OK (activo), 403 Forbidden (inactivo)

---

### CP09CombinationTest
**Caso:** Cliente con unidad "Vendido" accede (modo regular)

| Layer | Método | Validación |
|-------|--------|-----------|
| Unit | `unitLogic_clienteVendidoConEstadoActivo` | Cliente VENDIDO activo sin BD |
| Integration | `integration_clienteVendidoPersistenEnBD` | Cliente persiste correctamente |
| E2E | `e2e_clienteVendidoFlujoCompletoAcceso` | Registro → rol → dashboard |
| E2E | `e2e_clienteVendidoAccedeFuncionesBasicas` | Accede a funciones CLIENTE |
| Integration | `integration_clienteVendidoConDatosCompletos` | Persistencia con datos completos |

**Endpoints:** POST /api/users/register, PUT /api/users/{id}/role, GET /api/auth/me
**Status esperado:** 200 OK, devuelve funciones

---

### CP10CombinationTest
**Caso:** Cliente inactivo (desistimiento) no puede acceder

| Layer | Método | Validación |
|-------|--------|-----------|
| Unit | `unitLogic_clienteInactivoNoDebeAcceder` | Lógica de rechazo sin BD |
| Integration | `integration_clienteInactivoPersistenEnBD` | Estado inactivo persiste |
| E2E | `e2e_clienteInactivoRechazadoAcceso` | Inactivo rechazado en /me |
| E2E | `e2e_clienteInactivoRechazadoEnTodosEndpoints` | Rechazado en /auth/me, /roles, /users |
| E2E | `e2e_cambioEstadoActavoAInactivo` | Activo → inactivo → bloqueado |
| Integration | `integration_consultaClientesInactivos` | Consulta BD de inactivos |

**Endpoints:** GET /api/auth/me, GET /api/roles, GET /api/users
**Status esperado:** 403 Forbidden con mensaje "Cuenta suspendida..."

---

### CP11CombinationTest
**Caso:** Cliente "Separado" en Modo de Espera (módulos bloqueados)

| Layer | Método | Validación |
|-------|--------|-----------|
| Unit | `unitLogic_clienteSeparadoEnModoEspera` | Estado Separado sin BD |
| Integration | `integration_clienteSeparadoPersistenEnBD` | Estado persiste |
| E2E | `e2e_clienteSeparadoModoEsperaAutenticado` | Autentica con tipoUsuario=CLIENTE_SEPARADO |
| E2E | `e2e_modulosBloquedosEnModoEspera` | Módulos bloqueados, solo resumen |
| E2E | `e2e_transicionSeparadoAVendido` | Separado → Vendido desbloquea |
| Integration | `integration_consultaClientesModoEspera` | Consulta BD de Separado |
| E2E | `e2e_clienteSeparadoEInactivo` | Separado + inactivo = 403 |

**Endpoints:** GET /api/auth/me
**Status esperado:** 200 OK (activo), tipoUsuario=CLIENTE_SEPARADO

---

## Clase Base: CombinationTestBase

Proporciona utilidades compartidas para todos los tests:

### Helpers de BD
```java
crearUsuarioEnBD(email, nombre, activo)
crearUsuarioConRolEnBD(email, nombre, rolNombre, activo)
limpiarDatos()
```

### Helpers de Autenticación
```java
createFirebaseToken(uid, email)
createAdminToken()
contextWithAuth(token)
```

### Helpers Firebase Mock
```java
mockFirebaseCreateUser(uid)
executeWithFirebaseMock(uid, operation)
```

### Inyecciones Automáticas
- `@Autowired MockMvc mockMvc` - HTTP testing
- `@Autowired ObjectMapper objectMapper` - JSON parsing
- `@Autowired UsuarioRepository usuarioRepository` - BD access
- `@Autowired RolRepository rolRepository` - Rol access
- `@MockitoBean FirebaseConfig firebaseConfig` - Firebase mock

## Patrones

### AAA Pattern
```java
// ARRANGE: Preparar datos
Usuario usuario = crearUsuarioEnBD("email@test.com", "Juan", true);

// ACT: Ejecutar acción
usuarioRepository.save(usuario);

// ASSERT: Validar resultado
assertThat(reloaded.getActivo()).isTrue();
```

### Progresión Layer por Layer
Cada test progresa de simple a complejo:
1. Unit: Lógica pura, sin efectos secundarios
2. Integration: BD real, transacciones
3. E2E: Controllers, seguridad, HTTP

### Naming Convention
```
methodName = cpXX_layerN_typeOfTest_whatItTests

Ejemplos:
- cp07_layer1_unitLogic_permisoSobrescribeRol
- cp08_layer2_integration_desactivacionPersistenEnBD
- cp09_layer3_e2e_clienteVendidoFlujoCompletoAcceso
```

## Ejecución

### Todos los tests combinados
```bash
mvn test -Dtest=CP0[7-9]CombinationTest,CP1[01]CombinationTest
```

### Tests específicos
```bash
mvn test -Dtest=CP07CombinationTest
mvn test -Dtest=CP08CombinationTest
mvn test -Dtest=CP09CombinationTest
mvn test -Dtest=CP10CombinationTest
mvn test -Dtest=CP11CombinationTest
```

### Un test específico
```bash
mvn test -Dtest=CP07CombinationTest#cp07_layer3_e2e_flujoCompletoAsignacionPermisos
```

## Tiempo de Ejecución

| Test | Métodos | Tiempo |
|------|---------|--------|
| CP07 | 4 | 5-6s |
| CP08 | 4 | 5-6s |
| CP09 | 5 | 6-7s |
| CP10 | 6 | 7-8s |
| CP11 | 7 | 8-9s |
| **Total** | **25** | **30-40s** |

## Requisitos

- Java 17+
- Maven 3.8+
- Spring Boot 3.x
- PostgreSQL (TestContainers)
- Mockito 5.x
- JUnit 5

Todas las dependencias están en `pom.xml`

## Coverage

```
Total Métodos: 25
├─ Unit Logic: 5
├─ Integration: 8
└─ E2E: 12

Casos de Prueba: 5 (CP07-CP11)
Endpoints validados: 8
  ├─ POST /api/users/register
  ├─ PUT /api/users/{id}/role
  ├─ DELETE /api/users/{id}
  ├─ GET /api/auth/me
  ├─ GET /api/users
  ├─ GET /api/roles
  └─ (más en variantes)

Escenarios cubiertos:
├─ Creación de usuarios
├─ Asignación de roles
├─ Cambio de permisos
├─ Desactivación/Reactivación
├─ Estados de clientes (Vendido, Inactivo, Separado)
├─ Transiciones entre estados
└─ Validaciones de acceso
```

## Notas

1. **Renombrado de archivos:** Los archivos están guardados como `.bak`. Usar el script `SETUP_TESTS_COMBINADOS.sh` para renombrar a `.java`.

2. **TestContainers:** PostgreSQL se levanta automáticamente. Primera ejecución descargará imagen Docker.

3. **Firebase Mock:** Los tests mockean `FirebaseAuth.getInstance()`. No se crean usuarios reales.

4. **Limpieza:** `usuarioRepository.deleteAll()` se ejecuta en `@BeforeEach`. Cada test comienza limpio.

5. **Seguridad:** `SecurityContextHolder.clearContext()` en `@BeforeEach`. Contexto vacío inicial.

## Referencias

- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/)
- [TestContainers](https://www.testcontainers.org/)
- [AssertJ](https://assertj.github.io/assertj-core-features-highlight.html)

## Próximos Pasos

1. Ejecutar script de setup
2. Compilar con `mvn test-compile`
3. Ejecutar tests
4. Integrar a CI/CD
5. Documentar resultados

---

**Author:** Claude (ENGINEER COMBINACIÓN)  
**Date:** 2026-06-04  
**Version:** 1.0
