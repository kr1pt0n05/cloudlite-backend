package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.file.FileEntity;
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

/**
 * REST endpoints for mapped batch uploads.
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * Streams one or more files to their mapped filesystem paths in a single request.
     *
     * <p>The `mapping` JSON part must contain one entry per file. The multipart filename
     * must start with the client-generated UUID referenced by that mapping.
     *
     * <p>If any file fails, files already written in the same request are cleaned up on a
     * best-effort basis and no file metadata is committed.
     *
     * @param files     one or more multipart file parts; each part's {@code Content-Disposition}
     *                  filename is used as the client-prefixed source filename
     * @param jwt       authenticated user's JWT
     * @param mapping   JSON part mapping each file ID to a destination folder path
     * @return 201 Created with a list of committed file metadata entries
     */
    @PostMapping(value = "/files/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PublishedFileResponse>> uploadFiles(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("mapping") BatchUploadFileMappingRequest mapping,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        List<FileEntity> uploaded = uploadService.uploadFilesBatch(
                jwt.getSubject(), files, mapping);
        List<PublishedFileResponse> response = uploaded.stream()
                .map(PublishedFileResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
