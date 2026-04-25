package de.lind3.CloudLite.storage;

/**
 * Result of a single streaming blob write.
 * All three values are derived during one pass over the data — no second read is required.
 *
 * @param storageKey opaque key that locates the blob in the storage backend
 * @param sha256     hex-encoded SHA-256 digest of the written content
 * @param sizeBytes  exact number of bytes written
 */
public record BlobWriteResult(String storageKey, String sha256, long sizeBytes) {}
