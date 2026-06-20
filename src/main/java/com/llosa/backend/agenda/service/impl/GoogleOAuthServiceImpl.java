package com.llosa.backend.agenda.service.impl;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.llosa.backend.agenda.service.GoogleOAuthService;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private final UsuarioRepository usuarioRepository;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR_EVENTS);

    private AuthorizationCodeFlow buildFlow() throws Exception {
        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                SCOPES)
                .setAccessType("offline")   // necesario para obtener refresh_token
                .build();
    }

    @Override
    public String generarUrlAutorizacion(Integer idGestor) {
        try {
            AuthorizationCodeFlow flow = buildFlow();
            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(String.valueOf(idGestor)) // identificamos al gestor en el callback
                    .set("prompt", "consent")           // fuerza a Google a reenviar refresh_token
                    .build();
        } catch (Throwable e) {
            log.error("[Google OAuth] Error al generar URL de autorización: {}", e.getMessage());
            throw new RuntimeException("No se pudo generar la URL de autorización de Google", e);
        }
    }

    @Override
    @Transactional
    public void procesarCallback(String code, String state) {
        Integer idGestor;
        try {
            idGestor = Integer.parseInt(state);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parámetro 'state' inválido");
        }

        Usuario gestor = usuarioRepository.findById(idGestor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        try {
            AuthorizationCodeFlow flow = buildFlow();

            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            Credential credential = flow.createAndStoreCredential(tokenResponse, idGestor.toString());

            String refreshToken = credential.getRefreshToken();
            if (refreshToken == null) {
                // Google solo manda refresh_token la PRIMERA vez que el usuario autoriza.
                // Si el gestor ya había autorizado antes y no se le pidió "prompt=consent",
                // puede llegar null. Como forzamos "prompt=consent" arriba, esto no debería pasar,
                // pero lo registramos por si acaso.
                log.warn("[Google OAuth] No se recibió refresh_token para gestor {}", idGestor);
            } else {
                gestor.setGoogleRefreshToken(refreshToken);
            }

            gestor.setGoogleCalendarConectado(true);
            usuarioRepository.save(gestor);

            log.info("[Google OAuth] Gestor {} conectó su Google Calendar exitosamente", idGestor);

        } catch (Throwable e) {
            log.error("[Google OAuth] Error al procesar callback para gestor {}: {}",
                    idGestor, e.getMessage());
            throw new RuntimeException("No se pudo completar la autorización con Google", e);
        }
    }

    @Override
    @Transactional
    public void desconectar(Integer idGestor) {
        Usuario gestor = usuarioRepository.findById(idGestor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        gestor.setGoogleRefreshToken(null);
        gestor.setGoogleCalendarConectado(false);
        usuarioRepository.save(gestor);

        log.info("[Google OAuth] Gestor {} desconectó su Google Calendar", idGestor);
    }
}