package com.llosa.backend.seguridad.service;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.config.TestData;
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.repository.FuncionRepository;
import com.llosa.backend.seguridad.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    RolRepository rolRepository;

    @Mock
    FuncionRepository funcionRepository;

    @InjectMocks
    RolService rolService;

    // ── modificarFunciones ────────────────────────────────────────────────────

    @Test
    @CP(value = "CP07",
        scenario = "Asignar permisos granulares a rol",
        input = "rolId=1, funcionIds=[PROY_CREAR, OBRA_EDITAR]",
        expected = "Lista de funciones reemplazada completamente",
        type = CP.TestType.UNIT)
    void modificarFunciones_reemplazaCompletamenteLaLista() {
        Rol rol = TestData.rol();
        List<Funcion> nuevasFunciones = List.of(
                TestData.funcion("PROY_CREAR"),
                TestData.funcion("OBRA_EDITAR")
        );
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(funcionRepository.findAllById(List.of(1, 2))).thenReturn(nuevasFunciones);
        when(rolRepository.save(rol)).thenReturn(rol);

        Rol resultado = rolService.modificarFunciones(1, List.of(1, 2));

        assertThat(resultado.getFunciones()).isEqualTo(nuevasFunciones);
        assertThat(resultado.getFunciones())
                .extracting("nombreCodigo")
                .doesNotContain("PROY_VER", "DOCS_VER");
        verify(rolRepository).save(rol);
    }

    @Test
    @CP(value = "CP07",
        scenario = "Remover todos los permisos de un rol",
        input = "rolId=1, funcionIds=[]",
        expected = "Lista de funciones vacía",
        type = CP.TestType.UNIT)
    void modificarFunciones_conListaVacia_eliminaTodasLasFunciones() {
        Rol rol = TestData.rol();
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(funcionRepository.findAllById(List.of())).thenReturn(List.of());
        when(rolRepository.save(rol)).thenReturn(rol);

        Rol resultado = rolService.modificarFunciones(1, List.of());

        assertThat(resultado.getFunciones()).isEmpty();
    }

    @Test
    @CP(value = "CP07",
        scenario = "Validar error al asignar permisos a rol inexistente",
        input = "rolId=999 (no existe), funcionIds=[1,2]",
        expected = "RuntimeException con mensaje 'Rol no encontrado'",
        type = CP.TestType.UNIT)
    void modificarFunciones_rolInexistente_lanzaExcepcion() {
        when(rolRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.modificarFunciones(999, List.of(1, 2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no encontrado");

        verify(funcionRepository, never()).findAllById(any());
        verify(rolRepository, never()).save(any());
    }

    @Test
    @CP(value = "CP07",
        scenario = "Asignar permisos parciales (algunos no existen)",
        input = "rolId=1, funcionIds=[1,99] (99 no existe)",
        expected = "Solo función existente (ID=1) se asigna, 99 ignorado",
        type = CP.TestType.UNIT)
    void modificarFunciones_algunasIdsInexistentes_guardaSoloLasEncontradas() {
        Rol rol = TestData.rol();
        List<Funcion> soloUna = List.of(TestData.funcion("PROY_VER"));
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        // IDs 1 y 99 solicitados, sólo el 1 existe en BD
        when(funcionRepository.findAllById(List.of(1, 99))).thenReturn(soloUna);
        when(rolRepository.save(rol)).thenReturn(rol);

        Rol resultado = rolService.modificarFunciones(1, List.of(1, 99));

        assertThat(resultado.getFunciones()).hasSize(1);
        assertThat(resultado.getFunciones()).extracting("nombreCodigo").containsExactly("PROY_VER");
    }

    // ── listarTodos ───────────────────────────────────────────────────────────

    @Test
    @CP(value = "CP11",
        scenario = "Listar roles disponibles para asignación",
        input = "Query a repositorio de roles",
        expected = "Lista completa de roles con funciones",
        type = CP.TestType.UNIT)
    void listarTodos_delegaEnRepositorio() {
        List<Rol> roles = List.of(TestData.rol(), TestData.rol());
        when(rolRepository.findAll()).thenReturn(roles);

        List<Rol> resultado = rolService.listarTodos();

        assertThat(resultado).hasSize(2);
        verify(rolRepository).findAll();
    }

    @Test
    void listarTodos_sinRoles_devuelveListaVacia() {
        when(rolRepository.findAll()).thenReturn(List.of());

        assertThat(rolService.listarTodos()).isEmpty();
    }
}
