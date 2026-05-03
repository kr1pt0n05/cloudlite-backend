package de.lind3.CloudLite.folder;

import de.lind3.CloudLite.changelog.ChangeLogEntity;
import de.lind3.CloudLite.changelog.ChangeLogService;
import de.lind3.CloudLite.changelog.EntityType;
import de.lind3.CloudLite.changelog.EventType;
import de.lind3.CloudLite.user.UserEntity;
import de.lind3.CloudLite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ChangeLogService changeLogService;

    // -------------------------------------------------------------------------
    // createFolder
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public FolderEntity createFolder(String name, UUID parentId, String ownerSubject) {
        String folderName = validateFolderName(name);

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

        if (folderRepository.existsByParentAndOwnerAndNameAndDeletedAtIsNull(parent, owner, folderName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A folder named '" + folderName + "' already exists in this location");
        }

        FolderEntity folder = new FolderEntity();
        folder.setName(folderName);
        folder.setParent(parent);
        folder.setOwner(owner);
        folder.setPath(buildPath(parent, folderName));
        FolderEntity savedFolder = folderRepository.save(folder);
        changeLogService.logChange(
                EventType.CREATE,
                EntityType.DIRECTORY,
                null,
                savedFolder,
                owner
        );
        return savedFolder;
    }

    // -------------------------------------------------------------------------
    // createFoldersBatch
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public List<FolderEntity> createFoldersBatch(
            UUID parentId,
            List<CreateFolderTreeNodeRequest> directories,
            String ownerSubject
    ) {
        if (directories == null || directories.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one directory is required");
        }

        UserEntity owner = resolveOrProvisionUser(ownerSubject);
        FolderEntity parent = resolveParent(parentId, owner);

        List<FolderEntity> folders = new ArrayList<>();
        collectFolders(parent, directories, owner, folders);

        List<FolderEntity> savedFolders = folderRepository.saveAll(folders);
        changeLogService.logChanges(buildFolderChangeLogs(savedFolders, owner));
        return savedFolders;
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

    private FolderEntity resolveParent(UUID parentId, UserEntity owner) {
        if (parentId == null) {
            return null;
        }

        FolderEntity parent = folderRepository.findByIdAndDeletedAtIsNull(parentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Parent folder not found: " + parentId));
        if (!parent.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Parent folder does not belong to the requesting user");
        }
        return parent;
    }

    private void collectFolders(
            FolderEntity parent,
            List<CreateFolderTreeNodeRequest> nodes,
            UserEntity owner,
            List<FolderEntity> folders
    ) {
        Set<String> siblingNames = new HashSet<>();
        for (CreateFolderTreeNodeRequest node : nodes) {
            String name = validateFolderName(node.name());
            if (!siblingNames.add(name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Duplicate folder name in request: " + name);
            }
            if ((parent == null || parent.getId() != null)
                    && folderRepository.existsByParentAndOwnerAndNameAndDeletedAtIsNull(parent, owner, name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A folder named '" + name + "' already exists in this location");
            }

            FolderEntity folder = new FolderEntity();
            folder.setName(name);
            folder.setParent(parent);
            folder.setOwner(owner);
            folder.setPath(buildPath(parent, name));
            folders.add(folder);

            if (node.children() != null && !node.children().isEmpty()) {
                collectFolders(folder, node.children(), owner, folders);
            }
        }
    }

    private List<ChangeLogEntity> buildFolderChangeLogs(List<FolderEntity> folders, UserEntity owner) {
        List<ChangeLogEntity> changeLogs = new ArrayList<>(folders.size());
        for (FolderEntity folder : folders) {
            ChangeLogEntity changeLog = new ChangeLogEntity();
            changeLog.setEventType(EventType.CREATE);
            changeLog.setEntityType(EntityType.DIRECTORY);
            changeLog.setFolder(folder);
            changeLog.setUser(owner);
            changeLogs.add(changeLog);
        }
        return changeLogs;
    }

    private String validateFolderName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must not be blank");
        }
        String trimmedName = name.trim();
        if (".".equals(trimmedName) || "..".equals(trimmedName)
                || trimmedName.contains("/") || trimmedName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Folder name must be a single path segment: " + name);
        }
        return trimmedName;
    }

    private String buildPath(FolderEntity parent, String name) {
        if (parent == null) {
            return "/" + name;
        }
        return parent.getPath() + "/" + name;
    }
}
