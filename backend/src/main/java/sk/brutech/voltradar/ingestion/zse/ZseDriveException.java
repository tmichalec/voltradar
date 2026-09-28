package sk.brutech.voltradar.ingestion.zse;

/** Upstream failure; callers must not replace existing data with an empty successful snapshot. */
public class ZseDriveException extends RuntimeException {
    private final Integer statusCode;

    public ZseDriveException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
