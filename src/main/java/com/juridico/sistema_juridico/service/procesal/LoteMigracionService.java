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
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
    import com.fasterxml.jackson.databind.JsonNode;
    import com.juridico.sistema_juridico.Entity.catalogo.*;
    import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
    import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
    import com.juridico.sistema_juridico.Entity.expediente.Expediente;
    import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
    import com.juridico.sistema_juridico.Entity.procesal.Termino;
    import com.juridico.sistema_juridico.repository.Catalogo.*;
    import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
    import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
    import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;

    import java.time.LocalDate;
    import java.time.LocalTime;
    import java.time.format.DateTimeFormatter;
    import java.time.format.DateTimeParseException;

    import java.util.List;
    import java.util.Map;
    import java.util.HashMap;
    import java.util.ArrayList;
    import java.util.stream.Collectors;


    @Service
    public class LoteMigracionService {

        @Autowired private LoteMigracionRepository loteRepository;
        @Autowired private RegistroMigracionRepository registroRepository;
        @Autowired private UsuarioRepository usuarioRepository;
        @Autowired private CatalogoMigracionRepository catalogoRepository;
        @Autowired private ObjectMapper objectMapper;

        @Autowired private ExpedienteRepository expedienteRepository;
        @Autowired private TerminoRepository terminoRepository;
        @Autowired private AudienciaRepository audienciaRepository;
        @Autowired private GerenciaRepository gerenciaRepository;
        @Autowired private MateriaRepository materiaRepository;
        @Autowired private OrganoJurisdiccionalRepository organoRepository;
        @Autowired private TipoAudienciaRepository tipoAudienciaRepository;
        @Autowired private CatPrioridadRepository catPrioridadRepository;
        @Autowired private EstadoRepository estadoRepository;
        @Autowired private PasswordEncoder passwordEncoder;



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

        // 4.5. Retroceder el asistente de etapa
        @Transactional
        public LoteMigracion retrocederPaso(Long loteId) {
            LoteMigracion lote = loteRepository.findById(loteId).orElseThrow(() -> new RuntimeException("Lote no encontrado"));
            if(lote.getPasoActual() > 1) {
                lote.setPasoActual(lote.getPasoActual() - 1);
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

            // 1.5 Limpiamos catálogos anteriores que estén PENDIENTES para evitar basura de extracciones viejas (como Órganos)
            catalogoRepository.findByLoteId(loteId).stream()
                .filter(c -> "PENDIENTE".equals(c.getEstatus()))
                .forEach(c -> catalogoRepository.delete(c));

            // 2. Cache en memoria para evitar N+1
            java.util.Set<String> cacheMapeo = new java.util.HashSet<>();

            for (RegistroMigracion reg : registros) {
                try {
                    // 3. Leemos el JSON como un árbol de nodos
                    JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                    
                    // 4. Extraemos y guardamos usando el caché local
                    // LIMPIEZA AGRESIVA: Ignoramos cualquier texto que contenga "DEFINIR"
                    if (json.has("gerencia") && !json.get("gerencia").isNull()) {
                        String gerStr = json.get("gerencia").asText().trim();
                        if (!gerStr.toUpperCase().contains("DEFINIR") && !gerStr.isEmpty()) {
                            guardarCatalogoUnico(lote, "GERENCIA", gerStr, cacheMapeo);
                        }
                    }
                    if (json.has("materia") && !json.get("materia").isNull()) {
                        String matStr = json.get("materia").asText();
                        guardarCatalogoUnico(lote, "MATERIA", matStr, cacheMapeo);
                        
                        // INTELIGENCIA: Si la materia coincide con el sistema, inferimos la Gerencia automáticamente
                        materiaRepository.findByNombreIgnoreCase(matStr).ifPresent(m -> {
                            if (m.getGerencia() != null) {
                                guardarCatalogoUnico(lote, "GERENCIA", m.getGerencia().getNombre(), cacheMapeo);
                            }
                        });
                    }
                    if (json.has("sede") && !json.get("sede").isNull()) {
                        guardarCatalogoUnico(lote, "SEDE", json.get("sede").asText(), cacheMapeo);
                    }
                    
                    // NUEVO: Ahora sí extraemos el Órgano Jurisdiccional para la Etapa 1
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
            
            // Al terminar la extracción, intentamos auto-mapear lo que coincida exactamente con la DB
            // Hemos terminado la extracción. Los catálogos ya están en la DB (o en caché).

            intentarAutoMapeo(loteId);
        }

        @Transactional
        public void intentarAutoMapeo(Long loteId) {
            List<CatalogoMigracion> pendientes = catalogoRepository.findByLoteId(loteId).stream()
                .filter(c -> c.getIdMapeado() == null && "PENDIENTE".equals(c.getEstatus()))
                .toList();

            for (CatalogoMigracion c : pendientes) {
                String valor = c.getValorOriginal().trim();
                if (valor.isEmpty()) continue;

                switch (c.getTipoCatalogo()) {
                    case "GERENCIA":
                        gerenciaRepository.findByNombreIgnoreCase(valor).ifPresent(g -> {
                            c.setIdMapeado(g.getId().longValue());
                            c.setEstatus("MAPEADO");
                        });
                        break;
                    case "MATERIA":
                        materiaRepository.findByNombreIgnoreCase(valor).ifPresent(m -> {
                            c.setIdMapeado(m.getId().longValue());
                            c.setEstatus("MAPEADO");
                        });
                        break;
                    case "SEDE":
                        estadoRepository.findAll().stream()
                            .filter(e -> e.getNombre().equalsIgnoreCase(valor))
                            .findFirst().ifPresent(e -> {
                                c.setIdMapeado(e.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        break;
                    case "ORGANO":
                        organoRepository.findByNombreIgnoreCase(valor).ifPresent(o -> {
                            c.setIdMapeado(o.getId().longValue());
                            c.setEstatus("MAPEADO");
                        });
                        break;
                    case "TIPO_AUDIENCIA":
                        tipoAudienciaRepository.findByNombreIgnoreCase(valor).ifPresent(t -> {
                            c.setIdMapeado(t.getId().longValue());
                            c.setEstatus("MAPEADO");
                        });
                        break;
                    case "USUARIO":
                        usuarioRepository.findAll().stream()
                            .filter(u -> u.getNombreCompleto().equalsIgnoreCase(valor))
                            .findFirst().ifPresent(u -> {
                                c.setIdMapeado(u.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        break;
                }
                if (c.getIdMapeado() != null) {
                    catalogoRepository.save(c);
                }
            }
        }

        private void guardarCatalogoUnico(LoteMigracion lote, String tipo, String valor, java.util.Set<String> cache) {
            String valorLimpio = valor.trim();
            if (valorLimpio.isEmpty()) return;

            // Normalización agresiva para el caché (ignorar acentos, espacios múltiples y case)
            String valorNormalizado = normalizeValue(valorLimpio);
            String cacheKey = tipo + "___" + valorNormalizado;
            
            if (cache.contains(cacheKey)) return;

            // Validar en BDD si ya existe algo con la misma normalización para este lote/tipo
            java.util.List<CatalogoMigracion> existentes = catalogoRepository.findByLoteIdAndTipoCatalogo(lote.getId(), tipo);
            boolean yaExisteSimiliar = existentes.stream()
                .anyMatch(c -> normalizeValue(c.getValorOriginal()).equals(valorNormalizado));
            
            if (!yaExisteSimiliar) {
                CatalogoMigracion cat = new CatalogoMigracion();
                cat.setLote(lote);
                cat.setTipoCatalogo(tipo);
                cat.setValorOriginal(valorLimpio);
                cat.setEstatus("PENDIENTE");
                catalogoRepository.save(cat);
            }
            
            cache.add(cacheKey);
        }

        private String normalizeValue(String value) {
            if (value == null) return "";
            // Quitar acentos y diacríticos
            String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD);
            normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
            // Lowercase, quitar espacios extra y trim
            return normalized.toLowerCase().replaceAll("\\s+", " ").trim();
        }

        @Transactional
        public void guardarMapeoCatalogos(Long loteId, java.util.Map<String, String> formData) {
            // Leemos todo el formulario HTML que manda el usuario
            formData.forEach((key, value) -> {
                if (key.startsWith("cat_") && value != null && !value.isEmpty()) {
                    Long catalogoMigracionId = Long.parseLong(key.replace("cat_", ""));
                    CatalogoMigracion cat = catalogoRepository.findById(catalogoMigracionId).orElse(null);
                    
                    if (cat != null) {
                        if (value.equals("SKIP")) {
                            // El usuario descartó este valor
                            cat.setEstatus("DESCARTADO");
                        } else if (value.startsWith("NEW_")) {
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
                } catch (Exception e) { critico = true; }

                map.put("faltantes", camposFaltantes);
                map.put("esCritico", critico);
                map.put("estaListo", camposFaltantes.isEmpty());
                
                // Expose registroId of the first record for AJAX editing
                map.put("registroId", entry.getValue().get(0).getId());

                // --- LÓGICA DE VALORES MÁS REPETIDOS (MODA) ---
                String partesModa = findMostRepeated(entry.getValue(), "partes", "");
                String sedeModa = findMostRepeated(entry.getValue(), "sede", "");
                String gerenciaModa = findMostRepeated(entry.getValue(), "gerencia", "");
                String materiaModa = findMostRepeated(entry.getValue(), "materia", "");
                String organoModa = findMostRepeated(entry.getValue(), "organoJurisdiccional", "");
                String abogadoModa = findMostRepeated(entry.getValue(), "abogadoResponsable", "");
                String descModa = findMostRepeated(entry.getValue(), "descripcion", "Expediente migrado.");

                // LIMPIEZA AGRESIVA de basura antes de procesar
                if (gerenciaModa != null && gerenciaModa.toUpperCase().contains("DEFINIR")) gerenciaModa = "";
                if (materiaModa != null && materiaModa.toUpperCase().contains("DEFINIR")) materiaModa = "";

                // INTELIGENCIA MEJORADA: Inferir gerencia buscando la materia directamente en la Base de Datos
                if ((gerenciaModa == null || gerenciaModa.isEmpty()) && materiaModa != null && !materiaModa.isEmpty()) {
                    java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaModa.trim());
                    if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                        gerenciaModa = matOpt.get().getGerencia().getNombre();
                    }
                }

                map.put("partes", partesModa);
                map.put("sede", sedeModa);
                map.put("gerencia", gerenciaModa);
                map.put("materia", materiaModa);
                map.put("organo", organoModa);
                map.put("abogado", abogadoModa);
                map.put("descripcion", descModa);

    // --- RESOLUCIÓN DE IDs PARA EL MODAL ---
                // Buscamos si el valor de la moda ya fue mapeado en la Etapa 1
                Long materiaId = buscarIdMapeado(loteId, "MATERIA", materiaModa);
                Long gerenciaId = buscarIdMapeado(loteId, "GERENCIA", gerenciaModa);
                
    // Limpiamos la basura del JSON antes de mandarla al frontend
                if (gerenciaModa.equalsIgnoreCase("POR DEFINIR")) gerenciaModa = "";

                // INTELIGENCIA: Si la gerenciaId es nula pero sí tenemos una materiaId mapeada...
                if (gerenciaId == null && materiaId != null) {
                    java.util.Optional<Materia> matOpt = materiaRepository.findById(materiaId.intValue());
                    if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                        gerenciaId = matOpt.get().getGerencia().getId().longValue();
                        // FIX: Actualizamos también el texto para que el Select de Javascript lo encuentre
                        gerenciaModa = matOpt.get().getGerencia().getNombre(); 
                    }
                }
                
                map.put("gerencia", gerenciaModa); // Aseguramos que se mande el texto limpio/inferido
                map.put("gerenciaId", gerenciaId);
                map.put("materiaId", materiaId);
                map.put("sedeId", buscarIdMapeado(loteId, "SEDE", sedeModa));
                
                resumen.add(map);
            }
            return resumen;
        }
// =========================================================
    // ETAPA 3: LÓGICA DE AUDIENCIAS
    // =========================================================

    public java.util.List<java.util.Map<String, Object>> obtenerResumenAudiencias(Long loteId) {
        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndTipoDatoAndEstatusNot(loteId, "AUDIENCIA", "RECHAZADO");
        List<RegistroMigracion> todosLosRegistros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
        
        java.util.List<java.util.Map<String, Object>> resumen = new java.util.ArrayList<>();
        
        for (RegistroMigracion reg : registros) {
            try {
                JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                
                String numExp = reg.getNumeroExpedienteExtraido() != null ? reg.getNumeroExpedienteExtraido() : "";
                
                // --- Agregamos ID y Numero de Expediente ---
                map.put("id", reg.getId());
                map.put("numeroExpediente", numExp);

                // 1. Extraemos los valores en variables primero
                String tipoAud = json.has("tipoAudiencia") ? json.get("tipoAudiencia").asText() : "Sin especificar";
                String comp = json.has("abogadoCompareciente") ? json.get("abogadoCompareciente").asText() : "";
                String resp = json.has("abogadoResponsable") ? json.get("abogadoResponsable").asText() : "";
                String sedeVal = json.has("sede") ? json.get("sede").asText() : "";
                String organoVal = json.has("organoJurisdiccional") ? json.get("organoJurisdiccional").asText() : "";

                // 2. Guardamos los textos
                map.put("tipoAudiencia", tipoAud);
                map.put("fecha", json.has("fechaAudiencia") ? json.get("fechaAudiencia").asText() : "Sin fecha");
                map.put("hora", json.has("horaAudiencia") ? json.get("horaAudiencia").asText() : "--:--");
                map.put("compareciente", comp);
                map.put("abogadoCompareciente", comp);
                map.put("abogadoResponsable", resp);
                map.put("sede", sedeVal);
                map.put("sala", json.has("sala") ? json.get("sala").asText() : "");
                map.put("organo", organoVal);
                map.put("partes", json.has("partes") ? json.get("partes").asText() : "");

                // 3. RESOLUCIÓN DE IDs PARA LOS SELECTS DEL MODAL
                map.put("abogadoComparecienteId", buscarIdMapeado(loteId, "USUARIO", comp));
                map.put("abogadoResponsableId", buscarIdMapeado(loteId, "USUARIO", resp));
                map.put("sedeId", buscarIdMapeado(loteId, "SEDE", sedeVal));
                map.put("organoId", buscarIdMapeado(loteId, "ORGANO", organoVal));
                map.put("tipoAudienciaId", buscarIdMapeado(loteId, "TIPO_AUDIENCIA", tipoAud));

                map.put("esEnLinea", json.has("esEnLinea") ? json.get("esEnLinea").asText() : (json.has("esVirtual") ? json.get("esVirtual").asText() : "false"));
                map.put("enlaceReunion", json.has("enlaceReunion") ? json.get("enlaceReunion").asText() : (json.has("urlReunion") ? json.get("urlReunion").asText() : ""));
                
                // --- INTELIGENCIA DE HERENCIA ---
                String gerenciaAsignada = json.has("gerencia") && !json.get("gerencia").isNull() ? json.get("gerencia").asText().trim() : "";
                String materiaAsignada = json.has("materia") && !json.get("materia").isNull() ? json.get("materia").asText().trim() : "";
                
                // Limpieza INMEDIATA de basura
                if (gerenciaAsignada.toUpperCase().contains("DEFINIR")) gerenciaAsignada = "";
                if (materiaAsignada.toUpperCase().contains("DEFINIR")) materiaAsignada = "";
                
                // Si viene vacío, lo heredamos del expediente padre
                if (gerenciaAsignada.isEmpty() || materiaAsignada.isEmpty()) {
                    List<RegistroMigracion> contextoExpediente = todosLosRegistros.stream()
                        .filter(r -> r.getNumeroExpedienteExtraido() != null && r.getNumeroExpedienteExtraido().equals(numExp))
                        .toList();
                    
                    if (gerenciaAsignada.isEmpty()) {
                        gerenciaAsignada = findMostRepeated(contextoExpediente, "gerencia", "");
                        if (gerenciaAsignada.toUpperCase().contains("DEFINIR")) gerenciaAsignada = ""; 
                    }
                    if (materiaAsignada.isEmpty()) {
                        materiaAsignada = findMostRepeated(contextoExpediente, "materia", "");
                        if (materiaAsignada.toUpperCase().contains("DEFINIR")) materiaAsignada = ""; 
                    }
                }
                
                // INTELIGENCIA: Si la gerencia sigue vacía, inferirla basándonos en la materia real
                if (gerenciaAsignada.isEmpty() && !materiaAsignada.isEmpty()) {
                    java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaAsignada);
                    if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                        gerenciaAsignada = matOpt.get().getGerencia().getNombre();
                    }
                }
                
                // Extraer el ID exacto para el Frontend
                Long gerenciaId = null;
                if (!gerenciaAsignada.isEmpty()) {
                    final String searchGer = gerenciaAsignada;
                    gerenciaId = gerenciaRepository.findAll().stream()
                        .filter(g -> g.getNombre().equalsIgnoreCase(searchGer))
                        .findFirst().map(g -> g.getId().longValue()).orElse(null);
                }
                
                map.put("gerencia", gerenciaAsignada);
                map.put("gerenciaId", gerenciaId);
                map.put("materia", materiaAsignada);
                map.put("materiaId", buscarIdMapeado(loteId, "MATERIA", materiaAsignada));
                    
                resumen.add(map);
            } catch (Exception e) {
                // Ignorar errores de parseo individuales (Aquí se cierra correctamente el catch)
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
                    
                    map.put("id", reg.getId());
                    map.put("numeroExpediente", reg.getNumeroExpedienteExtraido());
                    map.put("actuacion", json.has("asunto") ? json.get("asunto").asText() : (json.has("actuacion") ? json.get("actuacion").asText() : (json.has("prestacion") ? json.get("prestacion").asText() : "Sin actuación")));
                    map.put("fechaVencimiento", json.has("fechaVencimiento") ? json.get("fechaVencimiento").asText() : "Sin fecha");
                    map.put("abogado", json.has("abogadoResponsable") ? json.get("abogadoResponsable").asText() : "N/A");
                    map.put("prioridad", json.has("prioridad") ? json.get("prioridad").asText() : "Media");
                    map.put("observaciones", json.has("observaciones") ? json.get("observaciones").asText() : "");
                    
                    // Nuevo campo para el modal de edición
                    map.put("fechaIngreso", json.has("fechaIngreso") ? json.get("fechaIngreso").asText() : "");
                    
                    resumen.add(map);
                } catch (Exception e) { }
            }
            return resumen;
        }

        /**
         * AJAX: Edita los campos de un RegistroMigracion individual y guarda el JSON actualizado.
         */
        @Transactional
        public void editarRegistro(Long registroId, java.util.Map<String, String> campos) {
            RegistroMigracion reg = registroRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro no encontrado: " + registroId));
            
            try {
                com.fasterxml.jackson.databind.node.ObjectNode json = 
                    (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(reg.getPayloadJson());
                
                // Actualizamos solo los campos que nos pasen (los no vacíos)
                campos.forEach((campo, valor) -> {
                    if (valor != null && !valor.trim().isEmpty()) {
                        json.put(campo, valor.trim());
                    }
                });
                
                // Actualizamos también el número de expediente si cambió
                if (campos.containsKey("numeroExpediente") && !campos.get("numeroExpediente").trim().isEmpty()) {
                    reg.setNumeroExpedienteExtraido(campos.get("numeroExpediente").trim());
                }
                
                reg.setPayloadJson(objectMapper.writeValueAsString(json));
                registroRepository.save(reg);
            } catch (Exception e) {
                throw new RuntimeException("Error al editar registro: " + e.getMessage(), e);
            }
        }

        @Transactional
        public void inyectarLoteOficial(Long loteId) {
            Map<String, CatalogoMigracion> lookup = new HashMap<>();
            try {
                LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();
                List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
                List<CatalogoMigracion> catalogosMap = catalogoRepository.findByLoteId(loteId);

                for (CatalogoMigracion c : catalogosMap) {
                    lookup.put(c.getTipoCatalogo() + "___" + c.getValorOriginal().toLowerCase(), c);
                }

                // --- PRE-PROCESAMIENTO: Crear entidades nuevas marcadas en la Etapa 1 ---
                crearEntidadesNuevas(catalogosMap);

                java.util.Map<String, List<RegistroMigracion>> expedientesAgrupados = registros.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getNumeroExpedienteExtraido() != null && !r.getNumeroExpedienteExtraido().trim().isEmpty() ? r.getNumeroExpedienteExtraido() : "SIN_NUMERO"
                    ));

                for (java.util.Map.Entry<String, List<RegistroMigracion>> entry : expedientesAgrupados.entrySet()) {
                    String numExp = entry.getKey();
                    List<RegistroMigracion> expRegistros = entry.getValue();

                    Expediente expediente = expedienteRepository.findByNumeroIgnoreCase(numExp).orElse(null);

                    if (expediente == null) {
                        expediente = new Expediente();
                        String numeroAsignado = numExp.equals("SIN_NUMERO") ? ("S/N-" + java.util.UUID.randomUUID().toString().substring(0,8)) : numExp;
                        if (numeroAsignado.length() > 50) numeroAsignado = numeroAsignado.substring(0, 50);
                        expediente.setNumero(numeroAsignado);
                        expediente.setDescripcion("Migrado desde sistema histórico - Pendiente de completar por el abogado responsable.");
                        
                        for (RegistroMigracion r : expRegistros) {
                            try {
                                JsonNode json = objectMapper.readTree(r.getPayloadJson());
                                
                                if (expediente.getGerencia() == null && json.has("gerencia") && !json.get("gerencia").isNull()) {
                                    Gerencia g = resolveGerencia(json.get("gerencia").asText(), lookup);
                                    if (g != null) expediente.setGerencia(g);
                                }
                                if (expediente.getMateria() == null && json.has("materia")) {
                                    Materia m = resolveMateria(json.get("materia").asText(), lookup, expediente.getGerencia());
                                    if (m != null) {
                                        expediente.setMateria(m);
                                        // Si después de esto la gerencia sigue nula (no venía en el JSON), 
                                        // asignamos la gerencia correspondiente a la materia.
                                        if (expediente.getGerencia() == null && m.getGerencia() != null) {
                                            expediente.setGerencia(m.getGerencia());
                                        }
                                    }
                                }
                                if (expediente.getOrganoJurisdiccional() == null && json.has("organoJurisdiccional")) {
                                    OrganoJurisdiccional o = resolveOrgano(json.get("organoJurisdiccional").asText(), lookup, expediente.getGerencia());
                                    if (o != null) expediente.setOrganoJurisdiccional(o);
                                }
                                if (expediente.getPartes() == null && json.has("partes") && !json.get("partes").isNull()) {
                                    expediente.setPartes(json.get("partes").asText());
                                }
                                if (expediente.getAbogadoResponsable() == null && json.has("abogadoResponsable")) {
                                    String nombreAbog = json.get("abogadoResponsable").asText();
                                    Usuario u = resolveUsuario(nombreAbog, lookup, expediente.getGerencia(), loteId);
                                    if (u != null) {
                                        expediente.setAbogadoResponsable(u);
                                    } else {
                                        if (nombreAbog.length() > 200) nombreAbog = nombreAbog.substring(0, 200);
                                        expediente.setAbogadoResponsableNombre(nombreAbog);
                                    }
                                }
                                // NUEVO CÓDIGO: Fuerza la creación de la sede en el catálogo si no existe
                                if (expediente.getSede() == null && json.has("sede") && !json.get("sede").isNull()) {
                                    String sedeOriginal = json.get("sede").asText().trim();
                                    CatalogoMigracion catSede = lookup.get("SEDE___" + sedeOriginal.toLowerCase());
                                    
                                    if (catSede != null && catSede.getIdMapeado() != null) {
                                        java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Estado> estadoOpt = estadoRepository.findById(catSede.getIdMapeado().intValue());
                                        if (estadoOpt.isPresent()) {
                                            expediente.setSede(estadoOpt.get().getNombre());
                                        }
                                    } else {
                                        // FIX: Si la sede no fue mapeada, la buscamos o la creamos en la tabla Estado
                                        String sedeFinal = sedeOriginal;
                                        if (sedeFinal.length() > 150) sedeFinal = sedeFinal.substring(0, 150);
                                        final String searchSede = sedeFinal;
                                        
                                        com.juridico.sistema_juridico.Entity.catalogo.Estado estadoExistente = estadoRepository.findAll().stream()
                                            .filter(e -> e.getNombre().equalsIgnoreCase(searchSede))
                                            .findFirst()
                                            .orElse(null);
                                            
                                        if (estadoExistente == null) {
                                            com.juridico.sistema_juridico.Entity.catalogo.Estado nuevoEstado = new com.juridico.sistema_juridico.Entity.catalogo.Estado();
                                            nuevoEstado.setNombre(searchSede);
                                            nuevoEstado.setGerencia(expediente.getGerencia() != null ? expediente.getGerencia() : getFallbackGerencia());
                                            estadoExistente = estadoRepository.save(nuevoEstado);
                                        }
                                        
                                        expediente.setSede(estadoExistente.getNombre());
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        
                        if (expediente.getMateria() == null) {
                            expediente.setMateria(getFallbackMateria());
                        }
                        
                        if (expediente.getGerencia() == null) {
                            expediente.setGerencia(getFallbackGerencia());
                        }

                        expediente = expedienteRepository.save(expediente);
                    }

                    for (RegistroMigracion r : expRegistros) {
                        try {
                            JsonNode json = objectMapper.readTree(r.getPayloadJson());
                            if ("TERMINO".equals(r.getTipoDato())) {
                                Termino termino = new Termino();
                                termino.setExpediente(expediente);
                                termino.setAsunto(json.has("prestacion") ? json.get("prestacion").asText() : (json.has("asunto") ? json.get("asunto").asText() : "Sin actuación"));

                                LocalDate fechaVenc = parseDate(json.get("fechaVencimiento"));
                                LocalDate fechaIng = parseDate(json.get("fechaIngreso"));
                                termino.setFechaVencimiento(fechaVenc);
                                termino.setFechaIngreso(fechaIng);

                                // Estatus automático basado en fecha de vencimiento
                                termino.setEstatusTermino(
                                    fechaVenc.isBefore(LocalDate.now()) ? EstatusTermino.CONCLUIDO : EstatusTermino.PROYECTISTA
                                );

                                // Resolver prioridad desde catálogo
                                String prioridadStr = json.has("prioridad") ? json.get("prioridad").asText("Media") : "Media";
                                CatPrioridad prioridad = catPrioridadRepository.findByNombreIgnoreCase(prioridadStr)
                                    .orElseGet(() -> catPrioridadRepository.findByNombreIgnoreCase("Media").orElse(null));
                                termino.setPrioridad(prioridad);

                                String acuseStr = json.has("acuse") ? json.get("acuse").asText() : "";
                                if (acuseStr.length() > 500) acuseStr = acuseStr.substring(0, 500);
                                termino.setAcuse(acuseStr);

                                String acuseNombreStr = json.has("archivoWord") ? json.get("archivoWord").asText() : (json.has("acuseNombre") ? json.get("acuseNombre").asText() : "");
                                if (acuseNombreStr.length() > 500) acuseNombreStr = acuseNombreStr.substring(0, 500);
                                termino.setAcuseNombre(acuseNombreStr);

                                termino.setObservaciones(json.has("observaciones") ? json.get("observaciones").asText() : "");
                                termino.setCreatedAt(java.time.LocalDateTime.now());
                                termino.setUpdatedAt(java.time.LocalDateTime.now());

                                if (json.has("abogadoResponsable")) {
                                    Usuario u = resolveUsuario(json.get("abogadoResponsable").asText(), lookup, expediente.getGerencia(), loteId);
                                    if (u != null) termino.setAbogadoResponsable(u);
                                }

                                terminoRepository.save(termino);

                            } else if ("AUDIENCIA".equals(r.getTipoDato())) {
                                Audiencia audiencia = new Audiencia();
                                audiencia.setExpediente(expediente);

                                LocalDate fechaAud = parseDate(json.get("fechaAudiencia"));
                                audiencia.setFechaAudiencia(fechaAud);
                                audiencia.setHoraAudiencia(parseTime(json.get("horaAudiencia")));

                                // Estatus automático basado en la fecha de audiencia
                                audiencia.setEstatusAudiencia(
                                    fechaAud.isBefore(LocalDate.now()) ? EstatusAudiencia.CONCLUIDA : EstatusAudiencia.PENDIENTE
                                );

                                // Sala y lugar (campo correcto: 'sala', no 'sede')
                                String salaStr = json.has("sala") ? json.get("sala").asText() : (json.has("salaLugar") ? json.get("salaLugar").asText() : "");
                                audiencia.setSalaLugar(salaStr);

                                // Modalidad virtual
                                boolean esVirtual = false;
                                if (json.has("esEnLinea")) {
                                    String esEnLineaVal = json.get("esEnLinea").asText("").toLowerCase();
                                    esVirtual = esEnLineaVal.equals("true") || esEnLineaVal.equals("1") || esEnLineaVal.equals("si") || esEnLineaVal.equals("sí");
                                } else if (json.has("esVirtual")) {
                                    esVirtual = json.get("esVirtual").asBoolean(false);
                                }
                                audiencia.setEsVirtual(esVirtual);

                                // Enlace de reunión
                                if (json.has("enlaceReunion") && !json.get("enlaceReunion").isNull()) {
                                    audiencia.setUrlReunion(json.get("enlaceReunion").asText());
                                } else if (json.has("urlReunion") && !json.get("urlReunion").isNull()) {
                                    audiencia.setUrlReunion(json.get("urlReunion").asText());
                                }

                                String actaStr = json.has("actaDocumento") ? json.get("actaDocumento").asText() : "";
                                if (actaStr.length() > 500) actaStr = actaStr.substring(0, 500);
                                audiencia.setActaDocumento(actaStr);
                                audiencia.setObservaciones(json.has("observaciones") ? json.get("observaciones").asText() : "");

                                if (json.has("tipoAudiencia")) {
                                    TipoAudiencia ta = resolveTipoAudiencia(json.get("tipoAudiencia").asText(), lookup, expediente.getGerencia());
                                    if (ta != null) audiencia.setTipoAudiencia(ta);
                                }

                                if (json.has("abogadoCompareciente")) {
                                    Usuario u = resolveUsuario(json.get("abogadoCompareciente").asText(), lookup, expediente.getGerencia(), loteId);
                                    if (u != null) audiencia.setAbogadoComparece(u);
                                }

                                audiencia.setCreatedAt(java.time.LocalDateTime.now());
                                audiencia.setUpdatedAt(java.time.LocalDateTime.now());
                                audienciaRepository.save(audiencia);
                            }
                        } catch (Exception e) {
                            System.err.println("Error procesando registro: " + r.getId() + " - " + e.getMessage());
                        }

                        r.setEstatus("PROCESADO");
                        r.setProcessedAt(java.time.LocalDateTime.now());
                    }
                }
                
                lote.setEstatus("COMPLETADO");
                lote.setPasoActual(5);
                loteRepository.save(lote);
                registroRepository.saveAll(registros);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error inyectando lote: " + e.getMessage(), e);
            }
        }

        private Gerencia getFallbackGerencia() {
            return gerenciaRepository.findAll().stream().findFirst().orElseGet(() -> {
                Gerencia g = new Gerencia(); g.setNombre("Sin Gerencia"); g.setActivo(true);
                return gerenciaRepository.save(g);
            });
        }

        private Materia getFallbackMateria() {
            return materiaRepository.findAll().stream().findFirst().orElseGet(() -> {
                Materia m = new Materia(); m.setNombre("Sin Materia"); m.setActivo(true);
                m.setGerencia(getFallbackGerencia());
                return materiaRepository.save(m);
            });
        }

        private Materia resolveMateria(String original, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerenciaContexto) {
            if (original == null || original.trim().isEmpty()) return null;
            String key = "MATERIA___" + original.trim().toLowerCase();
            CatalogoMigracion c = lookup.get(key);
            if (c != null) {
                if (c.getIdMapeado() != null) return materiaRepository.findById(c.getIdMapeado().intValue()).orElse(null);
                if (c.getNuevoValor() != null) {
                    String nombreFinal = c.getNuevoValor();
                    if (nombreFinal.length() > 100) nombreFinal = nombreFinal.substring(0, 100);

                    final String searchName = nombreFinal;
                    Materia existente = materiaRepository.findByNombreIgnoreCase(searchName).orElse(null);
                    if (existente == null) {
                        Materia m = new Materia(); 
                        m.setNombre(searchName); 
                        m.setActivo(true);
                        m.setGerencia(gerenciaContexto != null ? gerenciaContexto : getFallbackGerencia());
                        existente = materiaRepository.save(m);
                    }
                    c.setIdMapeado(existente.getId().longValue());
                    c.setNuevoValor(null);
                    return existente;
                }
            }
            // Fallback: buscar directamente en la DB por nombre
            return materiaRepository.findByNombreIgnoreCase(original.trim()).orElse(null);
        }
        private Gerencia resolveGerencia(String original, java.util.Map<String, CatalogoMigracion> lookup) {
            if (original == null || original.trim().isEmpty()) return null;
            String key = "GERENCIA___" + original.trim().toLowerCase();
            CatalogoMigracion c = lookup.get(key);
            if (c != null) {
                if (c.getIdMapeado() != null) return gerenciaRepository.findById(c.getIdMapeado().intValue()).orElse(null);
                if (c.getNuevoValor() != null) {
                    String nombreFinal = c.getNuevoValor();
                    if (nombreFinal.length() > 100) nombreFinal = nombreFinal.substring(0, 100);

                    final String searchName = nombreFinal;
                    Gerencia existente = gerenciaRepository.findAll().stream()
                        .filter(g -> g.getNombre().equalsIgnoreCase(searchName))
                        .findFirst().orElse(null);
                    if (existente == null) {
                        Gerencia g = new Gerencia(); g.setNombre(searchName); g.setActivo(true);
                        existente = gerenciaRepository.save(g);
                    }
                    c.setIdMapeado(existente.getId().longValue());
                    c.setNuevoValor(null);
                    return existente;
                }
            }
            // Fallback: Si no está en el mapa de mapeos, buscar directamente en la DB por nombre
            return gerenciaRepository.findAll().stream()
                    .filter(g -> g.getNombre().equalsIgnoreCase(original.trim()))
                    .findFirst().orElse(null);
        }
        private OrganoJurisdiccional resolveOrgano(String original, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerenciaContexto) {
            if (original == null || original.trim().isEmpty()) return null;
            String key = "ORGANO___" + original.trim().toLowerCase();
            CatalogoMigracion c = lookup.get(key);
            if (c != null) {
                if (c.getIdMapeado() != null) return organoRepository.findById(c.getIdMapeado().intValue()).orElse(null);
                if (c.getNuevoValor() != null) {
                    String nombreFinal = c.getNuevoValor();
                    if (nombreFinal.length() > 100) nombreFinal = nombreFinal.substring(0, 100);

                    final String searchName = nombreFinal;
                    OrganoJurisdiccional existente = organoRepository.findByNombreIgnoreCase(searchName).orElse(null);
                    if (existente == null) {
                        OrganoJurisdiccional o = new OrganoJurisdiccional(); 
                        o.setNombre(searchName); 
                        o.setActivo(true);
                        o.setGerencia(gerenciaContexto != null ? gerenciaContexto : getFallbackGerencia());
                        existente = organoRepository.save(o);
                    }
                    c.setIdMapeado(existente.getId().longValue());
                    c.setNuevoValor(null);
                    return existente;
                }
            }
            // Fallback: buscar directamente en la DB por nombre
            return organoRepository.findByNombreIgnoreCase(original.trim()).orElse(null);
        }

        private TipoAudiencia resolveTipoAudiencia(String original, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerenciaContexto) {
            if (original == null || original.trim().isEmpty()) return null;
            String key = "TIPO_AUDIENCIA___" + original.trim().toLowerCase();
            CatalogoMigracion c = lookup.get(key);
            if (c != null) {
                if (c.getIdMapeado() != null) return tipoAudienciaRepository.findById(c.getIdMapeado().intValue()).orElse(null);
                if (c.getNuevoValor() != null) {
                    String nombreFinal = c.getNuevoValor();
                    if (nombreFinal.length() > 100) nombreFinal = nombreFinal.substring(0, 100);

                    final String searchName = nombreFinal;
                    TipoAudiencia existente = tipoAudienciaRepository.findByNombreIgnoreCase(searchName).orElse(null);
                    if (existente == null) {
                        TipoAudiencia t = new TipoAudiencia(); 
                        t.setNombre(searchName); 
                        t.setActivo(true);
                        t.setGerencia(gerenciaContexto != null ? gerenciaContexto : getFallbackGerencia());
                        existente = tipoAudienciaRepository.save(t);
                    }
                    c.setIdMapeado(existente.getId().longValue());
                    c.setNuevoValor(null);
                    return existente;
                }
            }
            // Fallback: buscar directamente en la DB por nombre
            return tipoAudienciaRepository.findByNombreIgnoreCase(original.trim()).orElse(null);
        }

        private Usuario resolveUsuario(String original, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerencia, Long loteId) {
            if (original == null || original.trim().isEmpty()) return null;
            CatalogoMigracion c = lookup.get("USUARIO___" + original.trim().toLowerCase());
            if (c != null && c.getIdMapeado() != null) {
                return usuarioRepository.findById(c.getIdMapeado().intValue()).orElse(null);
            }
            // Si no hay mapeo, buscar por nombre o crear uno nuevo
            return crearOEncontrarAbogado(original.trim(), gerencia, loteId, lookup);
        }

        private Usuario crearOEncontrarAbogado(String nombreCompleto, Gerencia gerenciaExpediente, Long loteId, java.util.Map<String, CatalogoMigracion> lookup) {
            if (nombreCompleto == null || nombreCompleto.isEmpty()) return null;

            // 1. Buscar si ya existe alguien con ese nombre completo
            List<Usuario> existentes = usuarioRepository.findAll().stream()
                .filter(u -> u.getNombreCompleto().equalsIgnoreCase(nombreCompleto))
                .toList();
            if (!existentes.isEmpty()) return existentes.get(0);

            // 2. Generar email único a partir del nombre: "juan.perez@juridico.com"
            String emailBase = nombreCompleto.trim().toLowerCase()
                .replaceAll("[áàäâ]", "a").replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i").replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u").replaceAll("ñ", "n")
                .replaceAll("[^a-z0-9\\s]", "").trim().replaceAll("\\s+", ".");
            if (emailBase.length() > 50) emailBase = emailBase.substring(0, 50);
            
            String email = emailBase + "@juridico.com";
            int intento = 0;
            while (usuarioRepository.existsByEmail(email)) {
                intento++;
                email = emailBase + intento + "@juridico.com";
            }

            // 3. Generar contraseña temporal: "tempora" + 6 dígitos aleatorios
            String passwordRaw = "tempora" + String.format("%06d", new java.util.Random().nextInt(999999));
            String passwordHash = passwordEncoder.encode(passwordRaw);

            // 4. Usar la gerencia del expediente (mapeada en Etapa 1)
            Gerencia gerenciaFinal = gerenciaExpediente != null ? gerenciaExpediente : getFallbackGerencia();

            // 5. Escanear todos los registros del lote para encontrar las materias reales de este abogado
            java.util.Set<Materia> materiasAbogado = obtenerMateriasDeAbogado(nombreCompleto, loteId, lookup, gerenciaFinal);
            // Si no se encontraron materias, no asignar ninguna (el admin puede asignarlas después)
            if (materiasAbogado.isEmpty()) {
                System.out.println(">>> Migracion: No se encontraron materias en los registros para: " + nombreCompleto);
            }

            // 6. Construir y guardar el usuario
            Usuario nuevoAbogado = new Usuario();
            nuevoAbogado.setNombreCompleto(nombreCompleto.length() > 200 ? nombreCompleto.substring(0, 200) : nombreCompleto);
            nuevoAbogado.setEmail(email);
            nuevoAbogado.setPassword(passwordHash);
            nuevoAbogado.setRol(RolUsuario.ABOGADO);
            nuevoAbogado.setGerencia(gerenciaFinal);
            nuevoAbogado.setActivo(true);
            nuevoAbogado.setForcePasswordChange(true);
            nuevoAbogado.setMaterias(materiasAbogado);

            Usuario guardado = usuarioRepository.save(nuevoAbogado);
            System.out.println(">>> Migracion: Usuario creado: " + email + " / pass: " + passwordRaw 
                + " / gerencia: " + gerenciaFinal.getNombre() 
                + " / materias: " + materiasAbogado.stream().map(Materia::getNombre).toList());
            return guardado;
        }

        /**
         * Escanea todos los registros del lote buscando las materias asociadas a un abogado.
         * Busca en los campos abogadoResponsable y abogadoCompareciente, y extrae el campo "materia".
         */
        private java.util.Set<Materia> obtenerMateriasDeAbogado(String nombreAbogado, Long loteId, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerenciaContexto) {
            java.util.Set<Materia> materias = new java.util.HashSet<>();
            List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");

            for (RegistroMigracion reg : registros) {
                try {
                    JsonNode json = objectMapper.readTree(reg.getPayloadJson());

                    // Verificar si este registro pertenece a este abogado
                    String abogResp = json.has("abogadoResponsable") ? json.get("abogadoResponsable").asText("") : "";
                    String abogComp = json.has("abogadoCompareciente") ? json.get("abogadoCompareciente").asText("") : "";

                    boolean esDeEsteAbogado = abogResp.trim().equalsIgnoreCase(nombreAbogado) 
                                        || abogComp.trim().equalsIgnoreCase(nombreAbogado);

                    if (esDeEsteAbogado && json.has("materia") && !json.get("materia").isNull()) {
                        String materiaTexto = json.get("materia").asText().trim();
                        if (!materiaTexto.isEmpty()) {
                            Materia m = resolveMateria(materiaTexto, lookup, gerenciaContexto);
                            if (m != null) materias.add(m);
                        }
                    }
                } catch (Exception ignored) {}
            }
            return materias;
        }

        private Long buscarIdMapeado(Long loteId, String tipo, String valorOriginal) {
            if (valorOriginal == null || valorOriginal.isEmpty()) return null;
            String valorNormalizado = normalizeValue(valorOriginal);
            return catalogoRepository.findByLoteIdAndTipoCatalogo(loteId, tipo).stream()
                .filter(c -> normalizeValue(c.getValorOriginal()).equals(valorNormalizado))
                .map(CatalogoMigracion::getIdMapeado)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        }

        @Transactional
        public void crearEntidadesNuevas(List<CatalogoMigracion> catalogos) {
            for (CatalogoMigracion cat : catalogos) {
                if ("NUEVO".equals(cat.getEstatus()) && cat.getNuevoValor() != null) {
                    String valor = cat.getNuevoValor();
                    switch (cat.getTipoCatalogo()) {
                        case "SEDE":
                            if (!estadoRepository.findByNombreIgnoreCase(valor).isPresent()) {
                                com.juridico.sistema_juridico.Entity.catalogo.Estado e = new com.juridico.sistema_juridico.Entity.catalogo.Estado();
                                e.setNombre(valor);
                                e = estadoRepository.save(e);
                                cat.setIdMapeado(e.getId().longValue());
                                cat.setEstatus("MAPEADO");
                                catalogoRepository.save(cat);
                            }
                            break;
                        case "GERENCIA":
                            if (!gerenciaRepository.findByNombreIgnoreCase(valor).isPresent()) {
                                Gerencia g = new Gerencia();
                                g.setNombre(valor);
                                g = gerenciaRepository.save(g);
                                cat.setIdMapeado(g.getId().longValue());
                                cat.setEstatus("MAPEADO");
                                catalogoRepository.save(cat);
                            }
                            break;
                        // Materia y otros requieren más contexto (como gerencia_id),
                        // por ahora nos enfocamos en SEDE y GERENCIA que son raíz.
                    }
                }
            }
        }

        private LocalDate parseDate(JsonNode dateNode) {
            if (dateNode == null || dateNode.isNull() || dateNode.asText().trim().isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(dateNode.asText());
            } catch (DateTimeParseException e) {
                return LocalDate.now();
            }
        }

        /**
         * Encuentra el valor más repetido (moda) para un campo en el JSON de una lista de registros.
         */
        private String findMostRepeated(List<RegistroMigracion> registros, String fieldName, String defaultValue) {
            java.util.Map<String, Long> counts = new java.util.HashMap<>();
            for (RegistroMigracion r : registros) {
                try {
                    JsonNode json = objectMapper.readTree(r.getPayloadJson());
                    if (json.has(fieldName) && !json.get(fieldName).isNull()) {
                        String val = json.get(fieldName).asText().trim();
                        if (!val.isEmpty()) {
                            counts.put(val, counts.getOrDefault(val, 0L) + 1);
                        }
                    }
                } catch (Exception ignored) {}
            }
            return counts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(defaultValue);
        }

        private LocalTime parseTime(JsonNode timeNode) {
            if (timeNode == null || timeNode.isNull() || timeNode.asText().trim().isEmpty()) return LocalTime.of(0, 0);
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                return LocalTime.parse(timeNode.asText(), formatter);
            } catch (DateTimeParseException e) {
                return LocalTime.of(0, 0);
            }
        }
    }