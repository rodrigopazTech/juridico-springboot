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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
    import com.juridico.sistema_juridico.repository.configuracion.PerfilRecordatorioAudienciaRepository;
    import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;

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
        @Autowired private PerfilRecordatorioAudienciaRepository perfilRecordatorioAudienciaRepository;
        @Autowired private CatPrioridadRepository catPrioridadRepository;
        @Autowired private EstadoRepository estadoRepository;
        @Autowired private PasswordEncoder passwordEncoder;
        @Autowired private PlatformTransactionManager transactionManager;
        @Autowired @org.springframework.context.annotation.Lazy private LoteMigracionService self;



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
                java.util.Map<String, Object> itemMap;
                if (requestObjeto instanceof java.util.Map) {
                    itemMap = new java.util.HashMap<>((java.util.Map<String, Object>) requestObjeto);
                } else {
                    itemMap = objectMapper.convertValue(requestObjeto, java.util.Map.class);
                }
                
                // Aseguramos que la gerencia no esté vacía
                if (!itemMap.containsKey("gerencia") || itemMap.get("gerencia") == null || itemMap.get("gerencia").toString().trim().isEmpty()) {
                    itemMap.put("gerencia", "POR DEFINIR");
                }
                
                // Si viene el número de expediente por parámetro, lo agregamos/respetamos en el objeto
                if (numeroExpediente != null && !numeroExpediente.equals("SIN_NUMERO")) {
                    itemMap.put("numeroExpediente", numeroExpediente);
                }

                registrarLoteEnLote(tipoDato, java.util.List.of(itemMap));
            } catch (Exception e) {
                throw new RuntimeException("Error al registrar dato en el Lote: " + e.getMessage());
            }
        }

        // 2.5. Guardar una lista de datos en el lote activo de manera eficiente (Batch Insert con O(1) deduplicación)
        @Transactional
        public int registrarLoteEnLote(String tipoDato, java.util.List<java.util.Map<String, Object>> items) {
            try {
                if (items == null || items.isEmpty()) return 0;
                
                LoteMigracion loteActivo = obtenerOCrearLoteActivo();
                
                // 1. Obtener registros existentes del lote en una sola query
                java.util.List<RegistroMigracion> existentes = registroRepository.findByLoteIdAndEstatusNot(loteActivo.getId(), "RECHAZADO");
                
                // 2. Construir un Set de firmas para verificación en tiempo O(1)
                java.util.Set<String> firmasExistentes = existentes.stream()
                    .map(r -> {
                        String num = r.getNumeroExpedienteExtraido() != null ? r.getNumeroExpedienteExtraido().trim() : "";
                        return num + "|||" + r.getPayloadJson();
                    })
                    .collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new));
                
                java.util.List<RegistroMigracion> nuevosRegistros = new java.util.ArrayList<>();
                
                for (java.util.Map<String, Object> item : items) {
                    // Asegurar consistencia de Gerencia para la Etapa 1
                    if (!item.containsKey("gerencia") || item.get("gerencia") == null || item.get("gerencia").toString().trim().isEmpty()) {
                        item.put("gerencia", "POR DEFINIR");
                    }
                    
                    String numeroExpediente = item.containsKey("numeroExpediente") && item.get("numeroExpediente") != null
                            ? item.get("numeroExpediente").toString().trim()
                            : "SIN_NUMERO";
                    
                    String jsonStr = objectMapper.writeValueAsString(item);
                    String firmaNuevo = numeroExpediente + "|||" + jsonStr;
                    
                    // Si ya existe en BD o en el lote actual procesado, lo ignoramos para evitar duplicados exactos
                    if (firmasExistentes.contains(firmaNuevo)) {
                        continue;
                    }
                    
                    RegistroMigracion registro = new RegistroMigracion();
                    registro.setLote(loteActivo);
                    registro.setTipoDato(tipoDato); 
                    registro.setPayloadJson(jsonStr); 
                    registro.setNumeroExpedienteExtraido(numeroExpediente);
                    registro.setEstatus("PENDIENTE");
                    
                    nuevosRegistros.add(registro);
                    firmasExistentes.add(firmaNuevo); // Añadir a la caché local del lote actual
                }
                
                if (!nuevosRegistros.isEmpty()) {
                    registroRepository.saveAll(nuevosRegistros);
                }
                
                return nuevosRegistros.size();
            } catch (Exception e) {
                throw new RuntimeException("Error al procesar lote en el Lote: " + e.getMessage());
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
            // 1. Limpieza de catálogos "sombra" (duplicados PENDIENTES generados porque el valor fue actualizado en los payloads)
            List<CatalogoMigracion> todosCatalogos = catalogoRepository.findByLoteId(loteId);
            for (CatalogoMigracion cat : todosCatalogos) {
                if ("PENDIENTE".equals(cat.getEstatus())) {
                    String valNormal = normalizeValue(cat.getValorOriginal());
                    boolean esSombra = todosCatalogos.stream().anyMatch(otro -> 
                        !otro.getId().equals(cat.getId()) &&
                        (normalizeValue(otro.getNuevoValor()).equals(valNormal) || 
                         (otro.getIdMapeado() != null && normalizeValue(obtenerNombreEntidad(otro.getTipoCatalogo(), otro.getIdMapeado())).equals(valNormal)))
                    );
                    if (esSombra) {
                        catalogoRepository.delete(cat);
                    }
                }
            }

            // 2. Traemos todos los registros crudos del lote (que no estén en la papelera)
            List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
            LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();

            // 3. Cache en memoria para evitar N+1
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
                            java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Materia> opt = materiaRepository.findByNombreIgnoreCaseAndGerenciaId(searchFinal, finalGerencia.getId());
                            if (opt.isPresent()) {
                                c.setIdMapeado(opt.get().getId().longValue());
                                c.setEstatus("MAPEADO");
                            } else {
                                materiaRepository.findByNombreIgnoreCase(searchFinal).ifPresent(m -> {
                                    c.setIdMapeado(m.getId().longValue());
                                    c.setEstatus("MAPEADO");
                                });
                            }
                        } else {
                            materiaRepository.findByNombreIgnoreCase(searchFinal).ifPresent(m -> {
                                c.setIdMapeado(m.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        }
                        break;
                    case "SEDE":
                        estadoRepository.findByActivoTrue().stream()
                            .filter(e -> e.getNombre().equalsIgnoreCase(valor))
                            .findFirst().ifPresent(e -> {
                                c.setIdMapeado(e.getId().longValue());
                                c.setEstatus("MAPEADO");
                            });
                        break;
                    case "ORGANO":
                        if (finalGerencia != null) {
                            java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional> opt = organoRepository.findByNombreIgnoreCaseAndGerenciaId(searchFinal, finalGerencia.getId());
                            if (opt.isPresent() && Boolean.TRUE.equals(opt.get().getActivo())) {
                                c.setIdMapeado(opt.get().getId().longValue());
                                c.setEstatus("MAPEADO");
                            } else {
                                organoRepository.findByNombreIgnoreCase(searchFinal)
                                    .filter(o -> Boolean.TRUE.equals(o.getActivo()))
                                    .ifPresent(o -> {
                                        c.setIdMapeado(o.getId().longValue());
                                        c.setEstatus("MAPEADO");
                                    });
                            }
                        } else {
                            organoRepository.findByNombreIgnoreCase(searchFinal)
                                .filter(o -> Boolean.TRUE.equals(o.getActivo()))
                                .ifPresent(o -> {
                                    c.setIdMapeado(o.getId().longValue());
                                    c.setEstatus("MAPEADO");
                                });
                        }
                        break;
                    case "TIPO_AUDIENCIA":
                        if (finalGerencia != null) {
                            java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia> opt = tipoAudienciaRepository.findByNombreIgnoreCaseAndGerenciaId(searchFinal, finalGerencia.getId());
                            if (opt.isPresent() && Boolean.TRUE.equals(opt.get().getActivo())) {
                                c.setIdMapeado(opt.get().getId().longValue());
                                c.setEstatus("MAPEADO");
                            } else {
                                tipoAudienciaRepository.findByNombreIgnoreCase(searchFinal)
                                    .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                                    .ifPresent(t -> {
                                        c.setIdMapeado(t.getId().longValue());
                                        c.setEstatus("MAPEADO");
                                    });
                            }
                        } else {
                            tipoAudienciaRepository.findByNombreIgnoreCase(searchFinal)
                                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                                .ifPresent(t -> {
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
                .anyMatch(c -> {
                    String normOriginal = normalizeValue(c.getValorOriginal());
                    String normNuevo = normalizeValue(c.getNuevoValor());
                    String normMapeado = c.getIdMapeado() != null ? normalizeValue(obtenerNombreEntidad(tipo, c.getIdMapeado())) : "";
                    return normOriginal.equals(valorNormalizado) || normNuevo.equals(valorNormalizado) || normMapeado.equals(valorNormalizado);
                });
            
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
            java.util.List<Runnable> payloadUpdates = new java.util.ArrayList<>();

            // Leemos todo el formulario HTML que manda el usuario
            formData.forEach((key, value) -> {
                if (key.startsWith("cat_") && value != null && !value.isEmpty()) {
                    Long catalogoMigracionId = Long.parseLong(key.replace("cat_", ""));
                    CatalogoMigracion cat = catalogoRepository.findById(catalogoMigracionId).orElse(null);
                    
                    if (cat != null) {
                        String oldValor = cat.getValorOriginal();
                        String nuevoValorParaPayload = null;

                        if (value.equals("SKIP")) {
                            // El usuario descartó este valor
                            cat.setEstatus("DESCARTADO");
                        } else if (value.startsWith("NEW_")) {
                            // El usuario decidió crear uno nuevo
                            cat.setNuevoValor(value.replace("NEW_", ""));
                            cat.setEstatus("NUEVO");
                            nuevoValorParaPayload = cat.getNuevoValor();
                        } else {
                            // El usuario lo emparejó con uno existente
                            cat.setIdMapeado(Long.parseLong(value));
                            cat.setEstatus("MAPEADO");
                            nuevoValorParaPayload = obtenerNombreEntidad(cat.getTipoCatalogo(), cat.getIdMapeado());
                        }
                        catalogoRepository.save(cat);

                        if (nuevoValorParaPayload != null) {
                            String fTipo = cat.getTipoCatalogo();
                            String fNuevo = nuevoValorParaPayload;
                            payloadUpdates.add(() -> actualizarValorEnPayloads(loteId, fTipo, oldValor, fNuevo));
                        }
                    }
                }
            });

            // Aplicar reemplazos en el payload original
            for (Runnable update : payloadUpdates) {
                update.run();
            }

            invalidateCaches(loteId);
            
            // Al terminar de guardar, avanzamos automáticamente a la Etapa 2
            avanzarPaso(loteId);
        }

        private String obtenerNombreEntidad(String tipoCatalogo, Long id) {
            if (id == null) return null;
            switch(tipoCatalogo) {
                case "GERENCIA": return gerenciaRepository.findById(id.intValue()).map(com.juridico.sistema_juridico.Entity.catalogo.Gerencia::getNombre).orElse(null);
                case "MATERIA": return materiaRepository.findById(id.intValue()).map(com.juridico.sistema_juridico.Entity.catalogo.Materia::getNombre).orElse(null);
                case "SEDE": return estadoRepository.findById(id.intValue()).map(com.juridico.sistema_juridico.Entity.catalogo.Estado::getNombre).orElse(null);
                case "ORGANO": return organoRepository.findById(id.intValue()).map(com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional::getNombre).orElse(null);
                case "TIPO_AUDIENCIA": return tipoAudienciaRepository.findById(id.intValue()).map(com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia::getNombre).orElse(null);
                case "USUARIO": return usuarioRepository.findAll().stream().filter(u -> u.getId().equals(id)).findFirst().map(com.juridico.sistema_juridico.Entity.usuario.Usuario::getNombreCompleto).orElse(null);
            }
            return null;
        }
        
        public void invalidateCaches(Long loteId) {
            cacheResumenExpedientes.remove(loteId);
            cacheResumenAudiencias.remove(loteId);
            cacheResumenTerminos.remove(loteId);
        }
        
        @Transactional
        public void actualizarValorEnPayloads(Long loteId, String tipoCatalogo, String oldValor, String nuevoValor) {
            if (oldValor == null || oldValor.trim().isEmpty() || oldValor.equals(nuevoValor)) return;
            List<RegistroMigracion> regs = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
            List<String> campos = new java.util.ArrayList<>();
            switch(tipoCatalogo) {
                case "GERENCIA": campos.add("gerencia"); break;
                case "MATERIA": campos.add("materia"); break;
                case "SEDE": campos.add("sede"); campos.add("estado"); break;
                case "ORGANO": campos.add("organoJurisdiccional"); campos.add("sala"); break;
                case "TIPO_AUDIENCIA": campos.add("tipoAudiencia"); break;
                case "USUARIO": 
                    campos.add("abogadoResponsable"); 
                    campos.add("abogadoCompareciente"); 
                    campos.add("abogado"); 
                    break;
            }
            
            for (RegistroMigracion reg : regs) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode json = 
                        (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(reg.getPayloadJson());
                    boolean changed = false;
                    for (String c : campos) {
                        if (json.has(c) && json.get(c).asText().trim().equalsIgnoreCase(oldValor.trim())) {
                            json.put(c, nuevoValor);
                            changed = true;
                        }
                    }
                    if (changed) {
                        reg.setPayloadJson(json.toString());
                        registroRepository.save(reg);
                    }
                } catch (Exception e) {}
            }
            invalidateCaches(loteId);
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

        private java.util.Map<String, List<RegistroMigracion>> agruparRegistros(List<RegistroMigracion> registros) {
            java.util.Map<String, List<RegistroMigracion>> expedientesAgrupados = new java.util.LinkedHashMap<>();
            java.util.Map<String, List<RegistroMigracion>> porNumero = registros.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    r -> r.getNumeroExpedienteExtraido() != null && !r.getNumeroExpedienteExtraido().trim().isEmpty() 
                         ? r.getNumeroExpedienteExtraido().trim() : "SIN_NUMERO",
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.toList()
                ));

            for (java.util.Map.Entry<String, List<RegistroMigracion>> entry : porNumero.entrySet()) {
                String cleanNum = entry.getKey();
                List<RegistroMigracion> grupo = entry.getValue();

                boolean esSinNumero = cleanNum.equals("SIN_NUMERO") || cleanNum.equalsIgnoreCase("S/N") || cleanNum.equalsIgnoreCase("S/n") || cleanNum.toUpperCase().contains("POR ASIGNAR");

                if (!esSinNumero) {
                    expedientesAgrupados.put(cleanNum, grupo);
                } else {
                    for (RegistroMigracion reg : grupo) {
                        String firma;
                        try {
                            JsonNode jn = objectMapper.readTree(reg.getPayloadJson());
                            String p = jn.has("partes") ? jn.get("partes").asText("") : "";
                            String m = jn.has("materia") ? jn.get("materia").asText("") : "";
                            String o = jn.has("organoJurisdiccional") ? jn.get("organoJurisdiccional").asText("") : "";
                            firma = (p + "|" + m + "|" + o).toLowerCase().trim();
                        } catch (Exception e) { firma = reg.getPayloadJson(); }
                        
                        String targetKey = "S/N-" + String.format("%06x", firma.hashCode() & 0xFFFFFF).toUpperCase();
                        expedientesAgrupados.computeIfAbsent(targetKey, k -> new java.util.ArrayList<>()).add(reg);
                    }
                }
            }
            return expedientesAgrupados;
        }

        private LocalDate extractLatestDate(RegistroMigracion reg) {
            try {
                JsonNode json = objectMapper.readTree(reg.getPayloadJson());
                LocalDate d = null;
                if (json.has("fechaVencimiento") && !json.get("fechaVencimiento").isNull()) d = parseDate(json.get("fechaVencimiento"));
                if (d == null && json.has("fechaAudiencia") && !json.get("fechaAudiencia").isNull()) d = parseDate(json.get("fechaAudiencia"));
                if (d == null && json.has("fechaIngreso") && !json.get("fechaIngreso").isNull()) d = parseDate(json.get("fechaIngreso"));
                return d != null ? d : LocalDate.MIN;
            } catch (Exception e) { return LocalDate.MIN; }
        }

        public java.util.List<java.util.Map<String, Object>> obtenerResumenExpedientes(Long loteId) {
            if (cacheResumenExpedientes.containsKey(loteId)) return cacheResumenExpedientes.get(loteId);
            
            List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
            
            java.util.Map<String, List<RegistroMigracion>> agrupados = agruparRegistros(registros);

            LoteMigracion lote = loteRepository.findById(loteId).orElse(null);
            String gerenciaGlobal = (lote != null && lote.getGerenciaGlobal() != null) ? lote.getGerenciaGlobal().getNombre() : "";
            
            if (gerenciaGlobal.isEmpty()) {
                gerenciaGlobal = findMostRepeated(registros, "gerencia", "");
                if (gerenciaGlobal.toUpperCase().contains("DEFINIR")) gerenciaGlobal = "";
            }

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
                boolean esVariante = false; // Las variantes se eliminaron mediante agruparRegistros

                // La clave interna puede ser "S/N-HASH" o "SIN_NUMERO" — normalizamos para la UI
                String numExpDisplay = (numExp.startsWith("S/N-") || numExp.equals("SIN_NUMERO")) ? "S/N" : numExp;
                map.put("numeroExpediente", numExpDisplay); // Valor visible en el template
                map.put("numExpInterno", numExp);           // Clave interna para lógica Java
                map.put("esVariante", esVariante);
                
                long totalTerminos = entry.getValue().stream().filter(r -> "TERMINO".equals(r.getTipoDato())).count();
                long totalAudiencias = entry.getValue().stream().filter(r -> "AUDIENCIA".equals(r.getTipoDato())).count();
                
                map.put("totalTerminos", totalTerminos);
                map.put("totalAudiencias", totalAudiencias);
                map.put("totalRegistros", entry.getValue().size());

                List<String> camposFaltantes = new java.util.ArrayList<>();
                boolean critico = false;

                try {
                    if (numExp.equals("SIN_NUMERO")) { camposFaltantes.add("Número de Expediente"); critico = true; }
                } catch (Exception e) { critico = true; }


                map.put("registroId", entry.getValue().get(0).getId());

                // --- LÓGICA DE REGISTRO MÁS RECIENTE ---
                // Se extrae ANTES de yaExiste para que partesModa esté disponible en la búsqueda S/N
                RegistroMigracion masReciente = entry.getValue().stream()
                    .max((r1, r2) -> extractLatestDate(r1).compareTo(extractLatestDate(r2)))
                    .orElse(entry.getValue().get(0));

                JsonNode masRecJson = null;
                try { masRecJson = objectMapper.readTree(masReciente.getPayloadJson()); } catch (Exception e) {}

                String partesModa = masRecJson != null && masRecJson.hasNonNull("partes") && !masRecJson.get("partes").asText("").trim().isEmpty()
                    ? masRecJson.get("partes").asText("") : findMostRepeated(entry.getValue(), "partes", "");

                // yaExiste: para S/N verificar por número+partes; para normales por número único
                boolean yaExiste;
                if (numExp.startsWith("S/N-") || numExp.equals("SIN_NUMERO")) {
                    if (partesModa != null && !partesModa.isEmpty()) {
                        yaExiste = expedienteRepository
                            .findByNumeroIgnoreCaseAndPartesIgnoreCase("S/N", partesModa)
                            .isPresent();
                    } else {
                        yaExiste = false; // Sin partes no podemos saber → permitir crear
                    }
                } else {
                    yaExiste = expedienteRepository.existsByNumero(numExp);
                }
                map.put("yaExiste", yaExiste);

                // ── SI YA EXISTE EN BD: usar los datos del expediente real como fuente de verdad ──
                // Esto garantiza que los cambios hechos en el sistema (ej. asignar materia)
                // se reflejen correctamente en el resumen de migración.
                Expediente expExistente = null;
                if (yaExiste) {
                    if (numExp.startsWith("S/N-") || numExp.equals("SIN_NUMERO")) {
                        expExistente = expedienteRepository
                            .findByNumeroIgnoreCaseAndPartesIgnoreCase("S/N", partesModa)
                            .orElse(null);
                    } else {
                        expExistente = expedienteRepository.findByNumeroIgnoreCase(numExp).orElse(null);
                    }
                }

                String sedeModa, gerenciaModa, materiaModa, organoModa, abogadoModa, descModa;

                if (expExistente != null) {
                    // Datos reales del expediente en el sistema
                    partesModa    = expExistente.getPartes() != null ? expExistente.getPartes() : partesModa;
                    sedeModa      = expExistente.getSede() != null ? expExistente.getSede() : "";
                    gerenciaModa  = expExistente.getGerencia() != null ? expExistente.getGerencia().getNombre() : "";
                    materiaModa   = expExistente.getMateria() != null ? expExistente.getMateria().getNombre() : "Por asignar";
                    organoModa    = expExistente.getOrganoJurisdiccional() != null ? expExistente.getOrganoJurisdiccional().getNombre() : "";
                    abogadoModa   = expExistente.getAbogadoResponsable() != null ? expExistente.getAbogadoResponsable().getNombreCompleto()
                                    : (expExistente.getAbogadoResponsableNombre() != null ? expExistente.getAbogadoResponsableNombre() : "");
                    descModa      = expExistente.getDescripcion() != null && !expExistente.getDescripcion().isEmpty()
                                    ? expExistente.getDescripcion() : "Expediente migrado.";
                } else {
                    // Datos del payload (expediente nuevo o S/N sin partes)
                    sedeModa = masRecJson != null && masRecJson.hasNonNull("sede") && !masRecJson.get("sede").asText("").trim().isEmpty()
                        ? masRecJson.get("sede").asText("") : findMostRepeated(entry.getValue(), "sede", "");
                    gerenciaModa = masRecJson != null && masRecJson.hasNonNull("gerencia") && !masRecJson.get("gerencia").asText("").trim().isEmpty()
                        ? masRecJson.get("gerencia").asText("") : findMostRepeated(entry.getValue(), "gerencia", "");
                    materiaModa = masRecJson != null && masRecJson.hasNonNull("materia") && !masRecJson.get("materia").asText("").trim().isEmpty()
                        ? masRecJson.get("materia").asText("") : findMostRepeated(entry.getValue(), "materia", "");
                    organoModa = masRecJson != null && masRecJson.hasNonNull("organoJurisdiccional") && !masRecJson.get("organoJurisdiccional").asText("").trim().isEmpty()
                        ? masRecJson.get("organoJurisdiccional").asText("") : findMostRepeated(entry.getValue(), "organoJurisdiccional", "");
                    abogadoModa = masRecJson != null && masRecJson.hasNonNull("abogadoResponsable") && !masRecJson.get("abogadoResponsable").asText("").trim().isEmpty()
                        ? masRecJson.get("abogadoResponsable").asText("") : findMostRepeated(entry.getValue(), "abogadoResponsable", "");
                    descModa = masRecJson != null && masRecJson.hasNonNull("descripcion") && !masRecJson.get("descripcion").asText("").trim().isEmpty()
                        ? masRecJson.get("descripcion").asText("") : findMostRepeated(entry.getValue(), "descripcion", "Expediente migrado.");


                    // LIMPIEZA AGRESIVA de basura antes de procesar
                    if (gerenciaModa != null && gerenciaModa.toUpperCase().contains("DEFINIR")) gerenciaModa = "";
                    if (materiaModa != null && materiaModa.toUpperCase().contains("DEFINIR")) materiaModa = "";

                    // INTELIGENCIA: Inferir gerencia desde materia o el órgano
                    if ((gerenciaModa == null || gerenciaModa.isEmpty()) && materiaModa != null && !materiaModa.isEmpty() && !materiaModa.equals("Por asignar")) {
                        Long matMapId = buscarIdMapeado(loteId, "MATERIA", materiaModa);
                        if (matMapId != null) {
                            java.util.Optional<Materia> matOpt = materiaRepository.findById(matMapId.intValue());
                            if (matOpt.isPresent() && matOpt.get().getGerencia() != null) gerenciaModa = matOpt.get().getGerencia().getNombre();
                        } else {
                            java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaModa.trim());
                            if (matOpt.isPresent() && matOpt.get().getGerencia() != null) gerenciaModa = matOpt.get().getGerencia().getNombre();
                        }
                    }
                    if ((gerenciaModa == null || gerenciaModa.isEmpty()) && organoModa != null && !organoModa.isEmpty()) {
                        Long orgMapId = buscarIdMapeado(loteId, "ORGANO", organoModa);
                        if (orgMapId != null) {
                            java.util.Optional<OrganoJurisdiccional> orgOpt = organoRepository.findById(orgMapId.intValue());
                            if (orgOpt.isPresent() && orgOpt.get().getGerencia() != null) gerenciaModa = orgOpt.get().getGerencia().getNombre();
                        } else {
                            java.util.Optional<OrganoJurisdiccional> orgOpt = organoRepository.findByNombreIgnoreCase(organoModa.trim());
                            if (orgOpt.isPresent() && orgOpt.get().getGerencia() != null) gerenciaModa = orgOpt.get().getGerencia().getNombre();
                        }
                    }
                    // ANCLAR A GERENCIA GLOBAL si sigue vacía
                    if (gerenciaModa == null || gerenciaModa.isEmpty()) gerenciaModa = gerenciaGlobal;
                    if (materiaModa == null || materiaModa.isEmpty()) materiaModa = "Por asignar";
                } // fin else (expediente nuevo)



                map.put("partes", partesModa);
                map.put("sede", sedeModa);
                map.put("gerencia", gerenciaModa);
                map.put("materia", materiaModa);
                map.put("organo", organoModa);
                map.put("abogado", abogadoModa);
                map.put("descripcion", descModa);

                // --- ASIGNACIÓN AUTOMÁTICA DE MATERIA POR DEFECTO SEGÚN GERENCIA ---
                // Si el expediente no tiene materia pero sí gerencia, se asigna la materia
                // predeterminada: "Laboral" para Gerencia Laboral y Penal,
                // "Administrativo" para Gerencia Civil, Mercantil, Fiscal y Administrativo.
                if ((materiaModa == null || materiaModa.isEmpty() || materiaModa.equals("Por asignar"))
                        && gerenciaModa != null && !gerenciaModa.isEmpty()) {

                    String gerenciaNorm = gerenciaModa.toLowerCase();
                    String materiaDefecto = null;

                    if (gerenciaNorm.contains("laboral") || gerenciaNorm.contains("penal")) {
                        materiaDefecto = "Laboral";
                    } else if (gerenciaNorm.contains("civil") || gerenciaNorm.contains("mercantil")
                            || gerenciaNorm.contains("fiscal") || gerenciaNorm.contains("administrativo")) {
                        materiaDefecto = "Administrativo";
                    }

                    if (materiaDefecto != null) {
                        // Resolver el ID de la materia desde la BD para consistencia
                        final String materiaDefectoFinal = materiaDefecto;
                        java.util.Optional<Materia> matOpt = materiaRepository.findByNombreIgnoreCase(materiaDefectoFinal);
                        if (matOpt.isPresent()) {
                            materiaModa = matOpt.get().getNombre();
                            // Actualizar los mapas intermedios para que los pasos siguientes usen el ID correcto
                            map.put("materia", materiaModa);
                            map.put("materiaId", matOpt.get().getId().longValue());
                        } else {
                            // Si por alguna razón no está en BD, al menos ponemos el texto
                            materiaModa = materiaDefecto;
                            map.put("materia", materiaModa);
                        }
                    }
                }

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
                Long sedeId = buscarIdMapeado(loteId, "SEDE", sedeModa);
                Long abogadoId = buscarIdMapeado(loteId, "USUARIO", abogadoModa);
                
    // Limpiamos la basura del JSON antes de mandarla al frontend
                if (gerenciaModa.equalsIgnoreCase("POR DEFINIR")) gerenciaModa = "";

                // INTELIGENCIA: Si la gerenciaId es nula pero sí tenemos una materiaId u organoId mapeada...
                if (gerenciaId == null && materiaId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Materia> matOpt = materiaRepository.findById(materiaId.intValue());
                    if (matOpt.isPresent() && matOpt.get().getGerencia() != null) {
                        gerenciaId = matOpt.get().getGerencia().getId().longValue();
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

                // REEMPLAZAR NOMBRES CON DATOS OFICIALES MAPEADOS
                if (materiaId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Materia> m = materiaRepository.findById(materiaId.intValue());
                    if (m.isPresent()) materiaModa = m.get().getNombre();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "MATERIA", materiaModa);
                    if (nuevo != null) materiaModa = nuevo;
                }
                if (gerenciaId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Gerencia> g = gerenciaRepository.findById(gerenciaId.intValue());
                    if (g.isPresent()) gerenciaModa = g.get().getNombre();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "GERENCIA", gerenciaModa);
                    if (nuevo != null) gerenciaModa = nuevo;
                }
                if (organoId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional> o = organoRepository.findById(organoId.intValue());
                    if (o.isPresent()) organoModa = o.get().getNombre();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "ORGANO", organoModa);
                    if (nuevo != null) organoModa = nuevo;
                }
                if (sedeId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Estado> s = estadoRepository.findById(sedeId.intValue());
                    if (s.isPresent()) sedeModa = s.get().getNombre();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "SEDE", sedeModa);
                    if (nuevo != null) sedeModa = nuevo;
                }
                if (abogadoId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.usuario.Usuario> u = usuarioRepository.findById(abogadoId.intValue());
                    if (u.isPresent()) abogadoModa = u.get().getNombreCompleto();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "USUARIO", abogadoModa);
                    if (nuevo != null) abogadoModa = nuevo;
                }
                
                map.put("materia", materiaModa);
                map.put("organo", organoModa);
                map.put("sede", sedeModa);
                map.put("abogado", abogadoModa);
                
                map.put("gerencia", gerenciaModa); // Aseguramos que se mande el texto limpio/inferido
                map.put("gerenciaId", gerenciaId);
                map.put("materiaId", materiaId);
                map.put("organoId", organoId);
                map.put("sedeId", sedeId);
                map.put("abogadoId", abogadoId);
                
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
        
        LoteMigracion lote = loteRepository.findById(loteId).orElse(null);
        String gerenciaGlobal = (lote != null && lote.getGerenciaGlobal() != null) ? lote.getGerenciaGlobal().getNombre() : "";
        
        if (gerenciaGlobal.isEmpty()) {
            gerenciaGlobal = findMostRepeated(todosLosRegistros, "gerencia", "");
            if (gerenciaGlobal.toUpperCase().contains("DEFINIR")) gerenciaGlobal = "";
        }
        
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
                Long abogadoComparecienteId = buscarIdMapeado(loteId, "USUARIO", comp);
                Long abogadoResponsableId = buscarIdMapeado(loteId, "USUARIO", resp);
                Long sedeId = buscarIdMapeado(loteId, "SEDE", sedeVal);
                Long organoId = buscarIdMapeado(loteId, "ORGANO", organoVal);
                Long tipoAudienciaId = buscarIdMapeado(loteId, "TIPO_AUDIENCIA", tipoAud);

                // REEMPLAZAR TEXTOS CON OFICIALES
                if (abogadoComparecienteId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.usuario.Usuario> u = usuarioRepository.findById(abogadoComparecienteId.intValue());
                    if (u.isPresent()) comp = u.get().getNombreCompleto();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "USUARIO", comp);
                    if (nuevo != null) comp = nuevo;
                }
                if (abogadoResponsableId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.usuario.Usuario> u = usuarioRepository.findById(abogadoResponsableId.intValue());
                    if (u.isPresent()) resp = u.get().getNombreCompleto();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "USUARIO", resp);
                    if (nuevo != null) resp = nuevo;
                }
                if (sedeId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Estado> s = estadoRepository.findById(sedeId.intValue());
                    if (s.isPresent()) sedeVal = s.get().getNombre();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "SEDE", sedeVal);
                    if (nuevo != null) sedeVal = nuevo;
                }
                if (organoId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional> o = organoRepository.findById(organoId.intValue());
                    if (o.isPresent()) organoVal = o.get().getNombre();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "ORGANO", organoVal);
                    if (nuevo != null) organoVal = nuevo;
                }
                if (tipoAudienciaId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia> t = tipoAudienciaRepository.findById(tipoAudienciaId.intValue());
                    if (t.isPresent()) tipoAud = t.get().getNombre();
                } else {
                    String nuevo = buscarNuevoValorMapeado(loteId, "TIPO_AUDIENCIA", tipoAud);
                    if (nuevo != null) tipoAud = nuevo;
                }

                // Guardar/reescribir en mapa para la vista HTML
                map.put("compareciente", comp);
                map.put("abogadoCompareciente", comp);
                map.put("abogadoResponsable", resp);
                map.put("sede", sedeVal);
                map.put("organo", organoVal);
                map.put("tipoAudiencia", tipoAud);

                // Guardar los IDs en el mapa
                map.put("abogadoComparecienteId", abogadoComparecienteId);
                map.put("abogadoResponsableId", abogadoResponsableId);
                map.put("sedeId", sedeId);
                map.put("organoId", organoId);
                map.put("tipoAudienciaId", tipoAudienciaId);

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
                    if (gerenciaId == null) {
                        String nuevoG = buscarNuevoValorMapeado(loteId, "GERENCIA", gerenciaAsignada);
                        if (nuevoG != null) gerenciaAsignada = nuevoG;
                    }
                }
                
                Long materiaId = buscarIdMapeado(loteId, "MATERIA", materiaAsignada);
                if (materiaId != null) {
                    java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Materia> m = materiaRepository.findById(materiaId.intValue());
                    if (m.isPresent()) materiaAsignada = m.get().getNombre();
                } else {
                    String nuevoM = buscarNuevoValorMapeado(loteId, "MATERIA", materiaAsignada);
                    if (nuevoM != null) materiaAsignada = nuevoM;
                }
                
                map.put("gerencia", gerenciaAsignada);
                map.put("gerenciaId", gerenciaId);
                map.put("materia", materiaAsignada);
                map.put("materiaId", materiaId);
                    
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
            
            LoteMigracion lote = loteRepository.findById(loteId).orElse(null);
            String gerenciaGlobal = (lote != null && lote.getGerenciaGlobal() != null) ? lote.getGerenciaGlobal().getNombre() : "";
            
            if (gerenciaGlobal.isEmpty()) {
                gerenciaGlobal = findMostRepeated(todosLosRegistros, "gerencia", "");
                if (gerenciaGlobal.toUpperCase().contains("DEFINIR")) gerenciaGlobal = "";
            }
            
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
                        if (gerenciaId == null) {
                            String nuevoG = buscarNuevoValorMapeado(loteId, "GERENCIA", gerenciaAsignada);
                            if (nuevoG != null) gerenciaAsignada = nuevoG;
                        }
                    }
                    
                    Long materiaId = buscarIdMapeado(loteId, "MATERIA", materiaAsignada);
                    Long sedeId = buscarIdMapeado(loteId, "SEDE", sedeVal);
                    Long abogadoId = buscarIdMapeado(loteId, "USUARIO", abogadoVal);

                    // REEMPLAZAR TEXTOS CON OFICIALES
                    if (materiaId != null) {
                        java.util.Optional<Materia> m = materiaRepository.findById(materiaId.intValue());
                        if (m.isPresent()) materiaAsignada = m.get().getNombre();
                    } else {
                        String nuevo = buscarNuevoValorMapeado(loteId, "MATERIA", materiaAsignada);
                        if (nuevo != null) materiaAsignada = nuevo;
                    }
                    if (sedeId != null) {
                        java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Estado> s = estadoRepository.findById(sedeId.intValue());
                        if (s.isPresent()) sedeVal = s.get().getNombre();
                    } else {
                        String nuevo = buscarNuevoValorMapeado(loteId, "SEDE", sedeVal);
                        if (nuevo != null) sedeVal = nuevo;
                    }
                    if (abogadoId != null) {
                        java.util.Optional<com.juridico.sistema_juridico.Entity.usuario.Usuario> u = usuarioRepository.findById(abogadoId.intValue());
                        if (u.isPresent()) abogadoVal = u.get().getNombreCompleto();
                        map.put("abogado", abogadoVal); // Update the map because it used the raw value on map.put("abogado") earlier
                    } else {
                        String nuevo = buscarNuevoValorMapeado(loteId, "USUARIO", abogadoVal);
                        if (nuevo != null) {
                            abogadoVal = nuevo;
                            map.put("abogado", abogadoVal);
                        }
                    }

                    map.put("gerencia", gerenciaAsignada);
                    map.put("gerenciaId", gerenciaId);
                    map.put("materia", materiaAsignada);
                    map.put("materiaId", materiaId);
                    map.put("sede", sedeVal); // just in case the UI uses 'sede'
                    map.put("sedeId", sedeId);
                    map.put("abogadoId", abogadoId);
                    
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

                // --- CASCADA DE ACTUALIZACIONES ---
                if ("EXPEDIENTE".equals(reg.getTipoDato()) && reg.getNumeroExpedienteExtraido() != null && !reg.getNumeroExpedienteExtraido().trim().isEmpty()) {
                    List<RegistroMigracion> hijos = registroRepository.findByLoteIdAndNumeroExpedienteExtraidoAndEstatusNot(
                        reg.getLote().getId(), reg.getNumeroExpedienteExtraido(), "RECHAZADO"
                    );
                    for (RegistroMigracion hijo : hijos) {
                        if (hijo.getId().equals(reg.getId())) continue;
                        boolean hijoChanged = false;
                        com.fasterxml.jackson.databind.node.ObjectNode hijoJson = 
                            (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(hijo.getPayloadJson());
                            
                        // Si actualizamos abogadoResponsable, replicarlo
                        if (campos.containsKey("abogadoResponsable")) {
                            String nuevoAbogado = campos.get("abogadoResponsable");
                            if (hijoJson.has("abogadoResponsable")) { hijoJson.put("abogadoResponsable", nuevoAbogado); hijoChanged = true; }
                            if (hijoJson.has("abogadoCompareciente")) { hijoJson.put("abogadoCompareciente", nuevoAbogado); hijoChanged = true; }
                            if (hijoJson.has("abogado")) { hijoJson.put("abogado", nuevoAbogado); hijoChanged = true; }
                        }
                        
                        // Replicar jerarquías generales
                        if (campos.containsKey("gerencia") && hijoJson.has("gerencia")) { hijoJson.put("gerencia", campos.get("gerencia")); hijoChanged = true; }
                        if (campos.containsKey("materia") && hijoJson.has("materia")) { hijoJson.put("materia", campos.get("materia")); hijoChanged = true; }
                        if (campos.containsKey("organoJurisdiccional") && hijoJson.has("organoJurisdiccional")) { hijoJson.put("organoJurisdiccional", campos.get("organoJurisdiccional")); hijoChanged = true; }
                        if (campos.containsKey("sede") && hijoJson.has("sede")) { hijoJson.put("sede", campos.get("sede")); hijoChanged = true; }
                        
                        if (hijoChanged) {
                            hijo.setPayloadJson(objectMapper.writeValueAsString(hijoJson));
                            registroRepository.save(hijo);
                        }
                    }
                }

                invalidateCaches(reg.getLote().getId());
            } catch (Exception e) {
                throw new RuntimeException("Error al editar registro: " + e.getMessage(), e);
            }
        }

        public void inyectarLoteOficial(Long loteId) {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            Map<String, CatalogoMigracion> lookup = new HashMap<>();
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();
                    List<CatalogoMigracion> catalogosMap = catalogoRepository.findByLoteId(loteId);

                    for (CatalogoMigracion c : catalogosMap) {
                        lookup.put(c.getTipoCatalogo() + "___" + c.getValorOriginal().toLowerCase(), c);
                    }

                    crearEntidadesNuevas(catalogosMap);
                });

                LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();
                List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
                final Gerencia gerenciaGlobalLote = lote.getGerenciaGlobal();

                // --- AGRUPACIÓN ---
                java.util.Map<String, List<RegistroMigracion>> expedientesAgrupados = agruparRegistros(registros);

                for (java.util.Map.Entry<String, List<RegistroMigracion>> entry : expedientesAgrupados.entrySet()) {
                    String baseNum = entry.getKey();
                    List<RegistroMigracion> expRegistros = entry.getValue();

                    try {
                        self.procesarExpedienteEnNuevaTransaccion(baseNum, expRegistros, lookup, gerenciaGlobalLote, loteId);
                    } catch (Exception e) {
                        System.err.println("Error grave inyectando expediente del grupo " + baseNum + " (saneando con fallback silencioso): " + e.getMessage());
                        e.printStackTrace();
                        try {
                            self.marcarRegistrosComoProcesadosConFallback(expRegistros);
                        } catch (Exception ex) {
                            System.err.println("No se pudieron marcar registros del grupo " + baseNum + " como PROCESADOS: " + ex.getMessage());
                        }
                    }
                }

                transactionTemplate.executeWithoutResult(status -> {
                    LoteMigracion loteFinal = loteRepository.findById(loteId).orElseThrow();
                    loteFinal.setEstatus("COMPLETADO");
                    loteFinal.setPasoActual(5);
                    loteRepository.save(loteFinal);

                    // LIMPIEZA: Si hay otros lotes PENDIENTES (por ruido de n8n), los cancelamos
                    List<LoteMigracion> otrosLotes = loteRepository.findByEstatus("PENDIENTE");
                    for (LoteMigracion otro : otrosLotes) {
                        if (!otro.getId().equals(loteId)) {
                            otro.setEstatus("RECHAZADO");
                        }
                    }
                    loteRepository.saveAll(otrosLotes);
                });
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error inyectando lote: " + e.getMessage(), e);
            }
        }

        @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
        public void procesarExpedienteEnNuevaTransaccion(String baseNum, List<RegistroMigracion> expRegistros, Map<String, CatalogoMigracion> lookup, Gerencia gerenciaGlobalLote, Long loteId) throws Exception {
            String numeroFinal = baseNum;
            boolean esGrupoSinNumero = baseNum.startsWith("S/N-") || baseNum.equals("SIN_NUMERO");
            if (esGrupoSinNumero) {
                numeroFinal = "S/N";
            }

            // Truncar número de expediente proactivamente a 100 caracteres para evitar desbordes de BD
            if (numeroFinal.length() > 100) {
                numeroFinal = numeroFinal.substring(0, 100);
            }

            // Extraer las partes del registro más representativo para la búsqueda de duplicado
            String partesParaBusqueda = "";
            try {
                RegistroMigracion masRec = expRegistros.stream()
                    .max((r1, r2) -> extractLatestDate(r1).compareTo(extractLatestDate(r2)))
                    .orElse(expRegistros.get(0));
                JsonNode jn = objectMapper.readTree(masRec.getPayloadJson());
                partesParaBusqueda = jn.hasNonNull("partes") ? jn.get("partes").asText("").trim() : "";
            } catch (Exception ignored) {}

            // Buscar si ya existe para decidir si Crear o Actualizar
            Expediente expediente = null;

            if (esGrupoSinNumero && !partesParaBusqueda.isEmpty()) {
                final String partesFinales = partesParaBusqueda;
                expediente = expedienteRepository
                    .findByNumeroIgnoreCaseAndPartesIgnoreCase(numeroFinal, partesFinales)
                    .orElse(null);
            } else if (!esGrupoSinNumero) {
                expediente = expedienteRepository.findByNumeroIgnoreCase(numeroFinal).orElse(null);
            }

            if (expediente == null) {
                expediente = new Expediente();
                expediente.setNumero(numeroFinal);
                expediente.setDescripcion("Migrado desde sistema histórico - Pendiente de completar por el abogado responsable.");

                // Extraer valores iniciales del registro más reciente para asegurar mayor calidad
                RegistroMigracion masReciente = expRegistros.stream()
                    .max((r1, r2) -> extractLatestDate(r1).compareTo(extractLatestDate(r2)))
                    .orElse(expRegistros.get(0));
                
                java.util.List<RegistroMigracion> ordenados = new java.util.ArrayList<>();
                ordenados.add(masReciente);
                for (RegistroMigracion xr : expRegistros) {
                    if (!xr.getId().equals(masReciente.getId())) ordenados.add(xr);
                }

                for (RegistroMigracion r : ordenados) {
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
                                if (expediente.getGerencia() == null && m.getGerencia() != null) {
                                    expediente.setGerencia(m.getGerencia());
                                }
                            }
                        }
                        if (expediente.getOrganoJurisdiccional() == null && json.has("organoJurisdiccional") && !json.get("organoJurisdiccional").isNull()) {
                            OrganoJurisdiccional o = resolveOrgano(json.get("organoJurisdiccional").asText(), lookup, expediente.getGerencia());
                            if (o != null) expediente.setOrganoJurisdiccional(o);
                        }
                        if (expediente.getPartes() == null && json.has("partes") && !json.get("partes").isNull()) {
                            String partesStr = json.get("partes").asText();
                            // Truncar proactivamente
                            if (partesStr.length() > 500) partesStr = partesStr.substring(0, 500);
                            expediente.setPartes(partesStr);
                        }
                        if (expediente.getAbogadoResponsable() == null && json.has("abogadoResponsable") && !json.get("abogadoResponsable").isNull()) {
                            String nombreAbog = json.get("abogadoResponsable").asText();
                            Usuario u = resolveUsuario(nombreAbog, lookup, expediente.getGerencia(), loteId);
                            if (u != null) {
                                expediente.setAbogadoResponsable(u);
                            } else {
                                if (nombreAbog.length() > 190) nombreAbog = nombreAbog.substring(0, 190);
                                expediente.setAbogadoResponsableNombre(nombreAbog);
                            }
                        }
                        if (expediente.getSede() == null && json.has("sede") && !json.get("sede").isNull()) {
                            String sedeOriginal = json.get("sede").asText().trim();
                            CatalogoMigracion catSede = lookup.get("SEDE___" + sedeOriginal.toLowerCase());
                            
                            if (catSede != null && catSede.getIdMapeado() != null) {
                                java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Estado> estadoOpt = estadoRepository.findById(catSede.getIdMapeado().intValue());
                                if (estadoOpt.isPresent()) {
                                    expediente.setSede(estadoOpt.get().getNombre());
                                }
                            } else {
                                String sedeFinal = sedeOriginal;
                                if (sedeFinal.length() > 140) sedeFinal = sedeFinal.substring(0, 140);
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
                
                if (expediente.getGerencia() == null && gerenciaGlobalLote != null) {
                    expediente.setGerencia(gerenciaGlobalLote);
                }
                if (expediente.getGerencia() == null) {
                    expediente.setGerencia(getFallbackGerencia());
                }

                if (expediente.getMateria() == null) {
                    Materia materiaDefecto = resolverMateriaDefectoPorGerencia(expediente.getGerencia());
                    expediente.setMateria(materiaDefecto != null ? materiaDefecto : getFallbackMateria());
                }

                expediente = expedienteRepository.save(expediente);
            } else {
                boolean materiaSinAsignar = expediente.getMateria() == null
                        || expediente.getMateria().getNombre() == null
                        || expediente.getMateria().getNombre().equalsIgnoreCase("Por Asignar")
                        || expediente.getMateria().getNombre().equalsIgnoreCase("Por Definir");

                Gerencia gerenciaParaFix = expediente.getGerencia();
                if (gerenciaParaFix == null) gerenciaParaFix = gerenciaGlobalLote;

                if (materiaSinAsignar && gerenciaParaFix != null) {
                    Materia materiaCorrecta = resolverMateriaDefectoPorGerencia(gerenciaParaFix);
                    if (materiaCorrecta != null) {
                        expediente.setMateria(materiaCorrecta);
                        if (expediente.getGerencia() == null) expediente.setGerencia(gerenciaParaFix);
                        expediente = expedienteRepository.save(expediente);
                    }
                }
            }

            final Expediente expFinal = expediente;

            for (RegistroMigracion r : expRegistros) {
                try {
                    JsonNode json = objectMapper.readTree(r.getPayloadJson());
                    if ("TERMINO".equals(r.getTipoDato())) {
                        String asuntoVal = json.has("prestacion") ? json.get("prestacion").asText() : (json.has("asunto") ? json.get("asunto").asText() : "Sin actuación");
                        // Truncar proactivamente asunto
                        if (asuntoVal.length() > 490) asuntoVal = asuntoVal.substring(0, 490);
                        
                        LocalDate fechaVenc = parseDate(json.get("fechaVencimiento"));
                        LocalDate fechaIng = parseDate(json.get("fechaIngreso"));

                        List<Termino> tExistentes = terminoRepository.findByExpedienteAndAsuntoAndFechaVencimiento(expFinal, asuntoVal, fechaVenc);
                        if (!tExistentes.isEmpty()) {
                            System.out.println("Omitiendo término duplicado para expediente " + expFinal.getNumero() + ": " + asuntoVal);
                            r.setEstatus("PROCESADO");
                            r.setProcessedAt(java.time.LocalDateTime.now());
                            registroRepository.save(r);
                            continue; 
                        }

                        Termino termino = new Termino();
                        termino.setExpediente(expFinal);
                        termino.setAsunto(asuntoVal);
                        termino.setFechaVencimiento(fechaVenc);
                        termino.setFechaIngreso(fechaIng);

                        termino.setEstatusTermino(
                            fechaVenc.isBefore(LocalDate.now()) ? EstatusTermino.CONCLUIDO : EstatusTermino.PROYECTISTA
                        );

                        String prioridadStr = json.has("prioridad") ? json.get("prioridad").asText("Media") : "Media";
                        CatPrioridad prioridad = catPrioridadRepository.findByNombreIgnoreCase(prioridadStr)
                            .orElseGet(() -> catPrioridadRepository.findByNombreIgnoreCase("Media").orElse(null));
                        termino.setPrioridad(prioridad);

                        String acuseStr = json.has("acuse") ? json.get("acuse").asText() : "";
                        if (acuseStr.length() > 490) acuseStr = acuseStr.substring(0, 490);
                        termino.setAcuse(acuseStr);

                        String acuseNombreStr = json.has("archivoWord") ? json.get("archivoWord").asText() : (json.has("acuseNombre") ? json.get("acuseNombre").asText() : "");
                        if (acuseNombreStr.length() > 490) acuseNombreStr = acuseNombreStr.substring(0, 490);
                        termino.setAcuseNombre(acuseNombreStr);

                        termino.setObservaciones(json.has("observaciones") ? json.get("observaciones").asText() : "");
                        termino.setCreatedAt(java.time.LocalDateTime.now());
                        termino.setUpdatedAt(java.time.LocalDateTime.now());

                        if (json.has("abogadoResponsable")) {
                            Usuario u = resolveUsuario(json.get("abogadoResponsable").asText(), lookup, expFinal.getGerencia(), loteId);
                            if (u != null) termino.setAbogadoResponsable(u);
                        }

                        terminoRepository.save(termino);

                    } else if ("AUDIENCIA".equals(r.getTipoDato())) {
                        LocalDate fechaAud = parseDate(json.get("fechaAudiencia"));
                        LocalTime horaAud = parseTime(json.get("horaAudiencia"));

                        List<Audiencia> aExistentes = audienciaRepository.findByExpedienteAndFechaAudienciaAndHoraAudiencia(expFinal, fechaAud, horaAud);
                        if (!aExistentes.isEmpty()) {
                            System.out.println("Omitiendo audiencia duplicada para expediente " + expFinal.getNumero() + " el " + fechaAud);
                            r.setEstatus("PROCESADO");
                            r.setProcessedAt(java.time.LocalDateTime.now());
                            registroRepository.save(r);
                            continue;
                        }

                        Audiencia audiencia = new Audiencia();
                        audiencia.setExpediente(expFinal);
                        audiencia.setFechaAudiencia(fechaAud);
                        audiencia.setHoraAudiencia(horaAud);

                        audiencia.setEstatusAudiencia(
                            fechaAud.isBefore(LocalDate.now()) ? EstatusAudiencia.CONCLUIDA : EstatusAudiencia.PENDIENTE
                        );

                        String salaStr = json.has("sala") ? json.get("sala").asText() : (json.has("salaLugar") ? json.get("salaLugar").asText() : "");
                        // Truncar salaLugar si excede 490
                        if (salaStr.length() > 490) salaStr = salaStr.substring(0, 490);
                        audiencia.setSalaLugar(salaStr);

                        boolean esVirtual = false;
                        if (json.has("esEnLinea")) {
                            String esEnLineaVal = json.get("esEnLinea").asText("").toLowerCase();
                            esVirtual = esEnLineaVal.equals("true") || esEnLineaVal.equals("1") || esEnLineaVal.equals("si") || esEnLineaVal.equals("sí");
                        } else if (json.has("esVirtual")) {
                            esVirtual = json.get("esVirtual").asBoolean(false);
                        }
                        audiencia.setEsVirtual(esVirtual);

                        if (json.has("enlaceReunion") && !json.get("enlaceReunion").isNull()) {
                            String urlReunion = json.get("enlaceReunion").asText();
                            if (urlReunion.length() > 490) urlReunion = urlReunion.substring(0, 490);
                            audiencia.setUrlReunion(urlReunion);
                        } else if (json.has("urlReunion") && !json.get("urlReunion").isNull()) {
                            String urlReunion = json.get("urlReunion").asText();
                            if (urlReunion.length() > 490) urlReunion = urlReunion.substring(0, 490);
                            audiencia.setUrlReunion(urlReunion);
                        }

                        String actaStr = json.has("actaDocumento") ? json.get("actaDocumento").asText() : "";
                        if (actaStr.length() > 490) actaStr = actaStr.substring(0, 490);
                        audiencia.setActaDocumento(actaStr);
                        audiencia.setObservaciones(json.has("observaciones") ? json.get("observaciones").asText() : "");

                        if (json.has("tipoAudiencia")) {
                            TipoAudiencia ta = resolveTipoAudiencia(json.get("tipoAudiencia").asText(), lookup, expFinal.getGerencia());
                            if (ta != null) audiencia.setTipoAudiencia(ta);
                        }

                        if (json.has("abogadoCompareciente")) {
                            Usuario u = resolveUsuario(json.get("abogadoCompareciente").asText(), lookup, expFinal.getGerencia(), loteId);
                            if (u != null) audiencia.setAbogadoComparece(u);
                        }

                        audiencia.setCreatedAt(java.time.LocalDateTime.now());
                        audiencia.setUpdatedAt(java.time.LocalDateTime.now());
                        audienciaRepository.save(audiencia);
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando registro " + r.getId() + " (ignorado con fallback proactivo): " + e.getMessage());
                    // NO relanzamos la excepción para evitar invalidar la transacción completa del expediente.
                }

                r.setEstatus("PROCESADO");
                r.setProcessedAt(java.time.LocalDateTime.now());
                registroRepository.save(r);
            }
        }

        @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
        public void marcarRegistrosComoProcesadosConFallback(List<RegistroMigracion> expRegistros) {
            for (RegistroMigracion r : expRegistros) {
                r.setEstatus("PROCESADO");
                r.setProcessedAt(java.time.LocalDateTime.now());
                registroRepository.save(r);
            }
        }



        /**
         * Dado una gerencia, retorna la materia por defecto correspondiente:
         *  - "Laboral" para Gerencia Laboral y Penal
         *  - "Administrativo" para Gerencia Civil, Mercantil, Fiscal y Administrativo
         *  - null si la gerencia no coincide con ningún patrón (usar getFallbackMateria)
         */
        private Materia resolverMateriaDefectoPorGerencia(Gerencia gerencia) {
            if (gerencia == null || gerencia.getNombre() == null) return null;
            String gerNom = gerencia.getNombre().toLowerCase();
            if (gerNom.contains("laboral") || gerNom.contains("penal")) {
                return materiaRepository.findByNombreIgnoreCase("Laboral").orElse(null);
            } else if (gerNom.contains("civil") || gerNom.contains("mercantil")
                    || gerNom.contains("fiscal") || gerNom.contains("administrativo")) {
                return materiaRepository.findByNombreIgnoreCase("Administrativo").orElse(null);
            }
            return null;
        }

        /**
         * Corrección batch: recorre TODOS los expedientes en la BD que tienen materia
         * nula, "Por Asignar" o "Por Definir" y les asigna la materia correcta según
         * su gerencia. Se llama desde el endpoint /admin/importar/corregir-materias.
         * @return número de expedientes corregidos
         */
        @Transactional
        public int corregirMateriasPorAsignar() {
            List<com.juridico.sistema_juridico.Entity.expediente.Expediente> todos =
                    expedienteRepository.findAll();
            int count = 0;
            for (com.juridico.sistema_juridico.Entity.expediente.Expediente exp : todos) {
                boolean sinMateria = exp.getMateria() == null
                        || exp.getMateria().getNombre() == null
                        || exp.getMateria().getNombre().equalsIgnoreCase("Por Asignar")
                        || exp.getMateria().getNombre().equalsIgnoreCase("Por Definir");
                if (!sinMateria) continue;

                Gerencia g = exp.getGerencia();
                if (g == null) continue; // Sin gerencia no podemos inferir materia

                Materia correcta = resolverMateriaDefectoPorGerencia(g);
                if (correcta != null) {
                    exp.setMateria(correcta);
                    expedienteRepository.save(exp);
                    count++;
                }
            }
            return count;
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
            if (c == null) {
                c = lookup.values().stream()
                    .filter(cat -> "MATERIA".equals(cat.getTipoCatalogo()) && "NUEVO".equals(cat.getEstatus()) && trimmed.equalsIgnoreCase(cat.getNuevoValor()))
                    .findFirst().orElse(null);
            }
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
                if ("DESCARTADO".equals(c.getEstatus())) {
                    return null;
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
            if (c == null) {
                c = lookup.values().stream()
                    .filter(cat -> "GERENCIA".equals(cat.getTipoCatalogo()) && "NUEVO".equals(cat.getEstatus()) && original.trim().equalsIgnoreCase(cat.getNuevoValor()))
                    .findFirst().orElse(null);
            }
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
                if ("DESCARTADO".equals(c.getEstatus())) {
                    return null;
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
            if (c == null) {
                c = lookup.values().stream()
                    .filter(cat -> "ORGANO".equals(cat.getTipoCatalogo()) && "NUEVO".equals(cat.getEstatus()) && original.trim().equalsIgnoreCase(cat.getNuevoValor()))
                    .findFirst().orElse(null);
            }
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
                if ("DESCARTADO".equals(c.getEstatus())) {
                    return null;
                }
            }
            return organoRepository.findByNombreIgnoreCase(original.trim()).orElse(null);
        }

        private TipoAudiencia resolveTipoAudiencia(String original, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerenciaContexto) {
            if (original == null || original.trim().isEmpty()) return null;
            String key = "TIPO_AUDIENCIA___" + original.trim().toLowerCase();
            CatalogoMigracion c = lookup.get(key);
            if (c == null) {
                c = lookup.values().stream()
                    .filter(cat -> "TIPO_AUDIENCIA".equals(cat.getTipoCatalogo()) && "NUEVO".equals(cat.getEstatus()) && original.trim().equalsIgnoreCase(cat.getNuevoValor()))
                    .findFirst().orElse(null);
            }
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
                        t.setPerfilRecordatorio(perfilRecordatorioAudienciaRepository
                            .findByEstadoOrderByNombreAsc(EstadoPerfil.PUBLICADO).stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                "Configura y publica un perfil de audiencia antes de importar nuevos tipos.")));
                        existente = tipoAudienciaRepository.save(t);
                    }
                    c.setIdMapeado(existente.getId().longValue());
                    c.setNuevoValor(null);
                    return existente;
                }
                if ("DESCARTADO".equals(c.getEstatus())) {
                    return null;
                }
            }
            return tipoAudienciaRepository.findByNombreIgnoreCase(original.trim()).orElse(null);
        }

        private Usuario resolveUsuario(String original, java.util.Map<String, CatalogoMigracion> lookup, Gerencia gerencia, Long loteId) {
            if (original == null || original.trim().isEmpty()) return null;
            CatalogoMigracion c = lookup.get("USUARIO___" + original.trim().toLowerCase());
            if (c == null) {
                c = lookup.values().stream()
                    .filter(cat -> "USUARIO".equals(cat.getTipoCatalogo()) && "NUEVO".equals(cat.getEstatus()) && original.trim().equalsIgnoreCase(cat.getNuevoValor()))
                    .findFirst().orElse(null);
            }
            
            String finalAbogadoName = original.trim();
            if (c != null) {
                if (c.getIdMapeado() != null) return usuarioRepository.findById(c.getIdMapeado().intValue()).orElse(null);
                if ("DESCARTADO".equals(c.getEstatus())) return null;
                
                if (c.getNuevoValor() != null && !c.getNuevoValor().trim().isEmpty()) {
                    finalAbogadoName = c.getNuevoValor().trim();
                }
            }
            // Si no hay mapeo, buscar por nombre final (heredado de edición manual en Catálogo o DB cruda)
            return crearOEncontrarAbogado(finalAbogadoName, gerencia, loteId, lookup);
        }

        private Usuario crearOEncontrarAbogado(String nombreCompleto, Gerencia gerenciaExpediente, Long loteId, java.util.Map<String, CatalogoMigracion> lookup) {
            if (nombreCompleto == null || nombreCompleto.isEmpty()) return null;

            // 1. Buscar si ya existe alguien con ese nombre completo
            List<Usuario> existentes = usuarioRepository.findAll().stream()
                .filter(u -> u.getNombreCompleto().equalsIgnoreCase(nombreCompleto))
                .toList();
            if (!existentes.isEmpty()) {
                Usuario abogadoExistente = existentes.get(0);
                // BUG FIX: Asignar materias adicionales inferidas del lote al abogado existente
                // (preservando las que ya tenía, solo añadiendo las nuevas)
                Gerencia gerenciaFinalExistente = abogadoExistente.getGerencia() != null
                    ? abogadoExistente.getGerencia()
                    : (gerenciaExpediente != null ? gerenciaExpediente : getFallbackGerencia());
                java.util.Set<Materia> materiasLote = obtenerMateriasDeAbogado(nombreCompleto, loteId, lookup, gerenciaFinalExistente);
                boolean actualizado = false;
                for (Materia m : materiasLote) {
                    boolean yaLaTiene = abogadoExistente.getMaterias().stream()
                        .anyMatch(ex -> ex.getId().equals(m.getId()));
                    if (!yaLaTiene) {
                        abogadoExistente.getMaterias().add(m);
                        actualizado = true;
                        System.out.println(">>> Migracion: Añadida materia '" + m.getNombre() + "' al abogado existente: " + nombreCompleto);
                    }
                }
                if (actualizado) {
                    usuarioRepository.save(abogadoExistente);
                }
                return abogadoExistente;
            }

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
            String passwordHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());

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
            System.out.println(">>> Migracion: Usuario creado: " + email
                + " / gerencia: " + gerenciaFinal.getNombre() 
                + " / materias: " + materiasAbogado.stream().map(Materia::getNombre).toList());
            return guardado;
        }

        /**
         * Escanea todos los registros del lote buscando las materias asociadas a un abogado.
         * Busca en los campos abogadoResponsable y abogadoCompareciente, y extrae el campo "materia".
         */
        public List<Integer> getMateriasInferredForAbogadoNuevos(Long loteId, String nombreAbogado) {
            List<CatalogoMigracion> catalogos = catalogoRepository.findByLoteId(loteId);

            Map<String, CatalogoMigracion> lookup = new java.util.HashMap<>();
            catalogos.forEach(c ->
                lookup.put(c.getTipoCatalogo() + "_" + c.getValorOriginal().trim().toLowerCase(), c)
            );

            // Si el nombreAbogado es un nuevoValor editado en Stage 1, resolver al valorOriginal
            // para buscar correctamente en los registros del JSON (que preservan el nombre original).
            String nombreParaBuscar = resolverNombreOriginal(catalogos, nombreAbogado);

            // Buscar también por el nombre tal cual (por si el payload ya fue actualizado al nuevoValor)
            java.util.Set<Materia> materiasOrig = obtenerMateriasDeAbogado(nombreParaBuscar, loteId, lookup, null);
            if (!nombreParaBuscar.equalsIgnoreCase(nombreAbogado)) {
                // Combinar con búsqueda directa por nuevoValor (registros editados en Stage 2)
                materiasOrig.addAll(obtenerMateriasDeAbogado(nombreAbogado, loteId, lookup, null));
            }

            return materiasOrig.stream().map(m -> m.getId()).collect(Collectors.toList());
        }

        /**
         * Infiere la gerenciaId de un abogado del lote, derivándola de sus materias inferidas.
         * Útil para abogados "NUEVO" que aún no existen en BD.
         */
        public Long getGerenciaIdInferredForAbogado(Long loteId, String valorOriginal) {
            if (valorOriginal == null || valorOriginal.isBlank()) return null;
            List<CatalogoMigracion> catalogos = catalogoRepository.findByLoteId(loteId);

            Map<String, CatalogoMigracion> lookup = new java.util.HashMap<>();
            catalogos.forEach(c ->
                lookup.put(c.getTipoCatalogo() + "_" + c.getValorOriginal().trim().toLowerCase(), c)
            );

            // Resolver nuevoValor → valorOriginal si aplica
            String nombreParaBuscar = resolverNombreOriginal(catalogos, valorOriginal);

            java.util.Set<Materia> materias = obtenerMateriasDeAbogado(nombreParaBuscar, loteId, lookup, null);
            if (!nombreParaBuscar.equalsIgnoreCase(valorOriginal)) {
                materias.addAll(obtenerMateriasDeAbogado(valorOriginal, loteId, lookup, null));
            }

            return materias.stream()
                .filter(m -> m.getGerencia() != null)
                .map(m -> m.getGerencia().getId().longValue())
                .findFirst()
                .orElse(null);
        }

        /**
         * Dado un nombre que puede ser un nuevoValor editado, lo resuelve al valorOriginal
         * si existe una entrada USUARIO en el catálogo con ese nuevoValor.
         * Así la búsqueda en los registros JSON siempre usa el nombre original del batch.
         */
        private String resolverNombreOriginal(List<CatalogoMigracion> catalogos, String nombreBuscado) {
            if (nombreBuscado == null) return "";
            return catalogos.stream()
                .filter(c -> "USUARIO".equals(c.getTipoCatalogo())
                        && c.getNuevoValor() != null
                        && c.getNuevoValor().trim().equalsIgnoreCase(nombreBuscado.trim())
                        && c.getValorOriginal() != null)
                .map(CatalogoMigracion::getValorOriginal)
                .findFirst()
                .orElse(nombreBuscado); // fallback: usar el nombre tal cual
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


        private String buscarNuevoValorMapeado(Long loteId, String tipo, String originalValor) {
        if (originalValor == null) return null;
        return catalogoRepository.findByLoteId(loteId).stream()
                .filter(c -> c.getTipoCatalogo().equals(tipo) && "NUEVO".equals(c.getEstatus()) && (c.getValorOriginal() != null &&  c.getValorOriginal().trim().equalsIgnoreCase(originalValor.trim())))
                .map(CatalogoMigracion::getNuevoValor)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Long buscarIdMapeado(Long loteId, String tipo, String originalValor) {
            if (originalValor == null || originalValor.isEmpty()) return null;
            String valorNormalizado = normalizeValue(originalValor);
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
    
    public List<String> buscarExpedientesSimilares(String term, Long loteId) {
        List<String> resultados = new ArrayList<>();
        if (term == null || term.length() < 2) return resultados;

        // 1. Buscar en BD Oficial (Top 10)
        resultados.addAll(expedienteRepository.findTop10ByNumeroContainingIgnoreCase(term).stream()
            .map(Expediente::getNumero)
            .collect(Collectors.toList()));

        // 2. Buscar en el Lote Actual (Registros no procesados aún)
        List<RegistroMigracion> lotesRaw = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
        List<String> loteResults = lotesRaw.stream()
            .map(RegistroMigracion::getNumeroExpedienteExtraido)
            .filter(n -> n != null && n.toLowerCase().contains(term.toLowerCase()))
            .distinct()
            .limit(10)
            .collect(Collectors.toList());
        
        for (String num : loteResults) {
            if (!resultados.contains(num)) {
                resultados.add(num);
            }
        }

        return resultados;
    }
}
