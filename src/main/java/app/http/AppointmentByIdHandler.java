package app.http;

import app.domain.Appointment;
import app.service.AppointmentService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppointmentByIdHandler implements HttpHandler {
    private final AppointmentService service;
    private static final Pattern PATH = Pattern.compile("^/appointments/([^/]+)(?:/(complete|cancel))?$");

    public AppointmentByIdHandler(AppointmentService service) {
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
            String action = matcher.group(2); // "complete" "cancel" или null
            String method = exchange.getRequestMethod();

            if (action == null) {
                if ("GET".equals(method)) {
                    Appointment app = service.get(id);
                    sendJson(exchange, 200, Json.write(app));
                } else if ("PUT".equals(method)) {
                    Appointment input = Json.read(exchange.getRequestBody(), Appointment.class);
                    Appointment updated = service.update(id, input);
                    sendJson(exchange, 200, Json.write(updated));
                } else if ("DELETE".equals(method)) {
                    service.delete(id);
                    exchange.sendResponseHeaders(204, -1);
                } else {
                    exchange.getResponseHeaders().set("Allow", "GET, PUT, DELETE");
                    exchange.sendResponseHeaders(405, -1);
                }
            } else {
                // Операции по правилам
                if (!"POST".equals(method)) {
                    exchange.getResponseHeaders().set("Allow", "POST");
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                if ("complete".equals(action)) {
                    Appointment completed = service.complete(id);
                    sendJson(exchange, 200, Json.write(completed));
                } else if ("cancel".equals(action)) {
                    Appointment cancelled = service.cancel(id);
                    sendJson(exchange, 200, Json.write(cancelled));
                }
            }
        } catch (HttpError e) {
            ErrorMapper.write(exchange, e);
        } catch (Exception e) {
            ErrorMapper.write(exchange, HttpError.badRequest("Invalid request"));
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