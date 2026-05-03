package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.file.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Handles mapped batch uploads.
 */
public interface UploadService {

    /**
     * Validates and streams multiple files into their mapped destination folders.
     * <p>
     * SHA-256 and byte count are computed during the single streaming write for each file —
     * content is never fully buffered in heap memory.
     * Database conflicts are checked before any filesystem write occurs.
     *
     * @param ownerSubject the JWT {@code sub} claim used to verify ownership
     * @param files        one or more multipart files; each filename must be prefixed
     *                     with its client-generated file ID
     * @param mapping      client file ID to destination folder path mapping
     * @return the list of committed {@link FileEntity} records
     * @throws IOException if any filesystem write fails
     */
    List<FileEntity> uploadFilesBatch(String ownerSubject, List<MultipartFile> files,
                                      BatchUploadFileMappingRequest mapping) throws IOException;
}
