package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface RequisitoDocumentalService {
    RequisitoDocumental asociarArchivoARequisito(UUID requisitoId, MultipartFile file, String firebaseUid);
    void eliminarArchivoDeRequisito(UUID requisitoId, String firebaseUid);
    RequisitoDocumental crearRequisito(RequisitoCreateRequest request);
    RequisitoDocumental actualizarRequisito(UUID id, RequisitoUpdateRequest request);
    void eliminarRequisitoTotalmente(UUID id, String firebaseUid);
    void completarRequisitoConDocumento(UUID requisitoId, String rutaGcs, String nombreOriginal, String tipoMime, Integer subidoPor, String comentario);
}
