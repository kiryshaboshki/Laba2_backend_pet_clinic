package app.http;

import app.domain.Appointment;
import app.service.AppointmentService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class AppointmentsHandler implements HttpHandler {
    private final AppointmentService service;

    public AppointmentsHandler(AppointmentService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if ("GET".equals(method)) {
                //фильтры не передаем (null, null)
                byte[] body = Json.write(service.list(null, null));
                sendJson(exchange, 200, body);
            } else if ("POST".equals(method)) {
                Appointment input = Json.read(exchange.getRequestBody(), Appointment.class);
                Appointment created = service.create(input);
                sendJson(exchange, 201, Json.write(created));
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