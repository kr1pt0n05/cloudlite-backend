package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.upload.UploadSessionEntity;
import de.lind3.CloudLite.upload.UploadSessionFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UploadSessionFileRepository extends JpaRepository<UploadSessionFileEntity, UUID> {

    List<UploadSessionFileEntity> findBySession(UploadSessionEntity session);

    List<UploadSessionFileEntity> findBySessionAndStatus(UploadSessionEntity session, UploadSessionFileStatus status);

    /** Returns the file names among {@code names} that are already staged in the given session. */
    @Query("SELECT f.fileName FROM UploadSessionFileEntity f WHERE f.session = :session AND f.fileName IN :names")
    List<String> findExistingFileNamesInSession(@Param("session") UploadSessionEntity session,
                                                @Param("names") Collection<String> names);
}
