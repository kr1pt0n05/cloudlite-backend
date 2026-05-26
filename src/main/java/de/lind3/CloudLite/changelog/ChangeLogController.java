package de.lind3.CloudLite.changelog;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for client sync change logs.
 */
@RestController
@RequestMapping("/api/changelogs")
@RequiredArgsConstructor
public class ChangeLogController {

    private final ChangeLogService changeLogService;

    @GetMapping
    public ResponseEntity<List<ChangeLogResponse>> getChangesSince(
            @RequestParam(name = "latestSyncedId", defaultValue = "0") Long latestSyncedId,
            @AuthenticationPrincipal Jwt jwt) {

        List<ChangeLogResponse> response = changeLogService
                .getChangesSince(latestSyncedId, jwt.getSubject())
                .stream()
                .map(ChangeLogResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
