-- 1. Insertar la nueva función en la tabla 'funcion'
INSERT INTO funcion (nombre_codigo, descripcion)
VALUES ('USER_VER', 'Ver lista de usuarios');

-- 2. Asignar el permiso USER_VER al rol ADMIN
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'ADMIN'
  AND f.nombre_codigo = 'USER_VER';

-- 3. (Opcional) Asignar el permiso USER_VER al rol ASESOR
-- Ya que el asesor tiene USER_GESTIONAR, es lógico que pueda verlos.
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'ASESOR'
  AND f.nombre_codigo = 'USER_VER';