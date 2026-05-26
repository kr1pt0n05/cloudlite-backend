package de.lind3.CloudLite.file;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path basePath;

    public LocalFileStorageService(@Value("${file.storage.base-path:./.uploads}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(basePath);
    }

    @Override
    public FileWriteResult store(String ownerSubject, String folderPath, String fileName, InputStream content)
            throws IOException {
        Path targetPath = resolveUserFilePath(ownerSubject, folderPath, fileName);
        Files.createDirectories(targetPath.getParent());

        Path tempPath = targetPath.resolveSibling(fileName + ".upload.tmp");
        MessageDigest digest = sha256Digest();

        long sizeBytes;
        try (DigestInputStream dis = new DigestInputStream(content, digest);
             OutputStream out = Files.newOutputStream(tempPath)) {
            sizeBytes = dis.transferTo(out);
        } catch (IOException e) {
            Files.deleteIfExists(tempPath);
            throw e;
        }

        Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE);
        String storagePath = basePath.relativize(targetPath).toString();
        return new FileWriteResult(storagePath, HexFormat.of().formatHex(digest.digest()), sizeBytes);
    }

    @Override
    public boolean exists(String ownerSubject, String folderPath, String fileName) {
        return Files.exists(resolveUserFilePath(ownerSubject, folderPath, fileName));
    }

    @Override
    public void createDirectory(String ownerSubject, String folderPath) throws IOException {
        Files.createDirectories(resolveUserDirectoryPath(ownerSubject, folderPath));
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Files.deleteIfExists(resolveStoragePath(storagePath));
    }

    @Override
    public void deleteAll(Collection<FileWriteResult> writtenFiles) {
        for (FileWriteResult writtenFile : writtenFiles) {
            try {
                delete(writtenFile.storagePath());
            } catch (IOException ignored) {
                // Cleanup is best-effort; a maintenance task can remove stragglers.
            }
        }
    }

    private Path resolveUserFilePath(String ownerSubject, String folderPath, String fileName) {
        Path path = resolveUserDirectoryPath(ownerSubject, folderPath).resolve(fileName).normalize();
        if (!path.startsWith(basePath)) {
            throw new IllegalArgumentException("Resolved file path escapes storage base path");
        }
        return path;
    }

    private Path resolveUserDirectoryPath(String ownerSubject, String folderPath) {
        Path path = basePath.resolve(safeOwnerSegment(ownerSubject));
        for (String segment : normalizedSegments(folderPath)) {
            path = path.resolve(segment);
        }
        path = path.normalize();
        if (!path.startsWith(basePath)) {
            throw new IllegalArgumentException("Resolved path escapes storage base path");
        }
        return path;
    }

    private Path resolveStoragePath(String storagePath) {
        Path path = basePath.resolve(storagePath).normalize();
        if (!path.startsWith(basePath)) {
            throw new IllegalArgumentException("Resolved storage path escapes storage base path");
        }
        return path;
    }

    private String[] normalizedSegments(String folderPath) {
        String normalizedPath = folderPath == null ? "" : folderPath.trim();
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        if (normalizedPath.isBlank()) {
            return new String[0];
        }

        String[] segments = normalizedPath.split("/");
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment) || segment.contains("\\")) {
                throw new IllegalArgumentException("Invalid folder path segment: " + segment);
            }
        }
        return segments;
    }

    private String safeOwnerSegment(String ownerSubject) {
        return ownerSubject.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
