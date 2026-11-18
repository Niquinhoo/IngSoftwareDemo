package ar.edu.ingsoftware.prode;

import org.springframework.http.HttpStatus;

public final class ProdeException extends RuntimeException {
    private final HttpStatus status;

    public ProdeException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
