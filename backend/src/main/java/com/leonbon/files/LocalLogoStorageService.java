package com.leonbon.files;

import com.leonbon.web.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalLogoStorageService {
    private final Path uploadDir;
    private final String publicBaseUrl;

    public LocalLogoStorageService(
            @Value("${app.files.uploadDir}") String uploadDir,
            @Value("${app.files.publicBaseUrl}") String publicBaseUrl
    ) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    public String storeLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extension(original);
        if (!isAllowedExt(ext)) {
            throw new BadRequestException("only jpg, png, or webp are allowed");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("could not read file");
        }

        if (bytes.length == 0) {
            throw new BadRequestException("file is empty");
        }
        if (bytes.length > 2 * 1024 * 1024) {
            throw new BadRequestException("max file size is 2MB");
        }

        assertMagicBytes(bytes, ext);

        String filename = UUID.randomUUID() + "." + ext;
        Path logosDir = uploadDir.resolve("logos");
        try {
            Files.createDirectories(logosDir);
            Path out = logosDir.resolve(filename);
            Files.write(out, bytes);
        } catch (IOException e) {
            throw new BadRequestException("could not store file");
        }

        return publicBaseUrl + "/uploads/logos/" + filename;
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedExt(String ext) {
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp");
    }

    private static void assertMagicBytes(byte[] bytes, String ext) {
        if (bytes.length < 12) {
            throw new BadRequestException("invalid image file");
        }

        boolean jpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
        boolean png = (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;

        boolean riff = bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46; // RIFF
        boolean webp = riff
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50; // WEBP

        boolean ok =
                switch (ext) {
                    case "jpg", "jpeg" -> jpeg;
                    case "png" -> png;
                    case "webp" -> webp;
                    default -> false;
                };

        if (!ok) {
            throw new BadRequestException("file content does not match declared image type");
        }
    }
}
