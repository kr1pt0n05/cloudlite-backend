package de.lind3.CloudLite.controller;

import de.lind3.CloudLite.controller.dto.CreateSessionRequest;
import de.lind3.CloudLite.controller.dto.PublishedFileResponse;
import de.lind3.CloudLite.controller.dto.SessionResponse;
import de.lind3.CloudLite.controller.dto.StagedFileResponse;
import de.lind3.CloudLite.entity.FileEntity;
import de.lind3.CloudLite.entity.UploadSessionEntity;
import de.lind3.CloudLite.entity.UploadSessionFileEntity;
import de.lind3.CloudLite.service.UploadService;
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
 * POST   /api/upload/sessions               – create a session
 * POST   /api/upload/sessions/{id}/files    – stream a file into the session
 * POST   /api/upload/sessions/{id}/commit   – publish all staged files
 * DELETE /api/upload/sessions/{id}          – cancel and discard the session
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
     * Streams a single file into the session.
     *
     * <p>The file is uploaded as a {@code multipart/form-data} request. The SHA-256 digest
     * and byte count are computed during the single streaming write — the content is never
     * buffered in heap memory by the service layer.
     *
     * @param sessionId session to add the file to
     * @param file      multipart file part carrying the binary content
     * @param fileName  optional filename override; falls back to the multipart filename
     * @param jwt       authenticated user's JWT
     * @return 201 Created with the staged-file metadata (including sha256 and sizeBytes)
     */
    @PostMapping(value = "/{sessionId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StagedFileResponse> stageFile(
            @PathVariable UUID sessionId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String fileName,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        String resolvedName = (fileName != null && !fileName.isBlank())
                ? fileName
                : file.getOriginalFilename();

        UploadSessionFileEntity staged = uploadService.stageFile(
                sessionId,
                jwt.getSubject(),
                resolvedName,
                file.getInputStream(),
                file.getContentType());

        return ResponseEntity.status(HttpStatus.CREATED).body(StagedFileResponse.from(staged));
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
