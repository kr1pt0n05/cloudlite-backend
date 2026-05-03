package de.lind3.CloudLite.file;

/**
 * Result of a single streaming file write.
 *
 * @param storagePath relative path below the configured storage base path
 * @param sha256      hex-encoded SHA-256 digest of the written content
 * @param sizeBytes   exact number of bytes written
 */
public record FileWriteResult(String storagePath, String sha256, long sizeBytes) {}
