package app;

import app.http.*;
import app.service.AppointmentService;
import app.service.PetService;
import app.store.InMemoryStore;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {

        InMemoryStore store = new InMemoryStore(); // хранилище, сервис
        PetService petService = new PetService(store);
        AppointmentService appointmentService = new AppointmentService(store);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/ping", new PingHandler());
        server.createContext("/pets", new PetsHandler(petService));
        server.createContext("/pets/", new PetByIdHandler(petService));
        server.createContext("/appointments", new AppointmentsHandler(appointmentService));
        server.createContext("/appointments/", new AppointmentByIdHandler(appointmentService));

        server.start();
        System.out.println("Server started: http://localhost:" + PORT + "/ping");
    }
}

//  для коммита в новую ветку и пул рек