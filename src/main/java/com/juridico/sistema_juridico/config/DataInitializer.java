package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.Entity.catalogo.Estado;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.catalogo.TipoExpediente;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private EstadoRepository estadoRepository;
    @Autowired private TipoAudienciaRepository tipoAudienciaRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        
        System.out.println("🌱 Iniciando carga de datos (Semilla)...");

        // 1. GERENCIAS Y MATERIAS
        if (gerenciaRepository.count() == 0) {
            
            // --- Gerencia 1: Civil, Mercantil, etc. ---
            Gerencia gCivil = Gerencia.builder()
                    .nombre("Gerencia Civil, Mercantil, Fiscal y Administrativo")
                    .descripcion("Atención de asuntos civiles, mercantiles y administrativos.")
                    .activo(true)
                    .build();
            gCivil = gerenciaRepository.save(gCivil);

            materiaRepository.saveAll(List.of(
                Materia.builder().nombre("Civil").gerencia(gCivil).activo(true).build(),
                Materia.builder().nombre("Mercantil").gerencia(gCivil).activo(true).build(),
                Materia.builder().nombre("Fiscal").gerencia(gCivil).activo(true).build(),
                Materia.builder().nombre("Administrativo").gerencia(gCivil).activo(true).build()
            ));

            // --- Gerencia 2: Laboral y Penal ---
            Gerencia gLaboral = Gerencia.builder()
                    .nombre("Gerencia Laboral y Penal")
                    .descripcion("Atención de conflictos laborales y defensa penal.")
                    .activo(true)
                    .build();
            gLaboral = gerenciaRepository.save(gLaboral);

            materiaRepository.saveAll(List.of(
                Materia.builder().nombre("Laboral").gerencia(gLaboral).activo(true).build(),
                Materia.builder().nombre("Penal").gerencia(gLaboral).activo(true).build()
            ));

            // --- Gerencia 3: Transparencia y Amparo ---
            Gerencia gTransparencia = Gerencia.builder()
                    .nombre("Gerencia Transparencia y Amparo")
                    .descripcion("Atención de solicitudes de transparencia y juicios de amparo.")
                    .activo(true)
                    .build();
            gTransparencia = gerenciaRepository.save(gTransparencia);

            materiaRepository.saveAll(List.of(
                Materia.builder().nombre("Amparo").gerencia(gTransparencia).activo(true).build(),
                Materia.builder().nombre("Transparencia").gerencia(gTransparencia).activo(true).build()
            ));

            System.out.println("✅ Gerencias y Materias inicializadas correctamente.");
        }

       // 2. USUARIOS
        if (usuarioRepository.count() == 0) {
            Gerencia gerenciaDefault = gerenciaRepository.findAll().stream()
                .filter(g -> g.getNombre().contains("Civil"))
                .findFirst()
                .orElse(null);

            // 1. NIVEL ALTO (Ve todo)
            Usuario director = Usuario.builder()
                    .nombreCompleto("Director General")
                    .email("director@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.DIRECCION)
                    .activo(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Usuario subdirector = Usuario.builder()
                    .nombreCompleto("Subdirector Jurídico")
                    .email("subdirector@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.SUBDIRECCION)
                    .activo(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            // 2. NIVEL MEDIO (Gerente de área)
            Usuario gerente = Usuario.builder()
                    .nombreCompleto("Gerente Civil")
                    .email("gerente@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.GERENTE)
                    .activo(true)
                    .gerencia(gerenciaDefault)
                    .createdAt(LocalDateTime.now())
                    .build();

            // 3. NIVEL INTERMEDIO (Jefe de Depto)
            Usuario jefe = Usuario.builder()
                    .nombreCompleto("Lic. Ana López (Jefa)")
                    .email("jefe@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.JEFE_DEPTO)
                    .activo(true)
                    .gerencia(gerenciaDefault) // Asignado a la misma gerencia que los demás
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // 4. NIVEL OPERATIVO (Abogado)
            Usuario abogado = Usuario.builder()
                    .nombreCompleto("Lic. Juan Pérez")
                    .email("abogado@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.ABOGADO)
                    .activo(true)
                    .gerencia(gerenciaDefault)
                    .createdAt(LocalDateTime.now())
                    .build();

            usuarioRepository.saveAll(List.of(director, subdirector, gerente, abogado, jefe));
            System.out.println("✅ Usuarios de prueba creados.");
        }

        // 3. TIPOS DE EXPEDIENTE
        if (tipoExpedienteRepository.count() == 0) {
            tipoExpedienteRepository.saveAll(List.of(
                TipoExpediente.builder().nombre("Juicio Ordinario Civil").build(),
                TipoExpediente.builder().nombre("Juicio Ejecutivo Mercantil").build(),
                TipoExpediente.builder().nombre("Juicio Oral Mercantil").build(),
                TipoExpediente.builder().nombre("Amparo Indirecto").build(),
                TipoExpediente.builder().nombre("Amparo Directo").build(),
                TipoExpediente.builder().nombre("Procedimiento Laboral").build(),
                TipoExpediente.builder().nombre("Carpeta de Investigación").build()
            ));
            System.out.println("✅ Tipos de Expediente creados.");
        }

        // 4. ÓRGANOS JURISDICCIONALES (¡LISTA AMPLIADA!)
        if (organoRepository.count() == 0) {
            List<OrganoJurisdiccional> organos = List.of(
                // Civiles CDMX
                OrganoJurisdiccional.builder().nombre("Juzgado 34 de lo Civil CDMX").tipo("Juzgado").sede("CDMX").build(),
                OrganoJurisdiccional.builder().nombre("Juzgado 12 de lo Civil CDMX").tipo("Juzgado").sede("CDMX").build(),
                
                // Familiares CDMX
                OrganoJurisdiccional.builder().nombre("Juzgado 5 de lo Familiar CDMX").tipo("Juzgado").sede("CDMX").build(),
                
                // Federales (Distrito y Colegiados)
                OrganoJurisdiccional.builder().nombre("Juzgado Cuarto de Distrito en Materia Civil").tipo("Juzgado Federal").sede("CDMX").build(),
                OrganoJurisdiccional.builder().nombre("Juzgado Sexto de Distrito en Materia Administrativa").tipo("Juzgado Federal").sede("CDMX").build(),
                OrganoJurisdiccional.builder().nombre("Primer Tribunal Colegiado en Materia Civil").tipo("Tribunal Colegiado").sede("CDMX").build(),
                OrganoJurisdiccional.builder().nombre("Tercer Tribunal Colegiado en Materia de Trabajo").tipo("Tribunal Colegiado").sede("CDMX").build(),
                
                // Laborales
                OrganoJurisdiccional.builder().nombre("Tribunal Laboral Federal de Asuntos Individuales").tipo("Tribunal Laboral").sede("CDMX").build(),
                
                // Estado de México (Ejemplos)
                OrganoJurisdiccional.builder().nombre("Juzgado Primero Civil de Tlalnepantla").tipo("Juzgado").sede("EdoMex").build(),
                OrganoJurisdiccional.builder().nombre("Juzgado de Control de Ecatepec").tipo("Juzgado Penal").sede("EdoMex").build()
            );
            
            organoRepository.saveAll(organos);
            System.out.println("✅ Órganos Jurisdiccionales creados (Lista Ampliada).");
        }

        // 5. CATÁLOGO DE ESTADOS
        if (estadoRepository.count() == 0) {
            List<String> estadosMexico = List.of(
                "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", 
                "Chiapas", "Chihuahua", "Ciudad de México", "Coahuila", "Colima", 
                "Durango", "Estado de México", "Guanajuato", "Guerrero", "Hidalgo", 
                "Jalisco", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", 
                "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", 
                "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"
            );

            List<Estado> listaEstados = new ArrayList<>();
            for (String nombre : estadosMexico) {
                listaEstados.add(Estado.builder().nombre(nombre).build());
            }
            estadoRepository.saveAll(listaEstados);
            System.out.println("✅ Catálogo de Estados cargado.");
        }

        // 6. CATÁLOGO DE TIPOS DE AUDIENCIA
        if (tipoAudienciaRepository.count() == 0) {
            List<TipoAudiencia> tipos = List.of(
                TipoAudiencia.builder().nombre("Inicial").descripcion("Audiencia inicial del proceso").activo(true).build(),
                TipoAudiencia.builder().nombre("Intermedia").descripcion("Etapa intermedia").activo(true).build(),
                TipoAudiencia.builder().nombre("Juicio Oral").descripcion("Desahogo de pruebas").activo(true).build(),
                TipoAudiencia.builder().nombre("Constitucional").descripcion("Audiencia de Amparo").activo(true).build(),
                TipoAudiencia.builder().nombre("Incidental").descripcion("Para resolver incidentes").activo(true).build(),
                TipoAudiencia.builder().nombre("Conciliación").descripcion("Medios alternativos").activo(true).build(),
                TipoAudiencia.builder().nombre("Ejecución").descripcion("Vigilancia de sentencia").activo(true).build()
            );
            
            tipoAudienciaRepository.saveAll(tipos);
            System.out.println("✅ Tipos de Audiencia cargados automáticamente.");
        }
    }
}
