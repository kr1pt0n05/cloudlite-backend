package de.lind3.CloudLite.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Streams file content to deterministic filesystem paths below the storage root.
 */
public interface FileStorageService {

    FileWriteResult store(String ownerSubject, String folderPath, String fileName, InputStream content)
            throws IOException;

    boolean exists(String ownerSubject, String folderPath, String fileName);

    void createDirectory(String ownerSubject, String folderPath) throws IOException;

    void delete(String storagePath) throws IOException;

    void deleteAll(Collection<FileWriteResult> writtenFiles);
}
