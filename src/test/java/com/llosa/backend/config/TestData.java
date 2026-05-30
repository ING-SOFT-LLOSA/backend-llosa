package com.llosa.backend.config;

import com.llosa.backend.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class TestData {

    private TestData() {}

    public static Usuario usuario() {
        Usuario u = new Usuario();
        u.setFirebaseUuid("uid-test-" + UUID.randomUUID());
        u.setNombre("Juan");
        u.setApellidos("Perez");
        u.setEmail("juan.perez@test.com");
        u.setTipoUsuario("CLIENTE");
        u.setActivo(true);
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    public static Usuario usuarioEmpleado(String email) {
        Usuario u = usuario();
        u.setEmail(email);
        u.setTipoUsuario("EMPLEADO");
        return u;
    }

    public static Usuario usuarioConEmail(String email) {
        Usuario u = usuario();
        u.setEmail(email);
        u.setFirebaseUuid("uid-" + UUID.randomUUID());
        return u;
    }

    public static Rol rol() {
        Rol r = new Rol();
        r.setIdRol(1);
        r.setNombre("CLIENTE");
        r.setDescripcion("Cliente de la inmobiliaria");
        r.setFunciones(List.of(funcion("PROY_VER"), funcion("DOCS_VER")));
        return r;
    }

    public static Funcion funcion(String codigo) {
        Funcion f = new Funcion();
        f.setIdFuncion(1);
        f.setNombreCodigo(codigo);
        f.setDescripcion("Función " + codigo);
        return f;
    }

    public static CrearUsuarioRequest crearUsuarioRequest() {
        CrearUsuarioRequest req = new CrearUsuarioRequest();
        req.setNombre("Ana");
        req.setApellidos("Garcia");
        req.setEmail("ana.garcia@test.com");
        req.setTipoUsuario("CLIENTE");
        req.setTelefono("999000111");
        req.setDocumentoIdentidad("12345678");
        return req;
    }

    public static FirebaseAuthenticationToken authToken() {
        return new FirebaseAuthenticationToken(
                "test-uid",
                "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
