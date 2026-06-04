ALTER TABLE hito_unidad
ALTER COLUMN fecha_completado TYPE DATE
USING fecha_completado::DATE;