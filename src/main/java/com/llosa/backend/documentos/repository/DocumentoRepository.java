package com.llosa.backend.documentos.repository;

import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByIdReferenciaAndEntidadReferencia(String idReferencia, String entidadReferencia);

    List<Documento> findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(
            String idReferencia, String entidadReferencia, TipoDocumento tipoDocumento);

    List<Documento> findBySubidoPor(Integer usuarioId);
}