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
        
        // 1. GERENCIAS Y MATERIAS (Vinculadas correctamente)
        if (gerenciaRepository.count() == 0) {
            
            // Crear Gerencia 1
            Gerencia gLitigio = Gerencia.builder()
                    .nombre("Gerencia de Litigio y Defensa")
                    .descripcion("Encargada de atender juicios civiles y mercantiles.")
                    .activo(true)
                    .build();
            gLitigio = gerenciaRepository.save(gLitigio); // Guardamos para tener ID

            // Crear Materias para Gerencia 1
            Materia mCivil = Materia.builder().nombre("Civil").gerencia(gLitigio).activo(true).build();
            Materia mMercantil = Materia.builder().nombre("Mercantil").gerencia(gLitigio).activo(true).build();
            materiaRepository.saveAll(List.of(mCivil, mMercantil));

            // Crear Gerencia 2
            Gerencia gLaboral = Gerencia.builder()
                    .nombre("Gerencia de Asuntos Laborales")
                    .descripcion("Atención a demandas de trabajadores y sindicatos.")
                    .activo(true)
                    .build();
            gLaboral = gerenciaRepository.save(gLaboral); // Guardamos

            // Crear Materias para Gerencia 2
            Materia mLaboral = Materia.builder().nombre("Laboral").gerencia(gLaboral).activo(true).build();
            Materia mAmparo = Materia.builder().nombre("Amparo").gerencia(gLaboral).activo(true).build();
            materiaRepository.saveAll(List.of(mLaboral, mAmparo));

            System.out.println("✅ Gerencias y Materias inicializadas correctamente.");
        }

       // 2. USUARIOS
        if (usuarioRepository.count() == 0) {
            Gerencia gerenciaDefault = gerenciaRepository.findAll().stream().findFirst().orElse(null);

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

            // 2. NIVEL MEDIO (Ve Dashboard, pero NO Usuarios ni Agenda General)
            Usuario gerente = Usuario.builder()
                    .nombreCompleto("Gerente Civil")
                    .email("gerente@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.GERENTE)
                    .activo(true)
                    .gerencia(gerenciaDefault)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Usuario jefe = Usuario.builder()
                    .nombreCompleto("Jefe de Depto")
                    .email("jefe@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.JEFE_DEPTO)
                    .activo(true)
                    .gerencia(gerenciaDefault)
                    .createdAt(LocalDateTime.now())
                    .build();

            // 3. NIVEL OPERATIVO (Solo Expedientes, Términos, Audiencias)
            Usuario abogado = Usuario.builder()
                    .nombreCompleto("Abogado Litigante")
                    .email("abogado@juridico.com")
                    .password(passwordEncoder.encode("12345"))
                    .rol(RolUsuario.ABOGADO)
                    .activo(true)
                    .gerencia(gerenciaDefault)
                    .createdAt(LocalDateTime.now())
                    .build();

            usuarioRepository.saveAll(List.of(director, subdirector, gerente, jefe, abogado));
            System.out.println("✅ Usuarios de prueba creados para todos los roles.");
        }

        // 3. TIPOS DE EXPEDIENTE
        if (tipoExpedienteRepository.count() == 0) {
            TipoExpediente tJuicio = TipoExpediente.builder().nombre("Juicio Ordinario").build();
            TipoExpediente tEjecutivo = TipoExpediente.builder().nombre("Juicio Ejecutivo Mercantil").build();
            TipoExpediente tAmparo = TipoExpediente.builder().nombre("Amparo Indirecto").build();
            
            tipoExpedienteRepository.saveAll(List.of(tJuicio, tEjecutivo, tAmparo));
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
            
            organoRepository.saveAll(List.of(oJuzgado, oTribunal));
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