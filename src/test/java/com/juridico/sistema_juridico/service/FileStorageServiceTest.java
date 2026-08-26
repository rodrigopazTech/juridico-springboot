package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void guardarArchivoSanitizaCategoriaYPermaneceDentroDeUploadDir() throws Exception {
        FileStorageService service = serviceWithUploadDir();
        MockMultipartFile file = new MockMultipartFile(
                "file", "..\\acuse.pdf", "application/pdf", "contenido".getBytes());

        String rutaRelativa = service.guardarArchivo(file, UUID.randomUUID(), "../carpeta insegura");
        Path rutaGuardada = service.obtenerArchivo(rutaRelativa);

        assertTrue(rutaGuardada.toAbsolutePath().normalize().startsWith(tempDir.toAbsolutePath().normalize()));
        assertTrue(Files.exists(rutaGuardada));
    }

    @Test
    void obtenerArchivoRechazaTraversal() {
        FileStorageService service = serviceWithUploadDir();

        assertThrows(BadRequestException.class, () -> service.obtenerArchivo("../application.properties"));
    }

    @Test
    void obtenerArchivoRechazaRutaAbsoluta() {
        FileStorageService service = serviceWithUploadDir();

        assertThrows(BadRequestException.class, () -> service.obtenerArchivo(tempDir.resolve("x.pdf").toString()));
    }

    @Test
    void validarArchivoRechazaExtensionNoPermitida() {
        FileStorageService service = serviceWithUploadDir();
        MockMultipartFile scriptFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "dummy".getBytes());

        assertThrows(BadRequestException.class, () -> service.validarArchivo(scriptFile));
    }

    @Test
    void validarArchivoPermiteDocumentoNormalYRechazaDocumentoGigante() {
        FileStorageService service = serviceWithUploadDir();
        MockMultipartFile normalPdf = new MockMultipartFile("file", "demanda.pdf", "application/pdf", "dummy content".getBytes());
        service.validarArchivo(normalPdf); // No exception

        // Simular un PDF que excede 150MB
        MockMultipartFile giantPdf = new MockMultipartFile("file", "demanda.pdf", "application/pdf", new byte[]{1}) {
            @Override
            public long getSize() {
                return 160L * 1024 * 1024;
            }
        };

        assertThrows(BadRequestException.class, () -> service.validarArchivo(giantPdf));
    }

    @Test
    void validarArchivoPermiteVideoGrandeHasta1GB() {
        FileStorageService service = serviceWithUploadDir();
        // Video de 500MB permitido
        MockMultipartFile videoOk = new MockMultipartFile("file", "audiencia.mp4", "video/mp4", new byte[]{1}) {
            @Override
            public long getSize() {
                return 500L * 1024 * 1024;
            }
        };
        service.validarArchivo(videoOk); // No exception

        // Video de 1.2GB rechazado
        MockMultipartFile giantVideo = new MockMultipartFile("file", "audiencia.mp4", "video/mp4", new byte[]{1}) {
            @Override
            public long getSize() {
                return 1200L * 1024 * 1024;
            }
        };
        assertThrows(BadRequestException.class, () -> service.validarArchivo(giantVideo));
    }

    private FileStorageService serviceWithUploadDir() {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        return service;
    }
}
