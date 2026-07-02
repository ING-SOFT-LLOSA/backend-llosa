package com.llosa.backend.pagos.controller;

import com.llosa.backend.pagos.dto.PagoRequest;
import com.llosa.backend.pagos.dto.PagoResponse;
import com.llosa.backend.pagos.service.PagoService;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/api/cronogramas/{uuidCronograma}/pagos")
    @PreAuthorize("hasAuthority('PAGOS_VER')")
    public ResponseEntity<List<PagoResponse>> listarPorCronograma(@PathVariable UUID uuidCronograma) {
        return ResponseEntity.ok(pagoService.listarPorCronograma(uuidCronograma));
    }

    @PostMapping("/api/cronogramas/{uuidCronograma}/pagos")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<PagoResponse> agregarCuota(
            @PathVariable UUID uuidCronograma,
            @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.agregarCuota(uuidCronograma, request));
    }

    @PutMapping("/api/pagos/{uuidPago}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<PagoResponse> actualizarCuota(
            @PathVariable UUID uuidPago,
            @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(pagoService.actualizarCuota(uuidPago, request));
    }

    @DeleteMapping("/api/pagos/{uuidPago}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> eliminarCuota(@PathVariable UUID uuidPago) {
        pagoService.eliminarCuota(uuidPago);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/pagos/{uuidPago}/estado")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<PagoResponse> cambiarEstado(
            @PathVariable UUID uuidPago,
            @RequestParam String estado,
            Authentication authentication) {
        String uid = (String) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return ResponseEntity.ok(pagoService.cambiarEstado(uuidPago, estado, usuario.getId()));
    }

    @PostMapping(value = "/api/pagos/{uuidPago}/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<PagoResponse> subirComprobante(
            @PathVariable UUID uuidPago,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "comentario", required = false) String comentario,
            Authentication authentication) {
        String uid = (String) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return ResponseEntity.ok(pagoService.subirComprobante(uuidPago, file, usuario.getId(), comentario));
    }
}
