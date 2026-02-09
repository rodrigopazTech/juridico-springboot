package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.Entity.catalogo.Estado;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoExpediente;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
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
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private EstadoRepository estadoRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        
        // 1. GERENCIAS Y MATERIAS - Siempre verificar y crear si no existen
        inicializarGerenciasYMaterias();

        // 2. USUARIOS - Solo crear si no existen
        inicializarUsuarios();

        // 3. TIPOS DE EXPEDIENTE - Solo crear si no existen
        if (tipoExpedienteRepository.count() == 0) {
            TipoExpediente tJuicio = TipoExpediente.builder().nombre("Juicio Ordinario").build();
            TipoExpediente tEjecutivo = TipoExpediente.builder().nombre("Juicio Ejecutivo Mercantil").build();
            TipoExpediente tAmparo = TipoExpediente.builder().nombre("Amparo Indirecto").build();
            TipoExpediente tLaboral = TipoExpediente.builder().nombre("Conflicto Laboral").build();
            
            tipoExpedienteRepository.saveAll(List.of(tJuicio, tEjecutivo, tAmparo, tLaboral));
            System.out.println("✅ Tipos de Expediente creados.");
        }

        // 4. ÓRGANOS JURISDICCIONALES - Solo crear si no existen
        if (organoRepository.count() == 0) {
            OrganoJurisdiccional oJuzgado = OrganoJurisdiccional.builder()
                    .nombre("Juzgado 34 de lo Civil CDMX")
                    .tipo("Juzgado")
                    .sede("CDMX")
                    .build();
            
            OrganoJurisdiccional oTribunal = OrganoJurisdiccional.builder()
                    .nombre("Primer Tribunal Colegiado")
                    .tipo("Tribunal")
                    .sede("CDMX")
                    .build();
            
            OrganoJurisdiccional oJLCA = OrganoJurisdiccional.builder()
                    .nombre("Junta de Conciliación y Arbitraje")
                    .tipo("Junta")
                    .sede("CDMX")
                    .build();

            organoRepository.saveAll(List.of(oJuzgado, oTribunal, oJLCA));
            System.out.println("✅ Órganos creados.");
        }

        // 5. CATÁLOGO DE ESTADOS - Solo crear si no existen
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
    }

    /**
     * Inicializa las gerencias y materias, creando las que no existan
     */
    @Transactional
    public void inicializarGerenciasYMaterias() {
        
        // Gerencia 1: Civil, Mercantil, Fiscal y Administrativo
        Optional<Gerencia> optG1 = gerenciaRepository.findByNombre("Civil, Mercantil, Fiscal y Administrativo");
        Gerencia gCivil = optG1.orElseGet(() -> {
            Gerencia g = Gerencia.builder()
                    .nombre("Civil, Mercantil, Fiscal y Administrativo")
                    .descripcion("Atención a juicios civiles, mercantiles, fiscales y administrativos.")
                    .activo(true)
                    .build();
            return gerenciaRepository.save(g);
        });

        // Crear materias para Gerencia 1 si no existen
        crearMateriaSiNoExiste("Civil", gCivil);
        crearMateriaSiNoExiste("Mercantil", gCivil);
        crearMateriaSiNoExiste("Fiscal", gCivil);
        crearMateriaSiNoExiste("Administrativo", gCivil);

        // Gerencia 2: Laboral y Penal
        Optional<Gerencia> optG2 = gerenciaRepository.findByNombre("Laboral y Penal");
        Gerencia gLaboral = optG2.orElseGet(() -> {
            Gerencia g = Gerencia.builder()
                    .nombre("Laboral y Penal")
                    .descripcion("Atención a demandas laborales y procesos penales.")
                    .activo(true)
                    .build();
            return gerenciaRepository.save(g);
        });

        // Crear materias para Gerencia 2
        crearMateriaSiNoExiste("Laboral", gLaboral);
        crearMateriaSiNoExiste("Penal", gLaboral);

        // Gerencia 3: Transparencia y Amparo
        Optional<Gerencia> optG3 = gerenciaRepository.findByNombre("Transparencia y Amparo");
        Gerencia gTransparencia = optG3.orElseGet(() -> {
            Gerencia g = Gerencia.builder()
                    .nombre("Transparencia y Amparo")
                    .descripcion("Atención a solicitudes de transparencia y juicios de amparo.")
                    .activo(true)
                    .build();
            return gerenciaRepository.save(g);
        });

        // Crear materias para Gerencia 3
        crearMateriaSiNoExiste("Transparencia", gTransparencia);
        crearMateriaSiNoExiste("Amparo", gTransparencia);

        System.out.println("✅ Gerencias y Materias verificadas/creadas correctamente.");
        System.out.println("   - Civil, Mercantil, Fiscal y Administrativo");
        System.out.println("   - Laboral y Penal");
        System.out.println("   - Transparencia y Amparo");
    }

    /**
     * Crea una materia si no existe para la gerencia indicada
     */
    private void crearMateriaSiNoExiste(String nombreMateria, Gerencia gerencia) {
        materiaRepository.findByNombreAndGerenciaId(nombreMateria, gerencia.getId())
                .orElseGet(() -> {
                    Materia m = Materia.builder()
                            .nombre(nombreMateria)
                            .gerencia(gerencia)
                            .activo(true)
                            .build();
                    return materiaRepository.save(m);
                });
    }

    /**
     * Inicializa usuarios de prueba si no existen
     */
    @Transactional
    public void inicializarUsuarios() {
        if (usuarioRepository.count() > 0) {
            System.out.println("ℹ️ Usuarios ya existen, omitiendo creación.");
            return;
        }
        
        // Obtener todas las gerencias para asignar gerentes
        List<Gerencia> gerencias = gerenciaRepository.findAll();

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

        // 2. NIVEL MEDIO (Gerentes - uno por cada gerencia)
        Usuario gerenteCivil = Usuario.builder()
                .nombreCompleto("Gerente Civil, Mercantil, Fiscal y Administrativo")
                .email("gerente.civil@juridico.com")
                .password(passwordEncoder.encode("12345"))
                .rol(RolUsuario.GERENTE)
                .activo(true)
                .gerencia(gerencias.get(0))
                .createdAt(LocalDateTime.now())
                .build();
        
        Usuario gerenteLaboral = Usuario.builder()
                .nombreCompleto("Gerente Laboral y Penal")
                .email("gerente.laboral@juridico.com")
                .password(passwordEncoder.encode("12345"))
                .rol(RolUsuario.GERENTE)
                .activo(true)
                .gerencia(gerencias.get(1))
                .createdAt(LocalDateTime.now())
                .build();
        
        Usuario gerenteTransparencia = Usuario.builder()
                .nombreCompleto("Gerente Transparencia y Amparo")
                .email("gerente.transparencia@juridico.com")
                .password(passwordEncoder.encode("12345"))
                .rol(RolUsuario.GERENTE)
                .activo(true)
                .gerencia(gerencias.get(2))
                .createdAt(LocalDateTime.now())
                .build();

        // 3. NIVEL OPERATIVO (Abogados)
        Usuario abogadoCivil = Usuario.builder()
                .nombreCompleto("Abogado Civil")
                .email("abogado.civil@juridico.com")
                .password(passwordEncoder.encode("12345"))
                .rol(RolUsuario.ABOGADO)
                .activo(true)
                .gerencia(gerencias.get(0))
                .createdAt(LocalDateTime.now())
                .build();
        
        Usuario abogadoLaboral = Usuario.builder()
                .nombreCompleto("Abogado Laboral")
                .email("abogado.laboral@juridico.com")
                .password(passwordEncoder.encode("12345"))
                .rol(RolUsuario.ABOGADO)
                .activo(true)
                .gerencia(gerencias.get(1))
                .createdAt(LocalDateTime.now())
                .build();

        usuarioRepository.saveAll(List.of(director, subdirector, 
                gerenteCivil, gerenteLaboral, gerenteTransparencia,
                abogadoCivil, abogadoLaboral));
        System.out.println("✅ Usuarios de prueba creados.");
    }
}

