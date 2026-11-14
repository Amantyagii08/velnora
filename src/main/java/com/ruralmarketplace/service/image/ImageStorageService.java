
package com.ruralmarketplace.service.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    private final Path uploadRoot;

    public ImageStorageService(
            @Value("${app.image.upload-dir:C:/VelnoraData/uploads}")
            String uploadDirectory) {

        this.uploadRoot = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    public String storeImage(
            MultipartFile file,
            String imageType) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select an image to upload");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB");
        }

        if (!Set.of("products", "shops", "categories")
                .contains(imageType)) {
            throw new IllegalArgumentException(
                    "Invalid image type");
        }

        String originalFilename =
                StringUtils.cleanPath(
                        file.getOriginalFilename() == null
                                ? ""
                                : file.getOriginalFilename());

        if (originalFilename.isBlank()
                || originalFilename.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid image filename");
        }

        String extension = StringUtils
                .getFilenameExtension(originalFilename);

        if (extension == null
                || !ALLOWED_EXTENSIONS.contains(
                        extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Only JPG, JPEG, PNG, and WebP images are allowed");
        }

        byte[] imageBytes = file.getBytes();
        String normalizedExtension = extension.toLowerCase();
        if (!hasMatchingImageSignature(imageBytes, normalizedExtension)) {
            throw new IllegalArgumentException("The uploaded file content does not match a supported image format");
        }

        Path targetDirectory =
                uploadRoot.resolve(imageType).normalize();

        if (!targetDirectory.startsWith(uploadRoot)) {
            throw new IllegalArgumentException(
                    "Invalid image destination");
        }

        Files.createDirectories(targetDirectory);

        String filename =
                UUID.randomUUID() + "." + extension.toLowerCase();

        Path targetFile =
                targetDirectory.resolve(filename).normalize();

        if (!targetFile.startsWith(targetDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid image path");
        }

        try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            Files.copy(
                    inputStream,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING);
        }

        return imageType + "/" + filename;
    }

    public void deleteImage(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) return;
        Path imagePath = uploadRoot.resolve(relativePath).normalize();
        if (!imagePath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid image path");
        }
        Files.deleteIfExists(imagePath);
    }

    public void deleteImageAfterCommit(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        deleteImage(relativePath);
                    } catch (IOException ignored) {
                        // The database already points to the replacement image.
                    }
                }
            });
            return;
        }
        try {
            deleteImage(relativePath);
        } catch (IOException ignored) {
            // Keep the saved update successful if cleanup of an old image fails.
        }
    }

    private boolean hasMatchingImageSignature(byte[] bytes, String extension) {
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        }
        if ("png".equals(extension)) {
            byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
            if (bytes.length < signature.length) return false;
            for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) return false;
            return true;
        }
        return "webp".equals(extension) && bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}

    

