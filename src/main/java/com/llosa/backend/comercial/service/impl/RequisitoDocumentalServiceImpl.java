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
    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;
    private final HitoProcesoCompraRepository hitoRepository;

    @Transactional
    public RequisitoDocumental asociarArchivoARequisito(UUID requisitoId, MultipartFile file, String firebaseUid) {
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException("Requisito no encontrado"));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UUID usuarioActivoId = requisito.getHitoComercial().getUsuarioActivo().getUuidUsuarioActivo();

        documentoService.subirDocumentoPolimorfico(
                usuarioActivoId,
                file,
                TipoDocumento.PDF_LEGAL,
                requisitoId.toString(),
                "REQUISITO",
                usuario.getId()
        );

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

        Documento documento = documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc("REQUISITO", requisitoId.toString())
                .orElseThrow(() -> new EntityNotFoundException("No hay un archivo físico para este requisito"));

        documentoService.eliminarDocumento(documento.getId(), usuario.getId());

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
                .estado("PENDIENTE")
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

        documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc("REQUISITO", id.toString())
                .ifPresent(doc -> documentoService.eliminarDocumento(doc.getId(), usuario.getId()));

        requisitoRepository.delete(requisito);
    }
}