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
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
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
        Gerencia gLaboral = null;
        Gerencia gTransparencia = null;

        if (gerenciaRepository.count() == 0) {
            // Creamos las 3 Gerencias Nuevas
            gCivil = Gerencia.builder()
                    .nombre("Gerencia Civil, Mercantil, Fiscal y Administrativo")
                    .descripcion("Atención de asuntos civiles, mercantiles y administrativos.")
                    .build();

            gLaboral = Gerencia.builder()
                    .nombre("Gerencia Laboral y Penal")
                    .descripcion("Atención de conflictos laborales y defensa penal.")
                    .build();
            
            gTransparencia = Gerencia.builder()
                    .nombre("Gerencia Transparencia y Amparo")
                    .descripcion("Atención de solicitudes de transparencia y juicios de amparo.")
                    .build();
            
            gerenciaRepository.saveAll(List.of(gCivil, gLaboral, gTransparencia));
            System.out.println("Nuevas Gerencias creadas.");
        } else {
            // Si ya existen, las buscamos para usarlas abajo (Manejo de errores simplificado)
            List<Gerencia> todas = gerenciaRepository.findAll();
            gCivil = todas.stream().filter(g -> g.getNombre().contains("Civil")).findFirst().orElse(null);
            gLaboral = todas.stream().filter(g -> g.getNombre().contains("Laboral")).findFirst().orElse(null);
            gTransparencia = todas.stream().filter(g -> g.getNombre().contains("Transparencia")).findFirst().orElse(null);
        }

        // 3. MATERIAS (Ligadas a las nuevas Gerencias)
        if (materiaRepository.count() == 0 && gCivil != null) {
            
            // Materias para Gerencia Civil, Mercantil...
            Materia mCivil = Materia.builder().nombre("Civil").gerencia(gCivil).build();
            Materia mMercantil = Materia.builder().nombre("Mercantil").gerencia(gCivil).build();
            Materia mFiscal = Materia.builder().nombre("Fiscal").gerencia(gCivil).build();
            Materia mAdmin = Materia.builder().nombre("Administrativo").gerencia(gCivil).build();

            // Materias para Gerencia Laboral y Penal
            Materia mLaboral = Materia.builder().nombre("Laboral").gerencia(gLaboral).build();
            Materia mPenal = Materia.builder().nombre("Penal").gerencia(gLaboral).build();

            // Materias para Gerencia Transparencia y Amparo
            Materia mAmparo = Materia.builder().nombre("Amparo").gerencia(gTransparencia).build();
            Materia mTransp = Materia.builder().nombre("Transparencia").gerencia(gTransparencia).build();

            materiaRepository.saveAll(List.of(
                mCivil, mMercantil, mFiscal, mAdmin, 
                mLaboral, mPenal, 
                mAmparo, mTransp
            ));
            System.out.println("Nuevas Materias creadas y vinculadas.");
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