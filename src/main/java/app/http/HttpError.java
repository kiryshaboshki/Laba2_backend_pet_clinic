package app.http;

public class HttpError extends RuntimeException {

    private final int status;

    public HttpError(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static HttpError badRequest(String msg)  { return new HttpError(400, msg); }
    public static HttpError notFound(String msg)    { return new HttpError(404, msg); }
    public static HttpError conflict(String msg)    { return new HttpError(409, msg); }
}