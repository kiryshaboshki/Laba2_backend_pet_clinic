package app.repository;

import app.domain.Appointment;
import app.domain.AppointmentStatus;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAppointmentRepository {

    public static AppointmentRepository create() {
        Map<UUID, Appointment> storage = new ConcurrentHashMap<>();

        return (AppointmentRepository) Proxy.newProxyInstance(
                AppointmentRepository.class.getClassLoader(),
                new Class<?>[]{AppointmentRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        Appointment appointment = (Appointment) args[0];
                        if (appointment.getId() == null) {
                            appointment.setId(UUID.randomUUID());
                        }
                        storage.put(appointment.getId(), appointment);
                        yield appointment;
                    }
                    case "findById" -> {
                        UUID id = (UUID) args[0];
                        yield Optional.ofNullable(storage.get(id));
                    }
                    case "findAll" -> new ArrayList<>(storage.values());
                    case "delete" -> {
                        Appointment appointment = (Appointment) args[0];
                        if (appointment != null && appointment.getId() != null) {
                            storage.remove(appointment.getId());
                        }
                        yield null;
                    }
                    case "deleteById" -> {
                        UUID id = (UUID) args[0];
                        storage.remove(id);
                        yield null;
                    }
                    case "deleteAll" -> {
                        if (args != null && args.length > 0 && args[0] instanceof Iterable<?> iterable) {
                            for (Object item : iterable) {
                                if (item instanceof Appointment a && a.getId() != null) {
                                    storage.remove(a.getId());
                                }
                            }
                        } else {
                            storage.clear();
                        }
                        yield null;
                    }
                    case "findByPet_Id" -> {
                        UUID petId = (UUID) args[0];
                        yield storage.values().stream()
                                .filter(a -> a.getPet() != null && petId.equals(a.getPet().getId()))
                                .toList();
                    }
                    case "existsByPet_IdAndStatus" -> {
                        UUID petId = (UUID) args[0];
                        AppointmentStatus status = (AppointmentStatus) args[1];
                        yield storage.values().stream()
                                .anyMatch(a -> a.getPet() != null && petId.equals(a.getPet().getId()) && a.getStatus() == status);
                    }
                    case "count" -> (long) storage.size();
                    case "existsById" -> storage.containsKey(args[0]);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "InMemoryAppointmentRepository@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> throw new UnsupportedOperationException("Method " + method.getName() + " is not implemented in InMemoryAppointmentRepository");
                }
        );
    }
}
