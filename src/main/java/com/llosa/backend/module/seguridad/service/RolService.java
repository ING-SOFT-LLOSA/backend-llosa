package com.llosa.backend.module.seguridad.service;

import com.llosa.backend.module.seguridad.entity.Funcion;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.repository.FuncionRepository;
import com.llosa.backend.module.seguridad.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final FuncionRepository funcionRepository;

    @Transactional
    public Rol modificarFunciones(Integer idRol, List<Integer> idFunciones) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        List<Funcion> funciones = funcionRepository.findAllById(idFunciones);
        rol.setFunciones(funciones);
        return rolRepository.save(rol);
    }

    public List<Rol> listarTodos() {
        return rolRepository.findAll();
    }
}