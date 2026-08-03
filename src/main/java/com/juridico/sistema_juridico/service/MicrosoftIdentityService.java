package com.juridico.sistema_juridico.service;

import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;
import com.juridico.sistema_juridico.exception.UnauthorizedException;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicrosoftIdentityService {

    private final UsuarioRepository usuarioRepository;

    public MicrosoftIdentityService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario vincularYObtenerUsuarioLocal(MicrosoftUserProfile profile) {
        if (profile == null || profile.id() == null || profile.id().isBlank()) {
            throw new UnauthorizedException("Microsoft no regreso un identificador valido para la cuenta.");
        }

        String email = profile.preferredEmail();
        Usuario usuario = usuarioRepository.findByMicrosoftObjectId(profile.id())
                .or(() -> email == null ? java.util.Optional.empty() : usuarioRepository.findByEmailIgnoreCase(email))
                .orElseThrow(() -> new UnauthorizedException(
                        "La cuenta Microsoft autenticada no esta registrada en SIAJ."));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UnauthorizedException("El usuario SIAJ esta inactivo.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean primerVinculo = usuario.getMicrosoftObjectId() == null || usuario.getMicrosoftObjectId().isBlank();

        usuario.setMicrosoftObjectId(profile.id());
        usuario.setMicrosoftUserPrincipalName(profile.userPrincipalName());
        usuario.setMicrosoftDisplayName(profile.displayName());
        usuario.setMicrosoftJobTitle(profile.jobTitle());
        usuario.setMicrosoftBusinessPhones(profile.joinedBusinessPhones());
        usuario.setMicrosoftMobilePhone(profile.mobilePhone());
        usuario.setMicrosoftOfficeLocation(profile.officeLocation());
        usuario.setMicrosoftLoginEnabled(true);
        usuario.setMicrosoftLastSyncAt(now);
        if (primerVinculo) {
            usuario.setMicrosoftLinkedAt(now);
        }

        if (isBlank(usuario.getPuesto()) && !isBlank(profile.jobTitle())) {
            usuario.setPuesto(profile.jobTitle());
        }
        if (isBlank(usuario.getTelefonoInstitucional()) && !isBlank(profile.joinedBusinessPhones())) {
            usuario.setTelefonoInstitucional(profile.joinedBusinessPhones());
        }
        if (isBlank(usuario.getTelefonoMovil()) && !isBlank(profile.mobilePhone())) {
            usuario.setTelefonoMovil(profile.mobilePhone());
        }
        if (isBlank(usuario.getUbicacionOficina()) && !isBlank(profile.officeLocation())) {
            usuario.setUbicacionOficina(profile.officeLocation());
        }

        usuario.setUltimoAcceso(now);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario vincularPerfilAUsuario(Usuario usuario, MicrosoftUserProfile profile) {
        if (usuario == null) {
            throw new UnauthorizedException("No existe usuario local para vincular.");
        }
        if (profile == null || profile.id() == null || profile.id().isBlank()) {
            throw new UnauthorizedException("Microsoft no regreso un identificador valido para la cuenta.");
        }

        String microsoftEmail = profile.preferredEmail();
        if (isBlank(microsoftEmail) || isBlank(usuario.getEmail())
                || !usuario.getEmail().trim().equalsIgnoreCase(microsoftEmail)) {
            throw new UnauthorizedException("El correo autenticado en Microsoft no coincide con el usuario registrado en SIAJ.");
        }

        usuarioRepository.findByMicrosoftObjectId(profile.id()).ifPresent(existing -> {
            if (!existing.getId().equals(usuario.getId())) {
                throw new UnauthorizedException("La cuenta Microsoft ya esta vinculada a otro usuario SIAJ.");
            }
        });

        LocalDateTime now = LocalDateTime.now();
        boolean primerVinculo = usuario.getMicrosoftObjectId() == null || usuario.getMicrosoftObjectId().isBlank();
        usuario.setMicrosoftObjectId(profile.id());
        usuario.setMicrosoftUserPrincipalName(profile.userPrincipalName());
        usuario.setMicrosoftDisplayName(profile.displayName());
        usuario.setMicrosoftJobTitle(profile.jobTitle());
        usuario.setMicrosoftBusinessPhones(profile.joinedBusinessPhones());
        usuario.setMicrosoftMobilePhone(profile.mobilePhone());
        usuario.setMicrosoftOfficeLocation(profile.officeLocation());
        usuario.setMicrosoftLoginEnabled(true);
        usuario.setMicrosoftLastSyncAt(now);
        if (primerVinculo) {
            usuario.setMicrosoftLinkedAt(now);
        }

        if (isBlank(usuario.getPuesto()) && !isBlank(profile.jobTitle())) {
            usuario.setPuesto(profile.jobTitle());
        }
        if (isBlank(usuario.getTelefonoInstitucional()) && !isBlank(profile.joinedBusinessPhones())) {
            usuario.setTelefonoInstitucional(profile.joinedBusinessPhones());
        }
        if (isBlank(usuario.getTelefonoMovil()) && !isBlank(profile.mobilePhone())) {
            usuario.setTelefonoMovil(profile.mobilePhone());
        }
        if (isBlank(usuario.getUbicacionOficina()) && !isBlank(profile.officeLocation())) {
            usuario.setUbicacionOficina(profile.officeLocation());
        }

        return usuarioRepository.save(usuario);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
