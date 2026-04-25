package de.lind3.CloudLite.controller.dto;

import java.util.UUID;

/**
 * Request body for creating a new upload session.
 *
 * @param targetFolderId destination folder where files will be published on commit
 */
public record CreateSessionRequest(UUID targetFolderId) {}
