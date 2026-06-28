package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.dto.response.ReporteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ReporteService {

    ReporteResponse crear(ReporteCreateRequest request, List<MultipartFile> archivos, Integer usuarioId);

    ReporteResponse obtenerPorId(UUID id);

    Page<ReporteResponse> listarPorProyecto(UUID uuidProyecto, Pageable pageable);

    ReporteResponse actualizar(UUID id, ReporteUpdateRequest request);

    void eliminar(UUID id);
}
