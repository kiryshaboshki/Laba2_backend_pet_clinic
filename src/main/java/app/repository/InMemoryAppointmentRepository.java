package app.repository;

import app.domain.Appointment;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<String, Appointment> appointments = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public List<Appointment> findAll() {
        return new ArrayList<>(appointments.values());
    }

    @Override
    public Optional<Appointment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(appointments.get(id));
    }

    @Override
    public Appointment save(Appointment appointment) {
        if (appointment.getId() == null) {
            appointment.setId(String.valueOf(idCounter.getAndIncrement()));
        }
        appointments.put(appointment.getId(), appointment);
        return appointment;
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            appointments.remove(id);
        }
    }
}
