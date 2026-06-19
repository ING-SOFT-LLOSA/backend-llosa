package com.llosa.backend.comercial.service.impl;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.EtapaExpediente;
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
import com.llosa.backend.exception.RecursoNoEncontradoException;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequisitoDocumentalServiceImpl implements RequisitoDocumentalService {

    private final RequisitoDocumentalRepository requisitoRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;
    private final EtapaExpedienteRepository etapaExpedienteRepository;
    
    private static final String REQUISITO_NO_ENCONTRADO_MSG = "Requisito no encontrado";
    private static final String USUARIO_NO_ENCONTRADO_MSG = "Usuario no encontrado";
    private static final String ENTIDAD_REFERENCIA_REQUSITO = "REQUISITO";


    @Transactional
    public RequisitoDocumental asociarArchivoARequisito(UUID requisitoId, MultipartFile file, String firebaseUid) {
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException(REQUISITO_NO_ENCONTRADO_MSG));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO_MSG));

        // CORRECCIÓN: Llamamos a la nueva firma de subirDocumentoPolimorfico sin el UUID del contrato
        documentoService.subirDocumentoPolimorfico(
                file,
                TipoDocumento.PDF_LEGAL, // O el tipo dinámico si lo necesitas
                requisitoId.toString(),
                ENTIDAD_REFERENCIA_REQUSITO,
                usuario.getId()
        );

        requisito.setEstado(EtapaRequisitoDocumental.COMPLETADO); // Asegúrate de usar el Enum o String correcto según tu entidad
        requisito.setFechaEmision(LocalDate.now());
        return requisitoRepository.save(requisito);
    }

    @Transactional
    public void completarRequisitoConDocumento(UUID requisitoId, String rutaGcs, String nombreOriginal, String tipoMime, Integer subidoPor, String comentario) {
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException(REQUISITO_NO_ENCONTRADO_MSG));

        documentoService.crearReferenciaDocumento(
                rutaGcs, nombreOriginal, tipoMime,
                requisitoId.toString(), ENTIDAD_REFERENCIA_REQUSITO,
                TipoDocumento.PDF_LEGAL, subidoPor
        );

        requisito.setEstado(EtapaRequisitoDocumental.COMPLETADO);
        requisito.setFechaEmision(LocalDate.now());
        if (comentario != null) {
            requisito.setNotaCorporativa(comentario);
        }
        requisitoRepository.save(requisito);
    }

    @Transactional
    public void eliminarArchivoDeRequisito(UUID requisitoId, String firebaseUid) {
        RequisitoDocumental requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new EntityNotFoundException(REQUISITO_NO_ENCONTRADO_MSG));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO_MSG));

        Documento documento = documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(ENTIDAD_REFERENCIA_REQUSITO, requisitoId.toString())
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
                .orElseThrow(() -> new EntityNotFoundException(REQUISITO_NO_ENCONTRADO_MSG));

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
                .orElseThrow(() -> new EntityNotFoundException(REQUISITO_NO_ENCONTRADO_MSG));

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO_MSG));

        documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(ENTIDAD_REFERENCIA_REQUSITO, id.toString())
                .ifPresent(doc -> documentoService.eliminarDocumento(doc.getId(), usuario.getId()));

        requisitoRepository.delete(requisito);
    }
}