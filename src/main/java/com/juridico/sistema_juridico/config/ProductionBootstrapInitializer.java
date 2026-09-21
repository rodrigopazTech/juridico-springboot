package com.juridico.sistema_juridico.config;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga exclusivamente el catalogo organizacional y las cuentas de arranque
 * autorizadas para una base SIAJ nueva. No actualiza cuentas que ya existan.
 */
@Component
@Order(10)
@ConditionalOnProperty(prefix = "app.bootstrap.production", name = "enabled", havingValue = "true")
public class ProductionBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionBootstrapInitializer.class);

    private static final String GERENCIA_LABORAL = "Gerencia de lo Contencioso Laboral y Penal";
    private static final String GERENCIA_CIVIL =
            "Gerencia Jurídico para Contratos y lo Contencioso Civil, Fiscal y Administrativo";

    private final GerenciaRepository gerenciaRepository;
    private final MateriaRepository materiaRepository;
    private final CatPrioridadRepository catPrioridadRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.production.admin-temp-password:}")
    private String adminTempPassword;

    public ProductionBootstrapInitializer(GerenciaRepository gerenciaRepository,
            MateriaRepository materiaRepository,
            CatPrioridadRepository catPrioridadRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.gerenciaRepository = gerenciaRepository;
        this.materiaRepository = materiaRepository;
        this.catPrioridadRepository = catPrioridadRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarConfiguracion();
        asegurarPrioridades();

        Gerencia laboral = asegurarGerencia(GERENCIA_LABORAL);
        Gerencia civil = asegurarGerencia(GERENCIA_CIVIL);

        Set<Materia> materiasLaboral = asegurarMaterias(laboral, "Laboral", "Penal");
        Set<Materia> materiasCivil = asegurarMaterias(civil, "Civil", "Mercantil", "Administrativo");
        Set<Gerencia> todasLasGerencias = Set.of(laboral, civil);
        Set<Materia> todasLasMaterias = new LinkedHashSet<>();
        todasLasMaterias.addAll(materiasLaboral);
        todasLasMaterias.addAll(materiasCivil);

        Usuario admin = asegurarUsuario(new CuentaInicial(
                "Rodrigo Cristobal Paz Jimenez", "rodrigo.paz@finabien.gob.mx", "rodrigo.paz", "27720",
                "Administrador del sistema", RolUsuario.ADMIN),
                todasLasGerencias, todasLasMaterias, adminTempPassword, null);

        String passwordNoEntregado = UUID.randomUUID().toString() + UUID.randomUUID();
        asegurarUsuario(new CuentaInicial(
                "Juan Francisco Gutiérrez Motta", "juan.motta@finabien.gob.mx", "juan.motta", "26953",
                "Director Principal", RolUsuario.DIRECTOR_PRINCIPAL),
                todasLasGerencias, todasLasMaterias, passwordNoEntregado, admin);
        asegurarUsuario(new CuentaInicial(
                "Mayra Caritina Juárez Martínez", "mayra.juarez@finabien.gob.mx", "mayra.juarez", "1115",
                "Directora", RolUsuario.DIRECTOR),
                todasLasGerencias, todasLasMaterias, passwordNoEntregado, admin);
        asegurarUsuario(new CuentaInicial(
                "Julio Jesús Castellanos Tecanhuehue", "julio.castellanos@finabien.gob.mx", "julio.castellanos",
                "27395", "Subdirector", RolUsuario.SUBDIRECTOR),
                todasLasGerencias, todasLasMaterias, passwordNoEntregado, admin);
        asegurarUsuario(new CuentaInicial(
                "Hebert Mendoza Hernández", "hebert.mendoza@finabien.gob.mx", "hebert.mendoza", "27521",
                "Gerente de lo Contencioso Laboral y Penal", RolUsuario.GERENTE),
                Set.of(laboral), materiasLaboral, passwordNoEntregado, admin);

        log.warn("Alta inicial de produccion completada. Desactive SIAJ_BOOTSTRAP_ENABLED antes del siguiente arranque.");
    }

    private void validarConfiguracion() {
        if (adminTempPassword == null || adminTempPassword.length() < 12) {
            throw new IllegalStateException(
                    "SIAJ_BOOTSTRAP_ADMIN_TEMP_PASSWORD debe contener al menos 12 caracteres cuando el bootstrap esta activo.");
        }
    }

    private Gerencia asegurarGerencia(String nombre) {
        return gerenciaRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> gerenciaRepository.save(Gerencia.builder().nombre(nombre).activo(true).build()));
    }

    private void asegurarPrioridades() {
        asegurarPrioridad("ALTA", "bg-red-100 text-red-800");
        asegurarPrioridad("MEDIA", "bg-yellow-100 text-yellow-800");
        asegurarPrioridad("BAJA", "bg-green-100 text-green-800");
    }

    private void asegurarPrioridad(String nombre, String color) {
        catPrioridadRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> catPrioridadRepository.save(CatPrioridad.builder()
                        .nombre(nombre)
                        .color(color)
                        .activo(true)
                        .build()));
    }

    private Set<Materia> asegurarMaterias(Gerencia gerencia, String... nombres) {
        Set<Materia> materias = new LinkedHashSet<>();
        for (String nombre : nombres) {
            Materia materia = materiaRepository.findByNombreIgnoreCaseAndGerenciaId(nombre, gerencia.getId())
                    .orElseGet(() -> materiaRepository.save(Materia.builder()
                            .nombre(nombre)
                            .gerencia(gerencia)
                            .activo(true)
                            .build()));
            materias.add(materia);
        }
        return materias;
    }

    private Usuario asegurarUsuario(CuentaInicial cuenta, Set<Gerencia> gerencias, Set<Materia> materias,
            String passwordTemporal, Usuario creadoPor) {
        return usuarioRepository.findByEmailIgnoreCase(cuenta.email())
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .nombreCompleto(cuenta.nombreCompleto())
                        .email(cuenta.email())
                        .username(cuenta.username())
                        .numeroEmpleado(cuenta.numeroEmpleado())
                        .puesto(cuenta.puesto())
                        .rol(cuenta.rol())
                        .gerencias(new LinkedHashSet<>(gerencias))
                        .materias(new LinkedHashSet<>(materias))
                        .password(passwordEncoder.encode(passwordTemporal))
                        .passwordUpdatedAt(LocalDateTime.now())
                        .forcePasswordChange(true)
                        .microsoftLoginEnabled(false)
                        .activo(true)
                        .creadoPor(creadoPor)
                        .build()));
    }

    private record CuentaInicial(String nombreCompleto, String email, String username, String numeroEmpleado,
            String puesto, RolUsuario rol) {
    }
}
