# Explore project structure and existing entities

Excellent! Now I have all the information needed. Let me compile a comprehensive report:

## Spring Boot Backend Project - Complete Exploration Report

### 1. PROJECT STRUCTURE & PACKAGE TREE

**Base Package:** `com.llosa.backend`

**Directory Structure:**
```
src/main/java/com/llosa/backend/
├── BackendLlosaApplication.java (main entry point)
├── comercial/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── repository/
│   └── service/
├── config/
├── documentos/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── repository/
│   └── service/
├── exception/
├── pago/
├── proyecto/
│   ├── controller/
│   ├── dto/
│   │   ├── request/
│   │   ├── response/
│   │   └── shared/
│   ├── entity/
│   ├── enums/
│   ├── repository/
│   └── service/
└── seguridad/
    ├── controller/
    ├── dto/
    ├── entity/
    ├── repository/
    ├── security/
    └── service/
```

---

### 2. CORE ENTITIES

#### **Proyecto.java**
- **ID**: UUID (GeneratedValue.UUID)
- **Fields**: nombre, descripcion, precertificacionEdgeLeed, departamento, distrito, direccion, fechaInicio, fechaFin, createdAt
- **Relationships**:
    - `OneToMany torres` (CascadeType.ALL, orphanRemoval=true, FetchType.LAZY)
    - `OneToMany hitos` (CascadeType.ALL, orphanRemoval=true, FetchType.LAZY)
    - `OneToMany documentos` (CascadeType.ALL, orphanRemoval=true, FetchType.LAZY)

#### **Torre.java**
- **ID**: Long (GenerationType.IDENTITY)
- **Fields**: nombre
- **Relationships**:
    - `ManyToOne proyecto` (FetchType.LAZY)
    - `OneToMany pisos` (CascadeType.ALL, orphanRemoval=true, FetchType.LAZY)

#### **Piso.java**
- **ID**: Long (GenerationType.IDENTITY)
- **Fields**: nroPiso (nullable=false, can be negative)
- **Relationships**:
    - `ManyToOne torre` (FetchType.LAZY)
    - `OneToMany activos` (CascadeType.ALL, orphanRemoval=true, FetchType.LAZY)
    - `OneToMany hitosPiso` (CascadeType.ALL, orphanRemoval=true, FetchType.LAZY)

---

### 3. RELATED ENTITIES

#### **Activo.java**
- **ID**: UUID (GenerationType.UUID)
- **Fields**: nro (nullable=false), tipo (TipoActivo enum), areaM2 (BigDecimal, default 0.0), estadoComercial (EstadoComercialActivo enum), precio (BigDecimal, default 0.0), descripcion (default "No existe descripcion todavia"), linkRecorridoVirtual, createdAt, updatedAt
- **Relationships**:
    - `ManyToOne piso` (FetchType.LAZY)

#### **UsuarioActivo.java**
- **ID**: UUID (GenerationType.UUID) - uuidUsuarioActivo
- **Fields**: tipoFinanciamiento, faseComercial, estadoTramiteLegal, fechaAdquisicion, createdAt, updatedAt
- **Relationships**:
    - `ManyToMany clientes` (through usuario_activo_clientes table)
    - `ManyToOne activo` (FetchType.LAZY)
    - `OneToOne cochera` (FetchType.LAZY) - optional Activo

---

### 4. DOCUMENTOS MODULE

#### **DocumentoService.java**
**Key Methods:**
- `obtenerDetalleEtapa(String etapaProceso, UUID uuidUsuarioActivo): StageResponse` - Read-only transaction
- `subirDocumento(UUID usuarioActivoId, MultipartFile file, SubirDocumentoRequest request, Integer subidoPor): DocumentoResponse` - Validates file, uploads to GCS, persists metadata
- `listarPorUsuarioActivo(UUID usuarioActivoId, TipoDocumento tipoDocumento): List<DocumentoResponse>` - Read-only, filters by type
- `listarDocumentosCliente(String firebaseUid): StageDocumentResponse` - Aggregates documents from all user's expedientes
- `generarSignedUrl(UUID documentoId, Integer usuarioId): SignedUrlResponse` - 15-minute expiration signed URLs
- `eliminarDocumento(UUID documentoId, Integer usuarioId): void` - Removes from GCS and DB
- **Private Helpers**: `validarArchivo()`, `resolverIcono()`, `obtenerExtension()`
- **Dependencies**: DocumentoRepository, TipoDocumentoConfigRepository, UsuarioActivoRepository, UsuarioRepository, Storage (Google Cloud)

#### **DocumentoController.java**
```
POST   /api/documentos/usuario-activo/{usuarioActivoId}     @PreAuthorize('DOCS_SUBIR')
GET    /api/documentos/usuario-activo/{usuarioActivoId}     @PreAuthorize('DOCS_VER')
GET    /api/documentos/mis-documentos                        @PreAuthorize('DOCS_VER')
GET    /api/documentos/{documentoId}/signed-url              @PreAuthorize('DOCS_VER')
DELETE /api/documentos/{documentoId}                         @PreAuthorize('DOCS_SUBIR')
```

#### **Documento.java**
- **ID**: UUID (GenerationType.UUID)
- **Fields**: rutaGcs (GCS path), nombreOriginal, idReferencia (references entity UUID), entidadReferencia ("USUARIO_ACTIVO" or similar), tipoDocumento (TipoDocumento enum), tipoMime, accesoRestringido (default true), subidoPor (user ID), createdAt
- **Relationships**:
    - `ManyToOne proyecto` (nullable, FetchType.LAZY)

#### **DocumentoRepository.java**
```java
List<Documento> findByIdReferenciaAndEntidadReferencia(String, String)
List<Documento> findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(String, String, TipoDocumento)
List<Documento> findBySubidoPor(Integer usuarioId)
```

---

### 5. CONTROLLERS FOR REFERENCE

#### **UsuarioActivoController.java** (Key Example)
```
GET    /api/expedientes/mis-activos                         @PreAuthorize('CONTRATO_VER')
POST   /api/expedientes/asignar                             @PreAuthorize('CONTRATO_EDITAR')
GET    /api/expedientes/{uuidActivo}/contrato               @PreAuthorize('CONTRATO_VER')
GET    /api/expedientes/{id_usuario}                        @PreAuthorize('CONTRATO_VER')
GET    /api/expedientes/usuario/{id_usuario}/activos        @PreAuthorize('CONTRATO_VER')
DELETE /api/expedientes/delete/{uuidUsuarioActivo}          @PreAuthorize('CONTRATO_EDITAR')
```

**Key Patterns:**
- Extracts Firebase UID from Authentication principal
- Retrieves current user from UsuarioRepository
- Returns DTOs not entities
- Uses ResponseEntity for HTTP status control
- PreAuthorize for role-based access

#### **ProyectoController.java** (Additional Reference)
```
POST   /api/proyectos                              @PreAuthorize('PROY_CREAR')
PUT    /api/proyectos/{uuid}                       @PreAuthorize('PROY_EDITAR')
DELETE /api/proyectos/{uuid}                       @PreAuthorize('PROY_EDITAR')
GET    /api/proyectos                              (with search param)
POST   /api/proyectos/{uuid}/hitos                 @PreAuthorize('PROY_EDITAR')
GET    /api/proyectos/{uuid}/hitos                 @PreAuthorize('PROY_VER')
POST   /api/proyectos/{id_proyecto}/estructura-fisica
GET    /api/proyectos/{uuid}/avance-general
```

---

### 6. DTO PATTERNS (Using Records)

**Response DTOs (Records):**

#### **ActivoResponseDTO.java**
```java
public record ActivoResponseDTO(
    UUID id,
    Long pisoId,
    Integer nroPiso,
    String torreNombre,
    String proyectoNombre,
    String nro,
    TipoActivo tipo,
    BigDecimal areaM2,
    EstadoComercialActivo estadoComercial,
    BigDecimal precio,
    String descripcion
) {
    public static ActivoResponseDTO fromEntity(Activo a) { ... }
}
```

#### **DocumentoResponse.java**
```java
public record DocumentoResponse(
    UUID id,
    String nombreOriginal,
    TipoDocumento tipoDocumento,
    String tipoMime,
    String idReferencia,
    String entidadReferencia,
    LocalDateTime createdAt
) {
    public static DocumentoResponse fromEntity(Documento doc) { ... }
}
```

#### **UsuarioActivoResponseDTO.java**
```java
public record UsuarioActivoResponseDTO(
    UUID uuidUsuarioActivo,
    String tipoFinanciamiento,
    String faseComercial,
    String estadoTramiteLegal,
    LocalDateTime fechaAdquisicion,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<ClienteSimpleDTO> clientes,
    ActivoResponseDTO activo
) {
    public record ClienteSimpleDTO(
        Integer id, String nombre, String apellidos,
        String documentoIdentidad, String email, String telefono
    ) { ... }
}
```

**Request DTOs (Records):**

#### **AsignarActivoDTO.java**
```java
public record AsignarActivoDTO(
    @NotEmpty List<Integer> idsUsuarios,
    @NotNull UUID idActivo,
    String tipoFinanciamiento,
    String faseComercial,
    String estadoTramiteLegal,
    LocalDateTime fechaAdquisicion
) {}
```

#### **SubirDocumentoRequest.java**
```java
public record SubirDocumentoRequest(
    @NotNull TipoDocumento tipoDocumento
) {}
```

#### **ProyectoCreateDTO.java**
```java
public record ProyectoCreateDTO(
    @NotBlank String nombre,
    String descripcion,
    Boolean precertificacionEdgeLeed,
    String departamento,
    String distrito,
    String direccion,
    LocalDate fechaInicio,
    LocalDate fechaFin
) {}
```

**Nested DTOs:**

#### **StageResponse.java**
```java
public record StageResponse(StageDetailsResponse stageDetails) {
    public record StageDetailsResponse(
        ResumenContratoResponse resumenContrato,
        InformacionContratoResponse informacionContrato
    ) {}
    public record ResumenContratoResponse(
        int totalUnidades, String areaTechadaTotal,
        List<UnidadResponse> unidades, TotalesResponse totales
    ) {}
    public record UnidadResponse(
        String tipo, String nombre, String aporteAlContrato,
        String areaOcupada, String areaTechada, String icono
    ) {}
    public record TotalesResponse(int departamentos, int estacionamientos) {}
    public record InformacionContratoResponse(
        String firmaContrato, String fechaDesembolso, String modalidadPago
    ) {}
}
```

#### **StageDocumentResponse.java**
```java
public record StageDocumentResponse(
    String sectionTitle, int totalCount,
    List<DocumentoItemResponse> documents
) {
    public record DocumentoItemResponse(
        String id, String title, String description, String status,
        String emissionDate, boolean hasDownload, String downloadUrl,
        boolean hasPreview, String corporateNoteUrl, String icon
    ) {}
}
```

#### **SignedUrlResponse.java**
```java
public record SignedUrlResponse(
    String url,
    Instant expiracion
) {}
```

---

### 7. REPOSITORY PATTERNS

#### **ActivoRepository.java**
```java
public interface ActivoRepository extends JpaRepository<Activo, UUID> {
    List<Activo> findByPisoId(Long id);
    List<Activo> findByPisoIdAndNroContainingIgnoreCase(Long pisoId, String nro);
    List<Activo> findByPisoTorreProyectoId(UUID id);
    Page<Activo> findByPisoTorreProyectoId(UUID uuidProyecto, Pageable pageable);
    Page<Activo> findByPisoTorreProyectoIdAndEstadoComercial(
        UUID piso_torre_proyecto_id, EstadoComercialActivo estadoComercial, Pageable pageable);
}
```

#### **DocumentoRepository.java**
```java
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    List<Documento> findByIdReferenciaAndEntidadReferencia(String, String);
    List<Documento> findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(
        String, String, TipoDocumento);
    List<Documento> findBySubidoPor(Integer usuarioId);
}
```

#### **UsuarioActivoRepository.java**
```java
public interface UsuarioActivoRepository extends JpaRepository<UsuarioActivo, UUID> {
    @Query("SELECT ua FROM UsuarioActivo ua JOIN ua.clientes c WHERE c.id = :usuarioId")
    List<UsuarioActivo> findByClienteId(@Param("usuarioId") Integer usuarioId);
    
    Optional<UsuarioActivo> findByActivo_Id(UUID uuidActivo);
    void deleteById(@NonNull UUID id);
}
```

#### **ProyectoRepository.java**
```java
public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {
    List<Proyecto> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
        String nombre, String descripcion);
}
```

---

### 8. MAIN APPLICATION CLASS

#### **BackendLlosaApplication.java**
```java
@SpringBootApplication
public class BackendLlosaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendLlosaApplication.class, args);
    }
}
```
**Base Package**: `com.llosa.backend`

---

### 9. APPLICATION CONFIGURATION

#### **application.properties**
```properties
# Database (PostgreSQL)
spring.datasource.url=${DB_URL_LLOSA}
spring.datasource.username=${DB_USERNAME_LLOSA}
spring.datasource.password=${DB_PASSWORD_LLOSA}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=false
spring.flyway.locations=classpath:db/migration

# Firebase & GCS
firebase.service-account-path=${FIREBASE_CREDENTIALS_PATH_LLOSA}
app.dominio-corporativo=${DOMINIO_CORPORATIVO_LLOSA}
gcs.bucket-name=llosa-bucket
```

**Key Config Variables (Environment-based):**
- `DB_URL_LLOSA`, `DB_USERNAME_LLOSA`, `DB_PASSWORD_LLOSA`
- `FIREBASE_CREDENTIALS_PATH_LLOSA`
- `DOMINIO_CORPORATIVO_LLOSA`

---

### 10. ENUMS

#### **TipoDocumento.java**
```java
public enum TipoDocumento {
    PDF_LEGAL,
    COMPROBANTE,
    FOTO_OBRA,
    VIDEO_OBRA
}
```

#### **TipoActivo.java**
```java
public enum TipoActivo {
    DEPARTAMENTO,
    COCHERA,
    DEPOSITO
}
```

#### **EstadoComercialActivo.java**
```java
public enum EstadoComercialActivo {
    DISPONIBLE,
    SEPARADO,
    VENDIDO,
    NO_APLICA
}
```

---

### 11. SERVICE IMPLEMENTATION PATTERN

#### **UsuarioActivoServiceImpl.java** (Example)
```java
@Service
@RequiredArgsConstructor
public class UsuarioActivoServiceImpl implements UsuarioActivoService {
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final UsuarioService usuarioService;
    private final ActivoService activoService;

    @Transactional(readOnly = true)
    public UsuarioActivo findById(UUID id) { ... }

    @Transactional(readOnly = true)
    public List<UsuarioActivo> findByUsuario(Integer usuarioId) { ... }

    @Transactional
    public UsuarioActivo updateCustomerJourney(UUID id, String faseComercial, String estadoTramiteLegal) { ... }

    @Transactional
    public UsuarioActivo save(UsuarioActivo usuarioActivo) { ... }

    @Transactional
    public void asignarActivo(AsignarActivoDTO dto) { ... }

    @Transactional(readOnly = true)
    public Optional<UsuarioActivo> findByActivo(UUID activoId) { ... }

    @Transactional
    public void deleteById(UUID id) { ... }
}
```

**Patterns Used:**
- Constructor injection via `@RequiredArgsConstructor`
- `@Transactional` for write operations (default) or `readOnly=true` for queries
- EntityNotFoundException thrown when entities not found
- DTOs for service layer boundaries
- Composition of multiple repositories/services

---

### 12. MAVEN DEPENDENCIES (pom.xml)

**Key Dependencies:**
- Spring Boot 4.0.6
- Spring Web MVC, Security, Data JPA, Validation
- PostgreSQL Driver
- Firebase Admin SDK 9.3.0
- Google Cloud Storage 2.36.1
- Lombok 1.18.36
- Spring Dotenv 4.0.0
- Flyway
- Spring Boot Test

---

### 13. KEY TECHNICAL PATTERNS OBSERVED

1. **DTO Records**: All DTOs use Java Records with factory methods `fromEntity()`
2. **Nested Records**: Complex DTOs use nested record classes for structure
3. **Transactional Boundaries**: Clear read-only vs. write transactions
4. **Security**: Firebase authentication, `@PreAuthorize` with custom authorities
5. **Entity Relationships**: Extensive use of FetchType.LAZY, CascadeType.ALL, orphanRemoval
6. **UUIDs**: Proyecto, Activo, Documento, UsuarioActivo use UUID; Torre, Piso use IDENTITY
7. **Validation**: Jakarta validation annotations on DTOs
8. **Soft Dependencies**: Lombok for boilerplate reduction
9. **Cloud Integration**: Google Cloud Storage for document management
10. **Database**: PostgreSQL with Hibernate DDL auto-update (development mode)

---

### 14. FILE PATHS SUMMARY

**Core Entity Files:**
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/proyecto/entity/Proyecto.java`
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/proyecto/entity/Torre.java`
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/proyecto/entity/Piso.java`
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/proyecto/entity/Activo.java`
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/proyecto/entity/UsuarioActivo.java`

**Document Management:**
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/documentos/service/DocumentoService.java`
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/documentos/controller/DocumentoController.java`
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/documentos/entity/Documento.java`
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/documentos/repository/DocumentoRepository.java`

**Reference Controller:**
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/proyecto/controller/UsuarioActivoController.java`

**Application Entry Point:**
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/java/com/llosa/backend/BackendLlosaApplication.java`

**Configuration:**
- `/home/daros/academico/2026-01/ING-SOFTWARE/LlosaEdificaciones/backend-llosa/src/main/resources/application.properties`
  agentId: a460539775eb47636 (for resuming to continue this agent's work if needed)
  <usage>total_tokens: 60907
  tool_uses: 37
  duration_ms: 81571</usage>