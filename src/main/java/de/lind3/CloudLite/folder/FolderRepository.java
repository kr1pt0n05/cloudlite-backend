package de.lind3.CloudLite.folder;

import de.lind3.CloudLite.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<FolderEntity, UUID> {

    /** Returns all non-deleted direct children of a given parent for a specific owner. */
    List<FolderEntity> findByParentAndOwnerAndDeletedAtIsNull(FolderEntity parent, UserEntity owner);

    /** Returns all non-deleted root folders (parent == null) for a specific owner. */
    List<FolderEntity> findByParentIsNullAndOwnerAndDeletedAtIsNull(UserEntity owner);

    Optional<FolderEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByParentAndOwnerAndNameAndDeletedAtIsNull(FolderEntity parent, UserEntity owner, String name);
}
