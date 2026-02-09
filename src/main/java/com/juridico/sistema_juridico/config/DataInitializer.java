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
        
        // 1. GERENCIAS Y MATERIAS (Estructura Organizacional)
        if (gerenciaRepository.count() == 0) {
            
            // Gerencia 1: Civil, Mercantil, Fiscal y Administrativo
            Gerencia gCivil = Gerencia.builder()
                    .nombre("Civil, Mercantil, Fiscal y Administrativo")
                    .descripcion("Atención a juicios civiles, mercantiles, fiscales y administrativos.")
                    .activo(true)
                    .build();
            gCivil = gerenciaRepository.save(gCivil);

            // Materias para Gerencia 1
            Materia mCivil = Materia.builder().nombre("Civil").gerencia(gCivil).activo(true).build();
            Materia mMercantil = Materia.builder().nombre("Mercantil").gerencia(gCivil).activo(true).build();
            Materia mFiscal = Materia.builder().nombre("Fiscal").gerencia(gCivil).activo(true).build();
            Materia mAdministrativo = Materia.builder().nombre("Administrativo").gerencia(gCivil).activo(true).build();
            materiaRepository.saveAll(List.of(mCivil, mMercantil, mFiscal, mAdministrativo));

            // Gerencia 2: Laboral y Penal
            Gerencia gLaboral = Gerencia.builder()
                    .nombre("Laboral y Penal")
                    .descripcion("Atención a demandas laborales y procesos penales.")
                    .activo(true)
                    .build();
            gLaboral = gerenciaRepository.save(gLaboral);

            // Materias para Gerencia 2
            Materia mLaboral = Materia.builder().nombre("Laboral").gerencia(gLaboral).activo(true).build();
            Materia mPenal = Materia.builder().nombre("Penal").gerencia(gLaboral).activo(true).build();
            materiaRepository.saveAll(List.of(mLaboral, mPenal));

            // Gerencia 3: Transparencia y Amparo
            Gerencia gTransparencia = Gerencia.builder()
                    .nombre("Transparencia y Amparo")
                    .descripcion("Atención a solicitudes de transparencia y juicios de amparo.")
                    .activo(true)
                    .build();
            gTransparencia = gerenciaRepository.save(gTransparencia);

            // Materias para Gerencia 3
            Materia mTransparencia = Materia.builder().nombre("Transparencia").gerencia(gTransparencia).activo(true).build();
            Materia mAmparo = Materia.builder().nombre("Amparo").gerencia(gTransparencia).activo(true).build();
            materiaRepository.saveAll(List.of(mTransparencia, mAmparo));

            System.out.println("✅ Gerencias y Materias inicializadas correctamente:");
            System.out.println("   - Civil, Mercantil, Fiscal y Administrativo");
            System.out.println("   - Laboral y Penal");
            System.out.println("   - Transparencia y Amparo");
        }

       // 2. USUARIOS
        if (usuarioRepository.count() == 0) {
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
                    .gerencia(gerencias.get(0)) // Civil, Mercantil, Fiscal y Administrativo
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Usuario gerenteLaboral = Usuario.builder()
                    .nombreCompleto("Gerente Laboral y Penal")
                    .email("gerente.laboral@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.GERENTE)
                    .activo(true)
                    .gerencia(gerencias.get(1)) // Laboral y Penal
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Usuario gerenteTransparencia = Usuario.builder()
                    .nombreCompleto("Gerente Transparencia y Amparo")
                    .email("gerente.transparencia@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.GERENTE)
                    .activo(true)
                    .gerencia(gerencias.get(2)) // Transparencia y Amparo
                    .createdAt(LocalDateTime.now())
                    .build();

            // 3. NIVEL OPERATIVO (Abogados - asignados a gerencias)
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
            System.out.println("✅ Usuarios de prueba creados para todos los roles.");
        }

        // 3. TIPOS DE EXPEDIENTE
        if (tipoExpedienteRepository.count() == 0) {
            TipoExpediente tJuicio = TipoExpediente.builder().nombre("Juicio Ordinario").build();
            TipoExpediente tEjecutivo = TipoExpediente.builder().nombre("Juicio Ejecutivo Mercantil").build();
            TipoExpediente tAmparo = TipoExpediente.builder().nombre("Amparo Indirecto").build();
            TipoExpediente tLaboral = TipoExpediente.builder().nombre("Conflicto Laboral").build();
            
            tipoExpedienteRepository.saveAll(List.of(tJuicio, tEjecutivo, tAmparo, tLaboral));
            System.out.println("✅ Tipos de Expediente creados.");
        }

        // 4. ÓRGANOS JURISDICCIONALES
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
    }
}

