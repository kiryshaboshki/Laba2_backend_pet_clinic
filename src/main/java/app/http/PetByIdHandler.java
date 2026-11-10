package app.http;

import app.domain.Pet;
import app.service.PetService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetByIdHandler implements HttpHandler {
    private final PetService service;
    private static final Pattern PATH = Pattern.compile("^/pets/([^/]+)$");

    public PetByIdHandler(PetService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Matcher matcher = PATH.matcher(exchange.getRequestURI().getPath());
            if (!matcher.matches()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String id = matcher.group(1);
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                Pet pet = service.get(id);
                sendJson(exchange, 200, Json.write(pet));
            } else if ("PUT".equals(method)) {
                Pet input = Json.read(exchange.getRequestBody(), Pet.class);
                Pet updated = service.update(id, input);
                sendJson(exchange, 200, Json.write(updated));
            } else if ("DELETE".equals(method)) {
                service.delete(id);
                exchange.sendResponseHeaders(204, -1); // 204 No Content
            } else {
                exchange.getResponseHeaders().set("Allow", "GET, PUT, DELETE");
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (HttpError e) {
            ErrorMapper.write(exchange, e);
        } catch (Exception e) {
            ErrorMapper.write(exchange, HttpError.badRequest("Invalid JSON format"));
        } finally {
            exchange.close();
        }
    }

    private void sendJson(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}