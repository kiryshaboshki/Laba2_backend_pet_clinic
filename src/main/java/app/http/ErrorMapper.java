package app.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public final class ErrorMapper {

    private ErrorMapper() {}

    public static void write(HttpExchange exchange, HttpError error) throws IOException {
        //"error": "текст ошибки"
        byte[] body = Json.write(Map.of("error", error.getMessage()));

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(error.getStatus(), body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}