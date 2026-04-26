package de.lind3.CloudLite.folder;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for folder operations.
 *
 * <pre>
 * POST /api/folders – create a new folder
 * </pre>
 */
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

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
}
