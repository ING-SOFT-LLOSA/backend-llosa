package com.llosa.backend.documentos.controller;

import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.repository.UsuarioRepository;
import com.llosa.backend.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;

    @PreAuthorize("hasAuthority('DOCS_SUBIR')")
    @PostMapping(value = "/hito-unidad/{hitoUnidadId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> subirDocumento(
            @PathVariable UUID hitoUnidadId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") SubirDocumentoRequest request,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(
                documentoService.subirDocumento(hitoUnidadId, file, request, usuario.getId())
        );
    }

    @PreAuthorize("hasAuthority('DOCS_VER')")
    @GetMapping("/hito-unidad/{hitoUnidadId}")
    public ResponseEntity<List<DocumentoResponse>> listarPorHitoUnidad(
            @PathVariable UUID hitoUnidadId
    ) {
        return ResponseEntity.ok(
                documentoService.listarPorHitoUnidad(hitoUnidadId)
        );
    }

    @PreAuthorize("hasAuthority('DOCS_VER')")
    @GetMapping("/{documentoId}/signed-url")
    public ResponseEntity<SignedUrlResponse> generarSignedUrl(
            @PathVariable UUID documentoId,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(
                documentoService.generarSignedUrl(documentoId, usuario.getId())
        );
    }

    @PreAuthorize("hasAuthority('DOCS_SUBIR')")
    @DeleteMapping("/{documentoId}")
    public ResponseEntity<Void> eliminarDocumento(
            @PathVariable UUID documentoId,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        documentoService.eliminarDocumento(documentoId, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}