package de.lind3.CloudLite.blob;

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
import java.util.HexFormat;
import java.util.UUID;

/**
 * Local filesystem implementation of {@link BlobStorageService}.
 * Blobs are stored under {@code file.storage.base-path} in a two-level directory
 * derived from the first two characters of the storage key (git-style sharding),
 * e.g. {@code <base>/ab/abcdef1234…}.
 *
 * <p>The SHA-256 digest and byte count are computed <em>during</em> the single streaming
 * write via a {@link DigestInputStream} wrapper — no extra pass over the data is needed.
 * Content is written to a {@code .tmp} sibling first and atomically renamed on success
 * to prevent readers from observing partial blobs.</p>
 */
@Service
public class LocalBlobStorageService implements BlobStorageService {

    private final Path basePath;

    public LocalBlobStorageService(
            @Value("${file.storage.base-path:./.uploads}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(basePath);
    }

    /**
     * Streams {@code content} to the local filesystem.
     * SHA-256 and byte count are derived from a single pass — the caller does not need
     * to buffer or re-read the stream.
     *
     * @param content      the raw file stream (closed by caller)
     * @param suggestedKey preferred storage key; a random UUID is used when null or blank
     * @return write result containing storage key, hex SHA-256, and byte count
     * @throws IOException on any I/O failure
     */
    @Override
    public BlobWriteResult store(InputStream content, String suggestedKey) throws IOException {
        String storageKey = (suggestedKey != null && !suggestedKey.isBlank())
                ? suggestedKey
                : UUID.randomUUID().toString();

        Path targetPath = resolvePath(storageKey);
        Path tempPath   = targetPath.resolveSibling(storageKey + ".tmp");
        Files.createDirectories(targetPath.getParent());

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }

        long sizeBytes;
        try (DigestInputStream dis = new DigestInputStream(content, digest);
             OutputStream out = Files.newOutputStream(tempPath)) {
            sizeBytes = dis.transferTo(out);
        } catch (IOException e) {
            Files.deleteIfExists(tempPath);
            throw e;
        }

        Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE);

        String sha256 = HexFormat.of().formatHex(digest.digest());
        return new BlobWriteResult(storageKey, sha256, sizeBytes);
    }

    @Override
    public InputStream retrieve(String storageKey) throws IOException {
        return Files.newInputStream(resolvePath(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolvePath(storageKey));
    }

    /**
     * Maps a storage key to a filesystem path.
     * The first two characters become a bucket directory to avoid huge flat directories.
     */
    private Path resolvePath(String storageKey) {
        if (storageKey.length() < 2) {
            return basePath.resolve("__short").resolve(storageKey);
        }
        return basePath.resolve(storageKey.substring(0, 2)).resolve(storageKey);
    }
}
