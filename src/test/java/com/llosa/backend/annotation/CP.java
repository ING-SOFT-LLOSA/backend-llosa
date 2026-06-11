package com.llosa.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para mapear tests a Casos de Prueba (CP) específicos.
 * Proporciona trazabilidad bidireccional entre requirements y tests automatizados.
 *
 * Uso:
 * <pre>
 * @Test
 * @CP(value = "CP07", scenario = "Permisos granulares",
 *     input = "usuario=cperez, modulo=FINANCIERO, concedido=true",
 *     expected = "permiso registrado + token refrescado")
 * void asignarPermisoEspecial_correctamente() { ... }
 * </pre>
 *
 * @author Engineer BDD
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CP {

    /**
     * Identificador del Caso de Prueba (ej: "CP07", "CP08")
     */
    String value();

    /**
     * Descripción del escenario cubierto
     */
    String scenario() default "";

    /**
     * Datos de entrada relevantes para reproducción
     */
    String input() default "";

    /**
     * Salida o comportamiento esperado
     */
    String expected() default "";

    /**
     * Tipo de test: UNIT, INTEGRATION, E2E
     */
    TestType type() default TestType.UNIT;

    enum TestType {
        UNIT,
        INTEGRATION,
        E2E
    }
}
