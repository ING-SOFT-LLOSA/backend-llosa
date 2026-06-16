package com.llosa.backend.documentos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentoController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class DocumentoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean DocumentoService documentoService;
    @MockitoBean UsuarioRepository usuarioRepository;
    @MockitoBean FirebaseConfig firebaseConfig;

    private static FirebaseAuthenticationToken docsAuthToken() {
        return new FirebaseAuthenticationToken("test-uid", "test@test.com",
                List.of(
                        new SimpleGrantedAuthority("DOCS_SUBIR"),
                        new SimpleGrantedAuthority("DOCS_VER")
                ));
    }

    @Test
    void subirDocumento_sinAutenticar_devuelve403() throws Exception {
        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});
        var requestPart = new MockMultipartFile("data", "", "application/json",
                objectMapper.writeValueAsBytes(new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL)));

        mockMvc.perform(multipart("/api/documentos/{idReferencia}", UUID.randomUUID())
                        .file(file)
                        .file(requestPart)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void subirDocumento_autenticado_devuelve200() throws Exception {
        UUID idRef = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(1);
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.of(usuario));

        var response = new DocumentoResponse(UUID.randomUUID(), "doc.pdf", TipoDocumento.PDF_LEGAL, "application/pdf",
                idRef.toString(), "PROYECTO", LocalDateTime.now(), null);
        when(documentoService.subirDocumento(eq(idRef), any(), any(), eq(1))).thenReturn(response);

        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
        var requestPart = new MockMultipartFile("data", "", "application/json",
                objectMapper.writeValueAsBytes(new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL)));

        mockMvc.perform(multipart("/api/documentos/{idReferencia}", idRef)
                        .file(file)
                        .file(requestPart)
                        .with(authentication(docsAuthToken()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreOriginal").value("doc.pdf"));
    }

    @Test
    void subirDocumento_usuarioNoEncontrado_lanzaExcepcion() throws Exception {
        UUID idRef = UUID.randomUUID();
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.empty());

        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
        var requestPart = new MockMultipartFile("data", "", "application/json",
                objectMapper.writeValueAsBytes(new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL)));

        mockMvc.perform(multipart("/api/documentos/{idReferencia}", idRef)
                        .file(file)
                        .file(requestPart)
                        .with(authentication(docsAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/documentos/{idReferencia}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listar_autenticado_devuelveLista() throws Exception {
        UUID idRef = UUID.randomUUID();
        var response = List.of(new DocumentoResponse(UUID.randomUUID(), "doc.pdf", TipoDocumento.PDF_LEGAL, "application/pdf",
                idRef.toString(), "PROYECTO", LocalDateTime.now(), null));
        when(documentoService.listar(idRef, null)).thenReturn(response);

        mockMvc.perform(get("/api/documentos/{idReferencia}", idRef)
                        .with(authentication(docsAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreOriginal").value("doc.pdf"));
    }

    @Test
    void listar_conTipoDocumento_filtra() throws Exception {
        UUID idRef = UUID.randomUUID();
        when(documentoService.listar(idRef, TipoDocumento.PDF_LEGAL)).thenReturn(List.of());

        mockMvc.perform(get("/api/documentos/{idReferencia}", idRef)
                        .param("tipoDocumento", "PDF_LEGAL")
                        .with(authentication(docsAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void generarSignedUrl_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/documentos/{id}/signed-url", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void generarSignedUrl_autenticado_devuelveUrl() throws Exception {
        UUID docId = UUID.randomUUID();
        var response = new SignedUrlResponse("https://signed.url/doc", Instant.now().plusSeconds(900));
        when(documentoService.generarSignedUrl(docId)).thenReturn(response);

        mockMvc.perform(get("/api/documentos/{id}/signed-url", docId)
                        .with(authentication(docsAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://signed.url/doc"));
    }

    @Test
    void eliminarDocumento_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/documentos/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminarDocumento_autenticado_devuelve204() throws Exception {
        UUID docId = UUID.randomUUID();
        doNothing().when(documentoService).eliminarDocumento(docId);

        mockMvc.perform(delete("/api/documentos/{id}", docId)
                        .with(authentication(docsAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
