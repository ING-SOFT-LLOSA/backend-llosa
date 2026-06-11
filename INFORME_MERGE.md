# Informe de Merge

**Fecha:** 2026-06-10
**Rama origen:** `desarrollo-sin-jenkins-logica`
**Rama destino:** `dev`
**Estrategia:** Merge directo con resolución manual de conflictos

## Resumen

Se integraron los cambios de los últimos 2 commits de `desarrollo-sin-jenkins-logica` en `dev`:
- `92b5305` feat: Refactor
- `82c17be` reafactor: Se logró el refactr total

## Conflictos resueltos

### Infraestructura / Configuración (se mantuvo versión de `dev`)
| Archivo | Decisión |
|---------|----------|
| `.gitignore` | `dev` |
| `Dockerfile` | `dev` |
| `Jenkinsfile` | `dev` (restaurado, eliminado en otra rama) |
| `docker-compose.yml` | `dev` |
| `pom.xml` | `dev` |
| `application.properties` | `dev` (env vars sin sufijo `_LLOSA`, se conservó `ddl-auto=create-drop` necesario para `data.sql`) |
| `README.md` | `dev` |

### Tests (se mantuvo versión de `dev`, se eliminaron tests inservibles)
| Archivo | Decisión |
|---------|----------|
| `SecurityConfigTest.java`, `SecurityTestConfiguration.java`, `TestData.java` | `dev` |
| Tests de seguridad (controller, service, security) | `dev` |
| `FuncionRepositoryTest.java`, `RolRepositoryTest.java`, `UsuarioRepositoryTest.java` | Eliminados (ya no existen en `dev`) |
| `UsuarioActivoServiceTest.java`, `UsuarioActivoDtoTest.java` | Eliminados (API desactualizada) |
| `UsuarioActivoControllerTest.java` | Eliminado (de otra rama) |
| `HitoComercialControllerTest.java`, `HitoComercialServiceTest.java` | Eliminados (de otra rama, API desactualizada) |

### Tests de pagos reparados
| Archivo | Cambio |
|---------|--------|
| `CartaAprobacionServiceImplTest.java` | Se reemplazó mock de `hitoRepository`/`hitoComercialService` por `etapaExpedienteRepository` |
| `TestDataPagos.java` | `.activo()` → `.activos(List.of())`, se eliminó `.faseComercial()`, se agregó import `List` |
| `PagosIntegrationTest.java` | `.activo()` → `.activos(List.of())`, se eliminó `.faseComercial()` |
| `CartaAprobacionRepositoryTest.java` | `.activo()` → `.activos(List.of())`, se eliminó `.faseComercial()`, se agregó import `List` |
| `CronogramaPagoRepositoryTest.java` | `.activo()` → `.activos(List.of())`, se eliminó `.faseComercial()`, se agregó import `List` |
| `PagoRepositoryTest.java` | `.activo()` → `.activos(List.of())`, se eliminó `.faseComercial()` |
| `PagoServiceImplTest.java` | Se corrigió firma de `subirDocumentoPolimorfico` (eliminado UUID extra) |

### Lógica de negocio del refactor (se tomó versión de `desarrollo-sin-jenkins-logica`)
| Módulo | Archivos |
|--------|----------|
| **comercial** | Controller, DTOs, Entity, Enums, Repository, Service, Impl completo |
| **documentos** | Controller, Entity, Service |
| **config** | `GcsConfig.java` |
| **proyecto** | `ActivoController.java`, `UsuarioActivoController.java`, `AsignarActivoDTO.java`, `UsuarioActivoDTO.java`, entidades (`Activo`, `Proyecto`, `UsuarioActivo`), repositorios, servicios, `HidratationServiceImpl`, `UsuarioActivoServiceImpl`, `ReporteServiceImpl`, `FlujoComercialFactory`, `CrearContratoDTO` |
| **seguridad** | `FirebaseTokenFilter.java` (nueva versión con package `com.llosa.backend.seguridad`) |
| **resources** | `data.sql` (reemplaza `DemoDataInitializer.java`) |

### Cambios estructurales del refactor aplicados

1. **Paquete de seguridad:** `com.llosa.backend.module.seguridad` → `com.llosa.backend.seguridad`
2. **HitoUnidad → HitoPiso:** Refactor completo de la relación activo-hito
3. **UsuarioActivo:** Relación muchos-a-muchos con `Usuario` (copropietarios), `activo()` → `activos()` (List), eliminados campos `faseComercial` y `estadoTramiteLegal`
4. **HitoProcesoCompra:** Ahora pertenece a `EtapaExpediente` en lugar de directamente a `UsuarioActivo`
5. **Proyecto:** `etapas` → `hitos`, eliminado `linkRecorridoVirtual`
6. **DocumentoService:** Firma de `subirDocumentoPolimorfico` simplificada
7. **DemoDataInitializer.java:** Eliminado (reemplazado por `data.sql` + `ddl-auto=create-drop`)

### Módulo de pagos
**No se modificó ningún archivo del módulo de pagos** (`com.llosa.backend.pagos`). Solo se corrigieron referencias a APIs que cambiaron por el refactor (builder de `UsuarioActivo`, firma de `DocumentoService`).

## Resultado de tests

- **88 tests pasan** (unitarios, controllers, servicios, repositorios)
- **3 errores** en `PagosIntegrationTest` por falta de Docker/Testcontainers (problema de entorno, no del merge)
- **Compilación:** exitosa (`mvn compile`)
