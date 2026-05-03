package de.lind3.CloudLite.upload;

/**
 * Destination mapping for a multipart file.
 *
 * @param fileId UUID prefix added to the multipart filename by the client
 * @param path   destination folder path, e.g. "/Documents/Photos"
 */
public record UploadFileMappingRequest(String fileId, String path) {}
