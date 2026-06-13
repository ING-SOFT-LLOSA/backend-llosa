package com.llosa.backend.comercial.controller;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoResponseDTO;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.service.RequisitoDocumentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requisitos-documentales")
public class RequisitoDocumentalController {

    private final RequisitoDocumentalService requisitoService;

    /* falta el get de cómo obtener todos los requisitos*/
    /**
     * SUBIR ARCHIVO: Sube el documento físico (PDF, imagen, etc.) para cumplir
     * con un requisito específico del checklist.
     * Cambia el estado del requisito a 'COMPLETADA'.
     */
    @PreAuthorize("hasAuthority('DOCS_SUBIR')")
    @PostMapping(value = "/{requisitoId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RequisitoResponseDTO> subirArchivoRequisito(
            @PathVariable UUID requisitoId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        RequisitoDocumental resquisitoDocumental =  requisitoService.asociarArchivoARequisito(requisitoId, file, uid);
        RequisitoResponseDTO requisitoResponseDTO =  RequisitoResponseDTO.fromEntity(resquisitoDocumental);
        return ResponseEntity.ok(requisitoResponseDTO);
    }

    /**
     * ELIMINAR ARCHIVO: Borra el documento físico tanto de Google Cloud Storage
     * como de la tabla polimórfica 'documento'.
     * Regresa el estado del requisito a 'PENDIENTE'.
     */
    @PreAuthorize("hasAuthority('DOCS_SUBIR')")
    @DeleteMapping("/{requisitoId}/upload")
    public ResponseEntity<Void> eliminarArchivoRequisito(
            @PathVariable UUID requisitoId,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        requisitoService.eliminarArchivoDeRequisito(requisitoId, uid);
        return ResponseEntity.noContent().build();
    }

    /**
     * CREAR REQUISITO: El administrador añade dinámicamente un nuevo requisito
     * (un nuevo ítem de checklist) a un hito de un cliente.
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @PostMapping
    public ResponseEntity<RequisitoResponseDTO> crearRequisito(
            @Valid @RequestBody RequisitoCreateRequest request
    ) {
        RequisitoDocumental requisitoDocumental = requisitoService.crearRequisito(request);
        RequisitoResponseDTO requisitoResponseDTO =  RequisitoResponseDTO.fromEntity(requisitoDocumental);
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitoResponseDTO);
    }

    /**
     * ACTUALIZAR REQUISITO: El administrador modifica el título, descripción,
     * notas corporativas o fuerza manualmente el cambio de estado de un requisito.
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @PutMapping("/{id}")
    public ResponseEntity<RequisitoResponseDTO> actualizarRequisito(
            @PathVariable UUID id,
            @Valid @RequestBody RequisitoUpdateRequest request
    ) {
        RequisitoDocumental reqisitoDocumental =  requisitoService.actualizarRequisito(id, request);
        RequisitoResponseDTO requisitoResponseDTO =  RequisitoResponseDTO.fromEntity(reqisitoDocumental);
        return ResponseEntity.ok(requisitoResponseDTO);
    }

    /**
     * ELIMINAR REQUISITO COMPLETO: El administrador borra el requisito del negocio.
     * NOTA: La lógica del servicio asegura que si había un archivo físico en GCS,
     * se borre primero para no dejar archivos basura huérfanos.
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRequisitoTotalmente(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        requisitoService.eliminarRequisitoTotalmente(id, uid);
        return ResponseEntity.noContent().build();
    }
}