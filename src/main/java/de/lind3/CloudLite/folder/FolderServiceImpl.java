package de.lind3.CloudLite.folder;

import de.lind3.CloudLite.user.UserEntity;
import de.lind3.CloudLite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // createFolder
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public FolderEntity createFolder(String name, UUID parentId, String ownerSubject) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must not be blank");
        }

        UserEntity owner = resolveOrProvisionUser(ownerSubject);

        FolderEntity parent = null;
        if (parentId != null) {
            parent = folderRepository.findByIdAndDeletedAtIsNull(parentId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Parent folder not found: " + parentId));
            if (!parent.getOwner().getId().equals(owner.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Parent folder does not belong to the requesting user");
            }
        }

        if (folderRepository.existsByParentAndOwnerAndNameAndDeletedAtIsNull(parent, owner, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A folder named '" + name + "' already exists in this location");
        }

        FolderEntity folder = new FolderEntity();
        folder.setName(name);
        folder.setParent(parent);
        folder.setOwner(owner);
        return folderRepository.save(folder);
    }

    // -------------------------------------------------------------------------
    // folderExists
    // -------------------------------------------------------------------------

    @Override
    public boolean folderExists(UUID folderId) {
        return folderRepository.findByIdAndDeletedAtIsNull(folderId).isPresent();
    }

    // -------------------------------------------------------------------------
    // getFolder
    // -------------------------------------------------------------------------

    @Override
    public FolderEntity getFolder(UUID folderId, String requesterSubject) {
        FolderEntity folder = folderRepository.findByIdAndDeletedAtIsNull(folderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Folder not found: " + folderId));

        // Intentionally use findBySubject here: a read operation should not
        // auto-provision a new user; if the subject is unknown, the folder
        // cannot belong to them.
        UserEntity requester = userRepository.findBySubject(requesterSubject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!folder.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Folder does not belong to the requesting user");
        }

        return folder;
    }

    // -------------------------------------------------------------------------
    // listSubFolders
    // -------------------------------------------------------------------------

    @Override
    public List<FolderEntity> listSubFolders(UUID folderId, String requesterSubject) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // -------------------------------------------------------------------------
    // renameFolder
    // -------------------------------------------------------------------------

    @Override
    public FolderEntity renameFolder(UUID folderId, String newName, String requesterSubject) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // -------------------------------------------------------------------------
    // moveFolder
    // -------------------------------------------------------------------------

    @Override
    public FolderEntity moveFolder(UUID folderId, UUID newParentId, String requesterSubject) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // -------------------------------------------------------------------------
    // deleteFolder
    // -------------------------------------------------------------------------

    @Override
    public void deleteFolder(UUID folderId, String requesterSubject) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link UserEntity} for the given JWT subject, creating one if
     * this is the user's first interaction with the system (JIT provisioning).
     */
    private UserEntity resolveOrProvisionUser(String subject) {
        return userRepository.findBySubject(subject)
                .orElseGet(() -> userRepository.save(new UserEntity(subject)));
    }
}
