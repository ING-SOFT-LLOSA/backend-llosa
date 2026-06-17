package com.llosa.backend.agenda.service;

/**
 * Maneja el flujo OAuth 2.0 que permite a un gestor autorizar
 * el acceso de la app a su propio Google Calendar.
 *
 * A diferencia de la service account, esto permite crear eventos
 * con asistentes (attendees) reales, porque el evento se crea
 * en nombre del gestor (una persona real), no de una cuenta de servicio.
 */
public interface GoogleOAuthService {

    /**
     * Genera la URL de consentimiento de Google a la que el frontend
     * debe redirigir/abrir para que el gestor autorice el acceso.
     *
     * @param idGestor id del usuario (gestor) que está autorizando.
     * @return URL de autorización de Google.
     */
    String generarUrlAutorizacion(Integer idGestor);

    /**
     * Procesa el callback de Google: intercambia el "code" recibido
     * por un access token + refresh token, y guarda el refresh token
     * en el usuario correspondiente.
     *
     * @param code  código de autorización recibido en el query param.
     * @param state contiene el id del gestor que inició el flujo.
     */
    void procesarCallback(String code, String state);

    /**
     * Revoca/desconecta la cuenta de Google del gestor (borra el refresh token).
     */
    void desconectar(Integer idGestor);
}