package com.llosa.backend.seguridad.repository;

import com.llosa.backend.seguridad.entity.Funcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FuncionRepository extends JpaRepository<Funcion, Integer> {
    List<Funcion> findByNombreCodigoIn(List<String> nombresCodigo);

}
