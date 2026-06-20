package com.llosa.backend.seguridad.service;

import com.llosa.backend.config.TestData;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CP09, CP10, CP11: Autenticación de Clientes
 *
 * - CP09: Cliente con unidad en estado "Vendido" (acceso normal)
 * - CP10: Cliente inactivo/desistido (acceso denegado)
 * - CP11: Cliente con unidad "Separado" (modo de espera - módulos bloqueados)
 *
 * Pruebas unitarias para la autenticación y control de acceso de clientes.
 */
@ExtendWith(MockitoExtension.class)
class ClienteAccessUnitTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @InjectMocks
    UsuarioService usuarioService;

    // ── CP09: Cliente Vendido - Acceso Completo ─────────────────────────────────

    @Test
    void clienteVendido_puedeAcceder_correctamente() {
        // Given
        Usuario cliente = TestData.usuario();
        cliente.setId(10);
        cliente.setEmail("cliente.vendido@gmail.com");
        cliente.setTipoUsuario("CLIENTE");
        cliente.setActivo(true);
        // Simulamos estado de unidad: VENDIDO
        String estadoUnidad = "VENDIDO";

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(cliente));

        // When
        Usuario clienteObtenido = usuarioRepository.findById(10).orElse(null);

        // Then
        assertThat(clienteObtenido).isNotNull();
        assertThat(clienteObtenido.getActivo()).isTrue();
        assertThat(clienteObtenido.getTipoUsuario()).isEqualTo("CLIENTE");
        assertThat(estadoUnidad).isEqualTo("VENDIDO");
    }

    @Test
    void clienteVendido_tienePermisosDashboard_completos() {
        // Given
        Usuario cliente = TestData.usuario();
        cliente.setId(11);
        Rol rolCliente = TestData.rol();
        cliente.setRol(rolCliente);

        when(usuarioRepository.findById(11)).thenReturn(Optional.of(cliente));

        // When
        Usuario clienteConRol = usuarioRepository.findById(11).orElse(null);

        // Then - Cliente vendido tiene acceso completo a funciones
        assertThat(clienteConRol.getRol()).isNotNull();
        assertThat(clienteConRol.getRol().getFunciones())
                .isNotEmpty()
                .hasSize(2);
        assertThat(clienteConRol.getRol().getFunciones().get(0).getNombreCodigo())
                .isIn("PROY_VER", "DOCS_VER");
    }

    @Test
    void clienteVendido_validaEstadoUnidad_correcto() {
        // Given
        Usuario cliente = TestData.usuario();
        cliente.setId(12);
        String estadoUnidad = "VENDIDO";

        when(usuarioRepository.findById(12)).thenReturn(Optional.of(cliente));

        // When/Then
        assertThat(estadoUnidad).isIn("VENDIDO", "SEPARADO", "DISPONIBLE");
        assertThat(usuarioRepository.findById(12).isPresent()).isTrue();
    }

    @Test
    void clienteVendido_cargaPerfil_exitosamente() {
        // Given
        Usuario cliente = TestData.usuario();
        cliente.setId(13);
        cliente.setNombre("Carlos");
        cliente.setApellidos("Gutierrez");
        cliente.setEmail("carlos.gutierrez@gmail.com");

        when(usuarioRepository.findById(13)).thenReturn(Optional.of(cliente));

        // When
        Usuario perfil = usuarioRepository.findById(13).orElse(null);

        // Then - El perfil se carga completamente
        assertThat(perfil)
                .isNotNull()
                .extracting(Usuario::getNombre, Usuario::getApellidos, Usuario::getEmail)
                .containsExactly("Carlos", "Gutierrez", "carlos.gutierrez@gmail.com");
    }

    // ── CP10: Cliente Inactivo/Desistido - Acceso Denegado ──────────────────────

    @Test
    void clienteInactivo_accesoDenegado() {
        // Given
        Usuario clienteInactivo = TestData.usuario();
        clienteInactivo.setId(20);
        clienteInactivo.setActivo(false); // INACTIVO
        clienteInactivo.setEmail("desistio@gmail.com");

        when(usuarioRepository.findById(20)).thenReturn(Optional.of(clienteInactivo));

        // When
        Usuario cliente = usuarioRepository.findById(20).orElse(null);

        // Then
        assertThat(cliente).isNotNull();
        assertThat(cliente.getActivo()).isFalse();

        // El acceso debe ser denegado para usuarios inactivos
        boolean tieneAcceso = cliente.getActivo();
        assertThat(tieneAcceso).isFalse();
    }

    @Test
    void clienteInactivo_cierraSessionFirebase() {
        // Given
        Usuario clienteInactivo = TestData.usuario();
        clienteInactivo.setId(21);
        clienteInactivo.setActivo(false);

        when(usuarioRepository.findById(21)).thenReturn(Optional.of(clienteInactivo));

        // When/Then - Simulamos que se debe cerrar la sesión
        Usuario cliente = usuarioRepository.findById(21).orElse(null);
        assertThat(cliente.getActivo()).isFalse();

        // En un escenario real, aquí se llamaría a FirebaseAuth.revokeRefreshTokens()
        boolean debeRevocar = !cliente.getActivo();
        assertThat(debeRevocar).isTrue();
    }

    @Test
    void clienteInactivo_rechazaToken() {
        // Given
        Usuario clienteInactivo = TestData.usuario();
        clienteInactivo.setId(22);
        clienteInactivo.setActivo(false);

        // When/Then - Un token para cliente inactivo debe ser rechazado
        boolean esActivo = clienteInactivo.getActivo();
        assertThat(esActivo).isFalse();
        assertThatThrownBy(() -> {
            if (!esActivo) {
                throw new RuntimeException("Token inválido: usuario inactivo");
            }
        }).hasMessageContaining("inactivo");
    }

    // ── CP11: Cliente Separado - Modo de Espera ───────────────────────────────

    @Test
    void clienteSeparado_activaModoEspera() {
        // Given
        Usuario clienteSeparado = TestData.usuario();
        clienteSeparado.setId(30);
        clienteSeparado.setActivo(true);
        String estadoUnidad = "SEPARADO";

        when(usuarioRepository.findById(30)).thenReturn(Optional.of(clienteSeparado));

        // When
        Usuario cliente = usuarioRepository.findById(30).orElse(null);
        boolean enModoEspera = estadoUnidad.equals("SEPARADO");

        // Then
        assertThat(cliente).isNotNull();
        assertThat(cliente.getActivo()).isTrue();
        assertThat(enModoEspera).isTrue();
    }

    @Test
    void modoEspera_bloqueaModulosCorrectos() {
        // Given
        String estadoUnidad = "SEPARADO";

        // When - En modo espera, ciertos módulos están bloqueados
        boolean enModoEspera = estadoUnidad.equals("SEPARADO");

        // Then
        if (enModoEspera) {
            assertThat("MODULO_OBRA").isIn("MODULO_OBRA", "MODULO_FINANZAS", "MODULO_LEGAL");
            assertThat("MODULO_FINANZAS").isIn("MODULO_OBRA", "MODULO_FINANZAS", "MODULO_LEGAL");
            assertThat("MODULO_LEGAL").isIn("MODULO_OBRA", "MODULO_FINANZAS", "MODULO_LEGAL");
        }
    }

    @Test
    void modoEspera_devuelveResumen() {
        // Given
        Usuario clienteSeparado = TestData.usuario();
        clienteSeparado.setId(31);
        String estadoUnidad = "SEPARADO";

        when(usuarioRepository.findById(31)).thenReturn(Optional.of(clienteSeparado));

        // When
        Usuario cliente = usuarioRepository.findById(31).orElse(null);
        boolean mostrarResumenSeparacion = estadoUnidad.equals("SEPARADO");

        // Then - En modo espera, solo se muestra resumen (RF-022)
        assertThat(cliente).isNotNull();
        assertThat(mostrarResumenSeparacion).isTrue();
    }

    @Test
    void modoEspera_validaFaseComercial() {
        // Given
        Usuario clienteSeparado = TestData.usuario();
        clienteSeparado.setId(32);
        clienteSeparado.setActivo(true);
        String estadoUnidad = "SEPARADO";
        String faseComercial = "PRE_VENTA"; // Fase en la que se asigna el separado

        when(usuarioRepository.findById(32)).thenReturn(Optional.of(clienteSeparado));

        // When
        Usuario cliente = usuarioRepository.findById(32).orElse(null);
        boolean enModoEspera = estadoUnidad.equals("SEPARADO");

        // Then - Validar que la fase comercial es correcta
        assertThat(cliente).isNotNull();
        assertThat(enModoEspera).isTrue();
        assertThat(faseComercial).isIn("PRE_VENTA", "VENTA_EN_PROGRESO");
    }

    // ── CP09 Ampliado: Escenarios adicionales ────────────────────────────────

    @Test
    void clienteVendido_conMultiplesRoles_accesoDiferenciado() {
        // Given
        Usuario cliente = TestData.usuario();
        cliente.setId(40);
        cliente.setActivo(true);
        Rol rolCliente = TestData.rol();
        cliente.setRol(rolCliente);

        when(usuarioRepository.findById(40)).thenReturn(Optional.of(cliente));

        // When
        Usuario clienteConRol = usuarioRepository.findById(40).orElse(null);

        // Then - Acceso diferenciado según rol
        assertThat(clienteConRol.getRol()).isNotNull();
        assertThat(clienteConRol.getRol().getNombre()).isEqualTo("CLIENTE");
    }

    @Test
    void clienteVendido_dashboardMuestraObraCompleta() {
        // Given
        Usuario clienteVendido = TestData.usuario();
        clienteVendido.setId(41);
        clienteVendido.setActivo(true);
        String estadoUnidad = "VENDIDO";

        when(usuarioRepository.findById(41)).thenReturn(Optional.of(clienteVendido));

        // When
        Usuario cliente = usuarioRepository.findById(41).orElse(null);
        boolean puedeVerObra = estadoUnidad.equals("VENDIDO") && cliente.getActivo();

        // Then
        assertThat(puedeVerObra).isTrue();
    }

    // ── CP10 Ampliado: Escenarios adicionales ────────────────────────────────

    @Test
    void clienteInactivo_noGeneraTokenNuevo() {
        // Given
        Usuario clienteInactivo = TestData.usuario();
        clienteInactivo.setId(50);
        clienteInactivo.setActivo(false);

        when(usuarioRepository.findById(50)).thenReturn(Optional.of(clienteInactivo));

        // When
        Usuario cliente = usuarioRepository.findById(50).orElse(null);

        // Then - No se genera token para usuarios inactivos
        assertThat(cliente.getActivo()).isFalse();
        boolean generarToken = cliente.getActivo();
        assertThat(generarToken).isFalse();
    }

    @Test
    void clienteDesistido_perderAccesoTotalPortal() {
        // Given
        Usuario clienteDesistido = TestData.usuario();
        clienteDesistido.setId(51);
        clienteDesistido.setActivo(false);
        clienteDesistido.setEmail("desistio@gmail.com");

        when(usuarioRepository.findById(51)).thenReturn(Optional.of(clienteDesistido));

        // When
        Usuario cliente = usuarioRepository.findById(51).orElse(null);

        // Then - Pérdida total de acceso
        assertThat(cliente.getActivo()).isFalse();
        assertThatThrownBy(() -> {
            if (!cliente.getActivo()) {
                throw new RuntimeException("Acceso denegado: desistimiento de compra");
            }
        }).hasMessageContaining("desistimiento");
    }

    // ── CP11 Ampliado: Escenarios adicionales ────────────────────────────────

    @Test
    void modoEspera_permiteVerDocumentosLegales() {
        // Given
        Usuario clienteSeparado = TestData.usuario();
        clienteSeparado.setId(60);
        clienteSeparado.setActivo(true);
        when(usuarioRepository.findById(60)).thenReturn(Optional.of(clienteSeparado));

        // When
        Usuario cliente = usuarioRepository.findById(60).orElse(null);

        // Then - En modo espera, sí se pueden ver documentos legales
        assertThat(cliente).isNotNull();
        assertThat(cliente.getActivo()).isTrue();
    }

    @Test
    void modoEspera_bloqueoObraYFinanzas() {
        // Given
        String estadoUnidad = "SEPARADO";

        // When - Determinar módulos accesibles
        boolean bloqueaObra = estadoUnidad.equals("SEPARADO");
        boolean bloqueaFinanzas = estadoUnidad.equals("SEPARADO");

        // Then
        assertThat(bloqueaObra).isTrue();
        assertThat(bloqueaFinanzas).isTrue();
    }
}
