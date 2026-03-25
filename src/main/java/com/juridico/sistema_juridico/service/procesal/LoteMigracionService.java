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
    @Autowired private PasswordEncoder passwordEncoder;

    // La gerencia que se usará por defecto al crear nuevos usuarios durante la migración
    private static final String GERENCIA_DEFAULT_MIGRACION = "Gerencia de Asuntos Laborales";

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
        try {
            LoteMigracion lote = loteRepository.findById(loteId).orElseThrow();
        List<RegistroMigracion> registros = registroRepository.findByLoteIdAndEstatusNot(loteId, "RECHAZADO");
        List<CatalogoMigracion> catalogosMap = catalogoRepository.findByLoteId(loteId);

        java.util.Map<String, CatalogoMigracion> lookup = new java.util.HashMap<>();
        for (CatalogoMigracion c : catalogosMap) {
            lookup.put(c.getTipoCatalogo() + "___" + c.getValorOriginal().toLowerCase(), c);
        }

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
                        
                        if (expediente.getMateria() == null && json.has("materia")) {
                            Materia m = resolveMateria(json.get("materia").asText(), lookup);
                            if (m != null) expediente.setMateria(m);
                        }
                        if (expediente.getGerencia() == null && json.has("gerencia") && !json.get("gerencia").isNull()) {
                            Gerencia g = resolveGerencia(json.get("gerencia").asText(), lookup);
                            if (g != null) expediente.setGerencia(g);
                        }
                        if (expediente.getOrganoJurisdiccional() == null && json.has("organoJurisdiccional")) {
                            OrganoJurisdiccional o = resolveOrgano(json.get("organoJurisdiccional").asText(), lookup);
                            if (o != null) expediente.setOrganoJurisdiccional(o);
                        }
                        if (expediente.getPartes() == null && json.has("partes") && !json.get("partes").isNull()) {
                            expediente.setPartes(json.get("partes").asText());
                        }
                        if (expediente.getAbogadoResponsable() == null && json.has("abogadoResponsable")) {
                            String nombreAbog = json.get("abogadoResponsable").asText();
                            Usuario u = resolveUsuario(nombreAbog, lookup);
                            if (u != null) {
                                expediente.setAbogadoResponsable(u);
                            } else {
                                if (nombreAbog.length() > 200) nombreAbog = nombreAbog.substring(0, 200);
                                expediente.setAbogadoResponsableNombre(nombreAbog);
                            }
                        }
                        if (expediente.getSede() == null && json.has("sede") && !json.get("sede").isNull()) {
                            String sedeStr = json.get("sede").asText();
                            if (sedeStr.length() > 150) sedeStr = sedeStr.substring(0, 150);
                            expediente.setSede(sedeStr);
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
                        
                        termino.setFechaVencimiento(parseDate(json.get("fechaVencimiento")));
                        termino.setFechaIngreso(parseDate(json.get("fechaIngreso")));
                        termino.setEstatusTermino(EstatusTermino.PROYECTISTA);
                        String acuseStr = json.has("acuse") ? json.get("acuse").asText() : "";
                        if (acuseStr.length() > 500) acuseStr = acuseStr.substring(0, 500);
                        termino.setAcuse(acuseStr);
                        
                        String acuseNombreStr = json.has("archivoWord") ? json.get("archivoWord").asText() : (json.has("acuseNombre") ? json.get("acuseNombre").asText() : "");
                        if (acuseNombreStr.length() > 500) acuseNombreStr = acuseNombreStr.substring(0, 500);
                        termino.setAcuseNombre(acuseNombreStr);

                        termino.setObservaciones(json.has("observaciones") ? json.get("observaciones").asText() : "");
                        
                        if (json.has("abogadoResponsable")) {
                            Usuario u = resolveUsuario(json.get("abogadoResponsable").asText(), lookup);
                            if (u != null) termino.setAbogadoResponsable(u);
                        }
                        
                        terminoRepository.save(termino);

                    } else if ("AUDIENCIA".equals(r.getTipoDato())) {
                        Audiencia audiencia = new Audiencia();
                        audiencia.setExpediente(expediente);
                        
                        audiencia.setFechaAudiencia(parseDate(json.get("fechaAudiencia")));
                        audiencia.setHoraAudiencia(parseTime(json.get("horaAudiencia")));
                        audiencia.setEstatusAudiencia(EstatusAudiencia.PENDIENTE);
                        audiencia.setSalaLugar(json.has("sede") ? json.get("sede").asText() : "");
                        String actaStr = json.has("actaDocumento") ? json.get("actaDocumento").asText() : "";
                        if (actaStr.length() > 500) actaStr = actaStr.substring(0, 500);
                        audiencia.setActaDocumento(actaStr);
                        audiencia.setObservaciones(json.has("observaciones") ? json.get("observaciones").asText() : "");
                        
                        if (json.has("tipoAudiencia")) {
                            TipoAudiencia ta = resolveTipoAudiencia(json.get("tipoAudiencia").asText(), lookup);
                            if (ta != null) audiencia.setTipoAudiencia(ta);
                        }
                        
                        if (json.has("abogadoCompareciente")) {
                            Usuario u = resolveUsuario(json.get("abogadoCompareciente").asText(), lookup);
                            if (u != null) audiencia.setAbogadoComparece(u);
                        }

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

    private Materia resolveMateria(String original, java.util.Map<String, CatalogoMigracion> lookup) {
        CatalogoMigracion c = lookup.get("MATERIA___" + original.trim().toLowerCase());
        if (c != null) {
            if (c.getIdMapeado() != null) return materiaRepository.findById(c.getIdMapeado().intValue()).orElse(null);
            if (c.getNuevoValor() != null) {
                String nombreFinal = c.getNuevoValor();
                if (nombreFinal.length() > 100) nombreFinal = nombreFinal.substring(0, 100);
                
                final String searchName = nombreFinal;
                Materia existente = materiaRepository.findByNombreIgnoreCase(searchName).orElse(null);
                if (existente == null) {
                    Materia m = new Materia(); m.setNombre(searchName); m.setActivo(true);
                    m.setGerencia(getFallbackGerencia());
                    existente = materiaRepository.save(m);
                }
                c.setIdMapeado(existente.getId().longValue());
                c.setNuevoValor(null);
                return existente;
            }
        }
        return null;
    }

    private Gerencia resolveGerencia(String original, java.util.Map<String, CatalogoMigracion> lookup) {
        CatalogoMigracion c = lookup.get("GERENCIA___" + original.trim().toLowerCase());
        if (c != null) {
            if (c.getIdMapeado() != null) return gerenciaRepository.findById(c.getIdMapeado().intValue()).orElse(null);
            if (c.getNuevoValor() != null) {
                String nombreFinal = c.getNuevoValor();
                if (nombreFinal.length() > 200) nombreFinal = nombreFinal.substring(0, 200);

                final String searchName = nombreFinal;
                Gerencia existente = gerenciaRepository.findByNombreIgnoreCase(searchName).orElse(null);
                if (existente == null) {
                    Gerencia g = new Gerencia(); g.setNombre(searchName); g.setActivo(true);
                    existente = gerenciaRepository.save(g);
                }
                c.setIdMapeado(existente.getId().longValue());
                c.setNuevoValor(null);
                return existente;
            }
        }
        return null;
    }

    private OrganoJurisdiccional resolveOrgano(String original, java.util.Map<String, CatalogoMigracion> lookup) {
        CatalogoMigracion c = lookup.get("ORGANO___" + original.trim().toLowerCase());
        if (c != null) {
            if (c.getIdMapeado() != null) return organoRepository.findById(c.getIdMapeado().intValue()).orElse(null);
            if (c.getNuevoValor() != null) {
                String nombreFinal = c.getNuevoValor();
                if (nombreFinal.length() > 300) nombreFinal = nombreFinal.substring(0, 300);

                final String searchName = nombreFinal;
                OrganoJurisdiccional existente = organoRepository.findByNombreIgnoreCase(searchName).orElse(null);
                if (existente == null) {
                    OrganoJurisdiccional o = new OrganoJurisdiccional(); o.setNombre(searchName); o.setActivo(true);
                    existente = organoRepository.save(o);
                }
                c.setIdMapeado(existente.getId().longValue());
                c.setNuevoValor(null);
                return existente;
            }
        }
        return null;
    }

    private TipoAudiencia resolveTipoAudiencia(String original, java.util.Map<String, CatalogoMigracion> lookup) {
        CatalogoMigracion c = lookup.get("TIPO_AUDIENCIA___" + original.trim().toLowerCase());
        if (c != null) {
            if (c.getIdMapeado() != null) return tipoAudienciaRepository.findById(c.getIdMapeado().intValue()).orElse(null);
            if (c.getNuevoValor() != null) {
                String nombreFinal = c.getNuevoValor();
                if (nombreFinal.length() > 100) nombreFinal = nombreFinal.substring(0, 100);

                final String searchName = nombreFinal;
                TipoAudiencia existente = tipoAudienciaRepository.findByNombreIgnoreCase(searchName).orElse(null);
                if (existente == null) {
                    TipoAudiencia t = new TipoAudiencia(); t.setNombre(searchName); t.setActivo(true);
                    existente = tipoAudienciaRepository.save(t);
                }
                c.setIdMapeado(existente.getId().longValue());
                c.setNuevoValor(null);
                return existente;
            }
        }
        return null;
    }

    private Usuario resolveUsuario(String original, java.util.Map<String, CatalogoMigracion> lookup) {
        if (original == null || original.trim().isEmpty()) return null;
        CatalogoMigracion c = lookup.get("USUARIO___" + original.trim().toLowerCase());
        if (c != null && c.getIdMapeado() != null) {
            return usuarioRepository.findById(c.getIdMapeado().intValue()).orElse(null);
        }
        // Si no hay mapeo, buscar por nombre o crear uno nuevo
        return crearOEncontrarAbogado(original.trim());
    }

    private Usuario crearOEncontrarAbogado(String nombreCompleto) {
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
        // Si el email ya existe (raro pero posible), añadir sufijo numérico
        int intento = 0;
        while (usuarioRepository.existsByEmail(email)) {
            intento++;
            email = emailBase + intento + "@juridico.com";
        }

        // 3. Generar contraseña temporal: "tempora" + 6 dígitos aleatorios
        String passwordRaw = "tempora" + String.format("%06d", new java.util.Random().nextInt(999999));
        String passwordHash = passwordEncoder.encode(passwordRaw);

        // 4. Obtener la Gerencia por defecto para migración
        Gerencia gerencia = gerenciaRepository.findByNombreIgnoreCase(GERENCIA_DEFAULT_MIGRACION)
            .orElse(getFallbackGerencia());

        // 5. Obtener todas las materias de esa gerencia para asignarlas
        java.util.Set<Materia> materiasAutorizadas = new java.util.HashSet<>(
            materiaRepository.findByActivoTrueOrderByNombreAsc());

        // 6. Construir y guardar el usuario
        Usuario nuevoAbogado = new Usuario();
        nuevoAbogado.setNombreCompleto(nombreCompleto.length() > 200 ? nombreCompleto.substring(0, 200) : nombreCompleto);
        nuevoAbogado.setEmail(email);
        nuevoAbogado.setPassword(passwordHash);
        nuevoAbogado.setRol(RolUsuario.ABOGADO);
        nuevoAbogado.setGerencia(gerencia);
        nuevoAbogado.setActivo(true);
        nuevoAbogado.setForcePasswordChange(true);
        nuevoAbogado.setMaterias(materiasAutorizadas);

        Usuario guardado = usuarioRepository.save(nuevoAbogado);
        System.out.println(">>> Migracion: Usuario creado automaticamente: " + email + " / pass: " + passwordRaw);
        return guardado;
    }

    private LocalDate parseDate(JsonNode dateNode) {
        if (dateNode == null || dateNode.isNull() || dateNode.asText().trim().isEmpty()) return LocalDate.now();
        try {
            return LocalDate.parse(dateNode.asText());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
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