package com.llosa.backend.pagos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.pagos.service.CronogramaPagoService;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Disabled;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class, com.llosa.backend.config.SecurityTestConfiguration.class})
class MinimalControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CronogramaPagoService cronogramaPagoService;

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @MockitoBean
    UsuarioRepository usuarioRepository;

    @Test
    void testGetWithUuid() throws Exception {
        mockMvc.perform(get("/api/cronogramas/{uuid}", UUID.randomUUID())
                        .with(authentication(new FirebaseAuthenticationToken("uid", "e@m.com", List.of(new SimpleGrantedAuthority("CONTRATO_VER"))))))
                .andExpect(status().isOk());
    }

    @Test
    void testPost() throws Exception {
        mockMvc.perform(post("/api/cronogramas")
                        .with(authentication(new FirebaseAuthenticationToken("uid", "e@m.com", List.of(new SimpleGrantedAuthority("CONTRATO_EDITAR")))))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPostWithAuth() throws Exception {
        mockMvc.perform(post("/api/cronogramas")
                        .with(authentication(new FirebaseAuthenticationToken("uid", "e@m.com", List.of(new SimpleGrantedAuthority("CONTRATO_EDITAR")))))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
