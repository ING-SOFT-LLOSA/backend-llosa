package com.llosa.backend.comercial.service.impl;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.comercial.service.RequisitoDocumentalService;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequisitoDocumentalServiceImpl implements RequisitoDocumentalService {
    private final RequisitoDocumentalRepository requisitoRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService; // Tu servicio existente
    private final UsuarioRepository usuarioRepository; // Para auditoría/seguridad
    private final HitoProcesoCompraRepository hitoRepository;

    @Transactional
    public RequisitoDocumental asociarArchivoARequisito(UUID requisitoId, MultipartFile file, String firebaseUid) {
        // 1. Buscamos el requisito en el negocio
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException("Requisito no encontrado"));

        // 2. Buscamos el usuario que está subiendo el archivo (auditoría)
        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Obtenemos el ID del Usuario Activo navegando por el Hito Comercial
        // (Esto es necesario porque GCS organiza las carpetas usando este ID)
        UUID usuarioActivoId = requisito.getHitoComercial().getUsuarioActivo().getUuidUsuarioActivo();

        // 4. Llamamos al NUEVO método polimórfico de tu DocumentoService
        documentoService.subirDocumentoPolimorfico(
                usuarioActivoId,            // Para la ruta del Bucket GCS
                file,                       // El binario
                TipoDocumento.PDF_LEGAL,    // O el TipoDocumento que mapee con tu Enum
                requisitoId.toString(),     // id_referencia (ID del Requisito)
                "REQUISITO",                // entidad_referencia
                usuario.getId()             // subidoPor (Integer)
        );

        // 5. Actualizamos el estado del requisito en el negocio
        requisito.setEstado("COMPLETADA");
        requisito.setFechaEmision(LocalDate.now());
        return requisitoRepository.save(requisito);
    }
    @Transactional
    public void eliminarArchivoDeRequisito(UUID requisitoId, String firebaseUid) {
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException("Requisito no encontrado"));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Buscamos el registro en la tabla polimórfica de documentos
        Documento documento = documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc("REQUISITO", requisitoId.toString())
                .orElseThrow(() -> new EntityNotFoundException("No hay un archivo físico para este requisito"));

        // 2. Reutilizamos tu método para borrar de GCS y de la tabla 'documento'
        documentoService.eliminarDocumento(documento.getId(), usuario.getId());

        // 3. Reseteamos el estado del negocio
        requisito.setEstado("PENDIENTE");
        requisito.setFechaEmision(null);
        requisitoRepository.save(requisito);
    }

    @Transactional
    public RequisitoDocumental crearRequisito(RequisitoCreateRequest request) {
        HitoProcesoCompra hito = hitoRepository.findById(request.hitoProcesoCompraId())
                .orElseThrow(() -> new EntityNotFoundException("Hito de proceso de compra no encontrado"));

        RequisitoDocumental nuevoRequisito = RequisitoDocumental.builder()
                .hitoComercial(hito)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .notaCorporativa(request.notaCorporativa())
                .estado("PENDIENTE") // Todo requisito inicia pendiente
                .icono(request.icono() != null ? request.icono() : "description")
                .build();

        return requisitoRepository.save(nuevoRequisito);
    }

    @Transactional
    public RequisitoDocumental actualizarRequisito(UUID id, RequisitoUpdateRequest request) {
        RequisitoDocumental requisito = requisitoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Requisito no encontrado"));

        requisito.setTitulo(request.titulo());
        requisito.setDescripcion(request.descripcion());
        requisito.setNotaCorporativa(request.notaCorporativa());

        if (request.estado() != null) {
            requisito.setEstado(request.estado());
        }

        return requisitoRepository.save(requisito);
    }

    @Transactional
    public void eliminarRequisitoTotalmente(UUID id, String firebaseUid) {
        RequisitoDocumental requisito = requisitoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Requisito no encontrado"));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // CONTROL DE SEGURIDAD AMBIENTAL:
        // Si el requisito tiene un archivo físico subido, debemos borrarlo de GCS antes de eliminar el requisito
        documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc("REQUISITO", id.toString())
                .ifPresent(doc -> documentoService.eliminarDocumento(doc.getId(), usuario.getId()));

        // Ahora sí, borramos el registro del requisito en la base de datos de manera segura
        requisitoRepository.delete(requisito);
    }
}

