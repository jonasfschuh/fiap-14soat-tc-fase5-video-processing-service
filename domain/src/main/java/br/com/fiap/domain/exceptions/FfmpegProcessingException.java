package br.com.fiap.domain.exceptions;

public class FfmpegProcessingException extends RuntimeException {

    public FfmpegProcessingException(String message) {
        super(message);
    }

    public FfmpegProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
