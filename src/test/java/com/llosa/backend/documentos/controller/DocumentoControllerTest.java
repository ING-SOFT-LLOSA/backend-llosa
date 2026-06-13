package com.llosa.backend.documentos.controller;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador REST de la Bóveda Digital (CP25, CP27, CP28).
 * Verifica el contrato HTTP y la resolución del usuario autenticado a partir del
 * UID de Firebase del token.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoControllerTest {

    @Mock DocumentoService documentoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock Authentication authentication;

    @InjectMocks DocumentoController controller;

    private static final String UID = "firebase-uid";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(7);
        u.setFirebaseUuid(UID);
        return u;
    }

    private DocumentoResponse dummy() {
        return new DocumentoResponse(UUID.randomUUID(), "minuta.pdf",
                TipoDocumento.PDF_LEGAL, "application/pdf", "ref", "REQUISITO", null, null);
    }

    @Test
    @CP(value = "CP25", scenario = "Subida vía endpoint REST",
            input = "multipart file + uid autenticado",
            expected = "200 OK y resuelve el usuario por firebaseUuid")
    void subirDocumento_resuelveUsuarioYDelegaServicio() {
        UUID refId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile(
                "file", "minuta.pdf", "application/pdf", new byte[]{1});
        SubirDocumentoRequest request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);

        when(authentication.getPrincipal()).thenReturn(UID);
        when(usuarioRepository.findByFirebaseUuid(UID)).thenReturn(Optional.of(usuario()));
        when(documentoService.subirDocumento(refId, file, request, 7)).thenReturn(dummy());

        ResponseEntity<DocumentoResponse> r =
                controller.subirDocumento(refId, file, request, authentication);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentoService).subirDocumento(refId, file, request, 7);
    }

    @Test
    void subirDocumento_usuarioNoEncontrado_lanzaException() {
        UUID refId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile("file", new byte[]{1});
        SubirDocumentoRequest request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);

        when(authentication.getPrincipal()).thenReturn(UID);
        when(usuarioRepository.findByFirebaseUuid(UID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.subirDocumento(refId, file, request, authentication))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(documentoService);
    }

    @Test
    void listar_devuelve200ConDocumentos() {
        UUID refId = UUID.randomUUID();
        when(documentoService.listar(refId, TipoDocumento.PDF_LEGAL))
                .thenReturn(List.of(dummy()));

        ResponseEntity<List<DocumentoResponse>> r =
                controller.listar(refId, TipoDocumento.PDF_LEGAL);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(1);
    }

    @Test
    @CP(value = "CP27", scenario = "Signed URL vía endpoint",
            input = "documentoId",
            expected = "200 OK con URL firmada")
    void generarSignedUrl_devuelve200() {
        UUID docId = UUID.randomUUID();
        when(documentoService.generarSignedUrl(docId))
                .thenReturn(new SignedUrlResponse("https://signed", Instant.now()));

        ResponseEntity<SignedUrlResponse> r = controller.generarSignedUrl(docId);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().url()).isEqualTo("https://signed");
    }

    @Test
    void eliminarDocumento_devuelve204() {
        UUID docId = UUID.randomUUID();

        ResponseEntity<Void> r = controller.eliminarDocumento(docId);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(documentoService).eliminarDocumento(docId);
    }
}
