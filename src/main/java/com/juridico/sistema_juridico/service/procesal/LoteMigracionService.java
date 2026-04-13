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
                String jsonStr = objectMapper.writeValueAsString(requestObjeto);
                
                // PREVENCIÓN DE DUPLICADOS EXACTOS EN EL MISMO LOTE
                boolean duplicado = registroRepository.findByLoteIdAndEstatusNot(loteActivo.getId(), "RECHAZADO").stream()
                    .anyMatch(r -> r.getPayloadJson().equals(jsonStr) && 
                              ( (r.getNumeroExpedienteExtraido() == null && numeroExpediente == null) || 
                                (r.getNumeroExpedienteExtraido() != null && r.getNumeroExpedienteExtraido().equals(numeroExpediente)) ));
                
                if (duplicado) return; // Si es un duplicado exacto del mismo expediente en el mismo lote, ignoramos.

                RegistroMigracion registro = new RegistroMigracion();
                registro.setLote(loteActivo);
                registro.setTipoDato(tipoDato); 
                registro.setPayloadJson(jsonStr); 
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
            LoteMigracion loteActual = loteRepository.findById(loteId).orElse(null);
            if (loteActual != null) {
                loteActual.setEstatus("RECHAZADO");
                loteRepository.save(loteActual);
            }
            
            // LIMPIEZA ADICIONAL: Rechazamos TODOS los lotes pendientes 
            // para garantizar que la pantalla vuelva al inicio ("Esperando Datos Nuevos")
            // y no brinque automáticamente a otro lote que n8n haya enviado en background.
            List<LoteMigracion> pendientes = loteRepository.findByEstatus("PENDIENTE");
            for (LoteMigracion p : pendientes) {
                p.setEstatus("RECHAZADO");
            }
            loteRepository.saveAll(pendientes);
        }

        // =========================================================
        // ETAPA 1: LÓGICA DE CATÁLOGOS
        // =========================================================
        
        @Transactional
        public void extraerCatalogosDeLote(Long loteId) {
            // 1. Traemos todos los registros crudos del lote (que no estén en la papelera)
            List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
            LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();

            // COMENTADO: No borramos los pendientes para permitir extracción incremental y evitar retrabajos pesados
            /*
            catalogoRepository.findByLoteId(loteId).stream()
                .filter(c -> "PENDIENTE".equals(c.getEstatus()))
                .forEach(c -> catalogoRepository.delete(c));
            */

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
                        String matStr = json.get("materia").asText().trim();
                        if (!matStr.toUpperCase().contains("DEFINIR") && !matStr.isEmpty()) {
                            guardarCatalogoUnico(lote, "MATERIA", matStr, cacheMapeo);
                            
                            // INTELIGENCIA: Si la materia coincide con el sistema, inferimos la Gerencia automáticamente
                            materiaRepository.findByNombreIgnoreCase(matStr).ifPresent(m -> {
                                if (m.getGerencia() != null) {
                                    guardarCatalogoUnico(lote, "GERENCIA", m.getGerencia().getNombre(), cacheMapeo);
                                }
                            });
                        } else {
                            // No creamos catálogo para "Por asignar" para forzar asignación en Etapa 2
                        }
                    } else {
                        // No creamos catálogo para "Por asignar"
                    }
                    
                    if (json.has("sede") && !json.get("sede").isNull()) {
                        guardarCatalogoUnico(lote, "SEDE", json.get("sede").asText(), cacheMapeo);
                    }
                    
                    // NUEVO: Ahora sí extraemos el Órgano Jurisdiccional para la Etapa 1
                    if (json.has("organoJurisdiccional") && !json.get("organoJurisdiccional").isNull()) {
                        String orgStr = json.get("organoJurisdiccional").asText().trim();
                        guardarCatalogoUnico(lote, "ORGANO", orgStr, cacheMapeo);
                        
                        // INTELIGENCIA: Inferir Gerencia desde el Órgano si coincide exacto
                        organoRepository.findByNombreIgnoreCase(orgStr).ifPresent(o -> {
                            if (o.getGerencia() != null) {
                                guardarCatalogoUnico(lote, "GERENCIA", o.getGerencia().getNombre(), cacheMapeo);
                            }
                        });
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

        public boolean requiereGerenciaGlobal(Long loteId) {
            List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
            boolean todosSinMateria = true;
            for (RegistroMigracion reg : registros) {
                try {
                    com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                    boolean hasMateria = json.has("materia") && !json.get("materia").isNull() && !json.get("materia").asText().trim().isEmpty() && !json.get("materia").asText().toUpperCase().contains("DEFINIR");
                    if (hasMateria) {
                        todosSinMateria = false;
                        break;
                    }
                } catch (Exception e) {}
            }
            return !registros.isEmpty() && todosSinMateria;
        }

        @Transactional
        public void intentarAutoMapeo(Long loteId) {
            List<CatalogoMigracion> pendientes = catalogoRepository.findByLoteId(loteId).stream()
                .filter(c -> c.getIdMapeado() == null && "PENDIENTE".equals(c.getEstatus()))
                .toList();

            if (pendientes.isEmpty()) return;

            // --- PASO 1: MAPEAR GERENCIAS PRIMERO (Para establecer contexto) ---
            for (CatalogoMigracion c : pendientes) {
                if (!"GERENCIA".equals(c.getTipoCatalogo())) continue;
                
                String valor = c.getValorOriginal().trim();
                String searchFinal = (valor.length() > 100) ? valor.substring(0, 100) : valor;
                
                gerenciaRepository.findByNombreIgnoreCase(searchFinal).ifPresent(g -> {
                    c.setIdMapeado(g.getId().longValue());
                    c.setEstatus("MAPEADO");
                    catalogoRepository.save(c);
                });
            }

            // --- ACTUALIZAR CONTEXTO DE GERENCIA ---
            Gerencia gerenciaLote = null;
            CatalogoMigracion gerCat = catalogoRepository.findByLoteId(loteId).stream()
                .filter(c -> c.getTipoCatalogo().equals("GERENCIA") && (c.getIdMapeado() != null || "MAPEADO".equals(c.getEstatus())))
                .findFirst().orElse(null);
            
            if (gerCat != null && gerCat.getIdMapeado() != null) {
                gerenciaLote = gerenciaRepository.findById(gerCat.getIdMapeado().intValue()).orElse(null);
            } else if (gerCat != null) {
                gerenciaLote = gerenciaRepository.findByNombreIgnoreCase(gerCat.getValorOriginal().trim()).orElse(null);
            }

            final Gerencia finalGerencia = gerenciaLote;

            // --- PASO 2: MAPEAR EL RESTO CON CONTEXTO ---
            for (CatalogoMigracion c : pendientes) {
                if ("GERENCIA".equals(c.getTipoCatalogo()) || "MAPEADO".equals(c.getEstatus())) continue;
                
                String valor = c.getValorOriginal().trim();
                if (valor.isEmpty()) continue;
                final String searchFinal = (valor.length() > 100) ? valor.substring(0, 100) : valor;

                switch (c.getTipoCatalogo()) {
                    case "MATERIA":
                        if (finalGerencia != null) {
                            materiaRepository.findByNombreIgnoreCaseAndGerenciaId(searchFinal, finalGerencia.getId()).ifPresent(m -> {
                                c.setIdMapeado(m.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        } else {
                            materiaRepository.findByNombreIgnoreCase(searchFinal).ifPresent(m -> {
                                c.setIdMapeado(m.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        }
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
                        if (finalGerencia != null) {
                            organoRepository.findByNombreIgnoreCaseAndGerenciaId(searchFinal, finalGerencia.getId()).ifPresent(o -> {
                                c.setIdMapeado(o.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        } else {
                            organoRepository.findByNombreIgnoreCase(searchFinal).ifPresent(o -> {
                                c.setIdMapeado(o.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        }
                        break;
                    case "TIPO_AUDIENCIA":
                        if (finalGerencia != null) {
                            tipoAudienciaRepository.findByNombreIgnoreCaseAndGerenciaId(searchFinal, finalGerencia.getId()).ifPresent(t -> {
                                c.setIdMapeado(t.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        } else {
                            tipoAudienciaRepository.findByNombreIgnoreCase(searchFinal).ifPresent(t -> {
                                c.setIdMapeado(t.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        }
                        break;
                    case "USUARIO":
                        usuarioRepository.findAll().stream()
                            .filter(u -> u.getNombreCompleto().equalsIgnoreCase(valor) || u.getNombreCompleto().equalsIgnoreCase(searchFinal))
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

    // --- CACHING DE ALTO RENDIMIENTO ---
    private java.util.concurrent.ConcurrentHashMap<Long, java.util.List<java.util.Map<String, Object>>> cacheResumenExpedientes = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.concurrent.ConcurrentHashMap<Long, java.util.List<java.util.Map<String, Object>>> cacheResumenAudiencias = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.concurrent.ConcurrentHashMap<Long, java.util.List<java.util.Map<String, Object>>> cacheResumenTerminos = new java.util.concurrent.ConcurrentHashMap<>();

    public void limpiarCacheLote(Long loteId) {
        cacheResumenExpedientes.remove(loteId);
        cacheResumenAudiencias.remove(loteId);
        cacheResumenTerminos.remove(loteId);
    }

    public java.util.List<java.util.Map<String, Object>> obtenerResumenExpedientes(Long loteId) {
            if (cacheResumenExpedientes.containsKey(loteId)) return cacheResumenExpedientes.get(loteId);
            
            List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
            
            java.util.Map<String, List<RegistroMigracion>> agrupados = registros.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    r -> r.getNumeroExpedienteExtraido() != null ? r.getNumeroExpedienteExtraido() : "SIN_NUMERO"
                ));

            // --- INFERIR GERENCIA GLOBAL DEL LOTE ---
            String gerenciaGlobal = findMostRepeated(registros, "gerencia", "");
            if (gerenciaGlobal.toUpperCase().contains("DEFINIR")) gerenciaGlobal = "";
            
            if (gerenciaGlobal.isEmpty()) {
                String materiaGlobal = findMostRepeated(registros, "materia", "");
                if (!materiaGlobal.isEmpty() && !materiaGlobal.toUpperCase().contains("DEFINIR") && !materiaGlobal.equals("Por asignar")) {
                    Long matMapId = buscarIdMapeado(loteId, "MATERIA", materiaGlobal);
                    if (matMapId != null) {
                        java.util.Optional<Materia> matOpt = materiaRepository.findById(matMapId.intValue());
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaGlobal = matOpt.get().getGerencia().getNombre();
                        }
                    } else {
                        java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaGlobal.trim());
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaGlobal = matOpt.get().getGerencia().getNombre();
                        }
                    }
                }
            }

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

                // --- LÓGICA DE SEMÁFORO (VALIDACIÓN) --- se mueve al final para usar el valor ya resuelto
                // (placeholder, se llena más abajo con el valor real)
                List<String> camposFaltantes = new java.util.ArrayList<>();
                boolean critico = false;

                try {
                    JsonNode json = objectMapper.readTree(entry.getValue().get(0).getPayloadJson());
                    if (numExp.equals("SIN_NUMERO")) { camposFaltantes.add("Número de Expediente"); critico = true; }
                } catch (Exception e) { critico = true; }

                map.put("faltantes", camposFaltantes); // se actualiza más adelante
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

                // INTELIGENCIA MEJORADA: Inferir gerencia buscando la materia o el órgano DIRECTAMENTE DE LOS CATÁLOGOS MAPEADOS
                if ((gerenciaModa == null || gerenciaModa.isEmpty()) && materiaModa != null && !materiaModa.isEmpty() && !materiaModa.equals("Por asignar")) {
                    Long matMapId = buscarIdMapeado(loteId, "MATERIA", materiaModa);
                    if (matMapId != null) {
                        java.util.Optional<Materia> matOpt = materiaRepository.findById(matMapId.intValue());
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaModa = matOpt.get().getGerencia().getNombre();
                        }
                    } else {
                        // Respaldo
                        java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaModa.trim());
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaModa = matOpt.get().getGerencia().getNombre();
                        }
                    }
                }
                
                if ((gerenciaModa == null || gerenciaModa.isEmpty()) && organoModa != null && !organoModa.isEmpty()) {
                    Long orgMapId = buscarIdMapeado(loteId, "ORGANO", organoModa);
                    if (orgMapId != null) {
                        java.util.Optional<OrganoJurisdiccional> orgOpt = organoRepository.findById(orgMapId.intValue());
                        if (orgOpt.isPresent() && orgOpt.get().getGerencia() != null) {
                            gerenciaModa = orgOpt.get().getGerencia().getNombre();
                        }
                    } else {
                        java.util.Optional<OrganoJurisdiccional> orgOpt = organoRepository.findByNombreIgnoreCase(organoModa.trim());
                        if (orgOpt.isPresent() && orgOpt.get().getGerencia() != null) {
                            gerenciaModa = orgOpt.get().getGerencia().getNombre();
                        }
                    }
                }
                
                // ANCLAR A GERENCIA GLOBAL
                if (gerenciaModa == null || gerenciaModa.isEmpty()) {
                    gerenciaModa = gerenciaGlobal;
                }
                
                // Reemplazo para mantener la cascada funcional sin forzar en gerencia
                if (materiaModa == null || materiaModa.isEmpty()) materiaModa = "Por asignar";

                map.put("partes", partesModa);
                map.put("sede", sedeModa);
                map.put("gerencia", gerenciaModa);
                map.put("materia", materiaModa);
                map.put("organo", organoModa);
                map.put("abogado", abogadoModa);
                map.put("descripcion", descModa);

                // --- VALIDACIÓN con valores ya resueltos (post-catálogo) ---
                if (materiaModa == null || materiaModa.isEmpty() || materiaModa.equals("Por asignar")) {
                    if (!camposFaltantes.contains("Materia")) { camposFaltantes.add("Materia"); critico = true; }
                }
                if (gerenciaModa == null || gerenciaModa.isEmpty()) {
                    if (!camposFaltantes.contains("Gerencia")) { camposFaltantes.add("Gerencia"); critico = true; }
                }
                if (descModa == null || descModa.isEmpty() || descModa.equals("Expediente migrado.")) {
                    if (!camposFaltantes.contains("Descripción")) { camposFaltantes.add("Descripción"); }
                }
                map.put("faltantes", camposFaltantes);
                map.put("esCritico", critico);
                map.put("estaListo", camposFaltantes.isEmpty());

    // --- RESOLUCIÓN DE IDs PARA EL MODAL ---
                // Buscamos si el valor de la moda ya fue mapeado en la Etapa 1
                Long materiaId = buscarIdMapeado(loteId, "MATERIA", materiaModa);
                Long gerenciaId = buscarIdMapeado(loteId, "GERENCIA", gerenciaModa);
                Long organoId = buscarIdMapeado(loteId, "ORGANO", organoModa);
                
    // Limpiamos la basura del JSON antes de mandarla al frontend
                if (gerenciaModa.equalsIgnoreCase("POR DEFINIR")) gerenciaModa = "";

                // INTELIGENCIA: Si la gerenciaId es nula pero sí tenemos una materiaId u organoId mapeada...
                if (gerenciaId == null && materiaId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Materia> matOpt = materiaRepository.findById(materiaId.intValue());
                    if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                        gerenciaId = matOpt.get().getGerencia().getId().longValue();
                        // FIX: Actualizamos también el texto para que el Select de Javascript lo encuentre
                        gerenciaModa = matOpt.get().getGerencia().getNombre(); 
                    }
                }
                if (gerenciaId == null && organoId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional> orgOpt = organoRepository.findById(organoId.intValue());
                    if (orgOpt.isPresent() && orgOpt.get().getGerencia() != null) {
                        gerenciaId = orgOpt.get().getGerencia().getId().longValue();
                        gerenciaModa = orgOpt.get().getGerencia().getNombre(); 
                    }
                }
                
                // BACKUP FINAL: Si gerenciaId sigue siendo nula tras toda la inferencia, tomar el ID de la Global (si existe)
                LoteMigracion loteActual = loteRepository.findById(loteId).orElse(null);
                if (gerenciaId == null && loteActual != null && loteActual.getGerenciaGlobal() != null) {
                    gerenciaId = loteActual.getGerenciaGlobal().getId().longValue();
                }
                
                map.put("gerencia", gerenciaModa); // Aseguramos que se mande el texto limpio/inferido
                map.put("gerenciaId", gerenciaId);
                map.put("materiaId", materiaId);
                map.put("organoId", organoId);
                map.put("sedeId", buscarIdMapeado(loteId, "SEDE", sedeModa));
                map.put("abogadoId", buscarIdMapeado(loteId, "USUARIO", abogadoModa));
                
                resumen.add(map);
            }
            return resumen;
        }
// =========================================================
    // ETAPA 3: LÓGICA DE AUDIENCIAS
    // =========================================================

    public java.util.List<java.util.Map<String, Object>> obtenerResumenAudiencias(Long loteId) {
        if (cacheResumenAudiencias.containsKey(loteId)) return cacheResumenAudiencias.get(loteId);
        
        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndTipoDatoAndEstatusNot(loteId, "AUDIENCIA", "RECHAZADO");
        List<RegistroMigracion> todosLosRegistros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
        
        // --- INFERIR GERENCIA GLOBAL DEL LOTE ---
        String gerenciaGlobal = findMostRepeated(todosLosRegistros, "gerencia", "");
        if (gerenciaGlobal.toUpperCase().contains("DEFINIR")) gerenciaGlobal = "";
        
        if (gerenciaGlobal.isEmpty()) {
            String materiaGlobal = findMostRepeated(todosLosRegistros, "materia", "");
            if (!materiaGlobal.isEmpty() && !materiaGlobal.toUpperCase().contains("DEFINIR") && !materiaGlobal.equals("Por asignar")) {
                Long matMapId = buscarIdMapeado(loteId, "MATERIA", materiaGlobal);
                if (matMapId != null) {
                    java.util.Optional<Materia> matOpt = materiaRepository.findById(matMapId.intValue());
                    if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                        gerenciaGlobal = matOpt.get().getGerencia().getNombre();
                    }
                } else {
                    java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaGlobal.trim());
                    if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                        gerenciaGlobal = matOpt.get().getGerencia().getNombre();
                    }
                }
            }
        }
        
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
                map.put("observaciones", json.has("observaciones") ? json.get("observaciones").asText() : "");
                
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
                
                // INTELIGENCIA: Si la gerencia sigue vacía, inferirla basándonos en la materia real MAPEADA
                if (gerenciaAsignada.isEmpty() && !materiaAsignada.isEmpty() && !materiaAsignada.equals("Por asignar")) {
                    Long matMapId = buscarIdMapeado(loteId, "MATERIA", materiaAsignada);
                    if (matMapId != null) {
                        java.util.Optional<Materia> matOpt = materiaRepository.findById(matMapId.intValue());
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaAsignada = matOpt.get().getGerencia().getNombre();
                        }
                    } else {
                        java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaAsignada);
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaAsignada = matOpt.get().getGerencia().getNombre();
                        }
                    }
                }
                
                // ANCLAR A GERENCIA GLOBAL
                if (gerenciaAsignada == null || gerenciaAsignada.isEmpty()) {
                    gerenciaAsignada = gerenciaGlobal;
                }
                
                // Reemplazo para mantener la cascada funcional
                if (materiaAsignada == null || materiaAsignada.isEmpty()) materiaAsignada = "Por asignar";
                
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
        
        cacheResumenAudiencias.put(loteId, resumen);
        return resumen;
    }
        // =========================================================
        // ETAPA 4: LÓGICA DE TÉRMINOS E INYECCIÓN FINAL
        // =========================================================

        public java.util.List<java.util.Map<String, Object>> obtenerResumenTerminos(Long loteId) {
            if (cacheResumenTerminos.containsKey(loteId)) return cacheResumenTerminos.get(loteId);

            List<RegistroMigracion> registros = registroRepository.findByLoteIdAndTipoDatoAndEstatusNot(loteId, "TERMINO", "RECHAZADO");
            List<RegistroMigracion> todosLosRegistros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
            
            // --- INFERIR GERENCIA GLOBAL DEL LOTE ---
            String gerenciaGlobal = findMostRepeated(todosLosRegistros, "gerencia", "");
            if (gerenciaGlobal.toUpperCase().contains("DEFINIR")) gerenciaGlobal = "";
            
            if (gerenciaGlobal.isEmpty()) {
                String materiaGlobal = findMostRepeated(todosLosRegistros, "materia", "");
                if (!materiaGlobal.isEmpty() && !materiaGlobal.toUpperCase().contains("DEFINIR") && !materiaGlobal.equals("Por asignar")) {
                    Long matMapId = buscarIdMapeado(loteId, "MATERIA", materiaGlobal);
                    if (matMapId != null) {
                        java.util.Optional<Materia> matOpt = materiaRepository.findById(matMapId.intValue());
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaGlobal = matOpt.get().getGerencia().getNombre();
                        }
                    } else {
                        java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaGlobal.trim());
                        if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                            gerenciaGlobal = matOpt.get().getGerencia().getNombre();
                        }
                    }
                }
            }
            
            java.util.List<java.util.Map<String, Object>> resumen = new java.util.ArrayList<>();
            
            for (RegistroMigracion reg : registros) {
                try {
                    JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    
                    String numExp = reg.getNumeroExpedienteExtraido() != null ? reg.getNumeroExpedienteExtraido() : "";
                    
                    map.put("id", reg.getId());
                    map.put("numeroExpediente", numExp);
                    map.put("actuacion", json.has("asunto") ? json.get("asunto").asText() : (json.has("actuacion") ? json.get("actuacion").asText() : (json.has("prestacion") ? json.get("prestacion").asText() : "Sin actuación")));
                    map.put("fechaVencimiento", json.has("fechaVencimiento") ? json.get("fechaVencimiento").asText() : "Sin fecha");
                    map.put("abogado", json.has("abogadoResponsable") ? json.get("abogadoResponsable").asText() : "N/A");
                    map.put("prioridad", json.has("prioridad") ? json.get("prioridad").asText() : "Media");
                    map.put("observaciones", json.has("observaciones") ? json.get("observaciones").asText() : "");
                    map.put("fechaIngreso", json.has("fechaIngreso") ? json.get("fechaIngreso").asText() : "");
                    
                    String sedeVal = json.has("sede") ? json.get("sede").asText() : "";
                    String organoVal = json.has("organoJurisdiccional") ? json.get("organoJurisdiccional").asText() : "";
                    map.put("sede", sedeVal);
                    map.put("organo", organoVal);
                    
                    // --- HERENCIA: Inferir gerencia y materia del expediente padre ---
                    String gerenciaAsignada = json.has("gerencia") && !json.get("gerencia").isNull() ? json.get("gerencia").asText().trim() : "";
                    String materiaAsignada = json.has("materia") && !json.get("materia").isNull() ? json.get("materia").asText().trim() : "";
                    String abogadoVal = json.has("abogadoResponsable") ? json.get("abogadoResponsable").asText() : "";
                    
                    if (gerenciaAsignada.toUpperCase().contains("DEFINIR")) gerenciaAsignada = "";
                    if (materiaAsignada.toUpperCase().contains("DEFINIR")) materiaAsignada = "";
                    
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
                    
                    if (gerenciaAsignada.isEmpty() && !materiaAsignada.isEmpty() && !materiaAsignada.equals("Por asignar")) {
                        Long matMapId = buscarIdMapeado(loteId, "MATERIA", materiaAsignada);
                        if (matMapId != null) {
                            java.util.Optional<Materia> matOpt = materiaRepository.findById(matMapId.intValue());
                            if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                                gerenciaAsignada = matOpt.get().getGerencia().getNombre();
                            }
                        } else {
                            java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaAsignada);
                            if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                                gerenciaAsignada = matOpt.get().getGerencia().getNombre();
                            }
                        }
                    }
                    
                    // ANCLAR A GERENCIA GLOBAL
                    if (gerenciaAsignada == null || gerenciaAsignada.isEmpty()) {
                        gerenciaAsignada = gerenciaGlobal;
                    }
                    
                    // Reemplazo para mantener la cascada funcional
                    if (materiaAsignada == null || materiaAsignada.isEmpty()) materiaAsignada = "Por asignar";
                    
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
                    map.put("sedeId", buscarIdMapeado(loteId, "SEDE", sedeVal));
                    map.put("abogadoId", buscarIdMapeado(loteId, "USUARIO", abogadoVal));
                    
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
                                String asuntoVal = json.has("prestacion") ? json.get("prestacion").asText() : (json.has("asunto") ? json.get("asunto").asText() : "Sin actuación");
                                if (asuntoVal.length() > 500) asuntoVal = asuntoVal.substring(0, 500);
                                LocalDate fechaVenc = parseDate(json.get("fechaVencimiento"));
                                LocalDate fechaIng = parseDate(json.get("fechaIngreso"));

                                // PREVENCIÓN DE DUPLICADOS PARA TÉRMINO
                                List<Termino> tExistentes = terminoRepository.findByExpedienteAndAsuntoAndFechaVencimiento(expediente, asuntoVal, fechaVenc);
                                if (!tExistentes.isEmpty()) {
                                    System.out.println("Omitiendo término duplicado para expediente " + expediente.getNumero() + ": " + asuntoVal);
                                    continue; 
                                }

                                Termino termino = new Termino();
                                termino.setExpediente(expediente);
                                termino.setAsunto(asuntoVal);
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
                                LocalDate fechaAud = parseDate(json.get("fechaAudiencia"));
                                LocalTime horaAud = parseTime(json.get("horaAudiencia"));

                                // --- PREVENCIÓN DE DUPLICADOS PARA AUDIENCIA ---
                                List<Audiencia> aExistentes = audienciaRepository.findByExpedienteAndFechaAudienciaAndHoraAudiencia(expediente, fechaAud, horaAud);
                                if (!aExistentes.isEmpty()) {
                                    System.out.println("Omitiendo audiencia duplicada para expediente " + expediente.getNumero() + " el " + fechaAud);
                                    continue;
                                }

                                Audiencia audiencia = new Audiencia();
                                audiencia.setExpediente(expediente);
                                audiencia.setFechaAudiencia(fechaAud);
                                audiencia.setHoraAudiencia(horaAud);

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
                        registroRepository.save(r);
                    }
                }
                
                lote.setEstatus("COMPLETADO");
                lote.setPasoActual(5);
                loteRepository.save(lote);

                // LIMPIEZA: Si hay otros lotes PENDIENTES (por ruido de n8n), los cancelamos
                List<LoteMigracion> otrosLotes = loteRepository.findByEstatus("PENDIENTE");
                for (LoteMigracion otro : otrosLotes) {
                    if (!otro.getId().equals(loteId)) {
                        otro.setEstatus("RECHAZADO");
                    }
                }
                loteRepository.saveAll(otrosLotes);
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
            // Buscamos o creamos una materia específica llamada "Por Asignar" para no
            // contaminar expedientes con una materia real (ej: "Civil") cuando la original
            // era desconocida o no fue mapeada.
            String NOMBRE_FALLBACK = "Por Asignar";
            Gerencia gerenciaFallback = getFallbackGerencia();
            Materia existente = materiaRepository
                .findByNombreIgnoreCaseAndGerenciaId(NOMBRE_FALLBACK, gerenciaFallback.getId())
                .orElse(null);
            if (existente == null) {
                existente = materiaRepository.findByNombreIgnoreCase(NOMBRE_FALLBACK).orElse(null);
            }
            if (existente == null) {
                Materia m = new Materia();  
                m.setNombre(NOMBRE_FALLBACK);
                m.setActivo(true);
                m.setGerencia(gerenciaFallback);
                existente = materiaRepository.save(m);
            }
            return existente;
        }

        private Materia resolveMateria(String original, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerenciaContexto) {
            if (original == null || original.trim().isEmpty()) return null;

            // Si el valor es un marcador de "sin asignar", no buscamos una materia real.
            // Retornamos null para que el código superior use getFallbackMateria(),
            // que crea/reutiliza la materia especial "Por Asignar".
            String trimmed = original.trim();
            if (trimmed.equalsIgnoreCase("Por asignar")
                    || trimmed.equalsIgnoreCase("Por definir")
                    || trimmed.toUpperCase().contains("DEFINIR")
                    || trimmed.toUpperCase().contains("POR ASIGNAR")) {
                return null;
            }

            String key = "MATERIA___" + trimmed.toLowerCase();
            CatalogoMigracion c = lookup.get(key);
            if (c != null) {
                if (c.getIdMapeado() != null) return materiaRepository.findById(c.getIdMapeado().intValue()).orElse(null);
                if (c.getNuevoValor() != null) {
                    String nombreFinal = c.getNuevoValor();
                    if (nombreFinal.length() > 100) nombreFinal = nombreFinal.substring(0, 100);

                    final String searchName = nombreFinal;
                    Integer gerenciaId = (gerenciaContexto != null) ? gerenciaContexto.getId() : getFallbackGerencia().getId();
                    Materia existente = materiaRepository.findByNombreIgnoreCaseAndGerenciaId(searchName, gerenciaId).orElse(null);
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
            if (gerenciaContexto != null) {
                return materiaRepository.findByNombreIgnoreCaseAndGerenciaId(original.trim(), gerenciaContexto.getId()).orElse(null);
            }
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
                    
                    // IMPORTANTE: Primero buscar global para respetar la restricción UNIQUE del sistema
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

                    // IMPORTANTE: Buscar global para evitar DataIntegrityViolation por unique constraint
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
        public List<Integer> getMateriasInferredForAbogadoNuevos(Long loteId, String nombreAbogado) {
            Map<String, CatalogoMigracion> lookup = new java.util.HashMap<>();
            catalogoRepository.findByLoteId(loteId).forEach(c -> 
                lookup.put(c.getTipoCatalogo() + "_" + c.getValorOriginal().trim().toLowerCase(), c)
            );
            return obtenerMateriasDeAbogado(nombreAbogado, loteId, lookup, null).stream().map(m -> m.getId()).collect(Collectors.toList());
        }

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