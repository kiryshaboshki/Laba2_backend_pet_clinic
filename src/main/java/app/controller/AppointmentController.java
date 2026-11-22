package app.controller;

import app.domain.Appointment;
import app.domain.AppointmentStatus;
import app.domain.Pet;
import app.exception.BadRequestException;
import app.exception.NotFoundException;
import app.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@SuppressWarnings("unused")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<Appointment> list(
            @RequestParam(required = false) String vetName,
            @RequestParam(required = false) String status) {
        AppointmentStatus appointmentStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status. Use SCHEDULED, COMPLETED or CANCELLED");
            }
        }
        return appointmentService.list(vetName, appointmentStatus);
    }

    @GetMapping("/detailed")
    public List<Appointment> listDetailed() {
        return appointmentService.listDetailed();
    }

    @GetMapping("/{id}")
    public Appointment get(@PathVariable String id) {
        return appointmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment create(@RequestBody AppointmentRequest request) {
        return appointmentService.create(request.toAppointment());
    }

    @PutMapping("/{id}")
    public Appointment update(@PathVariable String id, @RequestBody AppointmentRequest request) {
        return appointmentService.update(id, request.toAppointment());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        appointmentService.delete(id);
    }

    @PostMapping("/{id}/complete")
    public Appointment complete(@PathVariable String id) {
        return appointmentService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public Appointment cancel(@PathVariable String id) {
        return appointmentService.cancel(id);
    }

    public record AppointmentRequest(
            String petId,
            String vetName,
            LocalDateTime dateTime,
            int durationMinutes,
            String notes
    ) {
        Appointment toAppointment() {
            if (petId == null || petId.isBlank()) {
                throw new BadRequestException("Pet ID must not be empty");
            }
            UUID parsedPetId;
            try {
                parsedPetId = UUID.fromString(petId);
            } catch (IllegalArgumentException e) {
                throw new NotFoundException("Pet not found: " + petId);
            }
            Pet pet = new Pet();
            pet.setId(parsedPetId);
            Appointment appointment = new Appointment();
            appointment.setPet(pet);
            appointment.setVetName(vetName);
            appointment.setDateTime(dateTime);
            appointment.setDurationMinutes(durationMinutes);
            appointment.setNotes(notes);
            return appointment;
        }
    }
}
