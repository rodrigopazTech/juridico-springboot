package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Set;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Valida si el archivo cumple con las extensiones permitidas y el límite de tamaño de 1GB.
     *
     * @param file Archivo a validar
     */
    public void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío.");
        }

        // Límite de 1GB en bytes
        long maxSizeBytes = 1024L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException("El archivo excede el tamaño máximo permitido de 1GB.");
        }

        String nombreOriginal = file.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw new BadRequestException("El archivo no tiene una extensión válida.");
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).toLowerCase();

        Set<String> extensionesPermitidas = Set.of(
            // PDF
            "pdf",
            // Word
            "doc", "docx", "docm",
            // Excel
            "xls", "xlsx", "xlsm", "xlsb",
            // PowerPoint
            "ppt", "pptx",
            // Imágenes
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg",
            // Videos
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "mpeg", "3gp",
            // Audio
            "mp3", "wav", "ogg", "m4a", "aac", "flac", "wma"
        );

        if (!extensionesPermitidas.contains(extension)) {
            throw new BadRequestException("El formato de archivo '." + extension + "' no está permitido. Solo se admiten archivos PDF, Office (Word, Excel, PowerPoint), imágenes, videos y audio.");
        }
    }

    /**
     * Guarda un archivo en el filesystem
     * 
     * @param file         Archivo a guardar
     * @param expedienteId ID del expediente
     * @param categoria    Categoría del documento
     * @return Ruta relativa del archivo guardado
     */
    public String guardarArchivo(MultipartFile file, UUID expedienteId, String categoria) throws IOException {
        validarArchivo(file);

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
     * Guarda un archivo en una ruta relativa personalizada dentro de app.upload.dir.
     *
     * @param file              Archivo a guardar
     * @param rutaRelativaBase  Ruta relativa base (ej. "expedientes/{id}/Terminos/...")
     * @return Ruta relativa final del archivo guardado
     */
    public String guardarArchivoEnRutaRelativa(MultipartFile file, String rutaRelativaBase) throws IOException {
        validarArchivo(file);

        Path base = Paths.get(uploadDir).resolve(rutaRelativaBase).normalize();
        Files.createDirectories(base);

        String nombreOriginal = file.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        }
        String nombreUnico = UUID.randomUUID() + extension;

        Path rutaDestino = base.resolve(nombreUnico).normalize();
        Files.copy(file.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        return Paths.get(rutaRelativaBase, nombreUnico).toString();
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
