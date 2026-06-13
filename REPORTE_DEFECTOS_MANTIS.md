# Reporte de Defectos — QA (Llosa Backend)

Hallazgos de las pruebas E2E / integración sobre la rama `test`.
Cada defecto tiene su test rojo como evidencia. Fecha: 2026-06-10.

---

## DEF-01 — CP02: No se valida el dominio corporativo en el login

| Campo | Valor |
|-------|-------|
| **Caso de prueba** | CP02 |
| **Módulo** | Seguridad y Control de Accesos / Autenticación Corporativa |
| **Severidad sugerida** | Alta (seguridad de acceso) |
| **Endpoint** | `GET /api/auth/me` |
| **Esperado** | Rechazar acceso a usuarios EMPLEADO con correo fuera de `@llosaedificaciones.com` |
| **Obtenido** | Acceso concedido (HTTP 200) a un correo `@gmail.com` |
| **Causa raíz** | La validación de dominio está **comentada** en `AuthService.java` (líneas 32-37), con la nota *"Para testing se invalida esto"*. Nunca se reactivó. |
| **Evidencia** | Test `CP01ToCP06E2ETest.cp02_correoNoCorporativo_esRechazado` (rojo) |

---

## DEF-02 — CP13: Se permite crear proyectos con nombre duplicado

| Campo | Valor |
|-------|-------|
| **Caso de prueba** | CP13 |
| **Módulo** | Gestión de Activos (Proyectos) |
| **Severidad sugerida** | Media |
| **Endpoint** | `POST /api/proyectos` |
| **Esperado** | Rechazar el registro de un proyecto cuyo nombre ya existe (exigir nombre único) |
| **Obtenido** | El segundo proyecto con el mismo nombre se crea con éxito (HTTP 201) |
| **Causa raíz** | `ProyectoServiceImpl.save()` solo llama a `proyectoRepository.save()` sin validar unicidad de nombre |
| **Evidencia** | Test `CP12ToCP14E2ETest.cp13_nombreDuplicado_esRechazado` (rojo) |

---

## DEF-03 — CP15: Al vincular un cliente, la unidad no pasa a "Separado"

| Campo | Valor |
|-------|-------|
| **Caso de prueba** | CP15 |
| **Módulo** | Gestión de Activos (Vinculación) |
| **Severidad sugerida** | Alta (inventario inconsistente) |
| **Endpoint** | `POST /api/expedientes/asignar` |
| **Esperado** | Al asignar, el activo cambia su `estadoComercial` a `SEPARADO` (bloquea inventario) |
| **Obtenido** | El activo permanece en `DISPONIBLE` tras la asignación |
| **Causa raíz** | `UsuarioActivoServiceImpl.asignarActivo()` crea el `UsuarioActivo` y lo guarda, pero **nunca modifica el estado del Activo** |
| **Evidencia** | Test `CP15ToCP20E2ETest.cp15_vincularCliente_unidadPasaASeparado` (rojo) |

---

## DEF-04 — CP17: Se permite asignar una unidad ya tomada (doble asignación)

| Campo | Valor |
|-------|-------|
| **Caso de prueba** | CP17 |
| **Módulo** | Gestión de Activos (Vinculación / Concurrencia) |
| **Severidad sugerida** | Alta (regla de negocio crítica) |
| **Endpoint** | `POST /api/expedientes/asignar` |
| **Esperado** | Detectar que la unidad ya está asignada y rechazar la segunda asignación |
| **Obtenido** | La segunda asignación sobre la misma unidad se realiza con éxito (HTTP 200) |
| **Causa raíz** | `asignarActivo()` no verifica si el activo ya tiene un expediente / no está disponible |
| **Evidencia** | Test `CP15ToCP20E2ETest.cp17_unidadYaTomada_esRechazada` (rojo) |

---

## DEF-05 — CP19: Al desvincular, la unidad no vuelve a "Disponible" ni se desactiva el cliente

| Campo | Valor |
|-------|-------|
| **Caso de prueba** | CP19 |
| **Módulo** | Gestión de Activos (Desvinculación) |
| **Severidad sugerida** | Alta |
| **Endpoint** | `DELETE /api/expedientes/delete/{uuid}` |
| **Esperado** | Al desvincular la única unidad del cliente: el activo retorna a `DISPONIBLE` y el perfil del cliente pasa a `Inactivo` |
| **Obtenido** | El activo permanece en `SEPARADO`; el cliente no se desactiva |
| **Causa raíz** | La desvinculación (`deleteById`) solo borra el expediente; no revierte el estado del activo ni el del usuario |
| **Evidencia** | Test `CP15ToCP20E2ETest.cp19_desvincularUnicaUnidad_clienteInactivo` (rojo) |

---

## DEF-06 (menor) — Reglas de negocio devuelven HTTP 500 en vez de 4xx

| Campo | Valor |
|-------|-------|
| **Caso de prueba** | Transversal (observado en CP22 y flujos de Reporte/Requisito) |
| **Módulo** | Manejo de errores global |
| **Severidad sugerida** | Baja / Media (contrato de API) |
| **Esperado** | Las violaciones de regla de negocio y recursos no encontrados devuelven un código 4xx limpio |
| **Obtenido** | `BusinessException` y `EntityNotFoundException` se propagan como HTTP 500 (error de servlet) |
| **Causa raíz** | `GlobalExceptionHandler` solo mapea `EmailDuplicadoException`, `RecursoNoEncontradoException` y `AccesoDenegadoException`. NO maneja `BusinessException` ni `jakarta.persistence.EntityNotFoundException` |
| **Nota** | La regla de negocio SÍ se aplica (ej. la precedencia de hitos bloquea correctamente); el problema es solo el código HTTP devuelto |

---

## Casos NO testeados a nivel backend (informativo, no defectos)

- **CP03** (recuperación de contraseña) y **CP05** (UI "Olvidé mi contraseña"): son funcionalidad **frontend** (Firebase client-side / renderizado). No se testean en backend.
- **CP23, CP24**: describen carga masiva de multimedia en el módulo de Obra, que **no está implementado** ahí (la multimedia/GCS vive en Bóveda Digital). Gap funcional a confirmar con el equipo.
- **CP27** (Signed URL): el backend implementa la firma; no se pudo verificar en local por limitación del emulador GCS (requiere private key). Validar en entorno con GCS real.

---

## Resumen

| Resultado | Casos |
|-----------|-------|
| ✅ Cumple (verde) | CP01, CP04, CP06, CP07, CP08, CP09, CP10, CP11, CP12, CP14, CP16, CP20, CP21, CP22, CP25, CP26, CP28 |
| 🔴 Defecto (Mantis) | CP02, CP13, CP15, CP17, CP19 (+ DEF-06 menor transversal) |
| ⬜ No aplica backend | CP03, CP05, CP23, CP24, CP27 |
