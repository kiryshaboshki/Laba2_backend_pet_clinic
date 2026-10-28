package app.http;

import app.domain.Pet;
import app.service.PetService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class PetsHandler implements HttpHandler {
    private final PetService service;

    public PetsHandler(PetService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if ("GET".equals(method)) {
                List<Pet> pets = service.list(null, null);
                byte[] body = Json.write(pets);
                sendJson(exchange, 200, body);
            } else if ("POST".equals(method)) {
                Pet input = Json.read(exchange.getRequestBody(), Pet.class);
                Pet created = service.create(input);
                byte[] body = Json.write(created);
                sendJson(exchange, 201, body);
            } else {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
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