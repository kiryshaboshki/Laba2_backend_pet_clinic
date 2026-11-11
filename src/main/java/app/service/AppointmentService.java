package app.service;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.domain.PetType;
import app.exception.BadRequestException;
import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.repository.AppointmentRepository;
import app.repository.PetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, PetRepository petRepository) {
        this.appointmentRepository = appointmentRepository;
        this.petRepository = petRepository;
    }

    public List<Appointment> list(String vetName, AppointmentStatus status) {
        return appointmentRepository.findAll().stream()
                .filter(a -> vetName == null || a.getVetName().equalsIgnoreCase(vetName))
                .filter(a -> status == null || a.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Appointment get(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
    }

    public Appointment create(Appointment appointment) {
        validate(appointment, null);

        appointment.setId(null);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointmentRepository.save(appointment);
    }

    public Appointment update(String id, Appointment updated) {
        Appointment existing = get(id);
        if (existing.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Only SCHEDULED appointments can be updated");
        }

        validate(updated, id);

        existing.setPetId(updated.getPetId());
        existing.setVetName(updated.getVetName());
        existing.setDateTime(updated.getDateTime());
        existing.setDurationMinutes(updated.getDurationMinutes());

        return appointmentRepository.save(existing);
    }

    public void delete(String id) {
        get(id);
        appointmentRepository.deleteById(id);
    }

    public Appointment complete(String id) {
        Appointment appointment = get(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Only SCHEDULED appointments can be completed");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    public Appointment cancel(String id) {
        Appointment appointment = get(id);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Cannot cancel a completed appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Appointment is already cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appointment);
    }

    private void validate(Appointment appointment, String currentId) {
        if (appointment == null) {
            throw new BadRequestException("Appointment data must not be null");
        }

        if (appointment.getVetName() == null || appointment.getVetName().isBlank()) {
            throw new BadRequestException("Vet name must not be empty");
        }

        if (appointment.getPetId() == null || appointment.getPetId().isBlank()) {
            throw new BadRequestException("Pet ID must not be empty");
        }

        Pet pet = petRepository.findById(appointment.getPetId())
                .orElseThrow(() -> new NotFoundException("Pet not found: " + appointment.getPetId()));

        if (pet.getType() == PetType.DOG && !pet.isVaccinated()) {
            throw new ConflictException("Dog must be vaccinated before appointment");
        }

        if (appointment.getDateTime() == null || appointment.getDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Appointment date must be in the future");
        }

        if (appointment.getDurationMinutes() <= 0) {
            throw new BadRequestException("Duration must be positive");
        }

        LocalDateTime newStart = appointment.getDateTime();
        LocalDateTime newEnd = newStart.plusMinutes(appointment.getDurationMinutes());

        for (Appointment existing : appointmentRepository.findAll()) {
            if (existing.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }
            if (existing.getId() != null && existing.getId().equals(currentId)) {
                continue;
            }

            if (existing.getVetName().equalsIgnoreCase(appointment.getVetName())) {
                LocalDateTime existingStart = existing.getDateTime();
                LocalDateTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes());

                if (newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd)) {
                    throw new ConflictException("Vet is already booked at this time");
                }
            }
        }
    }
}
