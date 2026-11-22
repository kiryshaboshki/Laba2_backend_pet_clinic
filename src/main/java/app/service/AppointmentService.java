package app.service;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.exception.BadRequestException;
import app.exception.ConflictException;
import app.exception.NotFoundException;
import app.repository.AppointmentRepository;
import app.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
                .toList();
    }

    public List<Appointment> listDetailed() {
        return appointmentRepository.findAllWithPet();
    }

    public Appointment get(String id) {
        return appointmentRepository.findById(parseId(id))
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
    }

    @Transactional
    public Appointment create(Appointment appointment) {
        validate(appointment, null);
        appointment.setId(null);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment update(String id, Appointment updated) {
        Appointment existing = get(id);
        if (existing.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Only SCHEDULED appointments can be updated");
        }

        validate(updated, existing.getId());

        existing.setPet(updated.getPet());
        existing.setVetName(updated.getVetName());
        existing.setDateTime(updated.getDateTime());
        existing.setDurationMinutes(updated.getDurationMinutes());
        existing.setNotes(updated.getNotes());

        return appointmentRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        Appointment appointment = get(id);
        appointmentRepository.delete(appointment);
    }

    @Transactional
    public Appointment complete(String id) {
        Appointment appointment = get(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Only SCHEDULED appointments can be completed");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
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

    private void validate(Appointment appointment, UUID currentId) {
        if (appointment == null) {
            throw new BadRequestException("Appointment data must not be null");
        }

        if (appointment.getVetName() == null || appointment.getVetName().isBlank()) {
            throw new BadRequestException("Vet name must not be empty");
        }

        if (appointment.getPet() == null || appointment.getPet().getId() == null) {
            throw new BadRequestException("Pet ID must not be empty");
        }

        Pet pet = petRepository.findById(appointment.getPet().getId())
                .orElseThrow(() -> new NotFoundException("Pet not found: " + appointment.getPet().getId()));

        if (pet.getType().isVaccinationRequired() && !pet.isVaccinated()) {
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
            if (existing.getStatus() != AppointmentStatus.SCHEDULED) continue;
            if (existing.getId() != null && existing.getId().equals(currentId)) continue;

            if (existing.getVetName().equalsIgnoreCase(appointment.getVetName())) {
                LocalDateTime existingStart = existing.getDateTime();
                LocalDateTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes());
                if (newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd)) {
                    throw new ConflictException("Vet is already booked at this time");
                }
            }
        }
    }

    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Appointment not found: " + id);
        }
    }
}
