package com.juridico.sistema_juridico.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Guarda un archivo en el filesystem
     * 
     * @param file         Archivo a guardar
     * @param expedienteId ID del expediente
     * @param categoria    Categoría del documento
     * @return Ruta relativa del archivo guardado
     */
    public String guardarArchivo(MultipartFile file, UUID expedienteId, String categoria) throws IOException {
        // Crear estructura de directorios:
        // uploads/expedientes/{expedienteId}/{categoria}/
        Path directorioExpediente = Paths.get(uploadDir, "expedientes", expedienteId.toString(), categoria);
        Files.createDirectories(directorioExpediente);

        // Generar nombre único para el archivo
        String nombreOriginal = file.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        }
        String nombreUnico = UUID.randomUUID().toString() + extension;

        // Guardar archivo
        Path rutaDestino = directorioExpediente.resolve(nombreUnico);
        Files.copy(file.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        // Retornar ruta relativa
        return Paths.get("expedientes", expedienteId.toString(), categoria, nombreUnico).toString();
    }

    /**
     * Obtiene un archivo del filesystem
     * 
     * @param rutaRelativa Ruta relativa del archivo
     * @return Path del archivo
     */
    public Path obtenerArchivo(String rutaRelativa) {
        return Paths.get(uploadDir).resolve(rutaRelativa).normalize();
    }

    /**
     * Elimina un archivo del filesystem
     * 
     * @param rutaRelativa Ruta relativa del archivo
     */
    public void eliminarArchivo(String rutaRelativa) throws IOException {
        Path rutaArchivo = obtenerArchivo(rutaRelativa);
        Files.deleteIfExists(rutaArchivo);
    }

    /**
     * Verifica si un archivo existe
     * 
     * @param rutaRelativa Ruta relativa del archivo
     * @return true si existe, false si no
     */
    public boolean existeArchivo(String rutaRelativa) {
        Path rutaArchivo = obtenerArchivo(rutaRelativa);
        return Files.exists(rutaArchivo);
    }
}
