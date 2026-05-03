package de.lind3.CloudLite.upload;

import java.util.List;

/**
 * JSON multipart part mapping client-prefixed files to destination folder paths.
 *
 * @param files one mapping per uploaded file
 */
public record BatchUploadFileMappingRequest(List<UploadFileMappingRequest> files) {}
