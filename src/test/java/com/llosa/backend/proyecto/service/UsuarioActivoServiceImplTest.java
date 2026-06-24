package com.llosa.backend.proyecto.service;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.factory.FlujoComercialFactory;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.dto.request.CrearContratoDTO;
import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.impl.UsuarioActivoServiceImpl;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioActivoServiceImplTest {

    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock ActivoService activoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock FlujoComercialFactory flujoComercialFactory;
    @Mock ActivoRepository activoRepository;

    @InjectMocks UsuarioActivoServiceImpl usuarioActivoService;

    private final UUID uuid = UUID.randomUUID();

    private Piso buildPiso() {
        var proyecto = Proyecto.builder().id(UUID.randomUUID()).nombre("Test Proyecto").build();
        var torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        return Piso.builder().id(1L).nroPiso(1).torre(torre).build();
    }

    @Test
    void findById_exitoso() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        assertThat(usuarioActivoService.findById(uuid)).isEqualTo(ua);
    }

    @Test
    void findById_noExiste_lanzaEntityNotFound() {
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> usuarioActivoService.findById(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByUsuario_retornaLista() {
        when(usuarioActivoRepository.findByClienteId(1)).thenReturn(List.of());
        assertThat(usuarioActivoService.findByUsuario(1)).isEmpty();
    }

    @Test
    void findByActivo_retornaOptional() {
        when(usuarioActivoRepository.findByActivos_Id(uuid)).thenReturn(Optional.empty());
        assertThat(usuarioActivoService.findByActivo(uuid)).isEmpty();
    }

    @Test
    void save_delegaEnRepository() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).build();
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);
        assertThat(usuarioActivoService.save(ua)).isEqualTo(ua);
    }

    @Test
    void findByUsuarioId_retornaActivos() {
        when(usuarioActivoRepository.findByUsuarioId(1)).thenReturn(List.of());
        assertThat(usuarioActivoService.findByUsuarioId(1)).isEmpty();
    }

    @Test
    void crearContratoBase_exitoso() {
        var dto = new CrearContratoDTO(List.of(1), "Credito Directo", "SEPARACION", null, null,null);
        var usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Test");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(flujoComercialFactory.generarEtapasPorDefecto(any())).thenReturn(new ArrayList<>());
        when(usuarioActivoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = usuarioActivoService.crearContratoBase(dto);
        assertThat(result.getTipoFinanciamiento()).isEqualTo("Credito Directo");
    }

    @Test
    void crearContratoBase_usuarioNoExiste_lanzaEntityNotFound() {
        var dto = new CrearContratoDTO(List.of(99), "Credito Directo", "SEPARACION", null, null,null);
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioActivoService.crearContratoBase(dto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void asignarActivo_exitoso() {
        var idActivo = UUID.randomUUID();
        var dto = new AsignarActivoDTO(uuid, List.of(idActivo));
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).activos(new ArrayList<>()).build();
        var activo = Activo.builder().id(idActivo).nro("A-101")
                .estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .piso(buildPiso()).build();

        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(activoRepository.findByIdsForUpdate(List.of(idActivo))).thenReturn(List.of(activo));
        when(activoRepository.save(any())).thenReturn(activo);

        var result = usuarioActivoService.asignarActivo(dto);
        assertThat(result).isNotNull();
        assertThat(activo.getEstadoComercial()).isEqualTo(EstadoComercialActivo.SEPARADO);
    }

    @Test
    void asignarActivo_uaNoExiste_lanzaEntityNotFound() {
        var dto = new AsignarActivoDTO(uuid, List.of(UUID.randomUUID()));
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioActivoService.asignarActivo(dto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void asignarActivo_activoNoDisponible_lanzaBusinessException() {
        var idActivo = UUID.randomUUID();
        var dto = new AsignarActivoDTO(uuid, List.of(idActivo));
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).activos(new ArrayList<>()).build();
        var activo = Activo.builder().id(idActivo).nro("A-101")
                .estadoComercial(EstadoComercialActivo.VENDIDO).build();

        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(activoRepository.findByIdsForUpdate(List.of(idActivo))).thenReturn(List.of(activo));

        assertThatThrownBy(() -> usuarioActivoService.asignarActivo(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya no está disponible");
    }

    @Test
    void asignarActivo_activoNoExiste_lanzaEntityNotFound() {
        var idActivo = UUID.randomUUID();
        var dto = new AsignarActivoDTO(uuid, List.of(idActivo));
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).build();

        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(activoRepository.findByIdsForUpdate(List.of(idActivo))).thenReturn(List.of());

        assertThatThrownBy(() -> usuarioActivoService.asignarActivo(dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("no existen");
    }

    @Test
    void deleteById_exitoso_liberaActivosYDesactivaCliente() {
        var activo = Activo.builder().id(UUID.randomUUID()).nro("A-101")
                .estadoComercial(EstadoComercialActivo.SEPARADO).build();
        var cliente = new Usuario();
        cliente.setId(1);
        cliente.setActivo(true);
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid)
                .activos(new ArrayList<>(List.of(activo)))
                .clientes(new ArrayList<>(List.of(cliente)))
                .build();

        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.findByClienteId(1)).thenReturn(List.of(ua));

        usuarioActivoService.deleteById(uuid);

        assertThat(activo.getEstadoComercial()).isEqualTo(EstadoComercialActivo.DISPONIBLE);
        assertThat(activo.getUsuarioActivo()).isNull();
        verify(activoRepository, atLeastOnce()).save(activo);
        verify(usuarioActivoRepository).delete(ua);
    }

    @Test
    void deleteById_clienteConOtrosContratos_noSeDesactiva() {
        var otroUa = UsuarioActivo.builder().uuidUsuarioActivo(UUID.randomUUID()).build();
        var activo = Activo.builder().id(UUID.randomUUID()).nro("A-101")
                .estadoComercial(EstadoComercialActivo.SEPARADO).build();
        var cliente = new Usuario();
        cliente.setId(1);
        cliente.setActivo(true);
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid)
                .activos(new ArrayList<>(List.of(activo)))
                .clientes(new ArrayList<>(List.of(cliente)))
                .build();

        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.findByClienteId(1)).thenReturn(List.of(ua, otroUa));

        usuarioActivoService.deleteById(uuid);

        assertThat(cliente.getActivo()).isTrue();
        verify(usuarioRepository, never()).save(cliente);
    }

    @Test
    void deleteById_noExiste_lanzaEntityNotFound() {
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioActivoService.deleteById(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminarContrato_exitoso() {
        var activo = Activo.builder().id(UUID.randomUUID()).nro("A-101")
                .estadoComercial(EstadoComercialActivo.SEPARADO).build();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid)
                .activos(new ArrayList<>(List.of(activo)))
                .build();

        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        usuarioActivoService.eliminarContrato(uuid);

        assertThat(activo.getEstadoComercial()).isEqualTo(EstadoComercialActivo.DISPONIBLE);
        assertThat(ua.getVigente()).isFalse();
    }

    @Test
    void listar_retornaPagina() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).build();
        var page = new PageImpl<>(List.of(ua));
        when(usuarioActivoRepository.findAll(any(PageRequest.class))).thenReturn(page);

        var result = usuarioActivoService.listar(PageRequest.of(0, 10));
        assertThat(result).hasSize(1);
    }

    // ── asignar/desasignar asesor ────────────────────────────────────────────

    @Test
    void asignarAsesorAContrato_exitoso() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).build();
        var asesor = new Usuario(); asesor.setId(5);
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(usuarioRepository.findById(5)).thenReturn(Optional.of(asesor));
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);

        var result = usuarioActivoService.asignarAsesorAContrato(uuid, 5);

        assertThat(result.getAsesor()).isEqualTo(asesor);
        verify(usuarioActivoRepository).save(ua);
    }

    @Test
    void asignarAsesorAContrato_contratoNoExiste_lanzaEntityNotFound() {
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioActivoService.asignarAsesorAContrato(uuid, 5))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void asignarAsesorAContrato_asesorNoExiste_lanzaEntityNotFound() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(usuarioRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioActivoService.asignarAsesorAContrato(uuid, 5))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void desasignarAsesorDelContrato_exitoso() {
        var asesor = new Usuario(); asesor.setId(5);
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).asesor(asesor).build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);

        var result = usuarioActivoService.desasignarAsesorDelContrato(uuid, 5);

        assertThat(result.getAsesor()).isNull();
    }

    @Test
    void desasignarAsesorDelContrato_contratoNoExiste_lanzaEntityNotFound() {
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioActivoService.desasignarAsesorDelContrato(uuid, 5))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── actualizarCompleto ───────────────────────────────────────────────────

    @Test
    void actualizarCompleto_actualizaTodosLosCampos() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).build();
        var cliente = new Usuario(); cliente.setId(3);
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(usuarioRepository.findAllById(List.of(3))).thenReturn(List.of(cliente));
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);

        var dto = new com.llosa.backend.proyecto.dto.request.UpdateContratoDTO(
                List.of(3), "Credito Hipotecario",
                java.time.LocalDateTime.of(2026, 1, 1, 0, 0),
                java.time.LocalDateTime.of(2026, 12, 31, 0, 0));

        var result = usuarioActivoService.actualizarCompleto(uuid, dto);

        assertThat(result.getTipoFinanciamiento()).isEqualTo("Credito Hipotecario");
        assertThat(result.getFechaAdquisicion()).isNotNull();
        assertThat(result.getFechaCompletado()).isNotNull();
        assertThat(result.getClientes()).containsExactly(cliente);
    }

    @Test
    void actualizarCompleto_dtoConNulls_noModificaCampos() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid)
                .tipoFinanciamiento("Credito Directo").build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));
        when(usuarioActivoRepository.save(ua)).thenReturn(ua);

        var dto = new com.llosa.backend.proyecto.dto.request.UpdateContratoDTO(
                null, null, null, null);

        var result = usuarioActivoService.actualizarCompleto(uuid, dto);

        assertThat(result.getTipoFinanciamiento()).isEqualTo("Credito Directo");
        verify(usuarioRepository, never()).findAllById(any());
    }

    @Test
    void actualizarCompleto_contratoNoExiste_lanzaEntityNotFound() {
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.empty());

        var dto = new com.llosa.backend.proyecto.dto.request.UpdateContratoDTO(
                null, null, null, null);

        assertThatThrownBy(() -> usuarioActivoService.actualizarCompleto(uuid, dto))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
