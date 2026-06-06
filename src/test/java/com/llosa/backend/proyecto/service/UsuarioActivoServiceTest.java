package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.impl.UsuarioActivoServiceImpl;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
class UsuarioActivoServiceTest {

    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock UsuarioService usuarioService;
    @Mock ActivoService activoService;

    @InjectMocks UsuarioActivoServiceImpl service;

    private UsuarioActivo buildExpediente() {
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        Activo activo = Activo.builder()
                .id(UUID.randomUUID()).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso)
                .build();
        return UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activo(activo)
                .faseComercial("Separación")
                .estadoTramiteLegal("Pendiente")
                .build();
    }

    @Test
    void findById_encontrado_devuelveExpediente() {
        UsuarioActivo ua = buildExpediente();
        when(usuarioActivoRepository.findById(ua.getUuidUsuarioActivo())).thenReturn(Optional.of(ua));

        UsuarioActivo result = service.findById(ua.getUuidUsuarioActivo());

        assertThat(result).isEqualTo(ua);
    }

    @Test
    void findById_noEncontrado_lanzaEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(usuarioActivoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Expediente de Usuario-Activo no encontrado");
    }

    @Test
    void findByUsuario_devuelveListaPorCliente() {
        UsuarioActivo ua = buildExpediente();
        when(usuarioActivoRepository.findByClienteId(1)).thenReturn(List.of(ua));

        List<UsuarioActivo> result = service.findByUsuario(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void updateCustomerJourney_actualizaCamposNoNulos() {
        UsuarioActivo ua = buildExpediente();
        when(usuarioActivoRepository.findById(ua.getUuidUsuarioActivo())).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);

        UsuarioActivo result = service.updateCustomerJourney(
                ua.getUuidUsuarioActivo(), "Contrato", "Minuta firmada");

        assertThat(result.getFaseComercial()).isEqualTo("Contrato");
        assertThat(result.getEstadoTramiteLegal()).isEqualTo("Minuta firmada");
    }

    @Test
    void updateCustomerJourney_camposNulos_noActualiza() {
        UsuarioActivo ua = buildExpediente();
        when(usuarioActivoRepository.findById(ua.getUuidUsuarioActivo())).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);

        service.updateCustomerJourney(ua.getUuidUsuarioActivo(), null, null);

        assertThat(ua.getFaseComercial()).isEqualTo("Separación");
        assertThat(ua.getEstadoTramiteLegal()).isEqualTo("Pendiente");
    }

    @Test
    void save_delegaEnRepository() {
        UsuarioActivo ua = buildExpediente();
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);

        UsuarioActivo result = service.save(ua);

        assertThat(result).isEqualTo(ua);
    }

    @Test
    void asignarActivo_creaExpedienteConCopropietarios() {
        UUID idActivo = UUID.randomUUID();
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        Activo activo = Activo.builder()
                .id(idActivo).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso)
                .build();
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Juan");

        when(usuarioService.findById(1)).thenReturn(usuario);
        when(activoService.findById(idActivo)).thenReturn(activo);

        AsignarActivoDTO dto = new AsignarActivoDTO(List.of(1), idActivo,
                "Crédito Hipotecario", "Separación", "Pendiente", null);

        service.asignarActivo(dto);

        verify(usuarioActivoRepository).save(any(UsuarioActivo.class));
    }

    @Test
    void findByActivo_encontrado_devuelveOptional() {
        UUID activoId = UUID.randomUUID();
        UsuarioActivo ua = buildExpediente();
        when(usuarioActivoRepository.findByActivo_Id(activoId)).thenReturn(Optional.of(ua));

        Optional<UsuarioActivo> result = service.findByActivo(activoId);

        assertThat(result).isPresent();
    }

    @Test
    void findByActivo_noEncontrado_devuelveEmpty() {
        UUID activoId = UUID.randomUUID();
        when(usuarioActivoRepository.findByActivo_Id(activoId)).thenReturn(Optional.empty());

        Optional<UsuarioActivo> result = service.findByActivo(activoId);

        assertThat(result).isEmpty();
    }

    // CP16: asignación en bloque — múltiples copropietarios en un mismo activo
    @Test
    void asignarActivo_multiples_copropietarios_creaExpedienteConTodos() {
        UUID idActivo = UUID.randomUUID();
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        Activo activo = Activo.builder()
                .id(idActivo).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso).build();

        Usuario u1 = new Usuario(); u1.setId(1); u1.setNombre("Ana");
        Usuario u2 = new Usuario(); u2.setId(2); u2.setNombre("Pedro");

        when(usuarioService.findById(1)).thenReturn(u1);
        when(usuarioService.findById(2)).thenReturn(u2);
        when(activoService.findById(idActivo)).thenReturn(activo);

        AsignarActivoDTO dto = new AsignarActivoDTO(
                List.of(1, 2), idActivo, "Crédito Hipotecario", "Separación", "Pendiente", null);

        service.asignarActivo(dto);

        verify(usuarioActivoRepository).save(argThat(ua ->
                ua.getClientes().size() == 2
                && ua.getClientes().contains(u1)
                && ua.getClientes().contains(u2)));
    }

    // CP19: al perder la única unidad, el perfil del cliente pasa a Inactivo
    @Test
    void deleteById_ultimaUnidad_desactivaCliente() throws Exception {
        Usuario cliente = new Usuario(); cliente.setId(42);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        Activo activo = Activo.builder()
                .id(UUID.randomUUID()).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso).build();

        UsuarioActivo ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activo(activo)
                .clientes(new java.util.ArrayList<>(List.of(cliente)))
                .build();

        when(usuarioActivoRepository.findById(ua.getUuidUsuarioActivo())).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.findByClienteId(42)).thenReturn(List.of()); // sin más activos
        doNothing().when(usuarioService).cambiarEstado(42, false);

        service.deleteById(ua.getUuidUsuarioActivo());

        verify(usuarioService).cambiarEstado(42, false);
    }

    // CP20: al tener otras unidades activas, el perfil del cliente NO se desactiva
    @Test
    void deleteById_conOtrasUnidades_noDesactivaCliente() throws Exception {
        Usuario cliente = new Usuario(); cliente.setId(42);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        Activo activo = Activo.builder()
                .id(UUID.randomUUID()).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso).build();

        UsuarioActivo ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activo(activo)
                .clientes(new java.util.ArrayList<>(List.of(cliente)))
                .build();

        UsuarioActivo otraUA = buildExpediente();

        when(usuarioActivoRepository.findById(ua.getUuidUsuarioActivo())).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.findByClienteId(42)).thenReturn(List.of(otraUA)); // aún tiene otra unidad

        service.deleteById(ua.getUuidUsuarioActivo());

        verify(usuarioService, never()).cambiarEstado(anyInt(), anyBoolean());
    }

    @Test
    void deleteById_llamaRepository() {
        UUID id = UUID.randomUUID();
        // buildExpediente() no tiene clientes → no se evalúa CP19
        UsuarioActivo ua = buildExpediente();
        when(usuarioActivoRepository.findById(id)).thenReturn(Optional.of(ua));

        service.deleteById(id);

        verify(usuarioActivoRepository).deleteById(id);
    }
}
