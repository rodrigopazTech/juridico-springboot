package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.Entity.catalogo.Estado;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoExpediente;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EstadoRepository estadoRepository;

    @Override
    @Transactional 
    public void run(String... args) throws Exception {
        
        System.out.println(" Iniciando sembrado de datos (Data Seeding)...");

        // 1. USUARIOS (Admin y Abogado)
        Usuario abogadoDemo = null;
        if (usuarioRepository.count() == 0) {
            // Admin
            Usuario admin = Usuario.builder()
                    .nombreCompleto("Administrador del Sistema")
                    .email("admin@juridico.com")
                    .passwordHash(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.DIRECCION)
                    .activo(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            usuarioRepository.save(admin);

            // Abogado Demo
            abogadoDemo = Usuario.builder()
                    .nombreCompleto("Lic. Juan Pérez")
                    .email("juan@juridico.com")
                    .passwordHash(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.ABOGADO) // Asegúrate de tener este ROL en tu Enum
                    .activo(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            usuarioRepository.save(abogadoDemo);
            
            System.out.println(" Usuarios creados.");
        } else {
            abogadoDemo = usuarioRepository.findByEmail("juan@juridico.com").orElse(null);
        }

        // 2. GERENCIAS
        Gerencia gCivil = null;
        if (gerenciaRepository.count() == 0) {
            gCivil = Gerencia.builder().nombre("Gerencia Civil").descripcion("Litigios civiles y mercantiles").build();
            Gerencia gPenal = Gerencia.builder().nombre("Gerencia Penal").descripcion("Defensa penal corporativa").build();
            Gerencia gLaboral = Gerencia.builder().nombre("Gerencia Laboral").descripcion("Conflictos obrero-patronales").build();
            
            gerenciaRepository.saveAll(List.of(gCivil, gPenal, gLaboral));
            System.out.println("Gerencias creadas.");
        } else {
            gCivil = gerenciaRepository.findAll().get(0); // Recuperamos una para el ejemplo
        }

        // 3. MATERIAS (Ligadas a Gerencias)
        Materia mArrendamiento = null;
        if (materiaRepository.count() == 0) {
            Gerencia civil = gerenciaRepository.findByNombre("Gerencia Civil").orElse(gCivil);
            Gerencia laboral = gerenciaRepository.findByNombre("Gerencia Laboral").orElse(null);

            mArrendamiento = Materia.builder().nombre("Arrendamiento Inmobiliario").gerencia(civil).build();
            Materia mMercantil = Materia.builder().nombre("Juicio Mercantil").gerencia(civil).build();
            Materia mDespido = Materia.builder().nombre("Despido Injustificado").gerencia(laboral).build();

            materiaRepository.saveAll(List.of(mArrendamiento, mMercantil, mDespido));
            System.out.println(" Materias creadas.");
        } else {
             mArrendamiento = materiaRepository.findAll().get(0);
        }

        // 4. TIPOS DE EXPEDIENTE
        TipoExpediente tOrdinario = null;
        if (tipoExpedienteRepository.count() == 0) {
            tOrdinario = TipoExpediente.builder().nombre("Juicio Ordinario").build();
            TipoExpediente tEjecutivo = TipoExpediente.builder().nombre("Juicio Ejecutivo").build();
            TipoExpediente tAmparo = TipoExpediente.builder().nombre("Amparo Indirecto").build();
            
            tipoExpedienteRepository.saveAll(List.of(tOrdinario, tEjecutivo, tAmparo));
            System.out.println("Tipos de Expediente creados.");
        } else {
            tOrdinario = tipoExpedienteRepository.findAll().get(0);
        }

        // 5. ORGANOS JURISDICCIONALES
        OrganoJurisdiccional oJuzgado = null;
        if (organoRepository.count() == 0) {
            oJuzgado = OrganoJurisdiccional.builder().nombre("Juzgado 34 de lo Civil CDMX").tipo("Juzgado").sede("CDMX").build();
            OrganoJurisdiccional oTribunal = OrganoJurisdiccional.builder().nombre("Primer Tribunal Colegiado").tipo("Tribunal").sede("CDMX").build();
            
            organoRepository.saveAll(List.of(oJuzgado, oTribunal));
            System.out.println("Órganos creados.");
        } else {
            oJuzgado = organoRepository.findAll().get(0);
        }

        // 6. EXPEDIENTE DE PRUEBA 
        if (expedienteRepository.count() == 0 && abogadoDemo != null && gCivil != null) {
            Expediente exp = Expediente.builder()
                    .numero("EXP-2026/001")
                    .descripcion("Incumplimiento de contrato de arrendamiento local comercial zona centro.")
                    .prioridad(Prioridad.ALTA)
                    .etapaProcesal(EtapaProcesal.TRAMITE) 
                    .gerencia(gCivil)
                    .materia(mArrendamiento)
                    .tipoExpediente(tOrdinario)
                    .organoJurisdiccional(oJuzgado)
                    .abogadoResponsable(abogadoDemo)
                    .abogadoResponsableNombre(abogadoDemo.getNombreCompleto())
                    .partes("Inmobiliaria SA de CV vs. Juan Pérez")
                    .sede("CDMX")
                    .createdAt(LocalDateTime.now())
                    .build();

            expedienteRepository.save(exp);
            System.out.println("Expediente de prueba creado: EXP-2026/001");
        }
        // 7. CATÁLOGO DE ESTADOS (MÉXICO)
        if (estadoRepository.count() == 0) {
            List<String> estadosMexico = List.of(
                "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", 
                "Chiapas", "Chihuahua", "Ciudad de México", "Coahuila", "Colima", 
                "Durango", "Estado de México", "Guanajuato", "Guerrero", "Hidalgo", 
                "Jalisco", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", 
                "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", 
                "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"
            );

            List<Estado> listaEstados = estadosMexico.stream()
                    .map(nombre -> Estado.builder().nombre(nombre).build())
                    .toList();

            estadoRepository.saveAll(listaEstados);
            System.out.println("Catálogo de 32 Estados insertado.");
        }
        System.out.println("Carga de datos inicial completada correctamente.");
    }
}