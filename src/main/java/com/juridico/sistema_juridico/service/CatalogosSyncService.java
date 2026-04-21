package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogosSyncService {

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Transactional
    public void sincronizarNombreUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) return;
        
        List<Expediente> expedientes = expedienteRepository.findAllByAbogadoResponsableId(usuario.getId().intValue());
        for (Expediente e : expedientes) {
            e.setAbogadoResponsableNombre(usuario.getNombreCompleto());
            expedienteRepository.save(e);
        }
    }

    @Transactional
    public void sincronizarNombreOrgano(OrganoJurisdiccional organo) {
        if (organo == null || organo.getId() == null) return;
        
        // Buscamos expedientes que tengan este órgano asignado por ID
        // Como no hay un método directo, podemos usar una consulta JPA
        // Pero ExpediaRepository ya tiene lo necesario para iterar
        List<Expediente> expedientes = expedienteRepository.findAll().stream()
            .filter(e -> e.getOrganoJurisdiccional() != null && e.getOrganoJurisdiccional().getId().equals(organo.getId()))
            .toList();
            
        for (Expediente e : expedientes) {
            e.setOrganoJurisdiccionalTexto(organo.getNombre());
            expedienteRepository.save(e);
        }
    }

    @Transactional
    public void sincronizarNombreSede(String nombreViejo, String nombreNuevo) {
        if (nombreViejo == null || nombreNuevo == null || nombreViejo.equals(nombreNuevo)) return;
        
        // Como la sede en Expediente es un String simple, buscamos coincidencia exacta de texto
        List<Expediente> expedientes = expedienteRepository.findAll().stream()
            .filter(e -> nombreViejo.equalsIgnoreCase(e.getSede()))
            .toList();
            
        for (Expediente e : expedientes) {
            e.setSede(nombreNuevo);
            expedienteRepository.save(e);
        }
    }
}
