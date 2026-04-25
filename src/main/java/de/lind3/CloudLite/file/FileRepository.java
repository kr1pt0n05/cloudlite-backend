package de.lind3.CloudLite.file;

import de.lind3.CloudLite.folder.FolderEntity;
import de.lind3.CloudLite.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    /** Returns all non-deleted files inside a folder. */
    List<FileEntity> findByFolderAndDeletedAtIsNull(FolderEntity folder);

    Optional<FileEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<FileEntity> findByFolderAndNameAndDeletedAtIsNull(FolderEntity folder, String name);

    boolean existsByFolderAndNameAndDeletedAtIsNull(FolderEntity folder, String name);

    List<FileEntity> findByOwnerAndDeletedAtIsNull(UserEntity owner);
}
