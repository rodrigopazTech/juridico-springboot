package com.juridico.sistema_juridico.service.procesal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juridico.sistema_juridico.Entity.procesal.LoteMigracion;
import com.juridico.sistema_juridico.Entity.procesal.RegistroMigracion;
import com.juridico.sistema_juridico.Entity.procesal.CatalogoMigracion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.procesal.LoteMigracionRepository;
import com.juridico.sistema_juridico.repository.procesal.RegistroMigracionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.CatalogoMigracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;


@Service
public class LoteMigracionService {

    @Autowired private LoteMigracionRepository loteRepository;
    @Autowired private RegistroMigracionRepository registroRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CatalogoMigracionRepository catalogoRepository;
    @Autowired private ObjectMapper objectMapper;

    // 1. Obtener el lote que está a medias, o crear uno nuevo si todo está limpio
    @Transactional
    public LoteMigracion obtenerOCrearLoteActivo() {
        return loteRepository.findFirstByEstatusOrderByFechaCreacionDesc("PENDIENTE")
                .orElseGet(() -> {
                    LoteMigracion nuevoLote = new LoteMigracion();
                    nuevoLote.setPasoActual(1); // Inicia en la Etapa 1: Catálogos
                    nuevoLote.setEstatus("PENDIENTE");
                    
                    // Asignar al usuario que está haciendo la importación (si aplica)
                    try {
                        String email = SecurityContextHolder.getContext().getAuthentication().getName();
                        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
                        nuevoLote.setUsuarioResponsable(usuario);
                    } catch (Exception e) { /* Ignorar si viene de n8n sin contexto de UI */ }
                    
                    return loteRepository.save(nuevoLote);
                });
    }

    // 2. Guardar un dato crudo en el lote activo (Para n8n y Excel)
    @Transactional
    public void registrarDatoEnLote(String tipoDato, Object requestObjeto, String numeroExpediente) {
        try {
            LoteMigracion loteActivo = obtenerOCrearLoteActivo();
            
            RegistroMigracion registro = new RegistroMigracion();
            registro.setLote(loteActivo);
            registro.setTipoDato(tipoDato); // "EXPEDIENTE", "TERMINO" o "AUDIENCIA"
            registro.setPayloadJson(objectMapper.writeValueAsString(requestObjeto)); // Convertimos a JSON texto
            registro.setNumeroExpedienteExtraido(numeroExpediente);
            registro.setEstatus("PENDIENTE");
            
            registroRepository.save(registro);
        } catch (Exception e) {
            throw new RuntimeException("Error al registrar dato en el Lote: " + e.getMessage());
        }
    }

    // 3. Obtener el lote activo sin forzar a crearlo (Para la vista)
    public LoteMigracion obtenerLoteActivoOpcional() {
        return loteRepository.findFirstByEstatusOrderByFechaCreacionDesc("PENDIENTE").orElse(null);
    }

    // 4. Avanzar el asistente de etapa
    @Transactional
    public LoteMigracion avanzarPaso(Long loteId) {
        LoteMigracion lote = loteRepository.findById(loteId).orElseThrow(() -> new RuntimeException("Lote no encontrado"));
        if(lote.getPasoActual() < 4) {
            lote.setPasoActual(lote.getPasoActual() + 1);
        } else {
            lote.setEstatus("COMPLETADO");
        }
        return loteRepository.save(lote);
    }

    // 5. Cancelar todo el lote (Mandar a la papelera)
    @Transactional
    public void cancelarLote(Long loteId) {
        LoteMigracion lote = loteRepository.findById(loteId).orElseThrow(() -> new RuntimeException("Lote no encontrado"));
        lote.setEstatus("RECHAZADO");
        loteRepository.save(lote);
    }

    // =========================================================
    // ETAPA 1: LÓGICA DE CATÁLOGOS
    // =========================================================
    
    @Transactional
    public void extraerCatalogosDeLote(Long loteId) {
        // 1. Traemos todos los registros crudos del lote (que no estén en la papelera)
        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
        LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();

        // 2. Cache en memoria para evitar N+1
        java.util.Set<String> cacheMapeo = new java.util.HashSet<>();

        for (RegistroMigracion reg : registros) {
            try {
                // 3. Leemos el JSON como un árbol de nodos
                JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                
                // 4. Extraemos y guardamos usando el caché local
                if (json.has("gerencia") && !json.get("gerencia").isNull()) {
                    guardarCatalogoUnico(lote, "GERENCIA", json.get("gerencia").asText(), cacheMapeo);
                }
                if (json.has("materia") && !json.get("materia").isNull()) {
                    guardarCatalogoUnico(lote, "MATERIA", json.get("materia").asText(), cacheMapeo);
                }
                if (json.has("organoJurisdiccional") && !json.get("organoJurisdiccional").isNull()) {
                    guardarCatalogoUnico(lote, "ORGANO", json.get("organoJurisdiccional").asText(), cacheMapeo);
                }
                if (json.has("tipoAudiencia") && !json.get("tipoAudiencia").isNull()) {
                    guardarCatalogoUnico(lote, "TIPO_AUDIENCIA", json.get("tipoAudiencia").asText(), cacheMapeo);
                }
                if (json.has("abogadoResponsable") && !json.get("abogadoResponsable").isNull()) {
                    guardarCatalogoUnico(lote, "USUARIO", json.get("abogadoResponsable").asText(), cacheMapeo);
                }
                if (json.has("abogadoCompareciente") && !json.get("abogadoCompareciente").isNull()) {
                    guardarCatalogoUnico(lote, "USUARIO", json.get("abogadoCompareciente").asText(), cacheMapeo);
                }
            } catch (Exception e) {
                // Ignorar JSON malformados en esta etapa
            }
        }
    }

    private void guardarCatalogoUnico(LoteMigracion lote, String tipo, String valor, java.util.Set<String> cache) {
        String valorLimpio = valor.trim();
        if (valorLimpio.isEmpty()) return;

        // Validar caché primero
        String cacheKey = tipo + "___" + valorLimpio.toLowerCase();
        if (cache.contains(cacheKey)) return;

        // Validar en BDD si no está en la caché
        if (!catalogoRepository.existsByLoteIdAndTipoCatalogoAndValorOriginal(lote.getId(), tipo, valorLimpio)) {
            CatalogoMigracion cat = new CatalogoMigracion();
            cat.setLote(lote);
            cat.setTipoCatalogo(tipo);
            cat.setValorOriginal(valorLimpio);
            cat.setEstatus("PENDIENTE");
            catalogoRepository.save(cat);
        }
        
        // Agregar al caché una vez verificado para no volver a consultarlo en la base
        cache.add(cacheKey);
    }

    @Transactional
    public void guardarMapeoCatalogos(Long loteId, java.util.Map<String, String> formData) {
        // Leemos todo el formulario HTML que manda el usuario
        formData.forEach((key, value) -> {
            if (key.startsWith("cat_") && value != null && !value.isEmpty()) {
                Long catalogoMigracionId = Long.parseLong(key.replace("cat_", ""));
                CatalogoMigracion cat = catalogoRepository.findById(catalogoMigracionId).orElse(null);
                
                if (cat != null) {
                    if (value.startsWith("NEW_")) {
                        // El usuario decidió crear uno nuevo
                        cat.setNuevoValor(value.replace("NEW_", ""));
                        cat.setEstatus("NUEVO");
                    } else {
                        // El usuario lo emparejó con uno existente
                        cat.setIdMapeado(Long.parseLong(value));
                        cat.setEstatus("MAPEADO");
                    }
                    catalogoRepository.save(cat);
                }
            }
        });
        
        // Al terminar de guardar, avanzamos automáticamente a la Etapa 2
        avanzarPaso(loteId);
    }

    // =========================================================
    // ETAPA 2: LÓGICA DE EXPEDIENTES
    // =========================================================

   public java.util.List<java.util.Map<String, Object>> obtenerResumenExpedientes(Long loteId) {
        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
        
        java.util.Map<String, List<RegistroMigracion>> agrupados = registros.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                r -> r.getNumeroExpedienteExtraido() != null ? r.getNumeroExpedienteExtraido() : "SIN_NUMERO"
            ));

        java.util.List<java.util.Map<String, Object>> resumen = new java.util.ArrayList<>();
        
        for (java.util.Map.Entry<String, List<RegistroMigracion>> entry : agrupados.entrySet()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            String numExp = entry.getKey();
            map.put("numeroExpediente", numExp);
            
            long totalTerminos = entry.getValue().stream().filter(r -> "TERMINO".equals(r.getTipoDato())).count();
            long totalAudiencias = entry.getValue().stream().filter(r -> "AUDIENCIA".equals(r.getTipoDato())).count();
            
            map.put("totalTerminos", totalTerminos);
            map.put("totalAudiencias", totalAudiencias);
            map.put("totalRegistros", entry.getValue().size());

            // --- LÓGICA DE SEMÁFORO (VALIDACIÓN) ---
            List<String> camposFaltantes = new java.util.ArrayList<>();
            boolean critico = false;

            // Validamos sobre el primer registro que encontremos del expediente (donde usualmente vienen los datos generales)
            try {
                JsonNode json = objectMapper.readTree(entry.getValue().get(0).getPayloadJson());
                
                if (numExp.equals("SIN_NUMERO")) { camposFaltantes.add("Número de Expediente"); critico = true; }
                if (!json.has("materia") || json.get("materia").asText().trim().isEmpty()) { camposFaltantes.add("Materia"); critico = true; }
                if (!json.has("gerencia") || json.get("gerencia").asText().trim().isEmpty()) { camposFaltantes.add("Gerencia"); critico = true; }
                if (!json.has("descripcion") || json.get("descripcion").asText().trim().isEmpty()) { camposFaltantes.add("Descripción"); }
                if (!json.has("abogadoResponsable") || json.get("abogadoResponsable").asText().trim().isEmpty()) { camposFaltantes.add("Abogado"); }
            } catch (Exception e) { critico = true; }

            map.put("faltantes", camposFaltantes);
            map.put("esCritico", critico);
            map.put("estaListo", camposFaltantes.isEmpty());
            
            resumen.add(map);
        }
        return resumen;
    }

    // =========================================================
    // ETAPA 3: LÓGICA DE AUDIENCIAS
    // =========================================================

    public java.util.List<java.util.Map<String, Object>> obtenerResumenAudiencias(Long loteId) {
        // Traemos solo los registros que sean de tipo "AUDIENCIA"
        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndTipoDatoAndEstatusNot(loteId, "AUDIENCIA", "RECHAZADO");
        
        java.util.List<java.util.Map<String, Object>> resumen = new java.util.ArrayList<>();
        
        for (RegistroMigracion reg : registros) {
            try {
                // Leemos el JSON para sacar los detalles específicos
                JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                
                map.put("id", reg.getId());
                map.put("numeroExpediente", reg.getNumeroExpedienteExtraido());
                map.put("tipoAudiencia", json.has("tipoAudiencia") ? json.get("tipoAudiencia").asText() : "Sin especificar");
                map.put("fecha", json.has("fechaAudiencia") ? json.get("fechaAudiencia").asText() : "Sin fecha");
                map.put("hora", json.has("horaAudiencia") ? json.get("horaAudiencia").asText() : "--:--");
                map.put("compareciente", json.has("abogadoCompareciente") ? json.get("abogadoCompareciente").asText() : "N/A");
                
                resumen.add(map);
            } catch (Exception e) {
                // Ignorar errores de parseo individuales
            }
        }
        
        return resumen;
    }

    // =========================================================
    // ETAPA 4: LÓGICA DE TÉRMINOS E INYECCIÓN FINAL
    // =========================================================

    public java.util.List<java.util.Map<String, Object>> obtenerResumenTerminos(Long loteId) {
        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndTipoDatoAndEstatusNot(loteId, "TERMINO", "RECHAZADO");
        java.util.List<java.util.Map<String, Object>> resumen = new java.util.ArrayList<>();
        
        for (RegistroMigracion reg : registros) {
            try {
                JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                
                map.put("numeroExpediente", reg.getNumeroExpedienteExtraido());
                map.put("actuacion", json.has("actuacion") ? json.get("actuacion").asText() : "Sin actuación");
                map.put("fechaVencimiento", json.has("fechaVencimiento") ? json.get("fechaVencimiento").asText() : "Sin fecha");
                map.put("abogado", json.has("abogadoResponsable") ? json.get("abogadoResponsable").asText() : "N/A");
                
                resumen.add(map);
            } catch (Exception e) { }
        }
        return resumen;
    }

    @Transactional
    public void inyectarLoteOficial(Long loteId) {
        LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();
        

        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
        
        String textoProvisional = "Migrado desde sistema histórico - Pendiente de completar por el abogado responsable.";
        // Aquí conectaremos el servicio de creación real (ExpedienteService, etc.)
        // Por ahora, simulamos que los datos se inyectan correctamente y cerramos el lote.
        
        lote.setEstatus("COMPLETADO");
        lote.setPasoActual(5); // El paso 5 ya no muestra el wizard, vuelve al inicio
        loteRepository.save(lote);
        
        // Marcamos los registros crudos como procesados para que queden en el historial
        
        for(RegistroMigracion reg : registros) {
            reg.setEstatus("PROCESADO");
            reg.setProcessedAt(java.time.LocalDateTime.now());
        }
        registroRepository.saveAll(registros);
    }
}