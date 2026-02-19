package com.juridico.sistema_juridico;

import com.juridico.sistema_juridico.Entity.catalogo.*;
import com.juridico.sistema_juridico.Entity.enums.*;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.*;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ExpedienteRepository expedienteRepository;
    private final AudienciaRepository audienciaRepository;
    private final TerminoRepository terminoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GerenciaRepository gerenciaRepository;
    private final MateriaRepository materiaRepository;
    private final OrganoJurisdiccionalRepository organoJurisdiccionalRepository;
    private final TipoExpedienteRepository tipoExpedienteRepository;
    private final TipoAudienciaRepository tipoAudienciaRepository;

    @Autowired
    public DataSeeder(ExpedienteRepository expedienteRepository,
            AudienciaRepository audienciaRepository,
            TerminoRepository terminoRepository,
            UsuarioRepository usuarioRepository,
            GerenciaRepository gerenciaRepository,
            MateriaRepository materiaRepository,
            OrganoJurisdiccionalRepository organoJurisdiccionalRepository,
            TipoExpedienteRepository tipoExpedienteRepository,
            TipoAudienciaRepository tipoAudienciaRepository) {
        this.expedienteRepository = expedienteRepository;
        this.audienciaRepository = audienciaRepository;
        this.terminoRepository = terminoRepository;
        this.usuarioRepository = usuarioRepository;
        this.gerenciaRepository = gerenciaRepository;
        this.materiaRepository = materiaRepository;
        this.organoJurisdiccionalRepository = organoJurisdiccionalRepository;
        this.tipoExpedienteRepository = tipoExpedienteRepository;
        this.tipoAudienciaRepository = tipoAudienciaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (expedienteRepository.count() > 0) {
            System.out.println("Database already has data. Skipping seeding.");
            return;
        }

        System.out.println("Seeding database with sample data...");

        // 1. Get or Create Default User
        Usuario admin = usuarioRepository.findByEmail("admin@juridico.com")
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setNombreCompleto("Administrador Sistema");
                    u.setEmail("admin@juridico.com");
                    u.setPassword("$2a$10$8.UnVuG9UMJom6BPNW1QCuzGgzS.YV6L1TND9pTmdkX0G/W3I/K9G"); // password123
                    u.setRol(RolUsuario.DIRECCION);
                    u.setActivo(true);
                    return usuarioRepository.save(u);
                });

        // 2. Get Catalogs (ensure they exist or create basic ones)
        Gerencia gerencia = gerenciaRepository.findAll().stream().findFirst().orElseGet(() -> {
            Gerencia g = new Gerencia();
            g.setNombre("Gerencia Jurídica Central");
            return gerenciaRepository.save(g);
        });

        Materia materia = materiaRepository.findAll().stream().findFirst().orElseGet(() -> {
            Materia m = new Materia();
            m.setNombre("Materia Civil");
            m.setDescripcion("Asuntos Civiles");
            return materiaRepository.save(m);
        });

        OrganoJurisdiccional organo = organoJurisdiccionalRepository.findAll().stream().findFirst().orElseGet(() -> {
            OrganoJurisdiccional o = new OrganoJurisdiccional();
            o.setNombre("Juzgado Primero de lo Civil");
            return organoJurisdiccionalRepository.save(o);
        });

        TipoExpediente tipoExp = tipoExpedienteRepository.findAll().stream().findFirst().orElseGet(() -> {
            TipoExpediente t = new TipoExpediente();
            t.setNombre("Ordinario");
            return tipoExpedienteRepository.save(t);
        });

        TipoAudiencia tipoAud = tipoAudienciaRepository.findAll().stream().findFirst().orElseGet(() -> {
            TipoAudiencia t = new TipoAudiencia();
            t.setNombre("Pruebas y Alegatos");
            return tipoAudienciaRepository.save(t);
        });

        // 3. Create 5 Expedientes
        for (int i = 1; i <= 5; i++) {
            Expediente exp = Expediente.builder()
                    .numero("EXP-00" + i + "/2026")
                    .descripcion("Descripción de prueba para el expediente " + i)
                    .materia(materia)
                    .gerencia(gerencia)
                    .organoJurisdiccional(organo)
                    .prioridad(Prioridad.values()[i % Prioridad.values().length])
                    .etapaProcesal(EtapaProcesal.TRAMITE)
                    .abogadoResponsable(admin)
                    .tipoExpediente(tipoExp)
                    .build();

            exp = expedienteRepository.save(exp);

            // 4. Create 1 Audiencia for each
            Audiencia aud = Audiencia.builder()
                    .expediente(exp)
                    .fechaAudiencia(LocalDate.now().plusDays(i * 2L))
                    .horaAudiencia(LocalTime.of(10 + i % 12, 0)) // Ensure valid time
                    .tipoAudiencia(tipoAud)
                    .esVirtual(false)
                    .salaLugar("Sala " + i)
                    .estatusAudiencia(EstatusAudiencia.PENDIENTE)
                    .abogadoComparece(admin)
                    .observaciones("Audiencia de prueba " + i)
                    .build();
            audienciaRepository.save(aud);

            // 5. Create 1 Termino for each
            Termino term = Termino.builder()
                    .expediente(exp)
                    .actuacion("Presentación de escrito de pruebas " + i)
                    .fechaIngreso(LocalDate.now())
                    .fechaVencimiento(LocalDate.now().plusDays(i * 3L))
                    .estatusTermino(EstatusTermino.PROYECTISTA)
                    .prioridad(exp.getPrioridad())
                    .abogadoResponsable(admin)
                    .observaciones("Término de prueba " + i)
                    .build();
            terminoRepository.save(term);
        }

        System.out.println("Database seeding completed successfully.");
    }
}
