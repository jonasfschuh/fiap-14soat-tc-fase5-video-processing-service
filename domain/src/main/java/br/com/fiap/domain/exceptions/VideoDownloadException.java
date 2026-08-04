package br.com.fiap.domain.exceptions;

public class VideoDownloadException extends RuntimeException {

    public VideoDownloadException(String storageKey, Throwable cause) {
        super("Failed to download video from storage: " + storageKey, cause);
    }
}
