package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TerminoImportService {

    @Autowired private TerminoRepository terminoRepository;
    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;    
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;


    @Transactional
    public void importar(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // Leer la primera hoja

            for (Row row : sheet) {
                // Saltar la primera fila (encabezados)
                if (row.getRowNum() == 0) continue;

                String numeroExp = getStringValue(row.getCell(0));
                
                // Si la fila está vacía, saltarla
                if (numeroExp == null || numeroExp.trim().isEmpty()) continue;

                String materiaStr = getStringValue(row.getCell(1));
                String organoStr = getStringValue(row.getCell(2));
                String emailAbogado = getStringValue(row.getCell(3));
                String actuacion = getStringValue(row.getCell(4));
                LocalDate fechaVencimiento = getDateValue(row.getCell(5));
                String prioridadStr = getStringValue(row.getCell(6));

                // ==========================================
                // MAGIA UPSERT: Buscar o Crear Expediente
                // ==========================================
                Expediente expediente = expedienteRepository.findByNumeroIgnoreCase(numeroExp).orElse(null);
                
                Usuario abogado = null;
                if (emailAbogado != null && !emailAbogado.isEmpty()) {
                    abogado = usuarioRepository.findByEmail(emailAbogado).orElse(null);
                }
if (expediente == null) {
                    // El expediente NO existe, lo creamos "al vuelo"
                    expediente = new Expediente();
                    expediente.setNumero(numeroExp);
                    expediente.setOrganoJurisdiccionalTexto(organoStr);
                    expediente.setAbogadoResponsable(abogado);
                    expediente.setEtapaProcesal(EtapaProcesal.TRAMITE); 
                    expediente.setDescripcion("Expediente autogenerado a partir de importación de reporte."); 
                    
                    // --- 1. Asignar Gerencia Automáticamente ---
                    Gerencia gerenciaAsignada = null;
                    if (abogado != null && abogado.getGerencia() != null) {
                        gerenciaAsignada = abogado.getGerencia(); 
                    } else {
                        gerenciaAsignada = gerenciaRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("Error: No hay gerencias registradas."));
                    }
                    expediente.setGerencia(gerenciaAsignada);
                    
                    // --- 2. NUEVA SOLUCIÓN: Asignar Materia Inteligente ---
                    Materia materiaAsignada = null;
                    if (materiaStr != null && !materiaStr.isEmpty()) {
                        // Buscamos si el texto del Excel coincide con alguna materia en BD
                        materiaAsignada = materiaRepository.findAll().stream()
                            .filter(m -> m.getNombre().equalsIgnoreCase(materiaStr.trim()))
                            .findFirst().orElse(null);
                    }
                    if (materiaAsignada == null) {
                        // Fallback: Si no coincide o viene vacía, tomamos la primera para que no falle
                        materiaAsignada = materiaRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("Error: No hay materias registradas en el sistema."));
                    }
                    expediente.setMateria(materiaAsignada);
                    
                    expediente.setCreatedAt(LocalDateTime.now());
                    
                    expediente = expedienteRepository.save(expediente);
                }

                // ==========================================
                // CREAR EL TÉRMINO Y VINCULARLO
                // ==========================================
                Termino termino = new Termino();
                termino.setExpediente(expediente);
                termino.setActuacion(actuacion);
                termino.setFechaVencimiento(fechaVencimiento);
                termino.setAbogadoResponsable(abogado);
                termino.setFechaIngreso(LocalDate.now());
                termino.setEstatusTermino("Proyectista"); // Inicia el flujo normal
                termino.setCreatedAt(LocalDateTime.now());

                // Manejo seguro de la prioridad
                try {
                    if(prioridadStr != null) {
                        termino.setPrioridad(Prioridad.valueOf(prioridadStr.toUpperCase()));
                    } else {
                        termino.setPrioridad(Prioridad.MEDIA);
                    }
                } catch (IllegalArgumentException e) {
                    termino.setPrioridad(Prioridad.MEDIA);
                }

                terminoRepository.save(termino);
            }
        }
    }

    // --- Métodos Helper para evitar nulos y errores de Apache POI ---
    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return null;
    }

   private LocalDate getDateValue(Cell cell) {
        if (cell == null) return null;
        
        // 1. Si Excel lo reconoce como formato de fecha nativo
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } 
        // 2. Si Excel lo reconoce como Texto (ej. "2026-03-10" o "10/03/2026")
        else if (cell.getCellType() == CellType.STRING) {
            String fechaTexto = cell.getStringCellValue().trim();
            try {
                // Intenta formato AAAA-MM-DD
                return LocalDate.parse(fechaTexto);
            } catch (Exception e1) {
                try {
                    // Intenta formato DD/MM/AAAA
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    return LocalDate.parse(fechaTexto, formatter);
                } catch (Exception e2) {
                    throw new RuntimeException("No se pudo leer la fecha: " + fechaTexto + ". Usa el formato AAAA-MM-DD.");
                }
            }
        }
        return null;
    }
}