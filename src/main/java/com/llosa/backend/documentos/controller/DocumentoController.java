package com.llosa.backend.documentos.controller;

import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;

    /**
     * POST /api/documentos/{idReferencia}
     * Sube un documento. El idReferencia es un UUID que puede pertenecer
     * a cualquier entidad del módulo proyecto (Proyecto, Activo, UsuarioActivo,
     * Hito, HitoPiso). El servicio determina automáticamente la entidad.
     */
    @PreAuthorize("hasAuthority('DOCS_SUBIR')")
    @PostMapping(value = "/{idReferencia}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> subirDocumento(
            @PathVariable UUID idReferencia,
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") SubirDocumentoRequest request,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return ResponseEntity.ok(
                documentoService.subirDocumento(idReferencia, file, request, usuario.getId())
        );
    }

    /**
     * GET /api/documentos/{idReferencia}?tipoDocumento=PDF_LEGAL
     *
     * Lista documentos de cualquier entidad. El filtro tipoDocumento es opcional.
     */
    @PreAuthorize("hasAuthority('DOCS_VER')")
    @GetMapping("/{idReferencia}")
    public ResponseEntity<List<DocumentoResponse>> listar(
            @PathVariable UUID idReferencia,
            @RequestParam(required = false) TipoDocumento tipoDocumento
    ) {
        return ResponseEntity.ok(documentoService.listar(idReferencia, tipoDocumento));
    }

    /**
     * GET /api/documentos/{documentoId}/signed-url
     * Genera URL firmada temporal (15 min) para descargar el documento.
     */
    @PreAuthorize("hasAuthority('DOCS_VER')")
    @GetMapping("/{documentoId}/signed-url")
    public ResponseEntity<SignedUrlResponse> generarSignedUrl(
            @PathVariable UUID documentoId
    ) {
        return ResponseEntity.ok(documentoService.generarSignedUrl(documentoId));
    }

    /**
     * DELETE /api/documentos/{documentoId}
     * Elimina el documento de GCS y de la BD.
     */
    @PreAuthorize("hasAuthority('DOCS_SUBIR')")
    @DeleteMapping("/{documentoId}")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable UUID documentoId) {
        documentoService.eliminarDocumento(documentoId);
        return ResponseEntity.noContent().build();
    }
}