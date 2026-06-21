package com.llosa.backend.comercial.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import com.llosa.backend.comercial.service.RequisitoDocumentalService;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequisitoDocumentalController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class RequisitoDocumentalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean RequisitoDocumentalService requisitoService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private static FirebaseAuthenticationToken docsAuthToken() {
        return new FirebaseAuthenticationToken("test-uid", "test@test.com",
                List.of(
                        new SimpleGrantedAuthority("DOCS_SUBIR"),
                        new SimpleGrantedAuthority("CONTRATO_VER")
                ));
    }

    private RequisitoDocumental buildRequisito(UUID id) {
        EtapaExpediente etapa = EtapaExpediente.builder()
                .uuidEtapaExpediente(UUID.randomUUID())
                .build();
        return RequisitoDocumental.builder()
                .id(id)
                .etapaExpediente(etapa)
                .titulo("DNI del titular")
                .descripcion("Copia del documento de identidad")
                .estado(EtapaRequisitoDocumental.PENDIENTE)
                .fechaEmision(LocalDate.now())
                .build();
    }

    @Test
    void subirArchivoRequisito_sinAutenticar_devuelve403() throws Exception {
        var file = new MockMultipartFile("file", "dni.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/requisitos-documentales/{requisitoId}/upload", UUID.randomUUID())
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void subirArchivoRequisito_autenticado_devuelve200() throws Exception {
        UUID requisitoId = UUID.randomUUID();
        RequisitoDocumental requisito = buildRequisito(requisitoId);
        when(requisitoService.asociarArchivoARequisito(eq(requisitoId), any(), eq("test-uid")))
                .thenReturn(requisito);

        var file = new MockMultipartFile("file", "dni.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/requisitos-documentales/{requisitoId}/upload", requisitoId)
                        .file(file)
                        .with(authentication(docsAuthToken()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("DNI del titular"));
    }

    @Test
    void eliminarArchivoRequisito_autenticado_devuelve204() throws Exception {
        UUID requisitoId = UUID.randomUUID();
        doNothing().when(requisitoService).eliminarArchivoDeRequisito(requisitoId, "test-uid");

        mockMvc.perform(delete("/api/requisitos-documentales/" + requisitoId + "/upload")
                        .with(authentication(docsAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void crearRequisito_valido_devuelve201() throws Exception {
        UUID id = UUID.randomUUID();
        RequisitoDocumental requisito = buildRequisito(id);
        RequisitoCreateRequest request = new RequisitoCreateRequest(
                UUID.randomUUID(), "DNI del titular", "Copia del documento de identidad",
                null, LocalDate.now(), null);
        when(requisitoService.crearRequisito(any(RequisitoCreateRequest.class))).thenReturn(requisito);

        mockMvc.perform(post("/api/requisitos-documentales")
                        .with(authentication(docsAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("DNI del titular"));
    }

    @Test
    void actualizarRequisito_valido_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        RequisitoDocumental requisito = buildRequisito(id);
        RequisitoUpdateRequest request = new RequisitoUpdateRequest(
                "DNI actualizado", "Desc", null, EtapaRequisitoDocumental.COMPLETADO, LocalDate.now(), null);
        when(requisitoService.actualizarRequisito(eq(id), any(RequisitoUpdateRequest.class))).thenReturn(requisito);

        mockMvc.perform(put("/api/requisitos-documentales/" + id)
                        .with(authentication(docsAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarRequisitoTotalmente_autenticado_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(requisitoService).eliminarRequisitoTotalmente(id, "test-uid");

        mockMvc.perform(delete("/api/requisitos-documentales/" + id)
                        .with(authentication(docsAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
