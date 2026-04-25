package de.lind3.CloudLite.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Pluggable abstraction over the binary blob storage backend.
 * The v1 implementation writes blobs to the local filesystem.
 * Future implementations may target S3, MinIO, or other object stores.
 */
public interface BlobStorageService {

    /**
     * Streams the given content to storage, computing the SHA-256 digest and byte count
     * in a single pass. Returns a {@link BlobWriteResult} containing the storage key,
     * hex SHA-256, and byte count — no second read of the content is required.
     *
     * @param content      readable stream of the raw file bytes (not buffered by the caller)
     * @param suggestedKey preferred storage key; implementations may ignore or adjust it
     * @return write result with storage key, sha256, and size
     * @throws IOException if the write fails
     */
    BlobWriteResult store(InputStream content, String suggestedKey) throws IOException;

    /**
     * Opens a stream to read the blob identified by {@code storageKey}.
     *
     * @param storageKey the key returned by {@link #store}
     * @return a readable stream; the caller is responsible for closing it
     * @throws IOException if the blob does not exist or cannot be read
     */
    InputStream retrieve(String storageKey) throws IOException;

    /**
     * Permanently removes the blob from storage.
     *
     * @param storageKey the key returned by {@link #store}
     * @throws IOException if the deletion fails
     */
    void delete(String storageKey) throws IOException;
}
