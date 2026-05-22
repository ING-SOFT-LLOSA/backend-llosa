-- Usuario superadmin por defecto para desarrollo
-- Firebase project: llosa-edificaciones
INSERT INTO usuario (firebase_uuid, tipo_usuario, id_rol, nombre, apellidos, email, activo)
VALUES (
           'AMLtHuGEe9dLVca353dLX3O6YKy1',
           'EMPLEADO',
           (SELECT id_rol FROM roles WHERE nombre = 'ADMIN'),
           'Super',
           'Admin',
           'superadmin@llosa.com',
           true
       )
    ON CONFLICT (firebase_uuid) DO NOTHING;