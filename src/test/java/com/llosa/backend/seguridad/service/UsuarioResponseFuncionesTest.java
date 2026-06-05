package com.llosa.backend.seguridad.service;

import com.llosa.backend.config.TestData;
import com.llosa.backend.seguridad.dto.UsuarioResponseFunciones;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioResponseFuncionesTest {

    @Mock UsuarioRepository usuarioRepository;

    @InjectMocks UsuarioService usuarioService;

    @Test
    void fromEntity_conRol_devuelveDtoConFunciones() {
        Usuario u = TestData.usuario();
        Rol rol = TestData.rol();
        u.setRol(rol);

        UsuarioResponseFunciones dto = UsuarioResponseFunciones.fromEntity(u);

        assertThat(dto.nombre()).isEqualTo("Juan");
        assertThat(dto.funciones()).isNotEmpty();
        assertThat(dto.rol()).isEqualTo(rol);
    }

    @Test
    void fromEntity_sinRol_devuelveListaVacia() {
        Usuario u = TestData.usuario();
        u.setRol(null);

        UsuarioResponseFunciones dto = UsuarioResponseFunciones.fromEntity(u);

        assertThat(dto.funciones()).isEmpty();
        assertThat(dto.rol()).isNull();
    }

    @Test
    void listarPaginadoYFiltrado_devuelvePaginaMapeada() {
        Usuario u = TestData.usuario();
        Rol rol = TestData.rol();
        u.setRol(rol);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(u), pageable, 1);
        when(usuarioRepository.buscarUsuariosPaginados(eq(""), any(Pageable.class))).thenReturn(page);

        Page<UsuarioResponseFunciones> result = usuarioService.listarPaginadoYFiltrado("", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).nombre()).isEqualTo("Juan");
    }
}
