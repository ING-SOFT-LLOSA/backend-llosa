package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.seguridad.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class UsuarioActivoDtoTest {

    @Test
    void clienteSimpleDTO_fromEntity_mapea() {
        Usuario u = new Usuario();
        u.setId(1);
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setDocumentoIdentidad("12345678");
        u.setEmail("ana@test.com");
        u.setTelefono("999000111");

        UsuarioActivoResponseDTO.ClienteSimpleDTO dto =
                UsuarioActivoResponseDTO.ClienteSimpleDTO.fromEntity(u);

        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.nombre()).isEqualTo("Ana");
        assertThat(dto.apellidos()).isEqualTo("García");
        assertThat(dto.email()).isEqualTo("ana@test.com");
        assertThat(dto.telefono()).isEqualTo("999000111");
    }

    @Test
    void usuarioActivoResponseDTO_fromEntity_mapea() {
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        Activo activo = Activo.builder()
                .id(UUID.randomUUID()).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso)
                .build();

        Usuario u = new Usuario();
        u.setId(1);
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setEmail("ana@test.com");

        UsuarioActivo ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activo(activo)
                .clientes(List.of(u))
                .faseComercial("Separación")
                .estadoTramiteLegal("Pendiente")
                .tipoFinanciamiento("Crédito Hipotecario")
                .fechaAdquisicion(LocalDateTime.now())
                .build();

        UsuarioActivoResponseDTO dto = UsuarioActivoResponseDTO.fromEntity(ua);

        assertThat(dto.faseComercial()).isEqualTo("Separación");
        assertThat(dto.clientes()).hasSize(1);
        assertThat(dto.clientes().get(0).nombre()).isEqualTo("Ana");
    }
}
