package de.lind3.CloudLite.folder;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for folder operations.
 *
 * <pre>
 * GET  /api/folders       – list root folders or children of a parent folder
 * POST /api/folders       – create a new folder
 * POST /api/folders/batch – create multiple folders as a tree
 * </pre>
 */
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    /**
     * Lists root folders or direct child folders of a parent folder.
     *
     * @param parentId optional parent folder ID; omitted for root folders
     * @param jwt      authenticated user's JWT
     * @return 200 OK with folder metadata
     */
    @GetMapping
    public ResponseEntity<List<FolderResponse>> listFolders(
            @RequestParam(required = false) UUID parentId,
            @AuthenticationPrincipal Jwt jwt) {

        List<FolderResponse> response = folderService
                .listSubFolders(parentId, jwt.getSubject())
                .stream()
                .map(FolderResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new folder for the authenticated user.
     *
     * @param request request body with {@code name} and optional {@code parentId}
     * @param jwt     authenticated user's JWT
     * @return 201 Created with the folder metadata
     */
    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @RequestBody CreateFolderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        FolderEntity folder = folderService.createFolder(
                request.name(), request.parentId(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(FolderResponse.from(folder));
    }

    /**
     * Creates multiple folders as a tree.
     *
     * @param request request body with optional {@code parentId} and directory tree
     * @param jwt     authenticated user's JWT
     * @return 201 Created with the created folder metadata in pre-order traversal
     */
    @PostMapping("/batch")
    public ResponseEntity<List<FolderResponse>> createFoldersBatch(
            @RequestBody CreateFolderBatchRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        List<FolderResponse> response = folderService
                .createFoldersBatch(request.parentId(), request.directories(), jwt.getSubject())
                .stream()
                .map(FolderResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
