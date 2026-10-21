
package com.ruralmarketplace.service.image;

import java.io.IOException;
import java.io.InputStream;
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

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING);
        }

        return imageType + "/" + filename;
    }
}

    

