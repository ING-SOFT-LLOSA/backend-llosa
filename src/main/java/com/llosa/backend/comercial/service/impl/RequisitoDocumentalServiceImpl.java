package com.llosa.backend.comercial.service.impl;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
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
    private final HitoProcesoCompraRepository hitoComercialRepository;
    private final EtapaExpedienteRepository etapaExpedienteRepository;

    @Transactional
    public RequisitoDocumental asociarArchivoARequisito(UUID requisitoId, MultipartFile file, String firebaseUid) {
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException("Requisito no encontrado"));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // CORRECCIÓN: Llamamos a la nueva firma de subirDocumentoPolimorfico sin el UUID del contrato
        documentoService.subirDocumentoPolimorfico(
                file,
                TipoDocumento.PDF_LEGAL, // O el tipo dinámico si lo necesitas
                requisitoId.toString(),
                "REQUISITO",
                usuario.getId()
        );

        requisito.setEstado(EtapaRequisitoDocumental.COMPLETADO); // Asegúrate de usar el Enum o String correcto según tu entidad
        requisito.setFechaEmision(LocalDate.now());
        return requisitoRepository.save(requisito);
    }

    @Transactional
    public void eliminarArchivoDeRequisito(UUID requisitoId, String firebaseUid) {
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException("Requisito no encontrado"));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Documento documento = documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc("REQUISITO", requisitoId.toString())
                .orElseThrow(() -> new EntityNotFoundException("No hay un archivo físico para este requisito"));

        documentoService.eliminarDocumento(documento.getId(), usuario.getId());

        requisito.setEstado(EtapaRequisitoDocumental.PENDIENTE);
        requisito.setFechaEmision(null);
        requisitoRepository.save(requisito);
    }

    @Transactional
    public RequisitoDocumental crearRequisito(RequisitoCreateRequest request) {
        EtapaExpediente etapaExpediente = etapaExpedienteRepository.findById(request.etapaProcesoCompraId()).orElseThrow(
                () -> new EntityNotFoundException("Etapa expediente no encontrada con UUID: " + request.etapaProcesoCompraId())
        );

        RequisitoDocumental nuevoRequisito = RequisitoDocumental.builder()
                .etapaExpediente(etapaExpediente)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .notaCorporativa(request.notaCorporativa())
                .fechaEmision(request.fechaEmision() != null ? request.fechaEmision() : LocalDate.now())
                .estado(EtapaRequisitoDocumental.PENDIENTE)
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc("REQUISITO", id.toString())
                .ifPresent(doc -> documentoService.eliminarDocumento(doc.getId(), usuario.getId()));

        requisitoRepository.delete(requisito);
    }
}