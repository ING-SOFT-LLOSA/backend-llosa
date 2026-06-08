package com.llosa.backend.comercial.controller;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
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

    /**
     * SUBIR ARCHIVO: Sube el documento físico (PDF, imagen, etc.) para cumplir
     * con un requisito específico del checklist.
     * Cambia el estado del requisito a 'COMPLETADA'.
     */
    @PreAuthorize("hasAuthority('DOCS_SUBIR')")
    @PostMapping(value = "/{requisitoId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> subirArchivoRequisito(
            @PathVariable UUID requisitoId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        requisitoService.asociarArchivoARequisito(requisitoId, file, uid);
        return ResponseEntity.ok().build();
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
    @PreAuthorize("hasAuthority('CONTRATO_VER')") // Ajusta la autoridad según tus roles de admin
    @PostMapping
    public ResponseEntity<Void> crearRequisito(
            @Valid @RequestBody RequisitoCreateRequest request
    ) {
        requisitoService.crearRequisito(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * ACTUALIZAR REQUISITO: El administrador modifica el título, descripción,
     * notas corporativas o fuerza manualmente el cambio de estado de un requisito.
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizarRequisito(
            @PathVariable UUID id,
            @Valid @RequestBody RequisitoUpdateRequest request
    ) {
        requisitoService.actualizarRequisito(id, request);
        return ResponseEntity.ok().build();
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