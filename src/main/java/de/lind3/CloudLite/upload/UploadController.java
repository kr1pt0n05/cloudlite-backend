package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.upload.CreateSessionRequest;
import de.lind3.CloudLite.upload.PublishedFileResponse;
import de.lind3.CloudLite.upload.SessionResponse;
import de.lind3.CloudLite.upload.StagedFileResponse;
import de.lind3.CloudLite.file.FileEntity;
import de.lind3.CloudLite.upload.UploadSessionEntity;
import de.lind3.CloudLite.upload.UploadSessionFileEntity;
import de.lind3.CloudLite.upload.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the batch-upload session lifecycle.
 *
 * <pre>
 * POST   /api/upload/sessions                   – create a session
 * POST   /api/upload/sessions/{id}/files/batch  – stream one or more files into the session
 * POST   /api/upload/sessions/{id}/commit        – publish all staged files
 * DELETE /api/upload/sessions/{id}              – cancel and discard the session
 * </pre>
 */
@RestController
@RequestMapping("/api/upload/sessions")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * Creates a new upload session targeting the given folder.
     *
     * @param request request body with {@code targetFolderId}
     * @param jwt     authenticated user's JWT
     * @return 201 Created with the session metadata
     */
    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UploadSessionEntity session = uploadService.createSession(jwt.getSubject(), request.targetFolderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.from(session));
    }

    /**
     * Streams one or more files into the session in a single request.
     *
     * <p>All duplicate-name checks are performed with a single database query before any
     * storage writes. Files are streamed individually to the storage backend without being
     * buffered in heap memory.
     *
     * <p>If any file fails, files already written to storage in the same request remain
     * staged; the caller should either commit or cancel the session accordingly.
     *
     * @param sessionId session to add the files to
     * @param files     one or more multipart file parts; each part's {@code Content-Disposition}
     *                  filename is used as the staged filename
     * @param jwt       authenticated user's JWT
     * @return 201 Created with a list of staged-file metadata entries, one per uploaded file
     */
    @PostMapping(value = "/{sessionId}/files/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<StagedFileResponse>> stageFiles(
            @PathVariable UUID sessionId,
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        List<UploadSessionFileEntity> staged = uploadService.stageFilesBatch(
                sessionId, jwt.getSubject(), files);
        List<StagedFileResponse> response = staged.stream()
                .map(StagedFileResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Commits the session, atomically publishing all staged files into the target folder.
     *
     * @param sessionId session to commit
     * @param jwt       authenticated user's JWT
     * @return 200 OK with the list of newly visible file records
     */
    @PostMapping("/{sessionId}/commit")
    public ResponseEntity<List<PublishedFileResponse>> commitSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        List<FileEntity> published = uploadService.commitSession(sessionId, jwt.getSubject());
        List<PublishedFileResponse> response = published.stream()
                .map(PublishedFileResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an open session, discarding all staged metadata.
     *
     * @param sessionId session to cancel
     * @param jwt       authenticated user's JWT
     * @return 204 No Content
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> cancelSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        uploadService.cancelSession(sessionId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
