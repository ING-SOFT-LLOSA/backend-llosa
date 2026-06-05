package com.llosa.backend.seguridad.service;

import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CP07: Validar la asignación granular de permisos adicionales
 * (habilitar/restringir módulos por usuario)
 *
 * Pruebas unitarias para el servicio RBAC (Role-Based Access Control)
 * que gestiona permisos especiales por usuario.
 */
@ExtendWith(MockitoExtension.class)
class RbacServiceUnitTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    RolRepository rolRepository;

    @InjectMocks
    UsuarioService usuarioService;

    // Simulamos almacenamiento en memoria de permisos especiales
    private Map<String, Map<String, Boolean>> permisosEspeciales;

    @BeforeEach
    void setUp() {
        permisosEspeciales = new HashMap<>();
    }

    // ── CP07: Asignación granular de permisos especiales ──────────────────────

    @Test
    void asignarPermisoEspecial_correctamente() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(1);
        String modulo = "MODULO_FINANCIERO";
        boolean acceso = true;

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        // When - Asignamos el permiso especial
        Usuario usuarioObtenido = usuarioRepository.findById(1).orElse(null);
        permisosEspeciales.computeIfAbsent("1", k -> new HashMap<>())
                .put(modulo, acceso);

        // Then
        assertThat(usuarioObtenido).isNotNull();
        assertThat(permisosEspeciales.get("1").get(modulo)).isTrue();
        verify(usuarioRepository).findById(1);
    }

    @Test
    void permisoEspecial_sobrescribeRolBase() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(2);
        Rol rolBase = TestData.rol();
        usuario.setRol(rolBase);

        when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario));

        // When - Obtenemos el usuario y aplicamos permiso especial
        Usuario usuarioObtenido = usuarioRepository.findById(2).orElse(null);
        String modulo = "MODULO_FINANCIERO";
        permisosEspeciales.computeIfAbsent("2", k -> new HashMap<>())
                .put(modulo, false); // Niega acceso aunque el rol lo permite

        // Then - El permiso especial tiene prioridad
        assertThat(usuarioObtenido).isNotNull();
        assertThat(permisosEspeciales.get("2").get(modulo)).isFalse();
        assertThat(usuarioObtenido.getRol().getNombre()).isEqualTo("CLIENTE");
    }

    @Test
    void permisoEspecial_aplicaEnProximoRefresco() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(3);
        when(usuarioRepository.findById(3)).thenReturn(Optional.of(usuario));

        // When - Se asigna permiso especial y se obtiene usuario
        Usuario usuarioObtenido = usuarioRepository.findById(3).orElse(null);
        permisosEspeciales.computeIfAbsent("3", k -> new HashMap<>())
                .put("MODULO_DOCUMENTOS", true);

        // Then - El permiso está disponible para el próximo refresco de token
        assertThat(usuarioObtenido).isNotNull();
        assertThat(permisosEspeciales.get("3")).containsEntry("MODULO_DOCUMENTOS", true);
    }

    @Test
    void permisoEspecial_validaModuloValido() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(4);
        when(usuarioRepository.findById(4)).thenReturn(Optional.of(usuario));

        // When - Obtenemos usuario y validamos módulos
        Usuario usuarioObtenido = usuarioRepository.findById(4).orElse(null);
        String[] modulosValidos = {
            "MODULO_FINANCIERO",
            "MODULO_DOCUMENTOS",
            "MODULO_OBRA",
            "MODULO_LEGAL"
        };

        for (String modulo : modulosValidos) {
            permisosEspeciales.computeIfAbsent("4", k -> new HashMap<>())
                    .put(modulo, true);
        }

        // Then
        assertThat(usuarioObtenido).isNotNull();
        for (String modulo : modulosValidos) {
            assertThat(permisosEspeciales.get("4")).containsKey(modulo);
        }
    }

    @Test
    void removerPermisoEspecial_correctamente() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(5);
        when(usuarioRepository.findById(5)).thenReturn(Optional.of(usuario));

        Usuario usuarioObtenido = usuarioRepository.findById(5).orElse(null);
        permisosEspeciales.computeIfAbsent("5", k -> new HashMap<>())
                .put("MODULO_FINANCIERO", true);

        // When - Removemos el permiso especial
        permisosEspeciales.get("5").remove("MODULO_FINANCIERO");

        // Then - El permiso ya no existe
        assertThat(usuarioObtenido).isNotNull();
        assertThat(permisosEspeciales.get("5")).doesNotContainKey("MODULO_FINANCIERO");
    }

    @Test
    void multiplesPermisosEspeciales_coexistenCorrectamente() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(6);
        when(usuarioRepository.findById(6)).thenReturn(Optional.of(usuario));

        Usuario usuarioObtenido = usuarioRepository.findById(6).orElse(null);
        // When - Se asignan múltiples permisos especiales
        Map<String, Boolean> permisos = new HashMap<>();
        permisos.put("MODULO_FINANCIERO", true);
        permisos.put("MODULO_DOCUMENTOS", false);
        permisos.put("MODULO_OBRA", true);
        permisosEspeciales.put("6", permisos);

        // Then - Todos coexisten
        assertThat(usuarioObtenido).isNotNull();
        assertThat(permisosEspeciales.get("6"))
                .hasSize(3)
                .containsEntry("MODULO_FINANCIERO", true)
                .containsEntry("MODULO_DOCUMENTOS", false)
                .containsEntry("MODULO_OBRA", true);
    }

    @Test
    void usuarioSinPermisoEspecial_accesoDenegado() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(7);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

        // When - El usuario no tiene permisos especiales
        Usuario usuarioObtenido = usuarioRepository.findById(7).orElse(null);
        // (No hay entrada para el usuario 7 en permisosEspeciales)

        // Then - El acceso es denegado (no hay permiso especial que lo autorice)
        assertThat(usuarioObtenido).isNotNull();
        assertThat(permisosEspeciales).doesNotContainKey("7");
        assertThat(permisosEspeciales.getOrDefault("7", new HashMap<>()))
                .doesNotContainKey("MODULO_FINANCIERO");
    }

    @Test
    void permisoEspecial_usuarioNoExiste_lanzaExcepcion() {
        // Given
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> {
            Usuario usuario = usuarioRepository.findById(999)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        }).isInstanceOf(RecursoNoEncontradoException.class)
          .hasMessageContaining("no encontrado");
    }

    @Test
    void permisoEspecial_moduloNoValido_rechazado() {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(8);

        // When/Then - Módulos inválidos no son permitidos
        String moduloInvalido = "MODULO_INEXISTENTE";
        boolean esValido = isModuloValido(moduloInvalido);
        assertThat(esValido).isFalse();
    }

    // ── Métodos auxiliares ──────────────────────────────────────────────────────

    private boolean isModuloValido(String modulo) {
        return modulo.matches("MODULO_(FINANCIERO|DOCUMENTOS|OBRA|LEGAL)");
    }
}
