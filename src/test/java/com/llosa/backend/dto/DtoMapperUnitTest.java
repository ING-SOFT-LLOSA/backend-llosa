package com.llosa.backend.dto;

import com.llosa.backend.comercial.dto.RequisitoResponseDTO;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.seguridad.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros (sin Spring/Docker) de los mappers fromEntity de los DTOs
 * de respuesta. Cubren las clases de mapeo que el CI no ejercitaba, subiendo la
 * cobertura del pipeline sin tocar código de producción.
 */
class DtoMapperUnitTest {

    @Test
    void requisitoResponseDTO_fromEntity_mapeaTodosLosCampos() {
        EtapaExpediente etapa = EtapaExpediente.builder()
                .uuidEtapaExpediente(UUID.randomUUID())
                .build();

        UUID id = UUID.randomUUID();
        RequisitoDocumental req = RequisitoDocumental.builder()
                .id(id)
                .etapaExpediente(etapa)
                .titulo("Pago Inicial")
                .descripcion("Comprobante del pago inicial")
                .notaCorporativa("Adjuntar voucher")
                .estado(EtapaRequisitoDocumental.PENDIENTE)
                .fechaEmision(LocalDate.of(2026, 1, 15))
                .icono("upload")
                .build();

        RequisitoResponseDTO dto = RequisitoResponseDTO.fromEntity(req);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.etapaProcesoCompraId()).isEqualTo(etapa.getUuidEtapaExpediente());
        assertThat(dto.titulo()).isEqualTo("Pago Inicial");
        assertThat(dto.descripcion()).isEqualTo("Comprobante del pago inicial");
        assertThat(dto.notaCorporativa()).isEqualTo("Adjuntar voucher");
        assertThat(dto.estado()).isEqualTo(EtapaRequisitoDocumental.PENDIENTE);
        assertThat(dto.fechaEmision()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(dto.icono()).isEqualTo("upload");
    }

    @Test
    void activoResponseDTO_fromEntity_mapeaJerarquiaCompleta() {
        Activo activo = activoDePrueba("A-101", new BigDecimal("85.50"));

        ActivoResponseDTO dto = ActivoResponseDTO.fromEntity(activo);

        assertThat(dto.nro()).isEqualTo("A-101");
        assertThat(dto.tipo()).isEqualTo(TipoActivo.DEPARTAMENTO);
        assertThat(dto.areaM2()).isEqualByComparingTo("85.50");
        assertThat(dto.estadoComercial()).isEqualTo(EstadoComercialActivo.VENDIDO);
        assertThat(dto.nroPiso()).isEqualTo(3);
        assertThat(dto.torreNombre()).isEqualTo("Torre A");
        assertThat(dto.proyectoNombre()).isEqualTo("Edificio Aurora");
    }

    @Test
    void clienteSimpleDTO_fromEntity_mapeaDatosBasicos() {
        Usuario u = new Usuario();
        u.setId(7);
        u.setNombre("Ana");
        u.setApellidos("Quispe");
        u.setDocumentoIdentidad("44556677");
        u.setEmail("ana@correo.com");
        u.setTelefono("999888777");

        UsuarioActivoResponseDTO.ClienteSimpleDTO dto =
                UsuarioActivoResponseDTO.ClienteSimpleDTO.fromEntity(u);

        assertThat(dto.id()).isEqualTo(7);
        assertThat(dto.nombre()).isEqualTo("Ana");
        assertThat(dto.apellidos()).isEqualTo("Quispe");
        assertThat(dto.documentoIdentidad()).isEqualTo("44556677");
        assertThat(dto.email()).isEqualTo("ana@correo.com");
        assertThat(dto.telefono()).isEqualTo("999888777");
    }

    @Test
    void usuarioActivoResponseDTO_fromEntity_conClientesActivosYAsesor() {
        Usuario cliente = new Usuario();
        cliente.setId(1); cliente.setNombre("Luis"); cliente.setApellidos("Rojas");
        cliente.setEmail("luis@correo.com");

        Usuario asesor = new Usuario();
        asesor.setId(2); asesor.setNombre("Marta"); asesor.setApellidos("Vega");
        asesor.setEmail("marta@utec.edu.pe");

        Activo activo = activoDePrueba("B-202", new BigDecimal("60.00"));

        UsuarioActivo ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .tipoFinanciamiento("Credito Directo")
                .vigente(true)
                .clientes(List.of(cliente))
                .activos(List.of(activo))
                .asesor(asesor)
                .build();

        UsuarioActivoResponseDTO dto = UsuarioActivoResponseDTO.fromEntity(ua);

        assertThat(dto.tipoFinanciamiento()).isEqualTo("Credito Directo");
        assertThat(dto.vigente()).isTrue();
        assertThat(dto.clientes()).hasSize(1);
        assertThat(dto.clientes().get(0).nombre()).isEqualTo("Luis");
        assertThat(dto.activos()).hasSize(1);
        assertThat(dto.activos().get(0).nro()).isEqualTo("B-202");
        assertThat(dto.asesor()).isNotNull();
        assertThat(dto.asesor().nombre()).isEqualTo("Marta");
    }

    @Test
    void usuarioActivoResponseDTO_fromEntity_conColeccionesNulas_devuelveListasVacias() {
        UsuarioActivo ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .tipoFinanciamiento("Credito Hipotecario")
                .vigente(false)
                .clientes(null)
                .activos(null)
                .asesor(null)
                .build();

        UsuarioActivoResponseDTO dto = UsuarioActivoResponseDTO.fromEntity(ua);

        assertThat(dto.clientes()).isEmpty();
        assertThat(dto.activos()).isEmpty();
        assertThat(dto.asesor()).isNull();
        assertThat(dto.vigente()).isFalse();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Activo activoDePrueba(String nro, BigDecimal area) {
        Proyecto proyecto = Proyecto.builder().nombre("Edificio Aurora").build();
        Torre torre = Torre.builder().nombre("Torre A").proyecto(proyecto).build();
        Piso piso = Piso.builder().nroPiso(3).torre(torre).build();
        return Activo.builder()
                .nro(nro)
                .tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(area)
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(piso)
                .build();
    }
}
