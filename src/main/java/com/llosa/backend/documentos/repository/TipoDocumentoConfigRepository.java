package com.llosa.backend.documentos.repository;

import com.llosa.backend.documentos.entity.TipoDocumentoConfig;
import com.llosa.backend.documentos.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDocumentoConfigRepository extends JpaRepository<TipoDocumentoConfig, TipoDocumento> {
}