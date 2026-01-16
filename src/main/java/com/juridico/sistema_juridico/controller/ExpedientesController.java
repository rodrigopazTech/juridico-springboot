package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/expedientes")
public class ExpedientesController {

    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstadoRepository estadoRepository;


    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Gestión de Expedientes");

        
        // Catálogos de Base de Datos
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("listaTipos", tipoExpedienteRepository.findAll());
        model.addAttribute("listaOrganos", organoRepository.findAll());
        model.addAttribute("listaAbogados", usuarioRepository.findAll()); 
        model.addAttribute("listaEstados", estadoRepository.findAllByOrderByNombreAsc());

        // Enums (Valores fijos en código)
        model.addAttribute("listaPrioridades", Prioridad.values());
        
        model.addAttribute("expedientes", expedienteRepository.findAll());

        return "views/expedientes/index";
    }

    @PostMapping("/guardar")
    public String guardarExpediente(@ModelAttribute Expediente expediente, RedirectAttributes flash) {
        try {
            if (expediente.getId() == null) {
                expediente.setCreatedAt(LocalDateTime.now());
                expediente.setEtapaProcesal(EtapaProcesal.TRAMITE); 
            }
            expediente.setUpdatedAt(LocalDateTime.now());

            // 2. (Temporal) Asignar un usuario por defecto si viene nulo
            // En el futuro, aquí obtendrás el usuario logueado desde el SecurityContext
            if (expediente.getAbogadoResponsable() == null) {
                 // Puedes buscar el admin o dejarlo nulo si tu BD lo permite (pero tu entidad dice que puede ser nulo en la relación, así que está bien)
            }

            // 3. Guardar en Base de Datos
            expedienteRepository.save(expediente);

            flash.addFlashAttribute("success", "Expediente guardado correctamente.");
            
        } catch (Exception e) {
            e.printStackTrace(); // Mira la consola para ver el error real si falla
            flash.addFlashAttribute("error", "Error al guardar el expediente: " + e.getMessage());
        }

        return "redirect:/expedientes";
    }
}