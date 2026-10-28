package app.service;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.domain.PetType;
import app.http.HttpError;
import app.store.InMemoryStore;

import java.time.LocalDateTime;
import java.util.List;

public class AppointmentService {
    private final InMemoryStore store;

    public AppointmentService(InMemoryStore store) {
        this.store = store;
    }

    public List<Appointment> list(String vetName, AppointmentStatus status) {
        return store.findAllAppointments();
    }

    public Appointment get(String id) {
        return store.findAppointmentById(id)
                .orElseThrow(() -> HttpError.notFound("Appointment not found: " + id));
    }

    public Appointment create(Appointment appointment) {
        Pet pet = store.findPetById(appointment.getPetId())
                .orElseThrow(() -> HttpError.notFound("Pet not found: " + appointment.getPetId()));

        if (pet.getType() == PetType.DOG && !pet.isVaccinated()) {
            throw HttpError.conflict("Dog must be vaccinated before appointment");
        }

        if (appointment.getDateTime() == null || appointment.getDateTime().isBefore(LocalDateTime.now())) {
            throw HttpError.badRequest("Appointment date must be in the future");
        }

        if (appointment.getDurationMinutes() <= 0) {
            throw HttpError.badRequest("Duration must be positive");
        }

        for (Appointment existing : store.findAllAppointments()) {
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;

            if (existing.getVetName().equals(appointment.getVetName())) {
                if (existing.getDateTime().equals(appointment.getDateTime())) {
                    throw HttpError.conflict("Vet is already booked at this time");
                }
            }
        }

        appointment.setId(null);
        appointment.setStatus(AppointmentStatus.SCHEDULED); //создание всегда SCHEDULED
        return store.saveAppointment(appointment);
    }

    public Appointment update(String id, Appointment updated) {
        Appointment existing = get(id);

        existing.setPetId(updated.getPetId());
        existing.setVetName(updated.getVetName());
        existing.setDateTime(updated.getDateTime());
        existing.setDurationMinutes(updated.getDurationMinutes());

        return store.saveAppointment(existing);
    }

    public Appointment complete(String id) {
        Appointment appointment = get(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw HttpError.conflict("Only SCHEDULED appointments can be completed");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return store.saveAppointment(appointment);
    }

    public Appointment cancel(String id) {
        Appointment appointment = get(id);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw HttpError.conflict("Cannot cancel a completed appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw HttpError.conflict("Appointment is already cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return store.saveAppointment(appointment);
    }
}