package app.repository;

import app.domain.Appointment;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    List<Appointment> findAll();

    Optional<Appointment> findById(String id);

    Appointment save(Appointment appointment);

    void deleteById(String id);
}
