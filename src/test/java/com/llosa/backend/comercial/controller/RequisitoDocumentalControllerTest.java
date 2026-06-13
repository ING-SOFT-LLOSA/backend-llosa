package com.llosa.backend.comercial.controller;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.service.RequisitoDocumentalService;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador REST de Requisitos Documentales (CP25).
 * Verifica el contrato HTTP y el paso del UID de Firebase al servicio.
 */
@ExtendWith(MockitoExtension.class)
class RequisitoDocumentalControllerTest {

    @Mock RequisitoDocumentalService requisitoService;
    @Mock Authentication authentication;

    @InjectMocks RequisitoDocumentalController controller;

    private static final String UID = "firebase-uid";

    @Test
    @CP(value = "CP25", scenario = "Subir archivo del requisito vía REST",
            input = "requisitoId + file + uid",
            expected = "200 OK; delega con el uid autenticado")
    void subirArchivoRequisito_devuelve200() {
        UUID reqId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile(
                "file", "minuta.pdf", "application/pdf", new byte[]{1});
        when(authentication.getPrincipal()).thenReturn(UID);

        ResponseEntity<Void> r = controller.subirArchivoRequisito(reqId, file, authentication);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(requisitoService).asociarArchivoARequisito(reqId, file, UID);
    }

    @Test
    void eliminarArchivoRequisito_devuelve204() {
        UUID reqId = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn(UID);

        ResponseEntity<Void> r = controller.eliminarArchivoRequisito(reqId, authentication);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(requisitoService).eliminarArchivoDeRequisito(reqId, UID);
    }

    @Test
    void crearRequisito_devuelve201() {
        RequisitoCreateRequest request = new RequisitoCreateRequest(
                UUID.randomUUID(), "DNI", "desc", null, null);

        ResponseEntity<Void> r = controller.crearRequisito(request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(requisitoService).crearRequisito(request);
    }

    @Test
    void actualizarRequisito_devuelve200() {
        UUID id = UUID.randomUUID();
        RequisitoUpdateRequest request = new RequisitoUpdateRequest("T", "D", "N", "COMPLETADA");

        ResponseEntity<Void> r = controller.actualizarRequisito(id, request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(requisitoService).actualizarRequisito(id, request);
    }

    @Test
    void eliminarRequisitoTotalmente_devuelve204() {
        UUID id = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn(UID);

        ResponseEntity<Void> r = controller.eliminarRequisitoTotalmente(id, authentication);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(requisitoService).eliminarRequisitoTotalmente(id, UID);
    }
}
