SELECT id,
       correo_prueba,
       proxima_fecha_cambio,
       estado,
       prueba_exitosa,
       ultimo_resultado_prueba,
       fecha_prueba,
       probada_por,
       fecha_activacion,
       activada_por,
       creada_por,
       fecha_creacion,
       fecha_actualizacion,
       version
FROM configuraciones_correo_smtp
ORDER BY fecha_creacion DESC;
